package io.mosip.credential.request.generator.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class Utilities {
	/**
	 * Generate id.
	 *
	 * @return the string
	 */
	public String generateId() {
		return UUID.randomUUID().toString();
	}

}
