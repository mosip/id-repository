package io.mosip.idrepository.credential.store.provider.impl;

import io.mosip.kernel.core.util.DateUtils2;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.credential.store.constant.JsonConstants;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.AllowedKycDto;
import io.mosip.idrepository.credential.store.dto.DataProviderResponse;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.CredentialFormatterException;
import io.mosip.idrepository.credential.store.exception.DataEncryptionFailureException;
import io.mosip.idrepository.credential.store.provider.CredentialProvider;
import io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl;
import io.mosip.idrepository.credential.store.util.EncryptionUtil;
import io.mosip.idrepository.credential.store.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Credential formatter for QR-code (print) partner credentials.
 * <p>
 * Role in the MOSIP credential pipeline: selected by {@link CredentialStoreServiceImpl} when
 * partner policy maps the credential type to {@code QrCodeProvider}. Builds a JSON-LD credential
 * envelope and encrypts policy-marked or request-flagged attributes with a partner-supplied PIN
 * ({@link CredentialServiceRequestDto#getEncryptionKey()}).
 * </p>
 *
 * @see CredentialStoreServiceImpl#getProvider(String)
 * @see EncryptionUtil#encryptDataWithPin(String, String, String, String)
 * @see CredentialProvider
 */
@Component
public class QrCodeProvider extends CredentialProvider {

	/** Delegates PIN-based attribute encryption to cryptomanager. */
	@Autowired
	EncryptionUtil encryptionUtil;

	/** Shared helpers for ID generation and config-backed property access. */
	@Autowired
	Utilities utilities;

	/**
	 * Config key for the datetime pattern used in credential issuance timestamps.
	 * Property: {@value}.
	 */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** Serializes non-string sharable attribute values to JSON strings. */
	@Lazy
	@Autowired
	private ObjectMapper mapper;

	/** Class logger. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(QrCodeProvider.class);

	/**
	 * Formats sharable identity attributes into a QR-code credential JSON document.
	 * <p>
	 * Attributes marked encrypted in partner policy or via {@code encrypt} on the request are
	 * encrypted with the partner PIN; cleartext attributes are placed directly in
	 * {@code credentialSubject}.
	 * </p>
	 *
	 * @param credentialServiceRequestDto issuance request (UIN/VID, issuer, encryptionKey, requestId)
	 * @param sharableAttributeMap       policy-filtered attributes keyed by {@link AllowedKycDto}
	 * @return formatted credential JSON, generated credentialId, and issuance timestamp
	 * @throws CredentialFormatterException if serialization, PIN encryption, or downstream API calls fail
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DataProviderResponse getFormattedCredentialData(
			CredentialServiceRequestDto credentialServiceRequestDto, Map<AllowedKycDto, Object> sharableAttributeMap)
			throws CredentialFormatterException {
		String requestId = credentialServiceRequestDto.getRequestId();
		DataProviderResponse dataProviderResponse = null;
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Formatting credential data");
			String pin = credentialServiceRequestDto.getEncryptionKey();

			Map<String, Object> formattedMap = new HashMap<>();
			List<String> protectedAttributes = new ArrayList<>();
			formattedMap.put(JsonConstants.ID, credentialServiceRequestDto.getId());

			for (Map.Entry<AllowedKycDto, Object> entry : sharableAttributeMap.entrySet()) {
				AllowedKycDto allowedKycDto = entry.getKey();
				String attributeName = allowedKycDto.getAttributeName();
				Object value = entry.getValue();
				String valueStr = null;
				if (value instanceof String) {
					valueStr = value.toString();
				} else {
					valueStr = mapper.writeValueAsString(value);
				}
				formattedMap.put(attributeName, valueStr);
				if (allowedKycDto.isEncrypted() || credentialServiceRequestDto.isEncrypt()) {
					if (!valueStr.isEmpty()) {
					String encryptedValue = encryptionUtil.encryptDataWithPin(attributeName, valueStr, pin, requestId);
					formattedMap.put(attributeName, encryptedValue);
					protectedAttributes.add(attributeName);
					}
				} else {
					formattedMap.put(attributeName, valueStr);
				}

			}

			String credentialId = utilities.generateId();


			dataProviderResponse = new DataProviderResponse();
			DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);

			JSONObject json = new JSONObject();
			List<String> typeList = new ArrayList<>();
			typeList.add(EnvUtil.getCredServiceSchema());
			json.put(JsonConstants.ID, EnvUtil.getCredServiceFormatId() + credentialId);
			json.put(JsonConstants.TYPE, typeList);
			json.put(JsonConstants.ISSUER, EnvUtil.getCredServiceFormatIssuer());
			json.put(JsonConstants.ISSUANCEDATE, DateUtils2.formatToISOString(localdatetime));
			json.put(JsonConstants.ISSUEDTO, credentialServiceRequestDto.getIssuer());
			json.put(JsonConstants.CONSENT, "");
			json.put(JsonConstants.CREDENTIALSUBJECT, formattedMap);
			json.put(JsonConstants.PROTECTEDATTRIBUTES, protectedAttributes);
			dataProviderResponse.setJSON(json);

			dataProviderResponse.setCredentialId(credentialId);

			dataProviderResponse.setIssuanceDate(localdatetime);
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"end formatting credential data");
			return dataProviderResponse;
		} catch (DataEncryptionFailureException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (ApiNotAccessibleException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (JsonProcessingException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new CredentialFormatterException(e);
		}
	}
	

}
