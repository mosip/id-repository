package io.mosip.credential.request.generator.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyLoader {

	/** The credential request type. */
	@Value("${credential.request.type}")
	public String credentialRequestType;

	@Value("${credential.batch.core.pool.size:10}")
	public int corePoolSize;

	@Value("${credential.batch.max.pool.size:10}")
	public int maxPoolSize;

	@Value("${credential.batch.queue.capacity:10}")
	public int queueCapacity;

	@Value("${credential.batch.page.size:10}")
	public int pageSize;

}
