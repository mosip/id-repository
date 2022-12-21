/**
 * 
 */
package io.mosip.idrepository.identity.entity;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Neha Farheen
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousProfileDto {
	
	private byte[] oldUinData;

	private byte[] newUinData;
	
	private String regId;

	private String oldCbeff;

	private String newCbeff;
	
	private String uinHash;
	
	private String oldCbeffRefId;
	
	private String newCbeffRefId;
	
	
	public AnonymousProfileDto setOldUinData(byte[] oldUinData) {
		this.oldUinData = oldUinData;
		return this;
	}

	public AnonymousProfileDto setNewUinData(byte[] newUinData) {
		this.newUinData = newUinData;
		return this;
	}

	public AnonymousProfileDto setOldCbeff(String oldCbeff) {
		this.oldCbeff = oldCbeff;
		return this;
	}
	
	public boolean isOldCbeffPresent() {
		return Objects.nonNull(this.oldCbeff);
	} 

	public AnonymousProfileDto setNewCbeff(String newCbeff) {
		this.newCbeff = newCbeff;
		return this;
	}

	public boolean isNewCbeffPresent() {
		return Objects.nonNull(this.newCbeff);
	}

	public AnonymousProfileDto setOldCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(oldCbeff)) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			this.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileDto setNewCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(newCbeff)) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			this.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.newCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileDto setRegId(String regId) {
		if (Objects.nonNull(this.regId) && !this.regId.contentEquals(regId))
			resetData();
		this.regId = regId;
		return this;
	}

	private void resetData() {
		this.oldUinData = null;
		this.newUinData = null;
		this.oldCbeff = null;
		this.newCbeff = null;
		this.uinHash = null;
		this.newCbeffRefId = null;
		this.oldCbeffRefId = null;
		this.regId = null;
	}

}
