package database;

public class FileStoredInfo {

	private String fileId;
	private Boolean iAmResponsible;
	private String peerRequesting;
	private Integer desiredRepDegree;
	
	public FileStoredInfo(String fileId, Boolean iAmResponsible, Integer desiredRepDegree) {
		this.fileId = fileId;
		this.iAmResponsible = iAmResponsible;
		this.desiredRepDegree = desiredRepDegree;
	}

	public FileStoredInfo(String fileId, Boolean iAmResponsible){
		this.fileId = fileId;
		this.iAmResponsible = iAmResponsible;
	}

	public String getFileId() {
		return fileId;
	}

	public Boolean getiAmResponsible() {
		return iAmResponsible;
	}

	public String getPeerRequesting() {
		return peerRequesting;
	}

	public void setPeerRequesting(String peerId) {
		this.peerRequesting = peerId;
	}

	public void setDesiredRepDegree(int desiredRepDegree) {
		this.desiredRepDegree = desiredRepDegree;
	}
	
	public Integer getDesiredRepDegree() {
		return this.desiredRepDegree;
	}

	
	
}
