package chord;

import communication.Client;
import communication.CreateMsg;
import communication.MsgType;
import util.Loggs;

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
			Loggs.LOG.finest("Running Stabilize\n");
			PeerI nextPeer = this.chordManager.getNextPeer();
			String stabilizeMessage = CreateMsg.getHeader(MsgType.STABILIZE, "1.0", myPeerId);
			String response = Client.sendMsg(nextPeer.getAddress(), nextPeer.getPort(), stabilizeMessage, true);
			if(response == null) {
				Loggs.LOG.warning("Next peer created");
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
			String successorsMsg = CreateMsg.getSuccessors(myPeerId, chordManager.getNextPeers());
			Client.sendMsg(predecessor.getAddress(), predecessor.getPort(), successorsMsg, false);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}