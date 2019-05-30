package database;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;

import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;

import chord.PeerI;
import communication.Leases;
import util.Utils;

public class DatabaseManager {

	private static final String storeFile = "INSERT INTO FILESSTORED " + "(fileid, responsible, requestingpeer, desiredrepdeg) VALUES (?,?,?,?)";
	
	private static final String storePeer = "INSERT INTO PEERS " + "(peerid,ip,port) VALUES (?,?,?)";
	
	private static final String storeChunk = "INSERT INTO CHUNKSSTORED "	+ "(chunkid, fileid, currentrepdeg, size) VALUES (?,?,?,?)";
	
	private static final String requestBackup = "INSERT INTO BACKUPSREQUESTED "	+ "(fileid, filename, desiredrepdeg,encrypt_key,chunksNum) VALUES (?,?,?,?,?)";
	
	private static final String getFileById = "SELECT * FROM FILESSTORED " + "WHERE fileid = ?";
	
	private static final String updatePeer = "UPDATE PEERS " + "SET ip = ?, port = ? " + "WHERE peerid = ?";
	
	private static final String updateFile = "UPDATE FILESSTORED " + "SET lasttimestored = CURRENT_TIMESTAMP, responsible = ?, requestingpeer = ?, desiredrepdeg = ?" + "WHERE fileid = ?";
	
	private static final String setStoring = "UPDATE FILESSTORED " + "SET storing = ? WHERE fileid = ?";
	
	private static final String updateChunkRepDegree = "UPDATE CHUNKSSTORED "	+ "SET currentrepdeg = ? WHERE chunkid = ? AND fileid = ?";
	
	private static final String updateBackup = "UPDATE BACKUPSREQUESTED " + "SET desiredrepdeg = ? WHERE fileid = ?";
	
	private static final String updateResponsible = "UPDATE FILESSTORED " + "SET responsible = ? WHERE fileid = ?";
	
	private static final String checkChunkStored = "SELECT * FROM CHUNKSSTORED " + "WHERE fileid = ? AND chunkid = ?";
	
	private static final String getRequestingPeer = "SELECT peerid,ip,port FROM PEERS " + "JOIN (SELECT requestingpeer FROM FILESSTORED WHERE fileid = ?) AS F ON PEERS.peerid = F.requestingpeer";
	
	private static final String getBackup = "SELECT * FROM BACKUPSREQUESTED WHERE fileid = ?";
	
	private static final String getFileStored = "SELECT fileid, desiredrepdeg, storing FROM FILESSTORED WHERE fileid = ?";
	
	private static final String getFileChunks = "SELECT chunkid, fileid, size FROM CHUNKSSTORED WHERE fileid = ?";
	
	private static final String getCurrentRepDeg = "SELECT max(currentrepdeg) FROM CHUNKSSTORED WHERE fileid = ?";
	
	private static final String deleteFile = "DELETE FROM FILESSTORED WHERE fileid = ?";
	
	private static final String deleteBackupRequest = "DELETE FROM BACKUPSREQUESTED WHERE fileid = ?";
	
	private static final String getFilesDelete = "SELECT fileid FROM FILESSTORED WHERE { fn TIMESTAMPADD(SQL_TSI_SECOND,?,lasttimestored)} < ?";
	
	private static final String updateFileTime = "UPDATE FILESSTORED " + "SET lasttimestored = CURRENT_TIMESTAMP " + "WHERE fileid = ?";
	
	private static final String getFilesUpdate = "SELECT * FROM BACKUPSREQUESTED";
	
	private static final String getFiles = "SELECT * FROM FILESSTORED "	+ "WHERE responsible";
	
	private static final String getAllBackups = "SELECT fileid, filename, desiredrepdeg, encrypt_key, chunksNum FROM BACKUPSREQUESTED";

