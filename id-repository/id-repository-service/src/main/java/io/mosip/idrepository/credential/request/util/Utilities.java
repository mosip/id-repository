package io.mosip.idrepository.credential.request.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * General-purpose helpers for the credential-request module.
 * <p>
 * Provides identifier generation for new queue rows and API request ids.
 * </p>
 */
@Component("credReqUtilities")
public class Utilities {

	/**
	 * Generates a random UUID string for credential-request identifiers.
	 *
	 * @return string form of {@link UUID#randomUUID()}
	 */
	public String generateId() {
		return UUID.randomUUID().toString();
	}

}
