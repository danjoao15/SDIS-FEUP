package database;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;

import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;

import chord.PeerI;
import communication.Leases;
import utils.Utils;

public class DBUtils {

	private static final String insertFileStored = "INSERT INTO FILESSTORED "
			+ "(file_id, i_am_responsible, peer_requesting, desired_rep_degree) VALUES (?,?,?,?)";
	private static final String insertPeer = "INSERT INTO PEERS "
			+ "(peer_id,ip,port) VALUES (?,?,?)";
	private static final String insertChunkStored = "INSERT INTO CHUNKSSTORED "
			+ "(chunk_id, file_id, actual_rep_degree, size) VALUES (?,?,?,?)";
	private static final String insertBackupRequested = "INSERT INTO BACKUPSREQUESTED "
			+ "(file_id, filename, desired_rep_degree,encrypt_key,numberOfChunks) VALUES (?,?,?,?,?)";
	private static final String getFileById = "SELECT * FROM FILESSTORED "
			+ "WHERE file_id = ?";
	private static final String updatePeer = "UPDATE PEERS " + 
			"SET ip = ?, port = ? " + 
			"WHERE peer_id = ?";
	private static final String updateFileStored = "UPDATE FILESSTORED "
			+ "SET last_time_stored = CURRENT_TIMESTAMP, i_am_responsible = ?, peer_requesting = ?, desired_rep_degree = ?"
			+ "WHERE file_id = ?";
	private static final String setIamStoring = "UPDATE FILESSTORED "
			+ "SET i_am_storing = ? WHERE file_id = ?";
	private static final String updateChunkStoredRepDegree = "UPDATE CHUNKSSTORED "
			+ "SET actual_rep_degree = ? WHERE chunk_id = ? AND file_id = ?";
	private static final String updateBackupRequested = "UPDATE BACKUPSREQUESTED "
			+ "SET desired_rep_degree = ? WHERE file_id = ?";
	private static final String updateResponsible = "UPDATE FILESSTORED "
			+ "SET i_am_responsible = ? WHERE file_id = ?";
	private static final String checkStoredChunk = "SELECT * FROM CHUNKSSTORED "
			+ "WHERE file_id = ? AND chunk_id = ?";
	private static final String getPeerWhichRequested = "SELECT peer_id,ip,port FROM PEERS "
			+ "JOIN (SELECT peer_requesting FROM FILESSTORED WHERE file_id = ?) AS F ON PEERS.peer_id = F.peer_requesting";
	private static final String getBackupRequested = "SELECT * FROM BACKUPSREQUESTED WHERE file_id = ?";
	private static final String getFileStored = "SELECT file_id, desired_rep_degree, i_am_storing FROM FILESSTORED WHERE file_id = ?";
	private static final String getChunksOfFile = "SELECT chunk_id, file_id, size FROM CHUNKSSTORED WHERE file_id = ?";
	private static final String getActualRepDegree = "SELECT max(actual_rep_degree) FROM CHUNKSSTORED WHERE file_id = ?";
	private static final String deleteFileStored = "DELETE FROM FILESSTORED WHERE file_id = ?";
	private static final String deleteFileRequested = "DELETE FROM BACKUPSREQUESTED WHERE file_id = ?";
	private static final String getFilesToDelete = "SELECT file_id FROM FILESSTORED WHERE { fn TIMESTAMPADD(SQL_TSI_SECOND,?,last_time_stored)} < ?";
	private static final String updateFile = "UPDATE FILESSTORED "
			+ "SET last_time_stored = CURRENT_TIMESTAMP "
			+ "WHERE file_id = ?";
	private static final String getFilesToUpdate = "SELECT * FROM BACKUPSREQUESTED";
	private static final String getFiles = 
			"SELECT * FROM FILESSTORED "
			+ "WHERE i_am_responsible";


