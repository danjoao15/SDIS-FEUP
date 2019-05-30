package chord;

import java.net.InetAddress;


public abstract class AbstractPeer {

	protected String id;
	protected InetAddress address;
	protected Integer port;
	
	public abstract boolean isNull();
	
	public abstract InetAddress getAddress();
	public abstract Integer getPort();
	public abstract String getId();
	public abstract String[] asArray();
}