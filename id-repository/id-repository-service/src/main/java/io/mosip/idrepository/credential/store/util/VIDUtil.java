package io.mosip.idrepository.credential.store.util;

import io.mosip.kernel.core.util.DateUtils2;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.credential.store.constant.ApiName;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.IdRepoException;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Outbound VID service client for credential issuance when policy includes virtual ID attributes.
 * <p>
 * Retrieves existing VIDs or generates new ones via consolidated or standalone VID HTTP APIs.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.provider.CredentialProvider
 */
@Component
public class VIDUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(VIDUtil.class);

	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** Outbound REST client for VID retrieve/generate APIs. */
	@Autowired
	private CredentialStoreRestUtil restUtil;

	/** Deserializes VID service JSON responses. */
	@Autowired
	private ObjectMapper mapper;

	/**
	 * MOSIP application id sent in VID generate request wrapper.
	 * Config key: {@value io.mosip.idrepository.core.constant.IdRepoConstants#VID_CREATE_ID}.
	 */
	@Value("${" + IdRepoConstants.VID_CREATE_ID + ":" + IdRepoConstants.VID_CREATE_ID_DEFAULT + "}")
	private String applicationId;

	/**
	 * API version for VID generate requests.
	 * Config key: {@value io.mosip.idrepository.core.constant.IdRepoConstants#APPLICATION_VERSION_VID}.
	 */
	@Value("${" + IdRepoConstants.APPLICATION_VERSION_VID + ":" + IdRepoConstants.APPLICATION_VERSION_VID_DEFAULT + "}")
	private String version;

	/** Resolves datetime pattern property for VID request timestamps. */
	@Autowired
	private Environment env;

	/**
	 * Retrieves VID metadata for a UIN, optionally filtered by type and specific VID value.
	 *
	 * @param uin     resident UIN
	 * @param vidType VID type from partner policy (e.g. PERPETUAL, TEMPORARY)
	 * @param vid     specific VID to match, or {@code null} to pick latest by expiry
	 * @return matching {@link VidInfoDTO} or {@code null} when none exists
	 * @throws ApiNotAccessibleException if VID service HTTP call fails
	 * @throws IdRepoException           for other integration errors
	 */
	public VidInfoDTO getVIDData(String uin, String vidType, String vid) throws ApiNotAccessibleException, IdRepoException {
		List<String> pathVariables = new ArrayList<>();
		pathVariables.add(uin);
		ResponseWrapper<List<VidInfoDTO>> response = null;
		List<VidInfoDTO> vidResponseDTO = new ArrayList<>();
		List<VidInfoDTO> vidInfoDTOS = new ArrayList<>();

		try {
			response = restUtil.getApi(ApiName.RETRIEVE_VID, pathVariables, "", "", ResponseWrapper.class);
			if (response.getResponse() != null && !response.getResponse().isEmpty()) {
				vidResponseDTO = mapper.readValue(mapper.writeValueAsString(response.getResponse()), List.class);
				for (Object infoDTO : vidResponseDTO) {
					VidInfoDTO vidInfoDTO = mapper.readValue(mapper.writeValueAsString(infoDTO), VidInfoDTO.class);
					vidInfoDTOS.add(vidInfoDTO);
				}
				if (vid != null) {
					vidInfoDTOS = vidInfoDTOS.stream()
							.filter(vidInfoDTO -> vidInfoDTO.getVidType().equalsIgnoreCase(vidType)
									&& vidInfoDTO.getVid().equals(vid))
							.collect(Collectors.toList());
					return vidInfoDTOS.get(0);
				}
				vidInfoDTOS = vidInfoDTOS.stream()
						.filter(vidInfoDTO -> vidInfoDTO.getVidType().equalsIgnoreCase(vidType))
						.collect(Collectors.toList());
				if (!vidInfoDTOS.isEmpty()) {
					vidInfoDTOS.sort(Comparator.comparing(VidInfoDTO::getExpiryTimestamp).reversed());
					return vidInfoDTOS.get(0);
				}
			}
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), uin,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoException(e);
			}
		}
		return null;
	}

	/**
	 * Generates a new VID for the given UIN and type.
	 *
	 * @param uin     resident UIN
	 * @param vidType VID type from partner policy
	 * @return newly created VID response
	 * @throws ApiNotAccessibleException if VID service HTTP call fails
	 * @throws IdRepoException           for other integration errors
	 */
	public VidResponseDTO generateVID(String uin, String vidType) throws ApiNotAccessibleException, IdRepoException {
		VidRequestDTO vidRequestDTO = new VidRequestDTO();
		vidRequestDTO.setUin(uin);
		vidRequestDTO.setVidType(vidType);
		RequestWrapper<VidRequestDTO> vidRequestDTORequestWrapper = new RequestWrapper<>();
		ResponseWrapper<VidResponseDTO> vidResponseDTOResponseWrapper = null;
		vidRequestDTORequestWrapper.setRequest(vidRequestDTO);
		VidResponseDTO vidResponseDTO;
		try {
			vidRequestDTORequestWrapper.setId(applicationId);
			vidRequestDTORequestWrapper.setVersion(version);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			vidRequestDTORequestWrapper.setRequesttime(localdatetime);
			vidResponseDTOResponseWrapper = restUtil.postApi(ApiName.GENERATE_VID, null, "", "",
					MediaType.APPLICATION_JSON, vidRequestDTORequestWrapper, ResponseWrapper.class);
			vidResponseDTO = mapper.readValue(mapper.writeValueAsString(vidResponseDTOResponseWrapper.getResponse()),
					VidResponseDTO.class);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), uin,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoException(e);
			}
		}
		return vidResponseDTO;
	}
}
