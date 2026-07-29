package io.mosip.idrepository.pipeline;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.credential.store.constant.CredentialConstants;
import io.mosip.idrepository.credential.store.exception.IdRepoException;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.identity.service.impl.IdRepoProxyServiceImpl;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.util.StringUtils;

/**
 * SDK adapter for identity retrieval within the consolidated ID Repository JVM.
 * <p>
 * Delegates to {@link IdRepoProxyServiceImpl} (no internal HTTP to identity REST).
 * </p>
 */
@Component
public class InProcessIdentityClient {

	@Value("${" + IdRepoConstants.FETCH_IDENTITY_TYPE + ":" + IdRepoConstants.FETCH_IDENTITY_TYPE_DEFAULT + "}")
	private String identityType;

	@Autowired
	@Lazy
	private IdRepoProxyServiceImpl idRepoProxyService;

	@Autowired
	@Lazy
	private IdRequestValidator idRequestValidator;

	/**
	 * Retrieves identity data for credential issuance (UIN, VID, or handle).
	 *
	 * @param request                  credential service request carrying the ID to retrieve
	 * @param bioAttributeFormatterMap partner-specific biometric format map
	 * @return identity response DTO
	 * @throws IdRepoException if identity retrieval fails
	 */
	public IdResponseDTO retrieveIdentity(CredentialServiceRequestDto request,
			Map<String, String> bioAttributeFormatterMap) throws IdRepoException {
		CredentialPipelineContext.State pipeline = CredentialPipelineContext.get();
		String cacheKey = buildIdentityCacheKey(request.getId(), bioAttributeFormatterMap);
		try {
			if (pipeline != null) {
				return pipeline.getIdentityCache().computeIfAbsent(cacheKey, key -> {
					try {
						return doRetrieveIdentity(request, bioAttributeFormatterMap);
					} catch (IdRepoAppException e) {
						throw new IdentityRetrieveUncheckedException(e);
					}
				});
			}
			return doRetrieveIdentity(request, bioAttributeFormatterMap);
		} catch (IdentityRetrieveUncheckedException e) {
			throw new IdRepoException(e.getCause());
		} catch (IdRepoAppException e) {
			throw new IdRepoException(e);
		} catch (Exception e) {
			throw new IdRepoException(e);
		}
	}

	private IdResponseDTO doRetrieveIdentity(CredentialServiceRequestDto request,
			Map<String, String> bioAttributeFormatterMap) throws IdRepoAppException {
		IdType idType = resolveIdType(request);
		Map<String, String> extractionFormats = new HashMap<>();
		if (bioAttributeFormatterMap != null) {
			putIfPresent(extractionFormats, CredentialConstants.FINGER,
					bioAttributeFormatterMap.get(CredentialConstants.FINGER));
			putIfPresent(extractionFormats, CredentialConstants.FACE,
					bioAttributeFormatterMap.get(CredentialConstants.FACE));
			putIfPresent(extractionFormats, CredentialConstants.IRIS,
					bioAttributeFormatterMap.get(CredentialConstants.IRIS));
		}
		return idRepoProxyService.retrieveIdentity(request.getId(), idType, identityType, extractionFormats);
	}

	/**
	 * Builds a stable cache key so partners with the same id + bio formats share one retrieve.
	 */
	static String buildIdentityCacheKey(String id, Map<String, String> bioAttributeFormatterMap) {
		if (bioAttributeFormatterMap == null || bioAttributeFormatterMap.isEmpty()) {
			return id + "|";
		}
		return id + "|" + bioAttributeFormatterMap.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(e -> e.getKey() + "=" + e.getValue())
				.reduce((a, b) -> a + "&" + b)
				.orElse("");
	}

	/** Wraps checked identity failures for {@code ConcurrentMap#computeIfAbsent}. */
	private static final class IdentityRetrieveUncheckedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private IdentityRetrieveUncheckedException(IdRepoAppException cause) {
			super(cause);
		}
	}

	/**
	 * Retrieves identity by plain UIN for VID lifecycle validation (e.g. active status check).
	 *
	 * @param uin plain UIN
	 * @return identity response DTO
	 * @throws IdRepoAppException when identity is missing or not active
	 */
	public IdResponseDTO retrieveIdentityByUin(String uin) throws IdRepoAppException {
		return idRepoProxyService.retrieveIdentity(uin, IdType.UIN, identityType, Collections.emptyMap());
	}

	private void putIfPresent(Map<String, String> target, String key, String value) {
		if (StringUtils.isNotEmpty(value)) {
			target.put(key, value);
		}
	}

	/**
	 * Resolves identifier type the same way as {@code GET /idrepository/v1/identity/idvid/{id}}:
	 * explicit {@code additionalData.idType} when present, otherwise UIN / VID / RID inference.
	 */
	private IdType resolveIdType(CredentialServiceRequestDto request) throws IdRepoAppException {
		Map<String, Object> additional = request.getAdditionalData();
		if (additional != null && additional.get("idType") != null) {
			return IdType.valueOf(((String) additional.get("idType")).toUpperCase());
		}
		String id = request.getId();
		if (idRequestValidator.validateUin(id)) {
			return IdType.UIN;
		}
		if (idRequestValidator.validateVid(id)) {
			return IdType.VID;
		}
		return IdType.ID;
	}
}
