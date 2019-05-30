package chord;

import communication.Client;
import communication.CreateMsg;
import util.Utils;

public class CheckPredecessor implements Runnable {

	private ManageChord chordmanage;
	
	CheckPredecessor(ManageChord chordmanage){
		this.chordmanage =  chordmanage;
	}
	
	@Override
	public void run() {
		try {
			AbstractPeer predecessor = chordmanage.getPredecessor();
			String myPeerId = chordmanage.getPeerInfo().getId();
			if (predecessor.isNull()) {
				Utils.LOG.warning("Predecessor not set");
				return;
			}
			Utils.LOG.info("My predecessor is " + predecessor.getId());
			if (predecessor.getId().equals(myPeerId)) return;
			String pingMessage = CreateMsg.getPing(myPeerId);
			String response = Client.sendMsg(predecessor.getAddress(), predecessor.getPort(), pingMessage, true);
			if (response == null) {
				Utils.LOG.finest("Could not establish connection with predecessor");
				chordmanage.setPredecessor(new NullPeer());
			}	
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
