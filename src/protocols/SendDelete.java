package protocols;

import chord.ManageChord;
import chord.PeerI;
import communication.Client;
import communication.CreateMsg;
import util.Utils;

public class SendDelete implements Runnable{

	private String IDsender;
	private String IDfile;
	private ManageChord chordMngr;
	
	public SendDelete(String IDfile, ManageChord chordMngr) {
		this.IDsender = chordMngr.getPeerInfo().getId();
		this.IDfile = IDfile;
		this.chordMngr = chordMngr;
	}
	
	@Override
	public void run() {
		PeerI successor = chordMngr.getChunkOwner(IDfile);
		String message = CreateMsg.getInitDelete(IDsender, IDfile);
		Utils.LOG.info("Sending Delete request for file: " + IDfile);
		Client.sendMsg(successor.getAddress(), successor.getPort(), message, false);
		System.out.println("Sent request to delete file: " + IDfile);
	}
	
	

}
