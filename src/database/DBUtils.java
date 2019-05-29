package database;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;

import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;

import chord.PeerI;
import chord.PeerI;
import communication.Leases;
import utils.Utils;

public class DBUtils {

	private static final String insertFileStored = "INSERT INTO FILESSTORED " + "(file_id, i_am_responsible, peer_requesting, desired_rep_degree) VALUES (?,?,?,?)";
	private static final String insertPeer = "INSERT INTO PEERS " + "(peer_id,ip,port) VALUES (?,?,?)";
	private static final String insertChunkStored = "INSERT INTO CHUNKSSTORED "	+ "(chunk_id, file_id, actual_rep_degree, size) VALUES (?,?,?,?)";
	private static final String insertBackupRequested = "INSERT INTO BACKUPSREQUESTED "	+ "(file_id, filename, desired_rep_degree,encrypt_key,numberOfChunks) VALUES (?,?,?,?,?)";
	private static final String getFileById = "SELECT * FROM FILESSTORED " + "WHERE file_id = ?";
	private static final String updatePeer = "UPDATE PEERS " + "SET ip = ?, port = ? " + "WHERE peer_id = ?";
	private static final String updateFileStored = "UPDATE FILESSTORED " + "SET last_time_stored = CURRENT_TIMESTAMP, i_am_responsible = ?, peer_requesting = ?, desired_rep_degree = ?" + "WHERE file_id = ?";
	private static final String setIamStoring = "UPDATE FILESSTORED " + "SET i_am_storing = ? WHERE file_id = ?";
	private static final String updateChunkStoredRepDegree = "UPDATE CHUNKSSTORED "	+ "SET actual_rep_degree = ? WHERE chunk_id = ? AND file_id = ?";
	private static final String updateBackupRequested = "UPDATE BACKUPSREQUESTED " + "SET desired_rep_degree = ? WHERE file_id = ?";
	private static final String updateResponsible = "UPDATE FILESSTORED " + "SET i_am_responsible = ? WHERE file_id = ?";
	private static final String checkStoredChunk = "SELECT * FROM CHUNKSSTORED " + "WHERE file_id = ? AND chunk_id = ?";
	private static final String getPeerWhichRequested = "SELECT peer_id,ip,port FROM PEERS " + "JOIN (SELECT peer_requesting FROM FILESSTORED WHERE file_id = ?) AS F ON PEERS.peer_id = F.peer_requesting";
	private static final String getBackupRequested = "SELECT * FROM BACKUPSREQUESTED WHERE file_id = ?";
	private static final String getFileStored = "SELECT file_id, desired_rep_degree, i_am_storing FROM FILESSTORED WHERE file_id = ?";
	private static final String getChunksOfFile = "SELECT chunk_id, file_id, size FROM CHUNKSSTORED WHERE file_id = ?";
	private static final String getActualRepDegree = "SELECT max(actual_rep_degree) FROM CHUNKSSTORED WHERE file_id = ?";
	private static final String deleteFileStored = "DELETE FROM FILESSTORED WHERE file_id = ?";
	private static final String deleteFileRequested = "DELETE FROM BACKUPSREQUESTED WHERE file_id = ?";
	private static final String getFilesToDelete = "SELECT file_id FROM FILESSTORED WHERE { fn TIMESTAMPADD(SQL_TSI_SECOND,?,last_time_stored)} < ?";
	private static final String updateFile = "UPDATE FILESSTORED " + "SET last_time_stored = CURRENT_TIMESTAMP " + "WHERE file_id = ?";
	private static final String getFilesToUpdate = "SELECT * FROM BACKUPSREQUESTED";
	private static final String getFiles = "SELECT * FROM FILESSTORED "	+ "WHERE i_am_responsible";

