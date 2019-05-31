package main;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import database.Backup;
import database.Chunk;
import database.DatabaseManager;
import util.Loggs;

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
	}

	private void updateFiles(Timestamp time) {
		ArrayList<Backup> filesToUpdate = DatabaseManager.getFilesUpdate(peer.getConnection());
		
		for(int i = 0; i < filesToUpdate.size(); i++) {
			
			peer.backup(filesToUpdate.get(i).getname(),
					filesToUpdate.get(i).getrepdegree(),
					filesToUpdate.get(i).getkey());
			Loggs.LOG.info("Lease updated file " + filesToUpdate.get(i));
		}

	}

}
