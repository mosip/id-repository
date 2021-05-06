package io.mosip.idrepository.credentialsfeeder.step;

import javax.annotation.PostConstruct;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestEntity;
import io.mosip.idrepository.credentialsfeeder.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class CredentialRequestsReader - Creates entities based on chunk size.
 * Start and end sequence for entity Id is provide via configuration.
 * Implements {@code ItemReader}.
 *
 * @author Manoj SP
 */
@Component
public class CredentialRequestsReader implements ItemReader<CredentialRequestEntity> {
	
	/** The mosip logger. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(CredentialRequestsReader.class);

	@PostConstruct
	public void initialize() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	@Override
	public CredentialRequestEntity read() {
		//TODO
		return null;
	}

}
