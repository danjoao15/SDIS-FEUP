package database;

public class BackupRequest {

	private String fileId;
	private String filename;
	private String encryptKey;
	private Integer numberOfChunks; 
	private Integer desiredRepDegree;

	public BackupRequest(String fileId, String filename, Integer desiredRepDegree) {
		this.fileId = fileId;
		this.filename = filename;
		this.encryptKey = null;
		this.setNumberOfChunks(null);
		this.desiredRepDegree = desiredRepDegree;
	}
	
	public BackupRequest(String fileId, String filename, 
			String encryptKey, Integer desiredRepDegree, 
			Integer numberOfChunks) {
		this.fileId = fileId;
		this.filename = filename;
		this.encryptKey = encryptKey;
		this.setNumberOfChunks(numberOfChunks);
		this.desiredRepDegree = desiredRepDegree;
	}
	
	public Integer getDesiredRepDegree() {
		return desiredRepDegree;
	}

	public String getFileId() {
		return fileId;
	}

	public String getFilename() {
		return filename;
	}

	public String getEncryptKey() {
		return encryptKey;
	}

	/**
	 * @return the numberOfChunks
	 */
	public Integer getNumberOfChunks() {
		return numberOfChunks;
	}

	/**
	 * @param numberOfChunks the numberOfChunks to set
	 */
	public void setNumberOfChunks(Integer numberOfChunks) {
		this.numberOfChunks = numberOfChunks;
	}

}
