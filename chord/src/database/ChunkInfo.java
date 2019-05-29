package database;

public class ChunkInfo {

	private Integer chunkId;
	private String fileId;
	private Integer actualRepDegree;
	private Integer size;
	
	public ChunkInfo(Integer chunkId,String fileId, Integer size) {
		this.fileId = fileId;
		this.chunkId = chunkId;
		this.size = size;
	}
	
	public ChunkInfo(Integer chunkId,String fileId) {
		this.chunkId = chunkId;
		this.fileId = fileId;
	}
	
	public Integer getChunkId() {
		return chunkId;
	}
	
	public String getFileId() {
		return fileId;
	}

	public Integer getActualRepDegree() {
		return actualRepDegree;
	}

	public void setActualRepDegree(Integer actualRepDegree) {
		this.actualRepDegree = actualRepDegree;
	}
	public String getFilename() {
		return fileId + "_" + chunkId;
	}

	public void setSize(Integer size) {
		this.size = size;
	}
	public Integer getSize() {
		return size;
	}
	
}
