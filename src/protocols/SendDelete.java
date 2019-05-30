package protocols;

import chord.ManageChord;
import chord.PeerI;
import communication.Client;
import communication.MsgFactory;
import util.Utils;

public class SendDelete implements Runnable{

	private String senderId;
	private String fileId;
	private ManageChord chordManager;
	
	public SendDelete(String fileId, ManageChord chordManager) {
		this.senderId = chordManager.getPeerInfo().getId();
		this.fileId = fileId;
		this.chordManager = chordManager;
	}
	
	@Override
	public void run() {
		PeerI successor = chordManager.getChunkOwner(fileId);
		String message = MsgFactory.getInitDelete(senderId, fileId);
		Utils.LOG.info("Sending Delete request for file: " + fileId);
		Client.sendMessage(successor.getAddr(), successor.getPort(), message, false);
		System.out.println("Sent request to delete file: " + fileId);
	}
	
	

}
