package database;

public class Stored {

	private String file;
	private Boolean chunkstored;
	private String peer;
	private Integer repdegree;
	
	public Stored(String file, Boolean chunkstored, Integer repdegree) {
		this.file = file;
		this.chunkstored = chunkstored;
		this.repdegree = repdegree;
	}

	public Stored(String file, Boolean chunkstored){
		this.file = file;
		this.chunkstored = chunkstored;
	}

	public String getfile() {
		return file;
	}

	public Boolean getchunkstored() {
		return chunkstored;
	}

	public String getpeer() {
		return peer;
	}
	
	public Integer getrepdegree() {
		return this.repdegree;
	}

	public void setpeer(String peer) {
		this.peer = peer;
	}

	public void setrepdegree(int repdegree) {
		this.repdegree = repdegree;
	}

	
	
}
