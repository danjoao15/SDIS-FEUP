package chordSetup;

import java.net.InetAddress;
import java.net.UnknownHostException;
public class Peer extends AbstractPeer {

	public Peer(String id, InetAddress address, Integer port) {
		this.id = id;
		this.address = address;
		this.port = port;
	}

	public Peer(String str) {
		str = str.trim();
		String[] attr = str.split("\r\n");

		attr = attr[1].split(" ");
		this.id = attr[0];

		try {
			this.address = InetAddress.getByName(attr[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}
		this.port = Integer.valueOf(attr[2]);
		
	}

	

	@Override
	public String[] asArray() {
		return new String[]{id,address.getHostAddress(),port.toString()}; 
	}

	public InetAddress getAddress() {
		return address;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}
	
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	
	
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Peer))return false;
	    Peer otherPeer = (Peer)other;
	    if (otherPeer.getId().equals(this.id)) {
	    	return true;
	    }
	    return false;
	}
	
	
	@Override
	public boolean isNull() {
		return false;
	}

}
