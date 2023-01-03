/**
 * 
 */
package io.mosip.idrepository.identity.entity;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Neha Farheen
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
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
			this.uinHash = splitUinHash(uinHash);
			this.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileDto setNewCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(newCbeff)) {
			this.uinHash = splitUinHash(uinHash);
			this.newCbeffRefId = fileRefId;
		}
		return this;
	}

	private static String splitUinHash(String uinHash) {
		String substringHash = StringUtils.substringAfter(uinHash, "_");
		return StringUtils.isBlank(substringHash) ? uinHash : substringHash;
	}

	public AnonymousProfileDto setRegId(String regId) {
		this.regId = regId;
		return this;
	}

	

}
