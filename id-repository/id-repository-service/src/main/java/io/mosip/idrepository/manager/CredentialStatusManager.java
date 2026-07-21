package io.mosip.idrepository.manager;

import io.mosip.kernel.core.util.DateUtils2;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_STATUS_UPDATE_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;

import java.time.LocalDateTime;
import java.util.*;

import io.mosip.idrepository.core.constant.CredentialTriggerAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.pipeline.CredentialPipelineContext;
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
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.websub.model.EventModel;

/**
 * Manages the credential request status lifecycle and synchronous event notifications.
 * <p>
 * Processes NEW and DELETED rows in {@code credential_request_status} synchronously after
 * identity save (see {@link #processSynchronouslyAfterIssueCredential}). Decrypts stored
 * individual IDs, delegates credential/IDA notifications to
 * {@link CredentialServiceManager}, and persists status updates from credreq responses.
 * </p>
 *
 * @see CredentialServiceManager
 * @see io.mosip.idrepository.core.repository.CredentialRequestStatusRepo
 * @see CredentialRequestStatusLifecycle
 *
 * @author Manoj SP
 */
public class CredentialStatusManager {
	
	private static final String TRANSACTION_LIMIT = "transaction_limit";

	private static final String ID_HASH = "id_hash";

	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialStatusManager.class);

	@Autowired
	private CredentialRequestStatusRepo statusRepo;

	@Autowired
	private HandleRepo handleRepo;

	@Autowired
	@Lazy
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
	
	@Autowired
	private DummyPartnerCheckUtil dummyPartner;

	@Autowired
	private AuditHelper auditHelper;
	
	/**
	 * Runs the credential pipeline synchronously after {@code issueCredential} commits on the identity DB.
	 * <p>
	 * Notifies partners / credreq and issues credentials in-process (no scheduled polling).
	 * </p>
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void processSynchronouslyAfterIssueCredential(String plainUin, String encryptedUin, String uinStatus,
			LocalDateTime expiryTimestamp, boolean isUpdate, String requestId) {
		try {
			String uinHash = securityManager.getIdHashWithSaltModuloByPlainIdHash(plainUin,
					uinHashSaltRepo::retrieveSaltById);
			String triggerAction = isUpdate ? CredentialTriggerAction.UPDATE.toString()
					: CredentialTriggerAction.CREATE.toString();
			removeExpiredRowsForIndividual(uinHash);
			String activeStatus = EnvUtil.getUinActiveStatus();
			if (uinStatus.contentEquals(activeStatus)) {
				processNewRowsForIndividual(plainUin, encryptedUin, triggerAction, requestId, uinHash, activeStatus,
						isUpdate);
			} else {
				processDeletedRowsForIndividual(plainUin, encryptedUin, requestId, uinHash);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
					"processSynchronouslyAfterIssueCredential", ExceptionUtils.getStackTrace(e));
		}
	}

	private void removeExpiredRowsForIndividual(String uinHash) {
		statusRepo.findByIndividualIdHash(uinHash).stream()
				.filter(row -> row.getIdExpiryTimestamp() != null
						&& row.getIdExpiryTimestamp().isBefore(DateUtils2.getUTCCurrentDateTime()))
				.forEach(row -> {
					cancelIssuedRequest(row.getRequestId());
					statusRepo.delete(row);
				});
	}

	private void processNewRowsForIndividual(String plainUin, String encryptedUin, String triggerAction,
			String requestId, String uinHash, String activeStatus, boolean isUpdate) {
		List<CredentialRequestStatus> newRows = statusRepo.findByIndividualIdHash(uinHash).stream()
				.filter(row -> CredentialRequestStatusLifecycle.NEW.toString().equals(row.getStatus()))
				.toList();
		for (CredentialRequestStatus credentialRequestStatus : newRows) {
			cancelIssuedRequest(requestId != null ? requestId : credentialRequestStatus.getRequestId());
			CredentialPipelineContext.set(plainUin, encryptedUin, triggerAction);
			try {
				credManager.notifyUinCredential(plainUin, credentialRequestStatus.getIdExpiryTimestamp(), activeStatus,
						isUpdate, null, uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseConsumer,
						this::idaEventConsumer, List.of(credentialRequestStatus.getPartnerId()),
						credentialRequestStatus.getRequestId());
				deleteDummyPartner(credentialRequestStatus);
			} finally {
				CredentialPipelineContext.clear();
			}
		}
	}

	private void processDeletedRowsForIndividual(String plainUin, String encryptedUin, String requestId,
			String uinHash) {
		List<CredentialRequestStatus> deletedRows = statusRepo.findByIndividualIdHash(uinHash).stream()
				.filter(row -> CredentialRequestStatusLifecycle.DELETED.toString().equals(row.getStatus()))
				.toList();
		for (CredentialRequestStatus credentialRequestStatus : deletedRows) {
			cancelIssuedRequest(requestId != null ? requestId : credentialRequestStatus.getRequestId());
			CredentialPipelineContext.set(plainUin, encryptedUin, credentialRequestStatus.getTriggerAction());
			try {
				credManager.notifyUinCredential(plainUin, credentialRequestStatus.getIdExpiryTimestamp(), "BLOCKED",
						true, null, uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseConsumer,
						this::idaEventConsumer, List.of(credentialRequestStatus.getPartnerId()),
						credentialRequestStatus.getRequestId());
			} finally {
				CredentialPipelineContext.clear();
			}
		}
	}

	/**
	 * Removes the dummy OLV partner placeholder row for an individual after real partner processing.
	 * <p>
	 * Only deletes when the dummy row exists and is not in {@link CredentialRequestStatusLifecycle#FAILED} status.
	 * Retried via {@code @WithRetry} on transient DB failures.
	 * </p>
	 *
	 * @param credentialRequestStatus the processed credential request status row
	 */
	@Transactional
	@WithRetry
	public void deleteDummyPartner(CredentialRequestStatus credentialRequestStatus) {
		Optional<CredentialRequestStatus> idWithDummyPartnerOptional = statusRepo.findByIndividualIdHashAndPartnerId(
				credentialRequestStatus.getIndividualIdHash(), dummyPartner.getDummyOLVPartnerId());
		if (idWithDummyPartnerOptional.isPresent() && !idWithDummyPartnerOptional.get().getStatus()
				.contentEquals(CredentialRequestStatusLifecycle.FAILED.toString())) {
			statusRepo.delete(idWithDummyPartnerOptional.get());
		}
	}

	/**
	 * Callback invoked after credential-request service responds to an issuance request.
	 * <p>
	 * Creates or updates the {@link CredentialRequestStatus} row with request ID, token,
	 * status ({@code REQUESTED} or {@code FAILED}), and transaction limit from the response.
	 * </p>
	 *
	 * @param request  original credential issue request wrapper
	 * @param response raw REST response map from credreq service
	 */
	@Transactional
	@WithRetry
	public void credentialRequestResponseConsumer(CredentialIssueRequestWrapperDto request, Map<String, Object> response) {
		try {

			CredentialIssueResponse credResponse = mapper.convertValue(response.getOrDefault("response", Map.of()), CredentialIssueResponse.class);
			Map<String, Object> additionalData = request.getRequest().getAdditionalData();

			String idHash = (String) additionalData.get(ID_HASH);

			Optional<CredentialRequestStatus> credStatusOptional = statusRepo
					.findByIndividualIdHashAndPartnerId(idHash, request.getRequest().getIssuer());

			CredentialRequestStatus credStatus = credStatusOptional.orElse(null);
			if (credStatus == null) {
				credStatus = new CredentialRequestStatus();
				credStatus.setIndividualId(resolveEncryptedIndividualId(request.getRequest()));
				credStatus.setIndividualIdHash(idHash);
				credStatus.setPartnerId(request.getRequest().getIssuer());
				credStatus.setIdExpiryTimestamp(Objects.nonNull(additionalData.get("expiry_timestamp"))
						? DateUtils2.parseToLocalDateTime((String) additionalData.get("expiry_timestamp"))
						: null);
				credStatus.setCreatedBy(IdRepoSecurityManager.getUser());
				credStatus.setCrDTimes(DateUtils2.getUTCCurrentDateTime());
				CredentialPipelineContext.State pipeline = CredentialPipelineContext.get();
				if (pipeline != null && pipeline.getTriggerAction() != null) {
					credStatus.setTriggerAction(pipeline.getTriggerAction());
				}
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
			credStatus.setUpdDTimes(DateUtils2.getUTCCurrentDateTime());
			statusRepo.saveAndFlush(credStatus);

		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "credentialRequestResponseConsumer", ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Callback invoked after IDA WebSub event delivery — cleans up credential status rows.
	 * <p>
	 * Deletes all {@link CredentialRequestStatus} rows matching the {@code id_hash}
	 * in the delivered event, indicating successful IDA processing.
	 * </p>
	 *
	 * @param event delivered WebSub event model from IDA
	 */
	public void idaEventConsumer(EventModel event) {
		try {
			List<CredentialRequestStatus> credStatusList = statusRepo
					.findByIndividualIdHash((String) event.getEvent().getData().get(ID_HASH));
			if (!credStatusList.isEmpty()) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "idaEventConsumer");
				statusRepo.deleteAll(credStatusList);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "idaEventConsumer", ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Encrypts a plain individual ID for storage in {@code credential_request_status}.
	 * <p>
	 * Format: {@code {saltId}|{urlSafeBase64Ciphertext}} using identity-service encrypt salts.
	 * </p>
	 *
	 * @param individualId plain UIN, VID, or handle
	 * @return encrypted storage form with salt prefix
	 * @throws IdRepoAppException if encryption fails
	 */
	public String encryptId(String individualId) throws IdRepoAppException {
		int saltId = securityManager.getSaltKeyForHashOfId(individualId);
		String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
		return saltId + SPLITTER + new String(securityManager.encryptWithSalt(individualId.getBytes(),
				CryptoUtil.decodePlainBase64(encryptSalt), uinRefId));
	}

	/**
	 * Decrypts a stored individual ID from {@code credential_request_status}.
	 *
	 * @param individualId encrypted form ({@code {saltId}|{ciphertext}})
	 * @return plain individual ID string
	 * @throws IdRepoAppException if decryption fails
	 */
	public String decryptId(String individualId) throws IdRepoAppException {
		String encryptSalt = uinEncryptSaltRepo
				.retrieveSaltById(Integer.valueOf(StringUtils.substringBefore(individualId, SPLITTER)));
		return new String(securityManager.decryptWithSalt(
				CryptoUtil.decodeURLSafeBase64(StringUtils.substringAfter(individualId, SPLITTER)),
				CryptoUtil.decodePlainBase64(encryptSalt), uinRefId));
	}

	/**
	 * Resolves the encrypted form of an individual id for {@code credential_request_status} persistence.
	 * <p>
	 * Reuses pre-encrypted values from pipeline context (UIN), handle additional data, or existing
	 * rows when possible to avoid redundant cryptomanager calls.
	 * </p>
	 */
	private String resolveEncryptedIndividualId(io.mosip.idrepository.core.dto.CredentialIssueRequestDto requestDto)
			throws IdRepoAppException {
		Map<String, Object> additional = requestDto.getAdditionalData();
		if (additional != null) {
			Object encrypted = additional.get(IdRepoConstants.ENCRYPTED_ID);
			if (encrypted instanceof String encryptedId && !encryptedId.isBlank()) {
				return encryptedId;
			}
		}
		String plainId = requestDto.getId();
		CredentialPipelineContext.State pipeline = CredentialPipelineContext.get();
		if (pipeline != null && plainId.equals(pipeline.getPlainIndividualId())) {
			return pipeline.getEncryptedIndividualId();
		}
		return encryptId(plainId);
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