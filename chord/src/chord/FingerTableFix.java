package chord;

import java.math.BigInteger;

import communication.Client;
import communication.MsgFactory;
import communication.MsgType;
import utils.Utils;


public class FingerTableFix implements Runnable {

	private ChordManager chord;
	
	
	@Override
	public void run() {
		Utils.LOGGER.info("Running fix finger table");
		fixFT();
	}

	

	public void fixFT() {
		try {
			for(int i = 0; i < ChordManager.getM(); i++) {
				String keyToLookup = getKey(chord.getPeerInfo().getId(), i);
				String lookupMessage = MsgFactory.getLookup(chord.getPeerInfo().getId(), keyToLookup);
				String response = chord.lookup(keyToLookup);
				response = response.trim();
				PeerI info = new PeerI(response);
				while(response.startsWith(MsgType.ASK.getType())) {
					response = Client.sendMessage(info.getAddr(), info.getPort(), lookupMessage, true);
					if (response == null) return;
					info = new PeerI(response);
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
		BigInteger mod =  new BigInteger((Math.pow(2, ChordManager.getM())+"").getBytes());
			
		BigInteger res = _id.add(add).mod(mod);
		return res.toString(16);
	}

	private void printFT() {
		String m = new String();
		for (int i = 0; i < chord.getFingerTable().size(); i++) {
			m += "\t" + chord.getFingerTable().get(i).getId() + "\n";
		}
		Utils.LOGGER.finest("Tabela de dedos: " + chord.getPeerInfo().getId() + "\n" + m);
	}
	
	public FingerTableFix(ChordManager chord) {
		this.chord = chord;
	}
}
