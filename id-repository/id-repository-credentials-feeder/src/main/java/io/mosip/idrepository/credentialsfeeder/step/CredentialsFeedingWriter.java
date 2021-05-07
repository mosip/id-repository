package io.mosip.idrepository.credentialsfeeder.step;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestStatusEntity;
import io.mosip.idrepository.credentialsfeeder.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using credential requests.
 * Implements {@code ItemWriter}.
 *
 * @author Manoj SP
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<CredentialRequestStatusEntity> {

	Logger mosipLogger = IdRepoLogger.getLogger(CredentialsFeedingWriter.class);


	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	@Transactional
	public void write(List<? extends CredentialRequestStatusEntity> entitiesCompositeList) throws Exception {
		
	}

}
