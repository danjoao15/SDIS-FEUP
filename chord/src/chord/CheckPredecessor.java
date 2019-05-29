package chord;

import communication.Client;
import communication.MessageFactory;
import utils.Utils;

public class CheckPredecessor implements Runnable {

	private ChordManager chordManager;
	
	CheckPredecessor(ChordManager chordManager){
		this.chordManager =  chordManager;
	}
	
	@Override
	public void run() {
		try {
			AbstractPeer predecessor = chordManager.getPredecessor();
			String myPeerId = chordManager.getPeerInfo().getId();
			if (predecessor.isNull()) {
				Utils.LOGGER.warning("Predecessor not set");
				return;
			}
			Utils.LOGGER.info("My predecessor is " + predecessor.getId());
			if (predecessor.getId().equals(myPeerId)) return;
			String pingMessage = MessageFactory.getPing(myPeerId);
			String response = Client.sendMessage(predecessor.getAddr(), predecessor.getPort(), pingMessage, true);
			if (response == null) {
				Utils.LOGGER.finest("Could not establish connection with predecessor");
				chordManager.setPredecessor(new NullPeer());
			}	
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
