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
import util.Utils;

public class ParseMessageAndSendResponse implements Runnable {

	private byte[] readData;
	private SSLSocket socket;
	private PeerMain peer;
	private Connection dbConnection;
	private String myPeerID;


	public ParseMessageAndSendResponse(PeerMain peer, byte[] readData, SSLSocket socket) {
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
			sendResponse(socket, response);
		}

	}

	String parseMessage(byte[] readData) {
		String request = new String(readData,StandardCharsets.ISO_8859_1);
		Utils.LOG.finest("SSLServer: " + request);

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
		case INITDELETE:
			parseInitDelete(firstLine, secondLine);
			break;
		case LOOKUP:
			if (secondLine != null) {
				response = peer.getChordManager().lookup(secondLine[0]);
			}else {
				Utils.LOG.warning("Invalid lookup message");
			}
			break;
		case PING:
			response = MsgFactory.getHeader(MsgType.OK, "1.0", myPeerID);
			break;
		case NOTIFY:
			parseNotifyMsg(firstLine,secondLine);
			response = MsgFactory.getHeader(MsgType.OK, "1.0", myPeerID);
			break;
		case PUTCHUNK:
			parsePutChunkMsg(secondLine, thirdLine);
			break;
		case KEEPCHUNK:
			parseKeepChunkMsg(secondLine, thirdLine);
			break;
		case STABILIZE:
			response = parseStabilize(firstLine);
			break;
		case STORED:
			response = parseStoredMsg(secondLine);
			break;
		case GETCHUNK: 
			response = parseGetChunkMsg(secondLine);
			break;
		case CHUNK:
			response = parseChunkMsg(secondLine,thirdLine);
			break;
		case RESPONSIBLE:
			response = parseResponsible(secondLine);
			break;
		case CONFIRMSTORED:
			parseConfirmStored(secondLine);
		default:
			Utils.LOG.warning("Unexpected message received: " + request);
			break;
		}
		return response;
	}
	
	private void parseConfirmStored(String[] secondLine) {
		String fileId = secondLine[0];
		Integer chunkNo = Integer.parseInt(secondLine[1]);
		Integer repDegree = Integer.parseInt(secondLine[2]);
		Utils.LOG.info("Chunk " + fileId + "_" + chunkNo + ", saved with rep degree=" + repDegree);
		
	}

	private String parseResponsible(String[] lines) {
		Utils.LOG.info("Received Responsible");
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
		String response = MsgFactory.getFirstLine(MsgType.PREDECESSOR, "1.0", myPeerID);
		return MsgFactory.appendLine(response, peer.getChordManager().getPredecessor().asArray());
	}

	private String parseChunkMsg(String[] secondLine, String body) {

		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String file_id = secondLine[0].trim();
		int chunkNo = Integer.parseInt(secondLine[1]);

		Backup b = DatabaseManager.getBackup(dbConnection, file_id);

		
		Confidential conf = new Confidential(b.getkey());
		
		body_bytes = conf.decrypt(body_bytes);
		Path filepath = PeerMain.getPath().resolve("restoreFile-" + b.getname());

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
				Utils.LOG.info("Finished writing!");
			}

			@Override
			public void failed(Throwable arg0, ByteBuffer arg1) {
				Utils.LOG.warning("Error: Could not write!");
			}

		};
		ByteBuffer src = ByteBuffer.allocate(body_bytes.length);
		src.put(body_bytes);
		src.flip();
		channel.write(src, chunkNo*Utils.MAX_CHUNK_SIZE, src, writter);

		return null;
	}

	private String parseGetChunkMsg(String[] secondLine) {
		InetAddress addr;
		try {
			addr = InetAddress.getByName(secondLine[0]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
		Integer port = Integer.valueOf(secondLine[1]);
		String fileID = secondLine[2];
		Integer chunkNo = Integer.valueOf(secondLine[3]);

		Chunk chunkInfo = new Chunk(chunkNo, fileID);
		if(DatabaseManager.checkChunkStored(dbConnection, chunkInfo )) {
			String body = Utils.read(PeerMain.getPath().resolve(chunkInfo.getfile()).toString());
			String message = MsgFactory.getChunk(this.myPeerID, fileID, chunkNo, body.getBytes(StandardCharsets.ISO_8859_1));
			Client.sendMessage(addr, port, message, false);
		} else {
			String message = MsgFactory.getGetChunk(this.myPeerID, addr, port, fileID, chunkNo);
			Client.sendMessage(this.peer.getChordManager().getSuccessor(0).getAddr(),
					this.peer.getChordManager().getSuccessor(0).getPort(), message, false);
		}
		return null;
	}


	private void delete(String fileToDelete, int repDegree) {
		System.out.println("Received Delete for file: " + fileToDelete + ". Rep Degree: " + repDegree);
		boolean isFileStored = DatabaseManager.checkFile(dbConnection, fileToDelete);
		if (isFileStored) {
			ArrayList<Chunk> allChunks = DatabaseManager.getFileChunks(dbConnection, fileToDelete);
			allChunks.forEach(chunk -> {
				Utils.delete(PeerMain.getPath().resolve(chunk.getfile()));
				PeerMain.decreaseStorageUsed(chunk.getsize());
			});
			DatabaseManager.deleteFile(dbConnection, fileToDelete);
			repDegree--;
			Utils.LOG.info("Deleted file: " + fileToDelete);
		}
		
		if (repDegree > 0 || !isFileStored) {
			System.out.println("Forwarding delete to peer: " + peer.getChordManager().getSuccessor(0).getId());
			String message = MsgFactory.getDelete(myPeerID, fileToDelete, repDegree);
			PeerI successor = peer.getChordManager().getSuccessor(0);
			Client.sendMessage(successor.getAddr(), successor.getPort(), message, false);
			Utils.LOG.info("Forwarded delete: " + fileToDelete);
		}
		
	}
	
	private void parseDelete(String [] secondLine) {
		String fileToDelete = secondLine[0].trim();
		int repDegree = Integer.parseInt(secondLine[1]);
		if (DatabaseManager.checkResponsible(dbConnection, fileToDelete)) return;
		delete(fileToDelete,repDegree);
	}
	private void parseInitDelete(String[] firstLine, String[] secondLine) {
		
		String fileToDelete = secondLine[0];
		int repDegree = DatabaseManager.getMaxRepDegree(dbConnection, fileToDelete);
		delete(fileToDelete,repDegree);
	}


	private String parseStoredMsg(String[] lines) {
		Utils.LOG.info("Stored Received");
		String fileID = lines[0];
		Integer chunkNo = Integer.valueOf(lines[1]);
		Integer repDegree = Integer.valueOf(lines[2]);
		
		boolean iAmResponsible = DatabaseManager.checkResponsible(dbConnection, fileID);
		Chunk chunkInfo = new Chunk(chunkNo,fileID);
		boolean chunkExists = DatabaseManager.checkChunkStored(dbConnection, chunkInfo);
		
		if(iAmResponsible) {
			
			PeerI peerWhichRequested = DatabaseManager.getRequestingPeer(dbConnection, fileID);
			if(chunkExists) {
				repDegree++;
				chunkInfo.setrepdegree(repDegree);
				DatabaseManager.updateRepDeg(dbConnection, chunkInfo);
			} else {
				chunkInfo.setrepdegree(repDegree);
				chunkInfo.setsize(-1);
				DatabaseManager.storeChunk(dbConnection, chunkInfo);
			}
			
			if(peerWhichRequested != null) {
				String message = MsgFactory.getConfirmStored(myPeerID, fileID, chunkNo, repDegree);
				Client.sendMessage(peerWhichRequested.getAddr(), peerWhichRequested.getPort(), message, false);
			} else {
				Utils.LOG.severe("ERROR: could not get peer which requested backup!");
			}
			return null;
		}
		if (chunkExists) {
			repDegree++;
		}
		AbstractPeer predecessor = peer.getChordManager().getPredecessor();
		if (predecessor.isNull()) {
			Utils.LOG.info("Null predecessor");
		}else {
			String message = MsgFactory.getStored(myPeerID, fileID, chunkNo, repDegree);
			Client.sendMessage(predecessor.getAddr(),predecessor.getPort(), message, false);
		}
		return null;
	}

	private void parsePutChunkMsg(String[] header, String body) {
		ManageChord chordManager = peer.getChordManager();
		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String id = header[0].trim();
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(header[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int port = Integer.parseInt(header[2].trim());

		String fileID = header[3];
		int chunkNo = Integer.parseInt(header[4]);
		int replicationDegree = Integer.parseInt(header[5]);

		Path filePath = PeerMain.getPath().resolve(fileID + "_" + chunkNo);

		PeerI peerThatRequestedBackup = new PeerI(id,addr,port);
		DatabaseManager.storePeer(dbConnection, peerThatRequestedBackup);
		Stored fileInfo = new Stored(fileID, true);
		fileInfo.setpeer(peerThatRequestedBackup.getId());
		fileInfo.setrepdegree(replicationDegree);
		DatabaseManager.storeFile(dbConnection, fileInfo);
		

		if(id.equals(myPeerID)) {
			DatabaseManager.setStoring(dbConnection, fileInfo.getfile(), false);
			PeerI nextPeer = chordManager.getSuccessor(0);
			String message = MsgFactory.getKeepChunk(id, addr, port, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(nextPeer.getAddr(),nextPeer.getPort(), message, false);
			return;
		}

		if(!PeerMain.capacityExceeded(body_bytes.length)) {
			Utils.LOG.info("Writing/Saving chunk");
			try {
				Utils.write(filePath, body_bytes);
				DatabaseManager.storeChunk(dbConnection, new Chunk(chunkNo,fileID, body_bytes.length));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(replicationDegree == 1) {
				String message = MsgFactory.getStored(chordManager.getPeerInfo().getId(), fileID, chunkNo, 1);
				Client.sendMessage(addr, port, message, false);
				return;
			} else {
				String message = MsgFactory.getKeepChunk(id, addr, port, fileID, chunkNo, replicationDegree - 1, body_bytes);
				Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			}
		} else {
			String message = MsgFactory.getKeepChunk(id, addr, port, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			Utils.LOG.warning("Capacity Exceeded");

		}
	}

	private void parseKeepChunkMsg(String[] header, String body) {
		ManageChord chordManager = peer.getChordManager();
		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String id_request = header[0].trim();
		InetAddress addr_request = null;
		try {
			addr_request = InetAddress.getByName(header[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int port_request = Integer.parseInt(header[2].trim());

		String fileID = header[3];
		int chunkNo = Integer.parseInt(header[4]);
		int replicationDegree = Integer.parseInt(header[5]);

		Path filePath = PeerMain.getPath().resolve(fileID + "_" + chunkNo);
		if(DatabaseManager.checkResponsible(dbConnection, fileID)) {
			Utils.LOG.info("KeepChunk: I am responsible ");
			PeerI predecessor = (PeerI) chordManager.getPredecessor();
			String message = MsgFactory.getStored(myPeerID, fileID, chunkNo, 0);
			Client.sendMessage(predecessor.getAddr(), predecessor.getPort(), message, false);
			return;
		}
		if(id_request.equals(myPeerID)) {
			Utils.LOG.info("I am responsible");
			
			PeerI nextPeer = chordManager.getSuccessor(0);
			String message = MsgFactory.getKeepChunk(id_request, addr_request, port_request, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(nextPeer.getAddr(),nextPeer.getPort(), message, false);
			return;
		}

		if(!PeerMain.capacityExceeded(body_bytes.length)) {
			DatabaseManager.storeFile(dbConnection, new Stored(fileID, false));
			DatabaseManager.storeChunk(dbConnection, new Chunk(chunkNo,fileID, body_bytes.length));
			try {
				Utils.write(filePath, body_bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(replicationDegree == 1) {
				String message = MsgFactory.getStored(chordManager.getPeerInfo().getId(), fileID, chunkNo, 1);
				Client.sendMessage(chordManager.getPredecessor().getAddr(),chordManager.getPredecessor().getPort(), message, false);

			} else {
				String message = MsgFactory.getKeepChunk(id_request, addr_request, port_request, fileID, chunkNo, replicationDegree - 1, body_bytes);
				Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			}
			return;
		} else {
			Utils.LOG.warning("NAO ESPACO");
			String message = MsgFactory.getKeepChunk(id_request, addr_request, port_request, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			return;
		}
	}


	private void parseNotifyMsg(String[] firstLine, String[] secondLine) {
		String id = firstLine[2];
		InetAddress addr = socket.getInetAddress();
		int port = Integer.parseInt(secondLine[0].trim());
		PeerI potentialNewPredecessor = new PeerI(id, addr, port);
		if (potentialNewPredecessor.getId() == myPeerID) return;
		AbstractPeer previousPredecessor = peer.getChordManager().getPredecessor();
		if (previousPredecessor.isNull() || Utils.inTheMiddle(previousPredecessor.getId(), myPeerID, potentialNewPredecessor.getId())) {
			PeerI newPredecessor = potentialNewPredecessor;
			Utils.LOG.info("Updated predecessor to " + newPredecessor.getId());
			this.peer.getChordManager().setPredecessor(newPredecessor);
			Utils.LOG.info("Updated predecessor, sending responsibility");
			peer.sendResponsability();
		}
	}

	void sendResponse(SSLSocket socket, String response) {
		OutputStream sendStream;
		try {
			sendStream = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		byte[] sendData = response.getBytes(StandardCharsets.ISO_8859_1);
		try {
			sendStream.write(sendData);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

}
