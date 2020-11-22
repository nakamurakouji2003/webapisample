package jp.ac.hal.response;

import java.io.Serializable;

public class FileStorageResponse implements Serializable {

	long uploadId = 0;
	long registPerson = 0;
	String registDate = null;
	String registLocation = null;
	String version = null;
	String storageStartDate = null;
	String storageEndDate = null;
	String registGroupName = null;
	String registGroupPassword = null;
	String jsessionId = null;
	String invalidFlag = null;

    public FileStorageResponse() {

    }

    public FileStorageResponse(
    		long uploadId, long registPerson, String registDate,
    		String registLocation, String version, String storageStartDate,
    		String storageEndDate, String registGroupName, String registGroupPassword,
    		String jsessionId, String invalidFlag) {

		this.uploadId = uploadId;
		this.registPerson = registPerson;
		this.registDate = registDate;
		this.registLocation = registLocation;
		this.version = version;
		this.storageStartDate = storageStartDate;
		this.storageEndDate = storageEndDate;
		this.registGroupName = registGroupName;
		this.registGroupPassword = registGroupPassword;
		this.jsessionId = jsessionId;
		this.invalidFlag = invalidFlag;

    }

	public long getUploadId() {
		return uploadId;
	}
	public void setUploadId(long uploadId) {
		this.uploadId = uploadId;
	}
	public long getRegistPerson() {
		return registPerson;
	}
	public void setRegistPerson(long registPerson) {
		this.registPerson = registPerson;
	}
	public String getRegistDate() {
		return registDate;
	}
	public void setRegistDate(String registDate) {
		this.registDate = registDate;
	}
	public String getRegistLocation() {
		return registLocation;
	}
	public void setRegistLocation(String registLocation) {
		this.registLocation = registLocation;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getStorageStartDate() {
		return storageStartDate;
	}
	public void setStorageStartDate(String storageStartDate) {
		this.storageStartDate = storageStartDate;
	}
	public String getStorageEndDate() {
		return storageEndDate;
	}
	public void setStorageEndDate(String storageEndDate) {
		this.storageEndDate = storageEndDate;
	}
	public String getRegistGroupName() {
		return registGroupName;
	}
	public void setRegistGroupName(String registGroupName) {
		this.registGroupName = registGroupName;
	}
	public String getRegistGroupPassword() {
		return registGroupPassword;
	}
	public void setRegistGroupPassword(String registGroupPassword) {
		this.registGroupPassword = registGroupPassword;
	}
	public String getJsessionId() {
		return jsessionId;
	}
	public void setJsessionId(String jsessionId) {
		this.jsessionId = jsessionId;
	}
	public String getInvalidFlag() {
		return invalidFlag;
	}
	public void setInvalidFlag(String invalidFlag) {
		this.invalidFlag = invalidFlag;
	}
}
