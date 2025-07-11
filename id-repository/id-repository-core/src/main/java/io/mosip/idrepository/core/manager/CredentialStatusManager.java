package io.mosip.idrepository.core.manager;

import static io.mosip.idrepository.core.constant.AuditModules.ID_REPO_CORE_SERVICE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_STATUS_UPDATE_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.core.constant.IdType.ID;

import java.util.*;

import io.mosip.idrepository.core.constant.CredentialTriggerAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.retry.WithRetry;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.websub.model.EventModel;

/**
 * @author Manoj SP
 *
 */
@Transactional
public class CredentialStatusManager {
	
	private static final String TRANSACTION_LIMIT = "transaction_limit";

	private static final String ID_HASH = "id_hash";

	Logger mosipLogger = IdRepoLogger.getLogger(CredentialStatusManager.class);

	@Autowired
	private CredentialRequestStatusRepo statusRepo;

	@Autowired
	private HandleRepo handleRepo;

	@Autowired
	private CredentialServiceManager credManager;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Autowired
	private IdRepoSecurityManager securityManager;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;
	
	@Value("${" + CREDENTIAL_STATUS_UPDATE_TOPIC + "}")
	private String credentailStatusUpdateTopic;

	@Value("${mosip.idrepo.credential.request.batch.page.size:50}")
	private int pageSize;
	
	@Autowired
	private DummyPartnerCheckUtil dummyPartner;
	
	@Autowired
	private AuditHelper auditHelper;
	
//	@Async("credentialStatusManagerJobExecutor")
	public void triggerEventNotifications() {
		handleDeletedRequests();
		handleExpiredRequests();
		handleNewOrUpdatedRequests();
	}

	private void handleDeletedRequests() {
		try {
			List<CredentialRequestStatus> deletedIssueRequestList = statusRepo
					.findByStatus(CredentialRequestStatusLifecycle.DELETED.toString());
			for (CredentialRequestStatus credentialRequestStatus : deletedIssueRequestList) {
				cancelIssuedRequest(credentialRequestStatus.getRequestId());
				String idvId = decryptId(credentialRequestStatus.getIndividualId());
				credManager.notifyUinCredential(idvId, credentialRequestStatus.getIdExpiryTimestamp(), "BLOCKED",
						true, null,
						uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseConsumer,
						this::idaEventConsumer, List.of(credentialRequestStatus.getPartnerId()),credentialRequestStatus.getRequestId());
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "handleDeletedRequests", ExceptionUtils.getStackTrace(e));
		}
	}

	private void handleExpiredRequests() {
		try {
			List<CredentialRequestStatus> expiredIssueRequestList = statusRepo
					.findByIdExpiryTimestampBefore(DateUtils.getUTCCurrentDateTime());
			for (CredentialRequestStatus credentialRequestStatus : expiredIssueRequestList) {
				cancelIssuedRequest(credentialRequestStatus.getRequestId());
				statusRepo.delete(credentialRequestStatus);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "handleExpiredRequests", ExceptionUtils.getStackTrace(e));
		}
	}

	public void handleNewOrUpdatedRequests() {
		try {
			String activeStatus = EnvUtil.getUinActiveStatus();
			List<CredentialRequestStatus> newIssueRequestList = statusRepo
					.findByStatus(CredentialRequestStatusLifecycle.NEW.toString(), pageSize);
			for (CredentialRequestStatus credentialRequestStatus : newIssueRequestList) {
				cancelIssuedRequest(credentialRequestStatus.getRequestId());
				String idvId = decryptId(credentialRequestStatus.getIndividualId());
				credManager.notifyUinCredential(idvId, credentialRequestStatus.getIdExpiryTimestamp(), activeStatus,
						Objects.equals(CredentialTriggerAction.UPDATE.toString(), credentialRequestStatus.getTriggerAction()), null,
						uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseConsumer,
						this::idaEventConsumer, List.of(credentialRequestStatus.getPartnerId()),credentialRequestStatus.getRequestId());
				deleteDummyPartner(credentialRequestStatus);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "handleNewOrUpdatedRequests", ExceptionUtils.getStackTrace(e));
		}
	}

