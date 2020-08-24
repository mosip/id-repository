/*
 * 
 * 
 * 
 * 
 */
package io.mosip.credentialstore.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Crypto-With-Pin-Response model
 * 
 * @author Mahammed Taheer
 *
 * @since 1.2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

public class EncryptWithPinResponseDto {

	private String data;
}