	public static void storePeer(Connection c, PeerI peer) {
		try {
			PreparedStatement s = c.prepareStatement(storePeer);
			s.setString(1, peer.getId());
			s.setString(2, peer.getAddress().getHostAddress());
			s.setInt(3, peer.getPort());
			s.executeUpdate();
			c.commit();
			Utils.logging("peer " + peer.getId() + " stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			Utils.LOG.info("peer already stored, updating peer instead");
			updatePeer(c, peer);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	private static void updatePeer(Connection c, PeerI peer) {
		try {
			PreparedStatement s = c.prepareStatement(updatePeer);
			s.setString(1, peer.getAddress().getHostAddress());
			s.setInt(2, peer.getPort());
			s.setString(3, peer.getId());
			s.executeUpdate();
			c.commit();
			Utils.logging("peer " + peer.getId() + " updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void storeFile(Connection c, Stored file) {
		String peer = file.getpeer();
		Integer repdeg = file.getrepdegree();
		try {
			PreparedStatement s = c.prepareStatement(storeFile);
			s.setString(1, file.getfile());
			s.setBoolean(2, file.getchunkstored());
			if (peer != null) {
				s.setString(3, peer);
			} else {
				s.setNull(3, Types.VARCHAR);
			}
			if (repdeg != null){
				s.setInt(4, repdeg);
			}else {
				s.setNull(4, Types.INTEGER);
			}
			s.executeUpdate();
			c.commit();
			System.out.println("file " + file.getfile() + " stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			Utils.LOG.info("file already stored, updating file");
			updateFile(c, file);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void updateFile(Connection c, Stored file) {
		String peerRequesting = file.getpeer();
		Integer desiredRepDegree = file.getrepdegree();
		try {
			PreparedStatement s = c.prepareStatement(updateFile);
			s.setBoolean(1, file.getchunkstored());
			if (peerRequesting != null) {
				s.setString(2, peerRequesting);
			} else {
				s.setNull(2, Types.VARCHAR);
			}
			if (desiredRepDegree != null){
				s.setInt(3, desiredRepDegree);
			}else {
				s.setNull(3, Types.INTEGER);
			}
			s.setString(4, file.getfile());
			s.executeUpdate();
			c.commit();
			Utils.logging("file " + file.getfile() + " updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void setStoring(Connection c, String fileId, Boolean storing) {
		try {
			PreparedStatement s = c.prepareStatement(setStoring);
			s.setBoolean(1, storing);
			s.setString(2, fileId);
			s.executeUpdate();
			c.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void storeChunk(Connection c, Chunk chunkInfo) {
		try {
			PreparedStatement s = c.prepareStatement(storeChunk);
			s.setInt(1, chunkInfo.getchunkid());
			s.setString(2, chunkInfo.getfileid());
			if (chunkInfo.getrepdegree() == null) {
				s.setNull(3, Types.INTEGER);
			}else {
				s.setInt(3, chunkInfo.getrepdegree());
			}
			s.setInt(4, chunkInfo.getsize());
			s.executeUpdate();
			c.commit();
			Utils.logging("file " + chunkInfo.getfileid() + " - chunk " + chunkInfo.getchunkid() + " - stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			updateRepDeg(c,chunkInfo);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void updateRepDeg(Connection c, Chunk chunkInfo) {
		try {
			PreparedStatement s = c.prepareStatement(updateChunkRepDegree);
			s.setInt(1, chunkInfo.getrepdegree());
			s.setInt(2, chunkInfo.getchunkid());
			s.setString(3, chunkInfo.getfileid());
			s.executeUpdate();
			c.commit();
			Utils.logging("file " + chunkInfo.getfileid() + " - chunk " + chunkInfo.getchunkid() + " - repdeg updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	private static void updateBackup(Connection c, Backup request) {
		try {
			PreparedStatement s = c.prepareStatement(updateBackup);
			s.setInt(1, request.getrepdegree());
			s.setString(2, request.getid());
			s.executeUpdate();
			c.commit();
			Utils.logging("backup request " + request.getid() + " updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void updateResponsible(Connection c, String fileId, Boolean bool) {
		try {
			PreparedStatement s = c.prepareStatement(updateResponsible);
			s.setBoolean(1, bool);
			s.setString(2, fileId);
			s.executeUpdate();
			c.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void requestBackup(Connection c, Backup backupRequest) {
		Boolean wasRequested = checkBackupRequested(c, backupRequest.getid());
		if (wasRequested) {
			updateBackup(c,backupRequest);
		}
		else {
			try {
				PreparedStatement s = c.prepareStatement(requestBackup);
				s.setString(1, backupRequest.getid());
				s.setString(2, backupRequest.getname());
				s.setInt(3, backupRequest.getrepdegree());
				if (backupRequest.getkey() == null) {
					s.setNull(4, Types.VARCHAR);
				}else {
					s.setString(4, backupRequest.getkey());
				}
				s.setInt(5, backupRequest.getchunksnumber());
				s.executeUpdate();
				c.commit();
				Utils.logging("file " + backupRequest.getname() + " backup request stored");
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	public static boolean checkResponsible(Connection c, String fileId) {
		PreparedStatement s = null;
		try {
			s = c.prepareStatement(getFileById);
			s.setString(1, fileId);
			ResultSet result = s.executeQuery();
			c.setAutoCommit(false);
			c.commit();
			if (result.next()) {
				return result.getBoolean("responsible");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				s.close();
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
		return false;
	}
	
	public static boolean checkChunkStored(Connection c, Chunk chunkInfo) {
		PreparedStatement s = null;
		try {	
			s = c.prepareStatement(checkChunkStored);
			s.setString(1, chunkInfo.getfileid());
			s.setInt(2, chunkInfo.getchunkid());
			ResultSet r = s.executeQuery();
			c.setAutoCommit(false);
			if (r.next()) {
				int size = r.getInt(5);
				return true && size >= 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				s.close();
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
		return false;
	}
	
	public static ArrayList<Backup> getRequestedBackups(Connection c){
		ArrayList<Backup> a = new ArrayList<Backup>();
		try {
			PreparedStatement s = c.prepareStatement(getAllBackups);
			ResultSet set = s.executeQuery();
			while (set.next()) {
				Backup currentBackupRequest = new Backup(set.getString(1),
						set.getString(2),
						set.getString("encrypt_key"),
						set.getInt(3), 
						set.getInt("chunksNum"));
				a.add(currentBackupRequest);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return a;
	}
	
	public static PeerI getRequestingPeer(Connection c, String fileId) {
		try {
			PreparedStatement s = c.prepareStatement(getRequestingPeer);
			s.setString(1, fileId);
			ResultSet set = s.executeQuery();
			if (set.next()) {
				InetAddress a = null;
				try {
					a = InetAddress.getByName(set.getString(2));
				} catch (UnknownHostException e) {
					e.printStackTrace();
					return null;
				}
				return new PeerI(set.getString(1),a,set.getInt(3));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	private static boolean checkBackupRequested(Connection c, String fileId) {
		PreparedStatement s;
		try {
			s = c.prepareStatement(getBackup);
			s.setString(1, fileId);
			ResultSet result =  s.executeQuery();
			if (result.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	public static Backup getBackup(Connection c, String fileId) {
		PreparedStatement s;
		try {
			s = c.prepareStatement(getBackup);
			s.setString(1, fileId);
			ResultSet set =  s.executeQuery();
			if (set.next()) {
				return new Backup(set.getString("fileid"),
						set.getString("filename"),
						set.getString("encrypt_key"),
						set.getInt("desiredrepdeg"),
						set.getInt("chunksNum"));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	public static int getRepDegree(Connection c, String fileId) {
		PreparedStatement s;
		try {
			s = c.prepareStatement(getFileStored);
			s.setString(1, fileId);
			ResultSet set =  s.executeQuery();
			if (set.next()) {
				return set.getInt(2);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}
	
	public static int getMaxRepDegree(Connection c, String fileId) {
		PreparedStatement s;
		try {
			s = c.prepareStatement(getCurrentRepDeg);
			s.setString(1, fileId);
			ResultSet result =  s.executeQuery();
			if (result.next()) {
				return result.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}
	
	public static boolean checkFile(Connection c, String fileId) {
		PreparedStatement s;
		try {
			s = c.prepareStatement(getFileStored);
			s.setString(1, fileId);
			ResultSet set =  s.executeQuery();
			return set.next() && set.getBoolean(3);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	public static ArrayList<Chunk> getFileChunks(Connection c, String fileId) {
		PreparedStatement s;
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		try {
			s = c.prepareStatement(getFileChunks);
			s.setString(1, fileId);
			ResultSet result =  s.executeQuery();
			while (result.next()) {
				chunks.add(new Chunk(result.getInt(1), result.getString(2), result.getInt(3)));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return chunks;
	}
	
	public static void deleteFile(Connection c, String fileId) {
		try {
			PreparedStatement s = c.prepareStatement(deleteFile);
			s.setString(1, fileId);
			s.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void deleteBackupRequest(Connection c, String fileId) {
		try {
			PreparedStatement s = c.prepareStatement(deleteBackupRequest);
			s.setString(1, fileId);
			s.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static ArrayList<String> getFilesDelete(Connection c, Timestamp t) {
		ArrayList<String> files = new ArrayList<String>();
		PreparedStatement s;
		try {
			s = c.prepareStatement(getFilesDelete);
			s.setInt(1, Leases.LEASE_TIME);
			s.setTimestamp(2, t);
			ResultSet set = s.executeQuery();
			while (set.next()) {
				files.add(set.getString(1));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			return null;
		}
		return files;
	}
	
	public static void updateFileTime(Connection c, String fileId) {
		try {
			PreparedStatement s = c.prepareStatement(updateFileTime);
			s.setString(1, fileId);
			s.executeUpdate();
			c.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static ArrayList<Backup> getFilesUpdate(Connection c) {
		ArrayList<Backup> files = new ArrayList<Backup>();
		PreparedStatement s;
		try {
			s = c.prepareStatement(getFilesUpdate);
			ResultSet result = s.executeQuery();
			while (result.next()) {
				files.add(new Backup(
						result.getString("fileid"),
						result.getString("filename"),
						result.getString("encrypt_key"),
						result.getInt("desiredrepdeg"),
						result.getInt("chunksNum")
						));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return files;
	}
	
	public static ArrayList<Stored> getFiles(Connection c) {
		ArrayList<Stored> files = new ArrayList<Stored>();
		PreparedStatement s;
		try {
			s = c.prepareStatement(getFiles);
			ResultSet result = s.executeQuery();
			while (result.next()) {
				files.add(new Stored(
						result.getString("fileid"), 
						true, result.getInt("desiredrepdeg")));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return files;
	}
}