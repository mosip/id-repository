package io.mosip.credential.request.generator.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyLoader {

	@Value("${credential.request.process.locktimeout}")
	public String processLockTimeout;

	@Value("${credential.request.reprocess.locktimeout}")
	public String reProcessLockTimeout;

}
