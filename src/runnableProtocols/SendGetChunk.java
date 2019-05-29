/**
 * 
 */
package runnableProtocols;

import chord.ChordManager;
import chord.PeerI;
import communication.Client;
import communication.MsgFactory;
import database.Backup;

/**
 * @author anabela
 *
 */
public class SendGetChunk implements Runnable {

	Backup backupRequest;
	int chunkNo;
	ChordManager chord;

	public SendGetChunk(Backup backupRequest, int chunkNo, ChordManager chord) {
		super();
		this.backupRequest = backupRequest;
		this.chunkNo = chunkNo;
		this.chord = chord;
	}

	@Override
	public void run() {
		PeerI owner = chord.getChunkOwner(backupRequest.getname());
		String getChunkMessage = MsgFactory.getGetChunk(chord.getPeerInfo().getId(), chord.getPeerInfo().getAddr(),chord.getPeerInfo().getPort(), this.backupRequest.getname(), this.chunkNo);
		Client.sendMessage(owner.getAddr(), owner.getPort(), getChunkMessage, false);
	}

}
