/**
 * 
 */
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
import chord.ChordManager;
import chord.PeerI;
import database.Backup;
import database.Chunk;
import database.DBUtils;
import database.Stored;
import utils.Confidentiality;
import utils.Utils;

/**
 * @author anabela
 *
 */
public class ParseMessageAndSendResponse implements Runnable {

	private byte[] readData;
	private SSLSocket socket;
	private Peer peer;
	private Connection dbConnection;
	private String myPeerID;


	public ParseMessageAndSendResponse(Peer peer, byte[] readData, SSLSocket socket) {
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

	/**
	 * Parses the received request, processes it and returns the protocol response
	 * @param readData
	 * @return
	 */
	String parseMessage(byte[] readData) {
		String request = new String(readData,StandardCharsets.ISO_8859_1);
		Utils.LOGGER.finest("SSLServer: " + request);

		request = request.trim();
		String[] lines = request.split("\r\n");
		String[] firstLine = lines[0].split(" ");
		String[] secondLine = null;
		String thirdLine = null;//chunk body
		if (lines.length > 1) {
			secondLine = lines[1].split(" ");
		}
		if (lines.length > 2) {
			thirdLine = request.substring(request.indexOf("\r\n\r\n")+4, request.length());
		}
		String response = null;

		switch (MessageType.valueOf(firstLine[0])) {
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
				Utils.LOGGER.warning("Invalid lookup message");
			}
			break;
		case PING:
			response = MessageFactory.getHeader(MessageType.OK, "1.0", myPeerID);
			break;
		case NOTIFY:
			parseNotifyMsg(firstLine,secondLine);
			response = MessageFactory.getHeader(MessageType.OK, "1.0", myPeerID);
			break;
		case PUTCHUNK:
			parsePutChunkMsg(secondLine, thirdLine);
			break;
		case KEEPCHUNK:
			parseKeepChunkMsg(secondLine, thirdLine);
			break;
		case REBUILD:
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
			Utils.LOGGER.warning("Unexpected message received: " + request);
			break;
		}
		return response;
	}
	
	private void parseConfirmStored(String[] secondLine) {
		String fileId = secondLine[0];
		Integer chunkNo = Integer.parseInt(secondLine[1]);
		Integer repDegree = Integer.parseInt(secondLine[2]);
		Utils.LOGGER.info("Chunk " + fileId + "_" + chunkNo + ", saved with rep degree=" + repDegree);
		
	}

