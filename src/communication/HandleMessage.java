package communication;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import javax.net.ssl.SSLSocket;

import chord.AbstractPeer;
import chord.ManageChord;
import chord.PeerI;
import database.Backup;
import database.Chunk;
import database.DatabaseManager;
import database.Stored;
import util.Confidential;
import util.Loggs;

public class HandleMessage implements Runnable {

	private byte[] readData;
	private SSLSocket socket;
	private PeerMain peer;
	private Connection dbConnection;
	private String myPeerID;


	public HandleMessage(PeerMain peer, byte[] readData, SSLSocket socket) {
		super();
		this.readData = readData;
		this.socket = socket;
		this.peer = peer;
		this.dbConnection = peer.getChordManager().getDatabase().getConnection();
		this.myPeerID = peer.getChordManager().getPeerInfo().getId();
	}

	@Override
	public void run() {
		String response = parseMessage(readData);
		if (response != null) {
			sendRespo(socket, response);
		}

	}

	String parseMessage(byte[] readData) {
		String request = new String(readData,StandardCharsets.ISO_8859_1);
		Loggs.LOG.finest("SSLServer: " + request);

		request = request.trim();
		String[] lines = request.split("\r\n");
		String[] firstLine = lines[0].split(" ");
		String[] secondLine = null;
		String thirdLine = null;
		if (lines.length > 1) {
			secondLine = lines[1].split(" ");
		}
		if (lines.length > 2) {
			thirdLine = request.substring(request.indexOf("\r\n\r\n")+4, request.length());
		}
		String response = null;

		switch (MsgType.valueOf(firstLine[0])) {
		case SUCCESSORS:
			parseSuccessors(secondLine);
			break;
		case DELETE:
			parseDelete(secondLine);
			break;
		case IDELETE:
			parseIDelete(firstLine, secondLine);
			break;
		case LOOKUP:
			if (secondLine != null) {
				response = peer.getChordManager().lookup(secondLine[0]);
			}else {
				Loggs.LOG.warning("Invalid lookup message");
			}
			break;
		case PING:
			response = CreateMsg.getHeader(MsgType.OK, "1.0", myPeerID);
			break;
		case NOTIFY:
			parseNotify(firstLine,secondLine);
			response = CreateMsg.getHeader(MsgType.OK, "1.0", myPeerID);
			break;
		case PUTCHUNK:
			parsePutChunk(secondLine, thirdLine);
			break;
		case KEEPCHUNK:
			parseKeepChunk(secondLine, thirdLine);
			break;
		case STABILIZE:
			response = parseStabilize(firstLine);
			break;
		case STORED:
			response = parseStored(secondLine);
			break;
		case GETCHUNK: 
			response = parseGetChunk(secondLine);
			break;
		case CHUNK:
			response = parseChunk(secondLine,thirdLine);
			break;
		case RESPONSIBLE:
			response = parseResponsible(secondLine);
			break;
		case CONFIRMSTORED:
			parseConfirmStored(secondLine);
		default:
			Loggs.LOG.warning("Unexpected message received: " + request);
			break;
		}
		return response;
	}
	
	private void parseConfirmStored(String[] secondLine) {
		String fileId = secondLine[0];
		Integer chunkNo = Integer.parseInt(secondLine[1]);
		Integer repDegree = Integer.parseInt(secondLine[2]);
		Loggs.LOG.info("Chunk " + fileId + "_" + chunkNo + ", saved with replication degree=" + repDegree);
		
	}

	private String parseResponsible(String[] lines) {
		Loggs.LOG.info("Received Responsible");
		for(int i=1;i<lines.length-1;i++) {
			String[] currentLine = lines[i].split(" ");
			String fileID = currentLine[0];
			int degree = Integer.valueOf(currentLine[1]);
			Stored fileInfo = new Stored(fileID, true);
			fileInfo.setrepdegree(degree);
			DatabaseManager.storeFile(dbConnection, fileInfo);
		}
		return null;
	}