	@WithRetry
	public void deleteDummyPartner(CredentialRequestStatus credentialRequestStatus) {
		Optional<CredentialRequestStatus> idWithDummyPartnerOptional = statusRepo.findByIndividualIdHashAndPartnerId(
				credentialRequestStatus.getIndividualIdHash(), dummyPartner.getDummyOLVPartnerId());
		if (idWithDummyPartnerOptional.isPresent() && !idWithDummyPartnerOptional.get().getStatus()
				.contentEquals(CredentialRequestStatusLifecycle.FAILED.toString())) {
			statusRepo.delete(idWithDummyPartnerOptional.get());
		}
	}

	@WithRetry
	public void credentialRequestResponseConsumer(CredentialIssueRequestWrapperDto request, Map<String, Object> response) {
		try {

			CredentialIssueResponse credResponse = mapper.convertValue(response.getOrDefault("response", Map.of()), CredentialIssueResponse.class);
			Map<String, Object> additionalData = request.getRequest().getAdditionalData();

			String idHash = (String) additionalData.get(ID_HASH);

			Optional<CredentialRequestStatus> credStatusOptional = statusRepo
					.findByIndividualIdHashAndPartnerId(idHash, request.getRequest().getIssuer());

			mosipLogger.info("DEBUG--- credentialRequestResponseConsumer issuer: {}, credStatusOptional : {} additionalData : {}",
					request.getRequest().getIssuer(), credStatusOptional.isPresent(), additionalData);

			CredentialRequestStatus credStatus = credStatusOptional.orElse(null);
			if (credStatus == null) {
				credStatus = new CredentialRequestStatus();
				// Encryption is done using identity service encryption salts for all id types
				credStatus.setIndividualId(encryptId(request.getRequest().getId()));
				credStatus.setIndividualIdHash(idHash);
				credStatus.setPartnerId(request.getRequest().getIssuer());
				credStatus.setIdExpiryTimestamp(Objects.nonNull(additionalData.get("expiry_timestamp"))
						? DateUtils.parseToLocalDateTime((String) additionalData.get("expiry_timestamp"))
						: null);
				credStatus.setCreatedBy(IdRepoSecurityManager.getUser());
				credStatus.setCrDTimes(DateUtils.getUTCCurrentDateTime());
			}

			if (Objects.nonNull(credResponse))
				credStatus.setRequestId(credResponse.getRequestId());

			credStatus.setTokenId((String) additionalData.get("TOKEN"));
			credStatus.setStatus(Objects.isNull(credResponse) ? CredentialRequestStatusLifecycle.FAILED.toString()
					: CredentialRequestStatusLifecycle.REQUESTED.toString());
			credStatus.setIdTransactionLimit(Objects.nonNull(additionalData.get(TRANSACTION_LIMIT))
					? (Integer) additionalData.get(TRANSACTION_LIMIT)
					: null);
			credStatus.setUpdatedBy(IdRepoSecurityManager.getUser());
			credStatus.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
			statusRepo.saveAndFlush(credStatus);

		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "credentialRequestResponseConsumer", e);
		}
	}

	public void idaEventConsumer(EventModel event) {
		try {
			List<CredentialRequestStatus> credStatusList = statusRepo
					.findByIndividualIdHash((String) event.getEvent().getData().get(ID_HASH));
			if (!credStatusList.isEmpty()) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "idaEventConsumer",
						credStatusList.get(0).getIndividualIdHash());
				statusRepo.deleteAll(credStatusList);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "idaEventConsumer", ExceptionUtils.getStackTrace(e));
		}
	}

	public String encryptId(String individualId) throws IdRepoAppException {
		int saltId = securityManager.getSaltKeyForHashOfId(individualId);
		String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
		return saltId + SPLITTER + new String(securityManager.encryptWithSalt(individualId.getBytes(),
				CryptoUtil.decodePlainBase64(encryptSalt), uinRefId));
	}

	public String decryptId(String individualId) throws IdRepoAppException {
		String encryptSalt = uinEncryptSaltRepo
				.retrieveSaltById(Integer.valueOf(StringUtils.substringBefore(individualId, SPLITTER)));
		return new String(securityManager.decryptWithSalt(
				CryptoUtil.decodeURLSafeBase64(StringUtils.substringAfter(individualId, SPLITTER)),
				CryptoUtil.decodePlainBase64(encryptSalt), uinRefId));
	}

	/**
	 * 
	 * @param requestId
	 * @throws IdRepoDataValidationException
	 */
	private void cancelIssuedRequest(String requestId) {
		if (Objects.nonNull(requestId)) {
			credManager.updateEventProcessingStatus(requestId, CredentialRequestStatusLifecycle.INVALID.toString(),
					credentailStatusUpdateTopic);
		}
	}
}
