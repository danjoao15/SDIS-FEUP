package protocols;

import chord.ManageChord;
import chord.PeerI;
import communication.Client;
import communication.MsgFactory;
import database.Backup;

public class SendGetChunk implements Runnable {

	Backup request;
	int nchunk;
	ManageChord chord;

	public SendGetChunk(Backup backupRequest, int chunkNo, ManageChord chord) {
		super();
		this.request = backupRequest;
		this.nchunk = chunkNo;
		this.chord = chord;
	}

	@Override
	public void run() {
		PeerI owner = chord.getChunkOwner(request.getname());
		String getChunkMessage = MsgFactory.getGetChunk(chord.getPeerInfo().getId(), chord.getPeerInfo().getAddr(),chord.getPeerInfo().getPort(), this.request.getname(), this.nchunk);
		Client.sendMessage(owner.getAddr(), owner.getPort(), getChunkMessage, false);
	}

}
