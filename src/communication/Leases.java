package communication;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import database.Backup;
import database.Chunk;
import database.DatabaseManager;
import util.Utils;

public class Leases implements Runnable {


	public static final int HALF_LEASE_TIME = 30;
	public static final int LEASE_TIME = 2*HALF_LEASE_TIME;
	public static final TimeUnit LEASE_UNIT = TimeUnit.SECONDS;
	PeerMain peer;

	public Leases(PeerMain peer) {
		super();
		this.peer = peer;
	}

	@Override
	public void run() {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		updateFiles(time);
		deleteFiles(time);
		

	}

	private void updateFiles(Timestamp time) {
		ArrayList<Backup> filesToUpdate = DatabaseManager.getFilesUpdate(peer.getConnection());
		
		for(int i = 0; i < filesToUpdate.size(); i++) {
			
			peer.backup(filesToUpdate.get(i).getname(),
					filesToUpdate.get(i).getrepdegree(),
					filesToUpdate.get(i).getkey());
			Utils.LOG.info("Lease:Updated file: " + filesToUpdate.get(i));
		}

	}

	private void deleteFiles(Timestamp time) {
		ArrayList<String> filesToDelete = DatabaseManager.getFilesDelete(peer.getConnection(), time);
		if (filesToDelete.size() > 0) {
			System.out.println("Leases: Found " + filesToDelete.size() + " to delete");
		}
		for(int i = 0; i < filesToDelete.size(); i++) {
			ArrayList<Chunk> allChunks = DatabaseManager.getFileChunks(peer.getConnection(), filesToDelete.get(i));
			allChunks.forEach(chunk -> {
				Utils.delete(PeerMain.getPath().resolve(chunk.getfile()));
				PeerMain.decreaseStorageUsed(chunk.getsize());
			});
			DatabaseManager.deleteFile(peer.getConnection(), filesToDelete.get(i));
			Utils.LOG.info("Lease:Deleted file: " + filesToDelete.get(i));
		}
	}

}