	public static void insertPeer(Connection conn, PeerI peerInfo) {
		try {
			PreparedStatement p = conn.prepareStatement(insertPeer);
			p.setString(1, peerInfo.getId());
			p.setString(2, peerInfo.getAddr().getHostAddress());
			p.setInt(3, peerInfo.getPort());
			p.executeUpdate();
			conn.commit();
			Utils.log("Peer " + peerInfo.getId() + " has been stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			Utils.LOGGER.info("Not a new INSERT, updating");
			updatePeer(conn, peerInfo);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	private static void updatePeer(Connection conn, PeerI peerInfo) {
		try {
			PreparedStatement p = conn.prepareStatement(updatePeer);
			p.setString(1, peerInfo.getAddr().getHostAddress());
			p.setInt(2, peerInfo.getPort());
			p.setString(3, peerInfo.getId());
			p.executeUpdate();
			conn.commit();
			Utils.log("Peer " + peerInfo.getId() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void insertStoredFile(Connection conn, FileStoredInfo fileInfo) {
		String peerRequesting = fileInfo.getPeerRequesting();
		Integer desiredRepDegree = fileInfo.getDesiredRepDegree();
		try {
			PreparedStatement p = conn.prepareStatement(insertFileStored);
			p.setString(1, fileInfo.getFileId());
			p.setBoolean(2, fileInfo.getiAmResponsible());
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
			conn.commit();
			System.out.println("File " + fileInfo.getFileId() + " has been stored in database");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			Utils.LOGGER.info("Not a new INSERT, updating");
			updateStoredFile(conn, fileInfo);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void updateStoredFile(Connection conn, FileStoredInfo fileInfo) {
		String peerRequesting = fileInfo.getPeerRequesting();
		Integer desiredRepDegree = fileInfo.getDesiredRepDegree();
		try {
			PreparedStatement p = conn.prepareStatement(updateFileStored);
			p.setBoolean(1, fileInfo.getiAmResponsible());
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
			p.setString(4, fileInfo.getFileId());

			p.executeUpdate();
			conn.commit();
			Utils.log("File " + fileInfo.getFileId() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void setIamStoring(Connection conn, String fileId, Boolean iAmStoring) {
		try {
			PreparedStatement p = conn.prepareStatement(setIamStoring);
			p.setBoolean(1, iAmStoring);
			p.setString(2, fileId);
			p.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	public static void insertStoredChunk(Connection conn, ChunkInfo chunkInfo) {
		try {
			PreparedStatement p = conn.prepareStatement(insertChunkStored);
			p.setInt(1, chunkInfo.getChunkId());
			p.setString(2, chunkInfo.getFileId());
			if (chunkInfo.getActualRepDegree() != null) {
				p.setInt(3, chunkInfo.getActualRepDegree());
			}else {
				p.setNull(3, Types.INTEGER);
			}
			p.setInt(4, chunkInfo.getSize());
			p.executeUpdate();
			conn.commit();
			Utils.log("Chunk " + chunkInfo.getFileId() + ":" + chunkInfo.getChunkId() + " has been stored");
		} catch (DerbySQLIntegrityConstraintViolationException e) {
			updateStoredChunkRepDegree(conn,chunkInfo);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void updateStoredChunkRepDegree(Connection conn, ChunkInfo chunkInfo) {
		//"UPDATE CHUNKSSTORED SET actual_rep_degree = ? WHERE file_id = ? AND chunk_id = ?"

		try {
			PreparedStatement p = conn.prepareStatement(updateChunkStoredRepDegree);
			p.setInt(1, chunkInfo.getActualRepDegree());
			p.setInt(2, chunkInfo.getChunkId());
			p.setString(3, chunkInfo.getFileId());
			p.executeUpdate();
			conn.commit();
			Utils.log("Chunk " + chunkInfo.getFileId() + ":" + chunkInfo.getChunkId() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	private static void updateBackupRequested(Connection conn, BackupRequest backupReq) {
		try {
			PreparedStatement p = conn.prepareStatement(updateBackupRequested);
			p.setInt(1, backupReq.getDesiredRepDegree());
			p.setString(2, backupReq.getFileId());
			p.executeUpdate();
			conn.commit();
			Utils.log("BackupRequest: " + backupReq.getFileId() + " has been updated");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void updateResponsible(Connection conn, String fileId, Boolean value) {
		try {
			PreparedStatement p = conn.prepareStatement(updateResponsible);
			p.setBoolean(1, value);
			p.setString(2, fileId);
			p.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public static void insertBackupRequested(Connection conn, BackupRequest backupRequest) {
		Boolean wasRequested = wasBackupRequestedBefore(conn, backupRequest.getFileId());
		if (!wasRequested) {
			// Create New
			try {
				PreparedStatement p = conn.prepareStatement(insertBackupRequested);
				p.setString(1, backupRequest.getFileId());
				p.setString(2, backupRequest.getFilename());
				p.setInt(3, backupRequest.getDesiredRepDegree());
				if (backupRequest.getEncryptKey() != null) {
					p.setString(4, backupRequest.getEncryptKey());
				}else {
					p.setNull(4, Types.VARCHAR);
				}
				p.setInt(5, backupRequest.getNumberOfChunks());
				p.executeUpdate();
				conn.commit();
				Utils.log("BackupRequest for file " + backupRequest.getFilename() + " has been stored");
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		} else {
			// Update old
			updateBackupRequested(conn,backupRequest);
		}

	}
	public static boolean amIResponsible(Connection conn, String fileId) {
		PreparedStatement p = null;
		try {
			p = conn.prepareStatement(getFileById);
			p.setString(1, fileId);
			ResultSet result = p.executeQuery();
			conn.setAutoCommit(false);
			conn.commit();
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
	public static boolean checkStoredChunk(Connection conn, ChunkInfo chunkInfo) {

		PreparedStatement p = null;
		try {
			
			p = conn.prepareStatement(checkStoredChunk);
			p.setString(1, chunkInfo.getFileId());
			p.setInt(2, chunkInfo.getChunkId());
			ResultSet result = p.executeQuery();
			conn.setAutoCommit(false);
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
	public static ArrayList<BackupRequest> getBackupsRequested(Connection conn){
		ArrayList<BackupRequest> array = new ArrayList<BackupRequest>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet res = stmt.executeQuery("SELECT file_id, filename, desired_rep_degree,encrypt_key, numberOfChunks FROM BACKUPSREQUESTED");
			while (res.next()) {
				BackupRequest currentBackupRequest = new BackupRequest(res.getString(1),
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
	public static PeerI getPeerWhichRequestedBackup(Connection conn, String fileId) {
		try {
			PreparedStatement p = conn.prepareStatement(getPeerWhichRequested);
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
	private static boolean wasBackupRequestedBefore(Connection conn, String fileId) {
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getBackupRequested);
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
	public static BackupRequest getBackupRequested(Connection conn, String fileId) {
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getBackupRequested);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			if (result.next()) {

				return new BackupRequest(result.getString("file_id"),
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
	public static int getDesiredRepDegree(Connection conn, String fileId) {
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getFileStored);
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
	public static int getMaxRepDegree(Connection conn, String fileId) {
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getActualRepDegree);
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
	public static boolean isFileStored(Connection conn, String fileId) {
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getFileStored);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			return result.next() && result.getBoolean(3);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	public static ArrayList<ChunkInfo> getAllChunksOfFile(Connection conn, String fileId) {
		PreparedStatement p;
		ArrayList<ChunkInfo> chunksInfo = new ArrayList<ChunkInfo>();
		try {
			p = conn.prepareStatement(getChunksOfFile);
			p.setString(1, fileId);
			ResultSet result =  p.executeQuery();
			while (result.next()) {
				chunksInfo.add(new ChunkInfo(result.getInt(1), result.getString(2), result.getInt(3)));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return chunksInfo;
	}
	public static void deleteFile(Connection conn, String fileId) {
		try {
			PreparedStatement p = conn.prepareStatement(deleteFileStored);
			p.setString(1, fileId);
			p.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static void deleteFileFromBackupsRequested(Connection conn, String fileId) {
		try {
			PreparedStatement p = conn.prepareStatement(deleteFileRequested);
			p.setString(1, fileId);
			p.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

	}
	public static ArrayList<String> getFilesToDelete(Connection conn, Timestamp time) {
		ArrayList<String> res = new ArrayList<String>();
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getFilesToDelete);
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
	public static void updateFile(Connection conn, String fileId) {
		try {
			PreparedStatement p = conn.prepareStatement(updateFile);
			p.setString(1, fileId);
			p.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	public static ArrayList<BackupRequest> getFilesToUpdate(Connection conn) {
		ArrayList<BackupRequest> res = new ArrayList<BackupRequest>();
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getFilesToUpdate);
			ResultSet result = p.executeQuery();
			while (result.next()) {
				res.add(new BackupRequest(
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
	public static ArrayList<FileStoredInfo> getFilesIAmResponsible(Connection conn) {
		ArrayList<FileStoredInfo> res = new ArrayList<FileStoredInfo>();
		PreparedStatement p;
		try {
			p = conn.prepareStatement(getFiles);
			ResultSet result = p.executeQuery();
			while (result.next()) {
				res.add(new FileStoredInfo(
						result.getString("file_id"), 
						true, result.getInt("desired_rep_degree")));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return res;
	}
}