	private String parseResponsible(String[] lines) {
		Utils.LOGGER.info("Received Responsible");
		for(int i=1;i<lines.length-1;i++) {
			String[] currentLine = lines[i].split(" ");
			String fileID = currentLine[0];
			int degree = Integer.valueOf(currentLine[1]);
			Stored fileInfo = new Stored(fileID, true);
			fileInfo.setrepdegree(degree);
			DBUtils.insertStoredFile(dbConnection, fileInfo);
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
		String response = MessageFactory.getFirstLine(MessageType.PREDECESSOR, "1.0", myPeerID);
		return MessageFactory.appendLine(response, peer.getChordManager().getPredecessor().asArray());
	}

	private String parseChunkMsg(String[] secondLine, String body) {

		byte [] body_bytes = body.getBytes(StandardCharsets.ISO_8859_1);

		String file_id = secondLine[0].trim();
		int chunkNo = Integer.parseInt(secondLine[1]);

		Backup b = DBUtils.getBackupRequested(dbConnection, file_id);

		
		Confidentiality c = new Confidentiality(b.getkey());
		
		body_bytes = c.decrypt(body_bytes);
		Path filepath = Peer.getPath().resolve("restoreFile-" + b.getname());

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
				Utils.LOGGER.info("Finished writing!");
			}

			@Override
			public void failed(Throwable arg0, ByteBuffer arg1) {
				Utils.LOGGER.warning("Error: Could not write!");
			}

		};
		ByteBuffer src = ByteBuffer.allocate(body_bytes.length);
		src.put(body_bytes);
		src.flip();
		channel.write(src, chunkNo*Utils.MAX_LENGTH_CHUNK, src, writter);

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
		if(DBUtils.checkStoredChunk(dbConnection, chunkInfo )) { //Tenho o chunk
			String body = Utils.readFile(Peer.getPath().resolve(chunkInfo.getfile()).toString());
			String message = MessageFactory.getChunk(this.myPeerID, fileID, chunkNo, body.getBytes(StandardCharsets.ISO_8859_1));
			Client.sendMessage(addr, port, message, false);
		} else { //ReSend GETCHUNK to successor
			String message = MessageFactory.getGetChunk(this.myPeerID, addr, port, fileID, chunkNo);
			Client.sendMessage(this.peer.getChordManager().getSuccessor(0).getAddr(),
					this.peer.getChordManager().getSuccessor(0).getPort(), message, false);
		}
		return null;
	}


	private void deleteFile(String fileToDelete, int repDegree) {
		System.out.println("Received Delete for file: " + fileToDelete + ". Rep Degree: " + repDegree);
		boolean isFileStored = DBUtils.isFileStored(dbConnection, fileToDelete);
		if (isFileStored) {
			ArrayList<Chunk> allChunks = DBUtils.getAllChunksOfFile(dbConnection, fileToDelete);
			allChunks.forEach(chunk -> {
				Utils.deleteFile(Peer.getPath().resolve(chunk.getfile()));
				Peer.decreaseStorageUsed(chunk.getsize());
			});
			DBUtils.deleteFile(dbConnection, fileToDelete);
			repDegree--;
			Utils.LOGGER.info("Deleted file: " + fileToDelete);
		}
		
		if (repDegree > 0 || !isFileStored) {
			System.out.println("Forwarding delete to peer: " + peer.getChordManager().getSuccessor(0).getId());
			String message = MessageFactory.getDelete(myPeerID, fileToDelete, repDegree);
			PeerI successor = peer.getChordManager().getSuccessor(0);
			Client.sendMessage(successor.getAddr(), successor.getPort(), message, false);
			Utils.LOGGER.info("Forwarded delete: " + fileToDelete);
		}
		
	}
	
	private void parseDelete(String [] secondLine) {
		String fileToDelete = secondLine[0].trim();
		int repDegree = Integer.parseInt(secondLine[1]);
		if (DBUtils.amIResponsible(dbConnection, fileToDelete)) return;
		deleteFile(fileToDelete,repDegree);
	}
	private void parseInitDelete(String[] firstLine, String[] secondLine) {
		
		String fileToDelete = secondLine[0];
		int repDegree = DBUtils.getMaxRepDegree(dbConnection, fileToDelete);
		deleteFile(fileToDelete,repDegree);
	}


	private String parseStoredMsg(String[] lines) {
		Utils.LOGGER.info("Stored Received");
		String fileID = lines[0];
		Integer chunkNo = Integer.valueOf(lines[1]);
		Integer repDegree = Integer.valueOf(lines[2]);
		
		boolean iAmResponsible = DBUtils.amIResponsible(dbConnection, fileID);
		Chunk chunkInfo = new Chunk(chunkNo,fileID);
		boolean chunkExists = DBUtils.checkStoredChunk(dbConnection, chunkInfo);
		
		if(iAmResponsible) {
			
			PeerI peerWhichRequested = DBUtils.getPeerWhichRequestedBackup(dbConnection, fileID);
			if(chunkExists) { //Exists
				repDegree++; // I am also storing the chunk
				chunkInfo.setrepdegree(repDegree);
				DBUtils.updateStoredChunkRepDegree(dbConnection, chunkInfo);
			} else {
				chunkInfo.setrepdegree(repDegree);
				chunkInfo.setsize(-1);
				DBUtils.insertStoredChunk(dbConnection, chunkInfo); //size -1 means that I do not have stored the chunk
			}
			
			if(peerWhichRequested != null) {
				String message = MessageFactory.getConfirmStored(myPeerID, fileID, chunkNo, repDegree);
				Client.sendMessage(peerWhichRequested.getAddr(), peerWhichRequested.getPort(), message, false);
			} else {
				Utils.LOGGER.severe("ERROR: could not get peer which requested backup!");
			}
			return null;
		}
		if (chunkExists) {
			repDegree++;
		}
		AbstractPeer predecessor = peer.getChordManager().getPredecessor();
		if (predecessor.isNull()) {
			Utils.LOGGER.info("Null predecessor");
		}else {
			String message = MessageFactory.getStored(myPeerID, fileID, chunkNo, repDegree);
			Client.sendMessage(predecessor.getAddr(),predecessor.getPort(), message, false);
		}
		return null;
	}

	private void parsePutChunkMsg(String[] header, String body) {
		ChordManager chordManager = peer.getChordManager();
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

		Path filePath = Peer.getPath().resolve(fileID + "_" + chunkNo);

		PeerI peerThatRequestedBackup = new PeerI(id,addr,port);
		DBUtils.insertPeer(dbConnection, peerThatRequestedBackup);
		Stored fileInfo = new Stored(fileID, true);
		fileInfo.setpeer(peerThatRequestedBackup.getId());
		fileInfo.setrepdegree(replicationDegree);
		DBUtils.insertStoredFile(dbConnection, fileInfo);
		

		if(id.equals(myPeerID)) {//sou o dono do ficheiro que quero fazer backup...
			//nao faz senido guardarmos um ficheiro com o chunk, visto que guardamos o ficheiro
			//enviar o KEEPCHUNK
			DBUtils.setIamStoring(dbConnection, fileInfo.getfile(), false);
			PeerI nextPeer = chordManager.getSuccessor(0);
			String message = MessageFactory.getKeepChunk(id, addr, port, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(nextPeer.getAddr(),nextPeer.getPort(), message, false);
			return;
		}

		if(!Peer.capacityExceeded(body_bytes.length)) { //tem espaco para fazer o backup
			Utils.LOGGER.info("Writing/Saving chunk");
			try {
				Utils.writeToFile(filePath, body_bytes);
				DBUtils.insertStoredChunk(dbConnection, new Chunk(chunkNo,fileID, body_bytes.length));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(replicationDegree == 1) {//sou o ultimo a guardar
				//enviar STORE ao que pediu o backup
				String message = MessageFactory.getStored(chordManager.getPeerInfo().getId(), fileID, chunkNo, 1);
				Client.sendMessage(addr, port, message, false);
				return;
			} else {
				//enivar KEEPCHUNK para o sucessor
				String message = MessageFactory.getKeepChunk(id, addr, port, fileID, chunkNo, replicationDegree - 1, body_bytes);
				Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			}
		} else {
			//enviar KEEPCHUNK para o seu sucessor
			String message = MessageFactory.getKeepChunk(id, addr, port, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			Utils.LOGGER.warning("Capacity Exceeded");

		}
	}

	private void parseKeepChunkMsg(String[] header, String body) {
		ChordManager chordManager = peer.getChordManager();
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

		Path filePath = Peer.getPath().resolve(fileID + "_" + chunkNo);
		if(DBUtils.amIResponsible(dbConnection, fileID)) {//a mensagem ja deu uma volta completa. repDeg nao vai ser o desejado
			//enviar STORE para o predecessor
			Utils.LOGGER.info("KeepChunk: I am responsible ");
			PeerI predecessor = (PeerI) chordManager.getPredecessor();
			String message = MessageFactory.getStored(myPeerID, fileID, chunkNo, 0);
			Client.sendMessage(predecessor.getAddr(), predecessor.getPort(), message, false);
			return;
		}
		if(id_request.equals(myPeerID)) {//I AM ASKING FOR THE BACKUP sou dono do ficheiro
			Utils.LOGGER.info("I am responsible");
			//reencaminhar a mensagem para o proximo
			
			PeerI nextPeer = chordManager.getSuccessor(0);
			String message = MessageFactory.getKeepChunk(id_request, addr_request, port_request, fileID, chunkNo, replicationDegree, body_bytes);
			Client.sendMessage(nextPeer.getAddr(),nextPeer.getPort(), message, false);
			return;
		}

		if(!Peer.capacityExceeded(body_bytes.length)) { //tem espaco para fazer o backup
			DBUtils.insertStoredFile(dbConnection, new Stored(fileID, false));
			DBUtils.insertStoredChunk(dbConnection, new Chunk(chunkNo,fileID, body_bytes.length));
			try {
				Utils.writeToFile(filePath, body_bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(replicationDegree == 1) {//sou o ultimo a guardar
				//enviar STORE para o predecessor
				String message = MessageFactory.getStored(chordManager.getPeerInfo().getId(), fileID, chunkNo, 1);
				Client.sendMessage(chordManager.getPredecessor().getAddr(),chordManager.getPredecessor().getPort(), message, false);

			} else {
				//enivar KEEPCHUNK para o sucessor
				String message = MessageFactory.getKeepChunk(id_request, addr_request, port_request, fileID, chunkNo, replicationDegree - 1, body_bytes);
				Client.sendMessage(chordManager.getSuccessor(0).getAddr(),chordManager.getSuccessor(0).getPort(), message, false);
			}
			return;
		} else {
			Utils.LOGGER.warning("NAO ESPACO");
			//reencaminhar KEEPCHUNK para o seu sucessor
			String message = MessageFactory.getKeepChunk(id_request, addr_request, port_request, fileID, chunkNo, replicationDegree, body_bytes);
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
		if (previousPredecessor.isNull() || Utils.inBetween(previousPredecessor.getId(), myPeerID, potentialNewPredecessor.getId())) {
			PeerI newPredecessor = potentialNewPredecessor;
			Utils.LOGGER.info("Updated predecessor to " + newPredecessor.getId());
			this.peer.getChordManager().setPredecessor(newPredecessor);
			Utils.LOGGER.info("Updated predecessor, sending responsibility");
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
