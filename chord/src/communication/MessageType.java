package communication;

public enum MessageType {

	ASK("ASK"),
	DELETE("DELETE"),
	INITDELETE("INITDELETE"),
	SUCCESSORS("SUCCESSORS"),
	PUTCHUNK("PUTCHUNK"),
	KEEPCHUNK("KEEPCHUNK"),
	PING("PING"),
	OK("OK"),
	NOTIFY("NOTIFY"),
	LOOKUP("LOOKUP"),
	PREDECESSOR("PREDECESSOR"),
	REBUILD("REBUILD"),
	SUCCESSOR("SUCCESSOR"),
	STORED("STORED"),
	CONFIRMSTORED("CONFIRMSTORED"),
	GETCHUNK("GETCHUNK"),
	CHUNK("CHUNK"),
	UPDATETIME("UPDATETIME"),
	RESPONSIBLE("RESPONSIBLE");


	private String type;

	MessageType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

}
