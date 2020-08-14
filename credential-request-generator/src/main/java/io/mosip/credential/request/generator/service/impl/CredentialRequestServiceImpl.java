package io.mosip.credential.request.generator.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialIssueException;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;

/**
 * 
 * @author Sowmya
 *
 * The Class CredentialRequestServiceImpl.
 */
public class CredentialRequestServiceImpl implements CredentialRequestService {
	
	/** The credential repositary. */
	@Autowired
	CredentialRepositary<CredentialEntity, String> credentialRepositary;
	
	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credential.request.generator.service.CredentialRequestService#
	 * createCredentialIssuance(io.mosip.credential.request.generator.dto.
	 * CredentialIssueRequestDto)
	 */
	@Override
	public CredentialIssueResponse createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto) {

		CredentialIssueResponse credentialIssueResponse = null;
		try{
			String requestId = generateId();
		

	    CredentialEntity credential=new CredentialEntity();
		credential.setRequestId(requestId);
		credential.setRequest(credentialIssueRequestDto.toString());
			credential.setStatusCode(CredentialStatusCode.NEW.name());
		credential.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setCreatedBy(USER);
			// TODO whether to add cellname
		credentialRepositary.save(credential);
			credentialIssueResponse = new CredentialIssueResponse();
			credentialIssueResponse.setRequestId(requestId);
	    }catch(DataAccessLayerException e) {
	    	throw new CredentialIssueException(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode(), CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage(),e);
	    }
		return credentialIssueResponse;
	}

	/**
	 * Generate id.
	 *
	 * @return the string
	 */
	private String generateId() {
		return UUID.randomUUID().toString();
	}

	@Override
	public CredentialIssueResponse cancelCredentialRequest(String requestId) {
		CredentialIssueResponse credentialIssueResponse = null;
		try {
			Optional<CredentialEntity> entity = credentialRepositary.findById(requestId);
			if (entity != null) {
				CredentialEntity credentialEntity = entity.get();
				credentialEntity.setStatusCode(CredentialStatusCode.CANCELLED.name());
				credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				credentialEntity.setUpdatedBy(USER);
				credentialRepositary.update(credentialEntity);
				credentialIssueResponse = new CredentialIssueResponse();
				credentialIssueResponse.setRequestId(requestId);
			} else {
				throw new CredentialIssueException(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode(),
						CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
			}
		} catch (DataAccessLayerException e) {
			throw new CredentialIssueException(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode(),
					CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage(), e);
		} catch (Exception e) {
			throw new CredentialIssueException(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode(),
					CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
		}
		return credentialIssueResponse;
	}
	;
}
