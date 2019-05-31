package chordSetup;

import java.math.BigInteger;

import main.Client;
import main.CreateMsg;
import main.MsgType;
import util.Loggs;


public class FingerTable implements Runnable {

	private ManageChord chord;
	
	
	@Override
	public void run() {
		Loggs.LOG.info("Running Fingertable Fixer");
		fixFT();
	}

	

	public void fixFT() {
		try {
			for(int i = 0; i < ManageChord.getM(); i++) {
				String keyToLookup = getKey(chord.getPeerInfo().getId(), i);
				String lookupMessage = CreateMsg.getLookup(chord.getPeerInfo().getId(), keyToLookup);
				String response = chord.lookup(keyToLookup);
				response = response.trim();
				Peer info = new Peer(response);
				while(response.startsWith(MsgType.ASK.getType())) {
					response = Client.sendMsg(info.getAddress(), info.getPort(), lookupMessage, true);
					if (response == null) return;
					info = new Peer(response);
				}
				chord.getFingerTable().set(i, info);
				
				
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	static String getKey(String id, int i) {
		
		BigInteger _id = new BigInteger(id, 16);
		BigInteger add = new BigInteger((Math.pow(2, i)+"").getBytes());
		BigInteger mod =  new BigInteger((Math.pow(2, ManageChord.getM())+"").getBytes());
			
		BigInteger res = _id.add(add).mod(mod);
		return res.toString(16);
	}
	
	public FingerTable(ManageChord chord) {
		this.chord = chord;
	}
}
