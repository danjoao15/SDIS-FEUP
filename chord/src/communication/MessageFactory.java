package communication;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import chord.PeerI;
import database.FileStoredInfo;

public class MessageFactory {

	private static String END_HEADER = "\r\n\r\n";
	private static String NEW_LINE = "\r\n";
	
	public static String getFirstLine(MessageType messageType, String version, String id) {
		return messageType.getType() + " " + version + " " + id + " " + NEW_LINE;
	}
	
	public static String getHeader(MessageType messageType, String version, String senderId) {
		return getFirstLine(messageType,version,senderId) + NEW_LINE;
	}
	
	public static String appendLine(String message, Object args[]) {
		for (Object arg: args) {
			message += arg.toString() + " ";
		}
		message += END_HEADER;
		return message;
	}
	public static String appendBody(String message, byte [] body) throws UnsupportedEncodingException {
		String bodyStr = new String(body, StandardCharsets.ISO_8859_1);
		message += bodyStr;
		return message;
	}
	public static String getLookup(String senderId, String key) {
		String msg = getFirstLine(MessageType.LOOKUP,"1.0",senderId);
		return appendLine(msg, new String[] {""+key});
	}
	
	public static String getNotify(String senderId,Integer senderListeningPort) {
		String message = MessageFactory.getFirstLine(MessageType.NOTIFY, "1.0", senderId);
		return MessageFactory.appendLine(message, new String[] {"" + senderListeningPort});
	}
	
	public static String getSuccessor(String senderId, PeerI peer) {
		String msg = getFirstLine(MessageType.SUCCESSOR,"1.0",senderId);
		return appendLine(msg, new Object[] {peer.getId(),peer.getAddr().getHostAddress(),peer.getPort()});
	}
	public static String getResponsible(String string, ArrayList<FileStoredInfo> toSend) {
		String msg = getFirstLine(MessageType.RESPONSIBLE,"1.0",string);
		for(int i = 0; i < toSend.size(); i++) {
			msg += appendLine(msg, new Object[] {toSend.get(i).getFileId(),toSend.get(i).getDesiredRepDegree()});
		}
		return msg;
	}
	
	public static String getSuccessors(String senderId, List<PeerI> list) {
		String msg = getFirstLine(MessageType.SUCCESSORS,"2.0",senderId);
		Object[] objectArray = new Object[list.size() * 3];
		for (int i = 0; i < list.size(); i++) {
			PeerI nextPeer = list.get(i);
			objectArray[i*3] = nextPeer.getId();
			objectArray[i*3 + 1] = nextPeer.getAddr().getHostAddress();
			objectArray[i*3 + 2] = nextPeer.getPort();
		}
		return appendLine(msg, objectArray);
	}
	
	public static String getPredecessor(String senderId, PeerI peer) {
		String msg = getFirstLine(MessageType.PREDECESSOR,"1.0",senderId);
		return appendLine(msg, new Object[] {peer.getId(),peer.getAddr().getHostAddress(),peer.getPort()});
	}
	public static String getAsk(String senderId, PeerI peer) {
		String msg = getFirstLine(MessageType.ASK,"1.0",senderId);
		return appendLine(msg, new Object[] {peer.getId(),peer.getAddr().getHostAddress(),peer.getPort()});
	}
	public static String getPutChunk(String id, InetAddress addr, int port, String fileID, int chunkNo, int replicationDeg, byte[] body) {
		String msg = getFirstLine(MessageType.PUTCHUNK,"1.0",id);
		String msg2 = appendLine(msg, new Object[] {id, addr.getHostAddress(), port, fileID, chunkNo,
				replicationDeg});
		try {
			return appendBody(msg2, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			
			return null;
		}
	}
	public static String getKeepChunk(String senderId, InetAddress addr, int port, String fileID, int chunkNo, int replicationDeg, byte[] body) {
		String msg = getFirstLine(MessageType.KEEPCHUNK,"1.0",senderId);
		String msg2 = appendLine(msg, new Object[] {senderId, addr.getHostAddress(), port, fileID, chunkNo, replicationDeg});
		try {
			return appendBody(msg2, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String getStored(String senderId, String fileID, int chunkNo, int replicationDeg) {
		String msg = getFirstLine(MessageType.STORED,"1.0",senderId);
		String msg2 = appendLine(msg, new Object[] {fileID, chunkNo, replicationDeg});
		return msg2;
	}
	public static String getConfirmStored(String senderId, String fileID, int chunkNo, int replicationDeg) {
		String msg = getFirstLine(MessageType.CONFIRMSTORED,"1.0",senderId);
		String msg2 = appendLine(msg, new Object[] {fileID, chunkNo, replicationDeg});
		return msg2;
	}
	public static String getGetChunk(String senderId, InetAddress addr, int port, String fileID, int chunkNo) {
		String msg = getFirstLine(MessageType.GETCHUNK,"1.0",senderId);
		String msg2 = appendLine(msg, new Object[] {addr.getHostAddress(), port, fileID, chunkNo});
		return msg2;
	}
	public static String getChunk(String senderId, String fileID, int chunkNo, byte[] body) {
		String msg = getFirstLine(MessageType.CHUNK,"1.0",senderId);
		String msg2 = appendLine(msg, new Object[] {fileID, chunkNo});
		try {
			return appendBody(msg2, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getInitDelete(String senderId, String fileId) {
		String msg = getFirstLine(MessageType.INITDELETE, "1.0",senderId);
		return appendLine(msg, new Object[] {fileId});
	}
	public static String getDelete(String senderId, String fileId, int repDegree) {
		String msg = getFirstLine(MessageType.DELETE, "1.0",senderId);
		return appendLine(msg, new Object[] {fileId, repDegree});
	}

	public static String getPing(String senderId) {
		return getHeader(MessageType.PING,"1.0",senderId);
	}

	public static String getUpdateTime(String senderId, String fileID) {
		String msg = getFirstLine(MessageType.UPDATETIME, "1.0",senderId);
		return appendLine(msg, new Object[] {fileID});
	}
}
