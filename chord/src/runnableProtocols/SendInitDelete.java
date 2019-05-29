package runnableProtocols;

import chord.ChordManager;
import chord.PeerI;
import communication.Client;
import communication.MsgFactory;
import utils.Utils;

public class SendInitDelete implements Runnable{

	private String senderId;
	private String fileId;
	private ChordManager chordManager;
	
	public SendInitDelete(String fileId, ChordManager chordManager) {
		this.senderId = chordManager.getPeerInfo().getId();
		this.fileId = fileId;
		this.chordManager = chordManager;
	}
	
	@Override
	public void run() {
		PeerI successor = chordManager.getChunkOwner(fileId);
		String message = MsgFactory.getInitDelete(senderId, fileId);
		Utils.LOGGER.info("Sending InitDelete for file: " + fileId);
		Client.sendMessage(successor.getAddr(), successor.getPort(), message, false);
		System.out.println("Sent request to delete file: " + fileId);
	}
	
	

}
