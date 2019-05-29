package communication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;

import chord.AbstractPeer;
import chord.ChordManager;
import database.Backup;
import database.DBUtils;
import database.Database;
import database.Stored;
import runnableProtocols.SendGetChunk;
import runnableProtocols.SendInitDelete;
import runnableProtocols.SendPutChunk;
import utils.Confidentiality;
import utils.ReadInput;
import utils.SingletonThreadPoolExecutor;
import utils.Utils;

public class PeerMain {

	private static final int LENGTH_OF_CHUNK = 64000;
	private ChordManager chordManager;
	private Server server;
	private Database database;
	private static Path path;
	private static int storageCapacity;
	private static int usedStorage = 0;


	public PeerMain(ChordManager chordManager, Server server, Database database) {
		
		/*System.out.println(chordManager.getPeerInfo().getId() + " "
		+ new BigInteger(chordManager.getPeerInfo().getId(),16));*/
		
		this.chordManager = chordManager;
		this.server = server;
		this.database = database;
		PeerMain.storageCapacity = Integer.MAX_VALUE;
		this.server.setPeer(this);
	}

	public static void main(String[] args) {
		if(args.length < 1) {
			System.err.println("Error: Need a port Number");
			return;
		}
		Integer port = Integer.valueOf(args[0]);
		ChordManager chordManager = new ChordManager(port);
		System.out.println("Your ID: " + chordManager.getPeerInfo().getId());
		generatePath(chordManager.getPeerInfo().getId());

		Server server;
		try {
			server = new Server(new String[] {"TLS_DHE_RSA_WITH_AES_128_CBC_SHA"}, port);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		chordManager.setDatabase(new Database(chordManager.getPeerInfo().getId()));

		PeerMain peer = new PeerMain(chordManager,server, chordManager.getDatabase());

		InetAddress addr = null;
		port = null;

		if(args.length >= 3) {
			try {
				addr = InetAddress.getByName(args[1]);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return;
			}
			port = Integer.valueOf(args[2]);
		}
		peer.joinNetwork(addr, port);
		
		ReadInput readInputThread = new ReadInput(peer);
		readInputThread.run();
		server.closeConnection();
		peer.getDatabase().endConnection();
		SingletonThreadPoolExecutor.getInstance().get().shutdownNow();
		System.out.println("Program Terminated");
	}

	public void joinNetwork(InetAddress addr, Integer port) {

		if(addr != null) {
			chordManager.join(addr, port);
		}
		
		SingletonThreadPoolExecutor.getInstance().get().execute(server);
		SingletonThreadPoolExecutor.getInstance().get().execute(chordManager);

		Leases l = new Leases(this);
		SingletonThreadPoolExecutor.getInstance().get().scheduleAtFixedRate(l, 0, Leases.HALF_LEASE_TIME, Leases.LEASE_UNIT);
	}

	public ChordManager getChordManager() {
		return this.chordManager;
	}
	public Connection getConnection() {
		return this.database.getConnection();
	}

	/**
	 * Generate a file ID
	 * @param filename - the filename
	 * @return Hexadecimal md5 encoded fileID
	 * @throws IOException, NoSuchAlgorithmException
	 * */
	public String getFileID(String filename) throws IOException, NoSuchAlgorithmException {
		Path filePath = Paths.get(filename); //The filename, not FileID
		BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
		MessageDigest digest = MessageDigest.getInstance("md5");
		byte[] hash = digest.digest((filename + attr.lastModifiedTime()).getBytes(StandardCharsets.UTF_8));
		return Utils.getIdFromHash(hash, ChordManager.getM() / 8);
	}

	/**
	 * @return the p
	 */
	public static Path getPath() {
		return path;
	}

	/**
	 * @param p the p to set
	 */
	public static void setPath(Path p) {
		PeerMain.path = p;
	}

	/**
	 * Creates (if necessary) the directory where the chunks are stored
	 * @param id
	 */
	public static void generatePath(String id) {
		setPath(Paths.get("peer_" + id));
		if(!Files.exists(getPath())) {
			try {
				Files.createDirectory(getPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns false if has space to store the chunk.
	 * 
	 * */
	public static boolean capacityExceeded(int amount) {
		if(usedStorage + amount > storageCapacity) {
			return true;
		}
		//atualizar espaco usado
		usedStorage += amount;
		return false;
	}

	public static void decreaseStorageUsed(int amount) {
		usedStorage -= amount;
	}

	public void backup(String filename, Integer degree, String encryptKey) {
		String fileID;
		try {
			fileID = this.getFileID(filename);
			Utils.LOGGER.severe(filename + " - " + fileID);
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			return;
		}
		byte[] file = Utils.readFile(filename).getBytes(StandardCharsets.ISO_8859_1);
		int n = Math.floorDiv(file.length,LENGTH_OF_CHUNK) + 1;
		Confidentiality c;
		if(encryptKey == null) {
			c = new Confidentiality();
		} else {
			c = new Confidentiality(encryptKey);
		}
		encryptKey = new String(c.getKey(), StandardCharsets.ISO_8859_1);
		Backup backupRequest = new Backup(fileID,filename,encryptKey, degree, n);
		DBUtils.insertBackupRequested(database.getConnection(), backupRequest);
		int chunkNo = 0;
		while(file.length > (chunkNo + 1)*LENGTH_OF_CHUNK) {
			byte[] body = Arrays.copyOfRange(file, chunkNo * LENGTH_OF_CHUNK, (chunkNo + 1) *LENGTH_OF_CHUNK);
			body = c.encript(body);
			SendPutChunk th = new SendPutChunk(fileID, chunkNo, degree, body, this.getChordManager());
			SingletonThreadPoolExecutor.getInstance().get().execute(th);
			chunkNo++;
		}
		byte[] body = Arrays.copyOfRange(file, chunkNo * LENGTH_OF_CHUNK, file.length);
		body = c.encript(body);
		SendPutChunk th = new SendPutChunk(fileID, chunkNo, degree, body, this.getChordManager());
		SingletonThreadPoolExecutor.getInstance().get().execute(th);
	}

	public void delete(String fileID) {
		DBUtils.deleteFileFromBackupsRequested(getConnection(), fileID);
		SendInitDelete th = new SendInitDelete(fileID,this.getChordManager());
		SingletonThreadPoolExecutor.getInstance().get().execute(th);
		
	}

	public void restore(Backup backupRequest) {
		Integer totalNumChunks = backupRequest.getchunksnumber();
		for(int i = 0; i < totalNumChunks; i++) {
			SendGetChunk th = new SendGetChunk(backupRequest, i,this.getChordManager());
			SingletonThreadPoolExecutor.getInstance().get().execute(th);
		}
	}
	
	public Database getDatabase() {
		return database;
	}
	
	/**
	 * When a peer joins, tell him which files he is responsible for.
	 */
	public void sendResponsability() {
		ArrayList<Stored> filesIAmResponsible = DBUtils.getFilesIAmResponsible(this.database.getConnection());
		ArrayList<Stored> toSend = new ArrayList<Stored> ();
		AbstractPeer predecessor = this.chordManager.getPredecessor();
		if (predecessor.isNull()) return;
		if (predecessor.getId().equals(this.chordManager.getPeerInfo().getId())) return;
		for(int i = 0; i < filesIAmResponsible.size(); i++) {
			if(Utils.inBetween(this.chordManager.getPeerInfo().getId(), predecessor.getId(), filesIAmResponsible.get(i).getfile())) {
				DBUtils.updateResponsible(this.database.getConnection(),filesIAmResponsible.get(i).getfile(), false);
				toSend.add(filesIAmResponsible.get(i));
			}
		}
		if (toSend.isEmpty())return;
		System.out.println("Sending resposibility for files to peer: " + predecessor.getId());
		toSend.forEach(k->System.out.println(k.getfile()));
		String msg = MsgFactory.getResponsible(this.chordManager.getPeerInfo().getId(), toSend);
		Client.sendMessage(predecessor.getAddr(), predecessor.getPort(), msg, false);
		System.out.println("Sent responsible");
	}

}
