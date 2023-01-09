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
@Data
public class AnonymousProfileDto {
	
	private byte[] oldUinData;

	private byte[] newUinData;
	
	private String regId;

	private String oldCbeff;

	private String newCbeff;
	
	private String uinHash;
	
	private String oldCbeffRefId;
	
	private String newCbeffRefId;

}
