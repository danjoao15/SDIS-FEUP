/**
 * 
 */
package communication;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import database.Backup;
import database.Chunk;
import database.DBUtils;
import utils.Utils;

/**
 * @author anabela
 *
 */
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
		ArrayList<Backup> filesToUpdate = DBUtils.getFilesToUpdate(peer.getConnection());
		
		for(int i = 0; i < filesToUpdate.size(); i++) {
			
			peer.backup(filesToUpdate.get(i).getname(),
					filesToUpdate.get(i).getrepdegree(),
					filesToUpdate.get(i).getkey());
			Utils.LOGGER.info("Lease:Updated file: " + filesToUpdate.get(i));
		}

	}

	private void deleteFiles(Timestamp time) {
		ArrayList<String> filesToDelete = DBUtils.getFilesToDelete(peer.getConnection(), time);
		if (filesToDelete.size() > 0) {
			System.out.println("Leases: Found " + filesToDelete.size() + " to delete");
		}
		for(int i = 0; i < filesToDelete.size(); i++) {
			ArrayList<Chunk> allChunks = DBUtils.getAllChunksOfFile(peer.getConnection(), filesToDelete.get(i));
			allChunks.forEach(chunk -> {
				Utils.deleteFile(PeerMain.getPath().resolve(chunk.getfile()));
				PeerMain.decreaseStorageUsed(chunk.getsize());
			});
			DBUtils.deleteFile(peer.getConnection(), filesToDelete.get(i));
			Utils.LOGGER.info("Lease:Deleted file: " + filesToDelete.get(i));
		}
	}

}