	private void parseSuccessors(String[] secondLine) {
		Deque<PeerI> peersReceived = new ArrayDeque<PeerI>();
		int numberOfSuccessorsReceived = secondLine.length / 3;
		for (int i = 0; i < numberOfSuccessorsReceived; i++) {
			String peerId = secondLine[i*3];
			InetAddress peerAddr = null;
			try {
				peerAddr = InetAddress.getByName(secondLine[i*3+1]);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			Integer port = Integer.parseInt(secondLine[i*3+2]);
			peersReceived.add(new PeerI(peerId,peerAddr,port));
		}
		peer.getChordManager().updateNextPeers(peersReceived);
	}

	private String parseStabilize(String[] firstLine) {
		String response = CreateMsg.getFirstLine(MsgType.PREDECESSOR, "1.0", myPeerID);
		return CreateMsg.appendLine(response, peer.getChordManager().getPredecessor().asArray());
	}

	private String parseChunk(String[] secondLine, String body) {

		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String file_id = secondLine[0].trim();
		int chunkNo = Integer.parseInt(secondLine[1]);

		Backup b = DatabaseManager.getBackup(dbConnection, file_id);

		
		Confidential conf = new Confidential(b.getkey());
		
		body_bytes = conf.decryptation(body_bytes);
		Path filepath = PeerMain.getPath().resolve("Restore File-" + b.getname());

		try {
			Files.createFile(filepath);
		} catch(FileAlreadyExistsException e) {
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}


		AsynchronousFileChannel channel;
		try {
			channel = AsynchronousFileChannel.open(filepath,StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		CompletionHandler<Integer, ByteBuffer> writter = new CompletionHandler<Integer, ByteBuffer>() {
			@Override
			public void completed(Integer result, ByteBuffer buffer) {
				Loggs.LOG.info("Finished writing!");
			}

			@Override
			public void failed(Throwable arg0, ByteBuffer arg1) {
				Loggs.LOG.warning("Could not write!");
			}

		};
		ByteBuffer src = ByteBuffer.allocate(body_bytes.length);
		src.put(body_bytes);
		src.flip();
		channel.write(src, chunkNo*Loggs.MAX_CHUNK_SIZE, src, writter);

		return null;
	}

	private String parseGetChunk(String[] secondLine) {
		InetAddress address;
		try {
			address = InetAddress.getByName(secondLine[0]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
		Integer port = Integer.valueOf(secondLine[1]);
		String fileID = secondLine[2];
		Integer chunkNo = Integer.valueOf(secondLine[3]);

		Chunk chunkInfo = new Chunk(chunkNo, fileID);
		if(DatabaseManager.checkChunkStored(dbConnection, chunkInfo )) {
			String body = Loggs.read(PeerMain.getPath().resolve(chunkInfo.getfile()).toString());
			String msg = CreateMsg.getChunk(this.myPeerID, fileID, chunkNo, body.getBytes(StandardCharsets.ISO_8859_1));
			Client.sendMsg(address, port, msg, false);
		} else {
			String msg = CreateMsg.getGetChunk(this.myPeerID, address, port, fileID, chunkNo);
			Client.sendMsg(this.peer.getChordManager().getSuccessor(0).getAddress(),
					this.peer.getChordManager().getSuccessor(0).getPort(), msg, false);
		}
		return null;
	}


	private void delete(String file, int repDeg) {
		System.out.println("Delete file: " + file + ". Replication Degree: " + repDeg);
		boolean isFileStored = DatabaseManager.checkFile(dbConnection, file);
		if (isFileStored) {
			ArrayList<Chunk> chunks = DatabaseManager.getFileChunks(dbConnection, file);
			chunks.forEach(chunk -> {
				Loggs.delete(PeerMain.getPath().resolve(chunk.getfile()));
				PeerMain.decreaseStorageUsed(chunk.getsize());
			});
			DatabaseManager.deleteFile(dbConnection, file);
			repDeg--;
			Loggs.LOG.info("Deleted file: " + file);
		}
		
		if (repDeg > 0 || !isFileStored) {
			System.out.println("Deleting from peer: " + peer.getChordManager().getSuccessor(0).getId());
			String message = CreateMsg.getDelete(myPeerID, file, repDeg);
			PeerI successor = peer.getChordManager().getSuccessor(0);
			Client.sendMsg(successor.getAddress(), successor.getPort(), message, false);
			Loggs.LOG.info("Forwarded delete: " + file);
		}
		
	}
	
	private void parseDelete(String [] secondLine) {
		String fileToDelete = secondLine[0].trim();
		int repDegree = Integer.parseInt(secondLine[1]);
		if (DatabaseManager.checkResponsible(dbConnection, fileToDelete)) return;
		delete(fileToDelete,repDegree);
	}
	private void parseIDelete(String[] firstLine, String[] secondLine) {
		
		String fileToDelete = secondLine[0];
		int repDegree = DatabaseManager.getMaxRepDegree(dbConnection, fileToDelete);
		delete(fileToDelete,repDegree);
	}


	private String parseStored(String[] lines) {
		Loggs.LOG.info("Stored Received");
		String fileID = lines[0];
		Integer chunkNo = Integer.valueOf(lines[1]);
		Integer repDeg = Integer.valueOf(lines[2]);
		
		boolean Responsible = DatabaseManager.checkResponsible(dbConnection, fileID);
		Chunk chunkInfo = new Chunk(chunkNo,fileID);
		boolean chunkExist = DatabaseManager.checkChunkStored(dbConnection, chunkInfo);
		
		if(Responsible) {
			
			PeerI peerRequested = DatabaseManager.getRequestingPeer(dbConnection, fileID);
			if(chunkExist) {
				repDeg++;
				chunkInfo.setrepdegree(repDeg);
				DatabaseManager.updateRepDeg(dbConnection, chunkInfo);
			} else {
				chunkInfo.setrepdegree(repDeg);
				chunkInfo.setsize(-1);
				DatabaseManager.storeChunk(dbConnection, chunkInfo);
			}
			
			if(peerRequested != null) {
				String msg = CreateMsg.getConfirmStored(myPeerID, fileID, chunkNo, repDeg);
				Client.sendMsg(peerRequested.getAddress(), peerRequested.getPort(), msg, false);
			} else {
				Loggs.LOG.severe("Could not get peer which requested backup!");
			}
			return null;
		}
		if (chunkExist) {
			repDeg++;
		}
		AbstractPeer predecessor = peer.getChordManager().getPredecessor();
		if (predecessor.isNull()) {
			Loggs.LOG.info("There's no predecessor");
		}else {
			String msg = CreateMsg.getStored(myPeerID, fileID, chunkNo, repDeg);
			Client.sendMsg(predecessor.getAddress(),predecessor.getPort(), msg, false);
		}
		return null;
	}

	private void parsePutChunk(String[] header, String body) {
		ManageChord chordManager = peer.getChordManager();
		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String id = header[0].trim();
		InetAddress address = null;
		try {
			address = InetAddress.getByName(header[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int port = Integer.parseInt(header[2].trim());

		String IDfile = header[3];
		int nChunk = Integer.parseInt(header[4]);
		int repDeg = Integer.parseInt(header[5]);

		Path path = PeerMain.getPath().resolve(IDfile + "_" + nChunk);

		PeerI peerRequesting = new PeerI(id,address,port);
		DatabaseManager.storePeer(dbConnection, peerRequesting);
		Stored fileInfo = new Stored(IDfile, true);
		fileInfo.setpeer(peerRequesting.getId());
		fileInfo.setrepdegree(repDeg);
		DatabaseManager.storeFile(dbConnection, fileInfo);
		

		if(id.equals(myPeerID)) {
			DatabaseManager.setStoring(dbConnection, fileInfo.getfile(), false);
			PeerI nextPeer = chordManager.getSuccessor(0);
			String msg = CreateMsg.getKeepChunk(id, address, port, IDfile, nChunk, repDeg, body_bytes);
			Client.sendMsg(nextPeer.getAddress(),nextPeer.getPort(), msg, false);
			return;
		}

		if(!PeerMain.capacityExceeded(body_bytes.length)) {
			Loggs.LOG.info("Writing/Saving chunk");
			try {
				Loggs.write(path, body_bytes);
				DatabaseManager.storeChunk(dbConnection, new Chunk(nChunk,IDfile, body_bytes.length));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(repDeg == 1) {
				String msg = CreateMsg.getStored(chordManager.getPeerInfo().getId(), IDfile, nChunk, 1);
				Client.sendMsg(address, port, msg, false);
				return;
			} else {
				String msg = CreateMsg.getKeepChunk(id, address, port, IDfile, nChunk, repDeg - 1, body_bytes);
				Client.sendMsg(chordManager.getSuccessor(0).getAddress(),chordManager.getSuccessor(0).getPort(), msg, false);
			}
		} else {
			String msg = CreateMsg.getKeepChunk(id, address, port, IDfile, nChunk, repDeg, body_bytes);
			Client.sendMsg(chordManager.getSuccessor(0).getAddress(),chordManager.getSuccessor(0).getPort(), msg, false);
			Loggs.LOG.warning("Capacity Exceeded");

		}
	}

	private void parseKeepChunk(String[] header, String body) {
		ManageChord chordManager = peer.getChordManager();
		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String id_request = header[0].trim();
		InetAddress address_request = null;
		try {
			address_request = InetAddress.getByName(header[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int port_request = Integer.parseInt(header[2].trim());

		String IDfile = header[3];
		int nChunk = Integer.parseInt(header[4]);
		int repDeg = Integer.parseInt(header[5]);

		Path path = PeerMain.getPath().resolve(IDfile + "_" + nChunk);
		if(DatabaseManager.checkResponsible(dbConnection, IDfile)) {
			Loggs.LOG.info("KeepChunk: I am responsible ");
			PeerI predecessor = (PeerI) chordManager.getPredecessor();
			String msg = CreateMsg.getStored(myPeerID, IDfile, nChunk, 0);
			Client.sendMsg(predecessor.getAddress(), predecessor.getPort(), msg, false);
			return;
		}
		if(id_request.equals(myPeerID)) {
			Loggs.LOG.info("I am responsible");
			
			PeerI nextPeer = chordManager.getSuccessor(0);
			String message = CreateMsg.getKeepChunk(id_request, address_request, port_request, IDfile, nChunk, repDeg, body_bytes);
			Client.sendMsg(nextPeer.getAddress(),nextPeer.getPort(), message, false);
			return;
		}

		if(!PeerMain.capacityExceeded(body_bytes.length)) {
			DatabaseManager.storeFile(dbConnection, new Stored(IDfile, false));
			DatabaseManager.storeChunk(dbConnection, new Chunk(nChunk,IDfile, body_bytes.length));
			try {
				Loggs.write(path, body_bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(repDeg == 1) {
				String msg = CreateMsg.getStored(chordManager.getPeerInfo().getId(), IDfile, nChunk, 1);
				Client.sendMsg(chordManager.getPredecessor().getAddress(),chordManager.getPredecessor().getPort(), msg, false);

			} else {
				String msg = CreateMsg.getKeepChunk(id_request, address_request, port_request, IDfile, nChunk, repDeg - 1, body_bytes);
				Client.sendMsg(chordManager.getSuccessor(0).getAddress(),chordManager.getSuccessor(0).getPort(), msg, false);
			}
			return;
		} else {
			Loggs.LOG.warning("No Space");
			String msg = CreateMsg.getKeepChunk(id_request, address_request, port_request, IDfile, nChunk, repDeg, body_bytes);
			Client.sendMsg(chordManager.getSuccessor(0).getAddress(),chordManager.getSuccessor(0).getPort(), msg, false);
			return;
		}
	}


	private void parseNotify(String[] firstLine, String[] secondLine) {
		String id = firstLine[2];
		InetAddress address = socket.getInetAddress();
		int port = Integer.parseInt(secondLine[0].trim());
		PeerI potentialPredecessor = new PeerI(id, address, port);
		if (potentialPredecessor.getId() == myPeerID) return;
		AbstractPeer previousPredecessor = peer.getChordManager().getPredecessor();
		if (previousPredecessor.isNull() || Loggs.inTheMiddle(previousPredecessor.getId(), myPeerID, potentialPredecessor.getId())) {
			PeerI newPredecessor = potentialPredecessor;
			Loggs.LOG.info("Update predecessor to " + newPredecessor.getId());
			this.peer.getChordManager().setPredecessor(newPredecessor);
			Loggs.LOG.info("Update predecessor, sending responsibility");
			peer.sendResponsability();
		}
	}

	void sendRespo(SSLSocket socket, String response) {
		OutputStream sendStream;
		try {
			sendStream = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		byte[] sendInfo = response.getBytes(StandardCharsets.ISO_8859_1);
		try {
			sendStream.write(sendInfo);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

}
