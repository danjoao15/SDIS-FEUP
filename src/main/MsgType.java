package main;

public enum MsgType {

	ASK("ASK"),
	CONFIRMSTORED("CONFIRMSTORED"),
	CHUNK("CHUNK"),
	GETCHUNK("GETCHUNK"),
	SUCCESSORS("SUCCESSORS"),
	PUTCHUNK("PUTCHUNK"),
	KEEPCHUNK("KEEPCHUNK"),
	PING("PING"),
	OK("OK"),
	NOTIFY("NOTIFY"),
	LOOKUP("LOOKUP"),
	PREDECESSOR("PREDECESSOR"),
	STABILIZE("STABILIZE"),
	SUCCESSOR("SUCCESSOR"),
	STORED("STORED"),
	RESPONSIBLE("RESPONSIBLE"),
	UPDATETIME("UPDATETIME");

	private String type;

	MsgType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

}
