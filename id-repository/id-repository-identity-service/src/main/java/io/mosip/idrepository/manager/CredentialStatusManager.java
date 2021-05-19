package io.mosip.idrepository.manager;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MODULO_VALUE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.entity.UinEncryptSalt;
import io.mosip.idrepository.identity.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.identity.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.identity.repository.UinHashSaltRepo;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.model.EventModel;

/**
 * @author Manoj SP
 *
 */
@Component
public class CredentialStatusManager {

	@Autowired
	private CredentialRequestStatusRepo statusRepo;

	@Autowired
	private CredentialServiceManager credManager;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private RestHelper restHelper;

	@Autowired
	private Environment env;

	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Autowired
	private IdRepoSecurityManager securityManager;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Async("asyncThreadPoolTaskExecutor")
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
				String idvId = decryptUin(credentialRequestStatus.getIndividualId());
				credManager.notifyUinCredential(idvId, credentialRequestStatus.getIdExpiryTimestamp(), "BLOCKED",
						Objects.nonNull(credentialRequestStatus.getUpdatedBy()) ? true : false, null,
						uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseHandler, this::idaEventHandler);
			}
		} catch (Exception e) {

		}
	}

	private void handleExpiredRequests() {
		try {
			List<CredentialRequestStatus> expiredIssueRequestList = statusRepo
					.findByIdExpiryTimestampBefore(DateUtils.getUTCCurrentDateTime());
			for (CredentialRequestStatus credentialRequestStatus : expiredIssueRequestList) {
				cancelIssuedRequest(credentialRequestStatus.getRequestId());
				String idvId = decryptUin(credentialRequestStatus.getIndividualId());
				credManager.notifyUinCredential(idvId, credentialRequestStatus.getIdExpiryTimestamp(), "BLOCKED",
						Objects.nonNull(credentialRequestStatus.getUpdatedBy()) ? true : false, null,
						uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseHandler, this::idaEventHandler);
			}
		} catch (Exception e) {

		}
	}

	private void handleNewOrUpdatedRequests() {
		try {
			String activeStatus = env.getProperty(ACTIVE_STATUS);
			List<CredentialRequestStatus> newIssueRequestList = statusRepo
					.findByStatus(CredentialRequestStatusLifecycle.NEW.toString());
			for (CredentialRequestStatus credentialRequestStatus : newIssueRequestList) {
				cancelIssuedRequest(credentialRequestStatus.getRequestId());
				String idvId = decryptUin(credentialRequestStatus.getIndividualId());
				credManager.notifyUinCredential(idvId, credentialRequestStatus.getIdExpiryTimestamp(), activeStatus,
						Objects.nonNull(credentialRequestStatus.getUpdatedBy()) ? true : false, null,
						uinHashSaltRepo::retrieveSaltById, this::credentialRequestResponseHandler, this::idaEventHandler);
			}
		} catch (Exception e) {

		}
	}

	public void credentialRequestResponseHandler(CredentialIssueRequestWrapperDto request, Map<String, Object> response) {
		try {
			CredentialIssueResponse credResponse = mapper.convertValue(response.get("response"), CredentialIssueResponse.class);
			Map<String, Object> additionalData = request.getRequest().getAdditionalData();
			Optional<CredentialRequestStatus> credStatusOptional = statusRepo
					.findByIndividualIdHashAndPartnerId((String) additionalData.get("id_hash"), request.getRequest().getIssuer());
			if (credStatusOptional.isPresent()) {
				CredentialRequestStatus credStatus = credStatusOptional.get();
				credStatus.setRequestId(credResponse.getRequestId());
				credStatus.setTokenId((String) additionalData.get("TOKEN"));
				credStatus.setStatus(CredentialRequestStatusLifecycle.REQUESTED.toString());
				credStatus.setIdTransactionLimit(Objects.nonNull(additionalData.get("transaction_limit"))
						? Integer.valueOf((String) additionalData.get("transaction_limit"))
						: null);
				credStatus.setUpdatedBy(IdRepoSecurityManager.getUser());
				credStatus.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
				statusRepo.save(credStatus);
			} else {
				CredentialRequestStatus credStatus = new CredentialRequestStatus();
				// Encryption is done using identity service encryption salts for all id types
				credStatus.setIndividualId(encryptId(credResponse.getId()));
				credStatus.setIndividualIdHash((String) additionalData.get("id_hash"));
				credStatus.setPartnerId(request.getRequest().getIssuer());
				credStatus.setRequestId(credResponse.getRequestId());
				credStatus.setTokenId((String) additionalData.get("TOKEN"));
				credStatus.setStatus(CredentialRequestStatusLifecycle.REQUESTED.toString());
				credStatus.setIdTransactionLimit(Objects.nonNull(additionalData.get("transaction_limit"))
						? Integer.valueOf((String) additionalData.get("transaction_limit"))
						: null);
				credStatus.setIdExpiryTimestamp(Objects.nonNull(additionalData.get("expiry_timestamp"))
						? DateUtils.parseToLocalDateTime((String) additionalData.get("expiry_timestamp"))
						: null);
				credStatus.setCreatedBy(IdRepoSecurityManager.getUser());
				credStatus.setCrDTimes(DateUtils.getUTCCurrentDateTime());
				statusRepo.save(credStatus);
			}
		} catch (Exception e) {

		}
	}

	public void idaEventHandler(EventModel event) {
		try {
			List<CredentialRequestStatus> credStatusList = statusRepo
					.findByIndividualIdHash((String) event.getEvent().getData().get("id_hash"));
			if (!credStatusList.isEmpty()) {
				statusRepo.deleteAll(credStatusList);
			}
		} catch (Exception e) {

		}
	}

	private String encryptId(String individualId) throws IdRepoAppException {
		Integer moduloValue = env.getProperty(MODULO_VALUE, Integer.class);
		int modResult = (int) (Long.parseLong(individualId) % moduloValue);
		String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(modResult);
		return modResult + SPLITTER + securityManager.encryptWithSalt(individualId.getBytes(), encryptSalt.getBytes(), uinRefId);
	}

	private String decryptUin(String individualId) throws IdRepoAppException {
		List<String> uinList = Arrays.asList(individualId.split(SPLITTER));
		Optional<UinEncryptSalt> encryptSalt = uinEncryptSaltRepo.findById(Integer.valueOf(uinList.get(0)));
		String idvId = new String(
				securityManager.decryptWithSalt(uinList.get(1).getBytes(), encryptSalt.get().getSalt().getBytes(), uinRefId));
		return idvId;
	}

	private void cancelIssuedRequest(String requestId) throws IdRepoDataValidationException {
		if (Objects.nonNull(requestId)) {
			RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.CREDENTIAL_CANCEL_SERVICE, null,
					ResponseWrapper.class);
			restRequest.setUri(restRequest.getUri().replace("{requestId}", requestId));
			restHelper.requestAsync(restRequest);
		}
	}
}
