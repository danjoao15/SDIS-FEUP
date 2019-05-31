package protocols;

import chordSetup.ManageChord;
import chordSetup.Peer;
import main.Client;
import main.CreateMsg;
import util.Loggs;

public class SendIDelete implements Runnable{

	private String IDsender;
	private String IDfile;
	private ManageChord chordMngr;
	
	public SendIDelete(String IDfile, ManageChord chordMngr) {
		this.IDsender = chordMngr.getPeerInfo().getId();
		this.IDfile = IDfile;
		this.chordMngr = chordMngr;
	}
	
	@Override
	public void run() {
		Peer successor = chordMngr.getChunkOwner(IDfile);
		String message = CreateMsg.getIDelete(IDsender, IDfile);
		Loggs.LOG.info("delete request for file " + IDfile);
		Client.sendMsg(successor.getAddress(), successor.getPort(), message, false);
		System.out.println("delete request for file " + IDfile);
	}
	
	

}
