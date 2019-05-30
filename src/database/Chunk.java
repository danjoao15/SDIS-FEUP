package database;

public class Chunk {

	private Integer id;
	private String file;
	private Integer repdegree;
	private Integer size;
	
	public Chunk(Integer id,String file, Integer size) {
		this.file = file;
		this.id = id;
		this.size = size;
	}
	
	public Chunk(Integer id,String file) {
		this.id = id;
		this.file = file;
	}
	
	public Integer getchunkid() {
		return id;
	}
	
	public String getfileid() {
		return file;
	}
	
	public Integer getrepdegree() {
		return repdegree;
	}
	
	public Integer getsize() {
		return size;
	}
		
	public String getinfo() {
		return file + "_" + id;
	}

	public void setrepdegree(Integer repdegree) {
		this.repdegree = repdegree;
	}

	public void setsize(Integer size) {
		this.size = size;
	}
	

}
