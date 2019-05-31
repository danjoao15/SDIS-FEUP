package protocols;

import java.util.Arrays;

import chordSetup.ManageChord;
import chordSetup.PeerI;
import main.Client;
import main.CreateMsg;

public class SendPutchunk implements Runnable {

	private String IDsender = null;
	private String IDfile= null;
	private int nChunk = 0;
	private int repDeg = 0;
	private byte[] body = null;
	private ManageChord chord = null;

	public SendPutchunk (String IDfile, int nChunk, int repDeg, byte[] body, ManageChord chord) {
		this.IDsender = chord.getPeerInfo().getId();
		this.IDfile = IDfile;
		this.nChunk = nChunk;
		this.repDeg = repDeg;
		this.body = Arrays.copyOf(body, body.length);

		this.chord = chord;
	}
	@Override
	public void run() {
		PeerI owner = chord.getChunkOwner(IDfile);
		String putChunkMessage = CreateMsg.getPutChunk(IDsender, owner.getAddress(),owner.getPort(), this.IDfile, this.nChunk, this.repDeg, this.body);
		Client.sendMsg(owner.getAddress(), owner.getPort(), putChunkMessage, false);

	}

}