package database;

public class Backup {

	private String id;
	private String name;
	private String key;
	private Integer chunks; 
	private Integer repdegree;

	public Backup(String id, String name, Integer repdegree) {
		this.id = id;
		this.name = name;
		this.key = null;
		this.setchunksnum(null);
		this.repdegree = repdegree;
	}
	
	public Backup(String id, String name, String key, Integer repdegree, Integer chunks) {
		this.id = id;
		this.name = name;
		this.key = key;
		this.setchunksnum(chunks);
		this.repdegree = repdegree;
	}
	
	public Integer getrepdegree() {
		return repdegree;
	}

	public String getid() {
		return id;
	}

	public String getname() {
		return name;
	}

	public String getkey() {
		return key;
	}

	public Integer getchunksnumber() {
		return chunks;
	}

	public void setchunksnum(Integer i) {
		this.chunks= i;
	}

}
