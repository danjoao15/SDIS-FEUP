package chord;

import communication.Client;
import communication.MsgFactory;
import communication.MsgType;
import util.Utils;

public class Stabilize implements Runnable {
	private ManageChord chordManager;

	public Stabilize(ManageChord chordManager) {
		this.chordManager = chordManager;
	}

	private AbstractPeer parseResponse(String response) { 
		try {
			response = response.trim();
			String[] args = response.split("\r\n")[1].split(" ");
			if (args.length == 3) {
				return new PeerI(response);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return new NullPeer();
	}

	@Override
	public void run() {
		try {
			String myPeerId = this.chordManager.getPeerInfo().getId();
			Utils.LOG.finest("Running Stabilize\n");
			PeerI nextPeer = this.chordManager.getNextPeer();
			String stabilizeMessage = MsgFactory.getHeader(MsgType.STABILIZE, "1.0", myPeerId);
			String response = Client.sendMessage(nextPeer.getAddr(), nextPeer.getPort(), stabilizeMessage, true);
			if(response == null) {
				Utils.LOG.warning("Next peer dropped");
				this.chordManager.popNextPeer();
				return;
			}
			
			AbstractPeer x = parseResponse(response);
			this.chordManager.stabilize(x);
			nextPeer = this.chordManager.getNextPeer();
			this.chordManager.notify(nextPeer);
			
			
			AbstractPeer predecessor = chordManager.getPredecessor();
			if (predecessor.isNull()) return;
			if(myPeerId.equals(predecessor.getId())) return;
			String successorsMsg = MsgFactory.getSuccessors(myPeerId, chordManager.getNextPeers());
			Client.sendMessage(predecessor.getAddr(), predecessor.getPort(), successorsMsg, false);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}