	public static void insertPeer(Connection c, PeerI peer) {
		try {
			PreparedStatement p = c.prepareStatement(insertPeer);
			p.setString(1, peer.getId());
			p.setString(2, peer.getAddr().getHostAddress());
			p.setInt(3, peer.getPort());
			p.executeUpdate();
			c.commit();
			Utils.log("PeerI " + peer.getId() + " has been stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			Utils.LOGGER.info("Not a new INSERT, updating");
			updatePeer(c, peer);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	private static void updatePeer(Connection c, PeerI peer) {
		try {
			PreparedStatement p = c.prepareStatement(updatePeer);
			p.setString(1, peer.getAddr().getHostAddress());
			p.setInt(2, peer.getPort());
			p.setString(3, peer.getId());
			p.executeUpdate();
			c.commit();
			Utils.log("PeerI " + peer.getId() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void insertStoredFile(Connection c, Stored fileInfo) {
		String peerRequesting = fileInfo.getpeer();
		Integer desiredRepDegree = fileInfo.getrepdegree();
		try {
			PreparedStatement p = c.prepareStatement(insertFileStored);
			p.setString(1, fileInfo.getfile());
			p.setBoolean(2, fileInfo.getchunkstored());
			if (peerRequesting == null) {
				p.setNull(3, Types.VARCHAR);
			} else {
				p.setString(3, peerRequesting);
			}
			if (desiredRepDegree == null){
				p.setNull(4, Types.INTEGER);
			}else {
				p.setInt(4, desiredRepDegree);
			}
			p.executeUpdate();
			c.commit();
			System.out.println("File " + fileInfo.getfile() + " has been stored in database");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			Utils.LOGGER.info("Not a new INSERT, updating");
			updateStoredFile(c, fileInfo);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void updateStoredFile(Connection c, Stored fileInfo) {
		String peerRequesting = fileInfo.getpeer();
		Integer desiredRepDegree = fileInfo.getrepdegree();
		try {
			PreparedStatement p = c.prepareStatement(updateFileStored);
			p.setBoolean(1, fileInfo.getchunkstored());
			if (peerRequesting == null) {
				p.setNull(2, Types.VARCHAR);
			} else {
				p.setString(2, peerRequesting);
			}
			if (desiredRepDegree == null){
				p.setNull(3, Types.INTEGER);
			}else {
				p.setInt(3, desiredRepDegree);
			}
			p.setString(4, fileInfo.getfile());

			p.executeUpdate();
			c.commit();
			Utils.log("File " + fileInfo.getfile() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void setIamStoring(Connection c, String fileId, Boolean iAmStoring) {
		try {
			PreparedStatement p = c.prepareStatement(setIamStoring);
			p.setBoolean(1, iAmStoring);
			p.setString(2, fileId);
			p.executeUpdate();
			c.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	public static void insertStoredChunk(Connection c, Chunk chunkInfo) {
		try {
			PreparedStatement p = c.prepareStatement(insertChunkStored);
			p.setInt(1, chunkInfo.getchunkid());
			p.setString(2, chunkInfo.getfileid());
			if (chunkInfo.getrepdegree() != null) {
				p.setInt(3, chunkInfo.getrepdegree());
			}else {
				p.setNull(3, Types.INTEGER);
			}
			p.setInt(4, chunkInfo.getsize());
			p.executeUpdate();
			c.commit();
			Utils.log("Chunk " + chunkInfo.getfileid() + ":" + chunkInfo.getchunkid() + " has been stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			updateStoredChunkRepDegree(c,chunkInfo);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void updateStoredChunkRepDegree(Connection c, Chunk chunkInfo) {
		//"UPDATE CHUNKSSTORED SET actual_rep_degree = ? WHERE file_id = ? AND chunk_id = ?"

		try {
			PreparedStatement p = c.prepareStatement(updateChunkStoredRepDegree);
			p.setInt(1, chunkInfo.getrepdegree());
			p.setInt(2, chunkInfo.getchunkid());
			p.setString(3, chunkInfo.getfileid());
			p.executeUpdate();
			c.commit();
			Utils.log("Chunk " + chunkInfo.getfileid() + ":" + chunkInfo.getchunkid() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	private static void updateBackupRequested(Connection c, Backup backupReq) {
		try {
			PreparedStatement p = c.prepareStatement(updateBackupRequested);
			p.setInt(1, backupReq.getrepdegree());
			p.setString(2, backupReq.getid());
			p.executeUpdate();
			c.commit();
			Utils.log("Backup: " + backupReq.getid() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void updateResponsible(Connection c, String fileId, Boolean value) {
		try {
			PreparedStatement p = c.prepareStatement(updateResponsible);
			p.setBoolean(1, value);
			p.setString(2, fileId);
			p.executeUpdate();
			c.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void insertBackupRequested(Connection c, Backup backupRequest) {
		Boolean wasRequested = wasBackupRequestedBefore(c, backupRequest.getid());
		if (!wasRequested) {
			// Create New
			try {
				PreparedStatement p = c.prepareStatement(insertBackupRequested);
				p.setString(1, backupRequest.getid());
				p.setString(2, backupRequest.getname());
				p.setInt(3, backupRequest.getrepdegree());
				if (backupRequest.getkey() != null) {
					p.setString(4, backupRequest.getkey());
				}else {
					p.setNull(4, Types.VARCHAR);
				}
				p.setInt(5, backupRequest.getchunksnumber());
				p.executeUpdate();
				c.commit();
				Utils.log("Backup for file " + backupRequest.getname() + " has been stored");
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		} else {
			// Update old
			updateBackupRequested(c,backupRequest);
		}

	}
	public static boolean amIResponsible(Connection c, String fileId) {
		PreparedStatement p = null;
		try {
			p = c.prepareStatement(getFileById);
			p.setString(1, fileId);
			ResultSet result = p.executeQuery();
			c.setAutoCommit(false);
			c.commit();
			if (result.next()) {
				return result.getBoolean("i_am_responsible");
			}
			System.out.println("amIResponsible: file not found");

		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				p.close();
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
		return false;
	}
	public static boolean checkStoredChunk(Connection c, Chunk chunkInfo) {

		PreparedStatement p = null;
		try {
			
			p = c.prepareStatement(checkStoredChunk);
			p.setString(1, chunkInfo.getfileid());
			p.setInt(2, chunkInfo.getchunkid());
			ResultSet result = p.executeQuery();
			c.setAutoCommit(false);
			if (result.next()) {
				int size = result.getInt(5);
				return true && size > -1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				p.close();
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
		return false;
	}
	public static ArrayList<Backup> getBackupsRequested(Connection c){
		ArrayList<Backup> array = new ArrayList<Backup>();
		try {
			Statement stmt = c.createStatement();
			ResultSet res = stmt.executeQuery("SELECT file_id, filename, desired_rep_degree,encrypt_key, numberOfChunks FROM BACKUPSREQUESTED");
			while (res.next()) {
				Backup currentBackupRequest = new Backup(res.getString(1),
						res.getString(2),
						res.getString("encrypt_key"),
						res.getInt(3), 
						res.getInt("numberOfChunks"));
				array.add(currentBackupRequest);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return array;
	}
	public static PeerI getPeerWhichRequestedBackup(Connection c, String fileId) {
		try {
			PreparedStatement p = c.prepareStatement(getPeerWhichRequested);
			p.setString(1, fileId);
			ResultSet result = p.executeQuery();
			if (result.next()) {
				InetAddress address = null;
				try {
					address = InetAddress.getByName(result.getString(2));
				} catch (UnknownHostException e) {
					e.printStackTrace();
					return null;
				}
				return new PeerI(result.getString(1),address,result.getInt(3));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	private static boolean wasBackupRequestedBefore(Connection c, String fileId) {
		PreparedStatement p;
		try {
			p = c.prepareStatement(getBackupRequested);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			if (result.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
	public static Backup getBackupRequested(Connection c, String fileId) {
		PreparedStatement p;
		try {
			p = c.prepareStatement(getBackupRequested);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			if (result.next()) {

				return new Backup(result.getString("file_id"),
						result.getString("filename"),
						result.getString("encrypt_key"),
						result.getInt("desired_rep_degree"),
						result.getInt("numberOfChunks"));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	public static int getDesiredRepDegree(Connection c, String fileId) {
		PreparedStatement p;
		try {
			p = c.prepareStatement(getFileStored);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			if (result.next()) {
				return result.getInt(2);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}
	public static int getMaxRepDegree(Connection c, String fileId) {
		PreparedStatement p;
		try {
			p = c.prepareStatement(getActualRepDegree);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			if (result.next()) {
				return result.getInt(1);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}
	public static boolean isFileStored(Connection c, String fileId) {
		PreparedStatement p;
		try {
			p = c.prepareStatement(getFileStored);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			return result.next() && result.getBoolean(3);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	public static ArrayList<Chunk> getAllChunksOfFile(Connection c, String fileId) {
		PreparedStatement p;
		ArrayList<Chunk> chunksInfo = new ArrayList<Chunk>();
		try {
			p = c.prepareStatement(getChunksOfFile);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			while (result.next()) {
				chunksInfo.add(new Chunk(result.getInt(1), result.getString(2), result.getInt(3)));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return chunksInfo;
	}
	public static void deleteFile(Connection c, String fileId) {
		try {
			PreparedStatement p = c.prepareStatement(deleteFileStored);
			p.setString(1, fileId);
			p.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void deleteFileFromBackupsRequested(Connection c, String fileId) {
		try {
			PreparedStatement p = c.prepareStatement(deleteFileRequested);
			p.setString(1, fileId);
			p.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

	}
	public static ArrayList<String> getFilesToDelete(Connection c, Timestamp time) {
		ArrayList<String> res = new ArrayList<String>();
		PreparedStatement p;
		try {
			p = c.prepareStatement(getFilesToDelete);
			p.setInt(1, Leases.LEASE_TIME);
			p.setTimestamp(2, time);
			ResultSet result = p.executeQuery();
			while (result.next()) {
				res.add(result.getString(1));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			return null;
		}
		return res;
	}
	public static void updateFile(Connection c, String fileId) {
		try {
			PreparedStatement p = c.prepareStatement(updateFile);
			p.setString(1, fileId);
			p.executeUpdate();
			c.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static ArrayList<Backup> getFilesToUpdate(Connection c) {
		ArrayList<Backup> res = new ArrayList<Backup>();
		PreparedStatement p;
		try {
			p = c.prepareStatement(getFilesToUpdate);
			ResultSet result = p.executeQuery();
			while (result.next()) {
				res.add(new Backup(
						result.getString("file_id"),
						result.getString("filename"),
						result.getString("encrypt_key"),
						result.getInt("desired_rep_degree"),
						result.getInt("numberOfChunks")
						));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return res;
	}
	public static ArrayList<Stored> getFilesIAmResponsible(Connection c) {
		ArrayList<Stored> res = new ArrayList<Stored>();
		PreparedStatement p;
		try {
			p = c.prepareStatement(getFiles);
			ResultSet result = p.executeQuery();
			while (result.next()) {
				res.add(new Stored(
						result.getString("file_id"), 
						true, result.getInt("desired_rep_degree")));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return res;
	}
}