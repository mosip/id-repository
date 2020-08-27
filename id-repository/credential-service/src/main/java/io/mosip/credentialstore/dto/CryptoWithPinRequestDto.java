/*
 * 
 * 
 * 
 * 
 */
package io.mosip.credentialstore.dto;





import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Crypto-With-Pin-Request model
 * 
 * @author Mahammed Taheer
 *
 * @since 1.2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CryptoWithPinRequestDto {
    
    /**
	 * Data in String to encrypt/decrypt
	 */
	

    private String data;
    
	/**
	 * Pin to be used for encrypt/decrypt
	 */

	private String userPin;
}
