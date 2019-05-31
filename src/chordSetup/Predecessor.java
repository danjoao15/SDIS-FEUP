package chordSetup;

import main.Client;
import main.CreateMsg;
import util.Loggs;

public class Predecessor implements Runnable {

	private ManageChord chord;

	Predecessor(ManageChord chord){
		this.chord =  chord;
	}

	@Override
	public void run() {
		try {
			String peerid = chord.getPeerInfo().getId();
			AbstractPeer p = chord.getPredecessor();
			if (p.isNull()) {
				Loggs.LOG.warning("predecessor not set");
				return;
			}
			if (p.getId().equals(peerid)) return;
			String s = CreateMsg.getPing(peerid);
			String r = Client.sendMsg(p.getAddress(), p.getPort(), s, true);
			if (r == null) {
				Loggs.LOG.finest("predecessor connection fail");
				chord.setPredecessor(new NullPeer());
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
