package io.mosip.credential.request.generator.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class CredentialDao {

	@Value("${credential.batch.status:NEW}")
	private String status;

	@Value("${credential.batch.page.size:100}")
	private int pageSize;

	@Value("${credential.request.reprocess.statuscodes}")
	private String reprocessStatusCodes;

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialDao.class);

	/** The crdential repo. */
	@Autowired
	private CredentialRepositary<CredentialEntity, String> credentialRepo;

	public void update(String batchId, List<CredentialEntity> credentialEntities) {
		credentialRepo.saveAll(credentialEntities);
		LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "batchid = " + batchId,
				"Record updated successfully. Total records : " + credentialEntities.size());
	}

	public List<CredentialEntity> getCredentials(String batchId) {
		LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "batchid = " + batchId,
				"Inside getCredentials() method");
		Sort sort = Sort.by(Sort.Direction.ASC, "createDateTime");
		Pageable pageable = PageRequest.of(0, pageSize, sort);
		List<CredentialEntity> credentialEntities = new ArrayList<>();
		Page<CredentialEntity> pagecredentialEntities = credentialRepo.findCredentialByStatusCode(status, pageable);
		if (pagecredentialEntities != null && pagecredentialEntities.getContent() != null
				&& !pagecredentialEntities.getContent().isEmpty()) {
			credentialEntities = pagecredentialEntities.getContent();
		}

		LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "batchid = " + batchId,
				"Total records picked from credential_transaction table for processing is "
						+ credentialEntities.size());

		return credentialEntities;
	}

	public List<CredentialEntity> getCredentialsForReprocess(String batchId) {
		LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "batchid = " + batchId,
				"Inside getCredentialsForReprocess() method");
		Sort sort = Sort.by(Sort.Direction.ASC, "updateDateTime");
		Pageable pageable = PageRequest.of(0, pageSize, sort);
		String[] statusCodes = reprocessStatusCodes.split(",");
		List<CredentialEntity> credentialEntities = new ArrayList<>();
		Page<CredentialEntity> pagecredentialEntities = credentialRepo.findCredentialByStatusCodes(statusCodes,
				pageable);
		if (pagecredentialEntities != null && pagecredentialEntities.getContent() != null
				&& !pagecredentialEntities.getContent().isEmpty()) {
			credentialEntities = pagecredentialEntities.getContent();
		}

		LOGGER.info(IdRepoSecurityManager.getUser(), "CredentialDao", "batchid = " + batchId,
				"Total records picked from credential_transaction table for reprocessing is "
						+ credentialEntities.size());

		return credentialEntities;
	}

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
