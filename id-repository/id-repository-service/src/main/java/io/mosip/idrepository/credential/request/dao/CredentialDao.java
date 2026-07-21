package io.mosip.idrepository.credential.request.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.credential.request.repository.CredentialRepository;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Data access for the credential issuance queue ({@code credential_transaction}).
 *
 * @see io.mosip.idrepository.pipeline.CredentialIssuanceProcessor
 */
@Component
public class CredentialDao {

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialDao.class);

	@Autowired
	private CredentialRepository credentialRepo;

	public Page<CredentialEntity> findByStatusCode(String statusCode, Pageable pageable) {
		return credentialRepo.findByStatusCode(statusCode, pageable);
	}

	public Page<CredentialEntity> findByStatusCodeWithEffectiveDtimes(String statusCode, LocalDateTime effectiveDTimes,
			Pageable pageable) {
		return credentialRepo.findByStatusCodeWithEffectiveDtimes(statusCode, effectiveDTimes, pageable);
	}

	public void save(CredentialEntity credential) {
		credentialRepo.save(credential);
		LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "requestId = " + credential.getRequestId(),
				"Record saved successfully.");
	}

	public Optional<CredentialEntity> findById(String requestId) {
		return credentialRepo.findById(requestId);
	}
}
