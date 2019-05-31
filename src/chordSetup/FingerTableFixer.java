package chordSetup;

import java.math.BigInteger;

import main.Client;
import main.CreateMsg;
import main.MsgType;
import util.Loggs;


public class FingerTableFixer implements Runnable {

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
				PeerI info = new PeerI(response);
				while(response.startsWith(MsgType.ASK.getType())) {
					response = Client.sendMsg(info.getAddress(), info.getPort(), lookupMessage, true);
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
		BigInteger mod =  new BigInteger((Math.pow(2, ManageChord.getM())+"").getBytes());
			
		BigInteger res = _id.add(add).mod(mod);
		return res.toString(16);
	}

	private void printFT() {
		String m = new String();
		for (int i = 0; i < chord.getFingerTable().size(); i++) {
			m += "\t" + chord.getFingerTable().get(i).getId() + "\n";
		}
		Loggs.LOG.finest("Tabela de dedos: " + chord.getPeerInfo().getId() + "\n" + m);
	}
	
	public FingerTableFixer(ManageChord chord) {
		this.chord = chord;
	}
}
