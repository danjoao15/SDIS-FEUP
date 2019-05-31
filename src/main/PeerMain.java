package main;

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

import chordSetup.AbstractPeer;
import chordSetup.ManageChord;
import database.Backup;
import database.DatabaseManager;
import database.Database;
import database.Stored;
import protocols.*;
import util.*;

public class PeerMain {

	private static final int CHUNKLENGTH = 64000;
	private ManageChord chordManager;
	private Server server;
	private Database database;
	private static Path path;
	private static int storageCapacity;
	private static int usedStorage = 0;


	public PeerMain(ManageChord chordManager, Server server, Database database) {

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
		ManageChord chordManager = new ManageChord(port);
		System.out.println("peer id - " + chordManager.getPeerInfo().getId());
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

		Interface readInputThread = new Interface(peer);
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

	public ManageChord getChordManager() {
		return this.chordManager;
	}
	public Connection getConnection() {
		return this.database.getConnection();
	}

	public String getFileID(String filename) throws IOException, NoSuchAlgorithmException {
		Path filePath = Paths.get(filename);
		BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
		MessageDigest digest = MessageDigest.getInstance("md5");
		byte[] hash = digest.digest((filename + attr.lastModifiedTime()).getBytes(StandardCharsets.UTF_8));
		return Loggs.getIdFromHash(hash, ManageChord.getM() / 8);
	}

	public static Path getPath() {
		return path;
	}

	public static void setPath(Path p) {
		PeerMain.path = p;
	}

	public static void generatePath(String id) {
		setPath(Paths.get("peer_" + id));
		if(!Files.exists(getPath())) {
			try {
				Files.createDirectory(getPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Path pathFile = Paths.get(getPath().toString() + "/backup/");
		if(!Files.exists(pathFile)) {
			try {
				Files.createDirectory(pathFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static boolean capacityExceeded(int amount) {
		if(usedStorage + amount > storageCapacity) {
			return true;
		}
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
			Loggs.LOG.severe(filename + " - " + fileID);
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			return;
		}
		byte[] file = Loggs.read(filename).getBytes(StandardCharsets.ISO_8859_1);
		int n = Math.floorDiv(file.length,CHUNKLENGTH) + 1;
		Encryption c;
		if(encryptKey == null) {
			c = new Encryption();
		} else {
			c = new Encryption(encryptKey);
		}
		encryptKey = new String(c.getKey(), StandardCharsets.ISO_8859_1);
		Backup backupRequest = new Backup(fileID,filename,encryptKey, degree, n);
		DatabaseManager.requestBackup(database.getConnection(), backupRequest);
		int chunkNo = 0;
		while(file.length > (chunkNo + 1)*CHUNKLENGTH) {
			byte[] body = Arrays.copyOfRange(file, chunkNo * CHUNKLENGTH, (chunkNo + 1) *CHUNKLENGTH);
			body = c.encriptation(body);
			Putchunk th = new Putchunk(fileID, chunkNo, degree, body, this.getChordManager());
			SingletonThreadPoolExecutor.getInstance().get().execute(th);
			chunkNo++;
		}
		byte[] body = Arrays.copyOfRange(file, chunkNo * CHUNKLENGTH, file.length);
		body = c.encriptation(body);
		Putchunk th = new Putchunk(fileID, chunkNo, degree, body, this.getChordManager());
		SingletonThreadPoolExecutor.getInstance().get().execute(th);
	}

	public void restore(Backup backupRequest) {
		Integer totalNumChunks = backupRequest.getchunksnumber();
		for(int i = 0; i < totalNumChunks; i++) {
			Getchunk th = new Getchunk(backupRequest, i,this.getChordManager());
			SingletonThreadPoolExecutor.getInstance().get().execute(th);
		}
	}

	public Database getDatabase() {
		return database;
	}

	public void sendResponsability() {
		ArrayList<Stored> filesIAmResponsible = DatabaseManager.getFiles(this.database.getConnection());
		ArrayList<Stored> toSend = new ArrayList<Stored> ();
		AbstractPeer predecessor = this.chordManager.getPredecessor();
		if (predecessor.isNull()) return;
		if (predecessor.getId().equals(this.chordManager.getPeerInfo().getId())) return;
		for(int i = 0; i < filesIAmResponsible.size(); i++) {
			if(Loggs.inTheMiddle(this.chordManager.getPeerInfo().getId(), predecessor.getId(), filesIAmResponsible.get(i).getfile())) {
				DatabaseManager.updateResponsible(this.database.getConnection(),filesIAmResponsible.get(i).getfile(), false);
				toSend.add(filesIAmResponsible.get(i));
			}
		}
		if (toSend.isEmpty())return;
		System.out.println("sending resposibility for files to peer " + predecessor.getId());
		toSend.forEach(k->System.out.println(k.getfile()));
		String msg = CreateMsg.getResponsible(this.chordManager.getPeerInfo().getId(), toSend);
		Client.sendMsg(predecessor.getAddress(), predecessor.getPort(), msg, false);
		System.out.println("sent responsible");
	}

}
