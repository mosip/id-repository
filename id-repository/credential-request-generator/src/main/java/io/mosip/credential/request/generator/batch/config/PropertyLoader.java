package io.mosip.credential.request.generator.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyLoader {

	/** The credential request type. */
	@Value("${credential.request.type}")
	public String credentialRequestType;

}
