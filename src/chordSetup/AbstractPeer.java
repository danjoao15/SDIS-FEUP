package chordSetup;

import java.net.InetAddress;

public abstract class AbstractPeer {
	protected String id;
	protected InetAddress address;
	protected Integer port;

	public abstract String getId();
	public abstract InetAddress getAddress();
	public abstract Integer getPort();
	public abstract String[] asArray();
	public abstract boolean isNull();
}
