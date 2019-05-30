package chord;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import communication.Client;
import communication.MsgFactory;
import communication.MsgType;
import database.Database;
import util.SingletonThreadPoolExecutor;
import util.Utils;


public class ManageChord implements Runnable {

	private static final int M = 32;
	private static final int NEXT_PEERS_MAX_SIZE = 5;
	private PeerI peerI;
	private ArrayList<PeerI> fingerTable = new ArrayList<PeerI>();
	private AbstractPeer predecessor;

	private Deque<PeerI> nextPeers;

	private String askMessage;
	private String successorMessage;
	private Database database;

	public ManageChord(Integer port) {

		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}

		MessageDigest msgDigest;
		try {
			msgDigest = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}

		byte[] hash = msgDigest.digest(("" + addr + port).getBytes(StandardCharsets.ISO_8859_1));
		String id = Utils.getIdFromHash(hash, M/8);
		this.setPeerInfo(new PeerI(id, addr, port));

		askMessage = MsgFactory.getFirstLine(MsgType.ASK, "1.0", this.getPeerInfo().getId());
		successorMessage = MsgFactory.getFirstLine(MsgType.SUCCESSOR, "1.0", this.getPeerInfo().getId());
		nextPeers = new ConcurrentLinkedDeque<PeerI>();

		for (int i = 0; i < getM(); i++) {
			fingerTable.add(getPeerInfo());
		}
		setNextPeer(getPeerInfo());
		predecessor = new NullPeer();
	}

	@Override
	public void run() {

		Stabilize RebuildThread = new Stabilize(this);
		SingletonThreadPoolExecutor.getInstance().get().scheduleAtFixedRate(RebuildThread, 10, 2500, TimeUnit.MILLISECONDS);

		CheckPredecessor checkPredecessorThread = new CheckPredecessor(this);
		SingletonThreadPoolExecutor.getInstance().get().scheduleAtFixedRate(checkPredecessorThread, 1000, 10000, TimeUnit.MILLISECONDS);

		FingerTableFix fixFingerTableThread = new FingerTableFix(this);
		SingletonThreadPoolExecutor.getInstance().get().scheduleAtFixedRate(fixFingerTableThread, 2000, 5000, TimeUnit.MILLISECONDS);

	}

	public void join(InetAddress addr, int port) {
		String lookupMessage = MsgFactory.getFirstLine(MsgType.LOOKUP, "1.0",getPeerInfo().getId());
		lookupMessage = MsgFactory.appendLine(lookupMessage, new String[]{""+getPeerInfo().getId()});
		String response = Client.sendMessage(addr, port, lookupMessage, true);

		PeerI nextPeer = new PeerI(response);

		while (response.startsWith("Ask")) {
			response = Client.sendMessage(nextPeer.getAddr(), nextPeer.getPort(), lookupMessage, true);
			if (response == null) {
				System.err.println("Could not join the network");
				Utils.LOG.severe("Could not join the network");
				return;
			}
			nextPeer = new PeerI(response);
		}
		setNextPeer(nextPeer);
	}

	public void setNextPeer(PeerI nextPeer) {
		if (nextPeer == null || nextPeer.isNull()) return;
		this.getFingerTable().set(0, nextPeer);
		PeerI first = this.nextPeers.peek();
		if (first != null) {
			if (first.getId().equals(nextPeer.getId())) return;
		}
		nextPeers.push(nextPeer);
	}

	public String lookup(String key) {
		if (!this.predecessor.isNull()) {
			if (Utils.inTheMiddle(this.predecessor.getId(), this.getPeerInfo().getId(), key)) {
				return MsgFactory.appendLine(successorMessage, this.getPeerInfo().asArray());
			}
		}
		if (fingerTable.size() != 0){ 
			PeerI temp = fingerTable.get(0);
			if(temp == null) {
				//System.out.println("Weird Stuff");
			}
			else {
				String temp1 = temp.getId();
				if (Utils.inTheMiddle(peerI.getId(),
						temp1,
						key)) {
					return MsgFactory.appendLine(successorMessage, this.getFingerTable().get(0).asArray());
				}
			}
		} else {
			//System.err.println("Weird Stuff");
		}
		String closestPrecedingNode = closestPrecedingNode(key);
		if (closestPrecedingNode != null) return closestPrecedingNode;
		return MsgFactory.appendLine(askMessage, this.getFingerTable().get(getM() - 1).asArray());
	}

	public String closestPrecedingNode(String key) {
		String fingersMsg = null;
		String nextPeersMsg = null;
		String fingerTableHighestId = null;
		String nextPeersHighestId = null;
		Iterator<PeerI> it = nextPeers.descendingIterator();
		AbstractPeer currentPeer;
		for (int i = getM() - 1; i > 0; i--) {
			currentPeer = this.getFingerTable().get(i);
			if (Utils.inTheMiddle(this.getPeerInfo().getId(), key, currentPeer.getId())) {
				fingerTableHighestId = currentPeer.getId();
				fingersMsg = MsgFactory.appendLine(askMessage, currentPeer.asArray());
				break;
			}
		}
		while(it.hasNext()) {
			currentPeer = it.next();
			if (Utils.inTheMiddle(this.getPeerInfo().getId(), key, currentPeer.getId())) {
				nextPeersHighestId = currentPeer.getId();
				nextPeersMsg = MsgFactory.appendLine(askMessage, currentPeer.asArray());
				break;
			}
		}
		if (fingerTableHighestId != null || nextPeersHighestId != null) {
			String highest = Utils.highest(fingerTableHighestId, nextPeersHighestId);
			if (highest == fingerTableHighestId) return fingersMsg;
			return nextPeersMsg;
		}
		return null;
	}

	public void printNextPeers() {
		Iterator<PeerI> it = nextPeers.iterator();
		Utils.LOG.info("Printing next peers");
		while(it.hasNext()) {
			Utils.LOG.info(it.next().getId());
		}
		Utils.LOG.info("------------");
	}

	public void notify(PeerI newSuccessor) {
		String message = MsgFactory.getNotify(this.getPeerInfo().getId(), this.getPeerInfo().getPort());
		String response = Client.sendMessage(newSuccessor.getAddr(), newSuccessor.getPort(), message, true);
		if (response == null) {
			Utils.LOG.warning("Next peer dropped");
			this.popNextPeer();
		}
	}

	public PeerI getChunkOwner(String key) {
		if (Utils.inTheMiddle(this.predecessor.getId(), this.getPeerInfo().getId(), key)) {
			return this.peerI;
		}
		if (Utils.inTheMiddle(this.getPeerInfo().getId(), this.getFingerTable().get(0).getId(), key)) {
			return this.getFingerTable().get(0);
		}

		InetAddress addr = null;
		int port = -1;

		for (int i = getM() - 1; i > 0; i--) {
			if (Utils.inTheMiddle(this.getPeerInfo().getId(), key, this.getFingerTable().get(i).getId())) {
				addr = this.getFingerTable().get(i).getAddr();
				port = this.getFingerTable().get(i).getPort();
			}
		}
		if(port == -1) {
			addr = this.getFingerTable().get(getM() - 1).getAddr();
			port = this.getFingerTable().get(getM() - 1).getPort();
		}

		String lookupMessage = MsgFactory.getLookup(this.peerI.getId(), key);
		String response = Client.sendMessage(addr, port, lookupMessage, true);

		PeerI owner = new PeerI(response);

		while (response.startsWith("Ask")) {
			response = Client.sendMessage(owner.getAddr(), owner.getPort(), lookupMessage, true);
			if (response == null) {
				System.err.println("Could not join the network");
				Utils.LOG.severe("Could not join the network");
				return null;
			}
			response = response.trim();
			owner = new PeerI(response);
		}

		return owner;
	}

	public List<PeerI> getNextPeers() {
		List<PeerI> nextPeersArray = new ArrayList<PeerI>();
		int i = 0;
		Iterator<PeerI> it = nextPeers.iterator();
		while(it.hasNext() && i++ < NEXT_PEERS_MAX_SIZE-1) {
			PeerI nextPeer = it.next();
			nextPeersArray.add(nextPeer);
		}
		return nextPeersArray;
	}

	public void popNextPeer() {
		nextPeers.pop();
		this.getFingerTable().set(0,nextPeers.peekFirst());
		try {
			String keyToLookup = FingerTableFix.getKey(this.getPeerInfo().getId(), 0);
			String lookupMessage = MsgFactory.getLookup(this.getPeerInfo().getId(), keyToLookup);
			String response = this.lookup(keyToLookup);
			response = response.trim();
			PeerI info = new PeerI(response);
			while(response.startsWith(MsgType.ASK.getType())) {
				response = Client.sendMessage(info.getAddr(), info.getPort(), lookupMessage, true);
				if (response == null) return;
				info = new PeerI(response);
			}
			this.getFingerTable().set(0, info);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PeerI getNextPeer() {
		return nextPeers.peek();
	}

	public static int getM() {
		return M;
	}

	public PeerI getSuccessor(int index) {
		return this.fingerTable.get(index);
	}

	public AbstractPeer getPredecessor() {
		return this.predecessor;
	}

	public void setPredecessor(AbstractPeer nullPeerInfo) {
		this.predecessor = nullPeerInfo;
	}

	public PeerI getPeerInfo() {
		return peerI;
	}

	public void setPeerInfo(PeerI peerI) {
		this.peerI = peerI;
	}

	public ArrayList<PeerI> getFingerTable() {
		return fingerTable;
	}

 void setFingerTable(ArrayList<PeerI> fingerTable) {
		this.fingerTable = fingerTable;
	}

	public Database getDatabase() {
		return database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public void updateNextPeers(Deque<PeerI> peersReceived) {
		PeerI nextPeer = nextPeers.pop();
		nextPeers.clear();
		nextPeers = peersReceived;
		nextPeers.push(nextPeer);
	}

	public void stabilize(AbstractPeer x) {
		if (x.isNull()) return;
		PeerI successor = getNextPeer();
		PeerI potentialSuccessor = (PeerI) x;

		if (Utils.inTheMiddle(this.peerI.getId(),
				successor.getId(),
				potentialSuccessor.getId())) {
			setNextPeer(potentialSuccessor);
		}
		
	}
}
