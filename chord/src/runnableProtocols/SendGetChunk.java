/**
 * 
 */
package runnableProtocols;

import chord.ChordManager;
import chord.PeerI;
import communication.Client;
import communication.MessageFactory;
import database.BackupRequest;

public class SendGetChunk implements Runnable {

	BackupRequest backupRequest;
	int chunkNo;
	ChordManager chord;

	public SendGetChunk(BackupRequest backupRequest, int chunkNo, ChordManager chord) {
		super();
		this.backupRequest = backupRequest;
		this.chunkNo = chunkNo;
		this.chord = chord;
	}

	@Override
	public void run() {
		PeerI owner = chord.getChunkOwner(backupRequest.getFileId());
		String getChunkMessage = MessageFactory.getGetChunk(chord.getPeerInfo().getId(), chord.getPeerInfo().getAddr(),chord.getPeerInfo().getPort(), this.backupRequest.getFileId(), this.chunkNo);
		Client.sendMessage(owner.getAddr(), owner.getPort(), getChunkMessage, false);
	}

}
