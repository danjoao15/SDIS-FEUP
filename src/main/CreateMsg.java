package main;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import chordSetup.Peer;
import database.Stored;

public class CreateMsg {

	private static String ENDHEADER = "\r\n\r\n";
	private static String NEWLINE = "\r\n";
	
	
	public static String getFirstLine(MsgType messageType, String version, String id) {
		return messageType.getType() + " " + version + " " + id + " " + NEWLINE;
	}
	
	public static String getHeader(MsgType messageType, String version, String senderId) {
		return getFirstLine(messageType,version,senderId) + NEWLINE;
	}
	
	
	
	public static String appendLine(String message, Object args[]) {
		for (Object arg: args) {
			message += arg.toString() + " ";
		}
		message += ENDHEADER;
		return message;
	}
	public static String appendBody(String message, byte [] body) throws UnsupportedEncodingException {
		String bodyStr = new String(body, StandardCharsets.ISO_8859_1);
		message += bodyStr;
		return message;
	}
	
	
	public static String getNotify(String senderId,Integer senderListeningPort) {
		String message = CreateMsg.getFirstLine(MsgType.NOTIFY, "1.0", senderId);
		return CreateMsg.appendLine(message, new String[] {"" + senderListeningPort});
	}
	
	public static String getLookup(String senderId, String key) {
		String msg = getFirstLine(MsgType.LOOKUP,"1.0",senderId);
		return appendLine(msg, new String[] {""+key});
	}
	
	
	public static String getResponsible(String string, ArrayList<Stored> toSend) {
		String msg = getFirstLine(MsgType.RESPONSIBLE,"1.0",string);
		for(int i = 0; i < toSend.size(); i++) {
			msg += appendLine(msg, new Object[] {toSend.get(i).getfile(),toSend.get(i).getrepdegree()});
		}
		return msg;
	}
	
	public static String getSuccessors(String senderId, List<Peer> list) {
		String msg = getFirstLine(MsgType.SUCCESSORS,"2.0",senderId);
		Object[] objectArray = new Object[list.size() * 3];
		for (int i = 0; i < list.size(); i++) {
			Peer nextPeer = list.get(i);
			objectArray[i*3] = nextPeer.getId();
			objectArray[i*3 + 1] = nextPeer.getAddress().getHostAddress();
			objectArray[i*3 + 2] = nextPeer.getPort();
		}
		return appendLine(msg, objectArray);
	}
	
	public static String getSuccessor(String senderId, Peer peer) {
		String msg = getFirstLine(MsgType.SUCCESSOR,"1.0",senderId);
		return appendLine(msg, new Object[] {peer.getId(),peer.getAddress().getHostAddress(),peer.getPort()});
	}
	
	
	public static String getPredecessor(String IDsender, Peer peer) {
		String msg = getFirstLine(MsgType.PREDECESSOR,"1.0",IDsender);
		return appendLine(msg, new Object[] {peer.getId(),peer.getAddress().getHostAddress(),peer.getPort()});
	}
	public static String getAsk(String IDsender, Peer peer) {
		String msg = getFirstLine(MsgType.ASK,"1.0",IDsender);
		return appendLine(msg, new Object[] {peer.getId(),peer.getAddress().getHostAddress(),peer.getPort()});
	}
	public static String getPutChunk(String id, InetAddress address, int port, String IDfile, int nChunk, int repDeg, byte[] body) {
		String msg = getFirstLine(MsgType.PUTCHUNK,"1.0",id);
		String msg2 = appendLine(msg, new Object[] {id, address.getHostAddress(), port, IDfile, nChunk,
				repDeg});
		try {
			return appendBody(msg2, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			
			return null;
		}
	}
	
	public static String getKeepChunk(String IDsender, InetAddress address, int port, String IDfile, int nChunk, int repDeg, byte[] body) {
		String msg = getFirstLine(MsgType.KEEPCHUNK,"1.0",IDsender);
		String msg2 = appendLine(msg, new Object[] {IDsender, address.getHostAddress(), port, IDfile, nChunk, repDeg});
		try {
			return appendBody(msg2, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String getStored(String IDsender, String IDfile, int nChunk, int repDeg) {
		String msg = getFirstLine(MsgType.STORED,"1.0",IDsender);
		String msg2 = appendLine(msg, new Object[] {IDfile, nChunk, repDeg});
		return msg2;
	}
	public static String getConfirmStored(String IDsender, String IDfile, int nChunk, int repDeg) {
		String msg = getFirstLine(MsgType.CONFIRMSTORED,"1.0",IDsender);
		String msg2 = appendLine(msg, new Object[] {IDfile, nChunk, repDeg});
		return msg2;
	}
	public static String getGetChunk(String IDsender, InetAddress address, int port, String IDfile, int nChunk) {
		String msg = getFirstLine(MsgType.GETCHUNK,"1.0",IDsender);
		String msg2 = appendLine(msg, new Object[] {address.getHostAddress(), port, IDfile, nChunk});
		return msg2;
	}
	public static String getChunk(String IDsender, String fileID, int nChunk, byte[] body) {
		String msg = getFirstLine(MsgType.CHUNK,"1.0",IDsender);
		String msg2 = appendLine(msg, new Object[] {fileID, nChunk});
		
		
		try {
			return appendBody(msg2, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getPing(String senderId) {
		return getHeader(MsgType.PING,"1.0",senderId);
	}

	public static String getUpdateTime(String senderId, String fileID) {
		String msg = getFirstLine(MsgType.UPDATETIME, "1.0",senderId);
		return appendLine(msg, new Object[] {fileID});
	}
}
