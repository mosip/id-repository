package io.mosip.idrepository.identity.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.dto.HandleDto;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.idobjectvalidator.constant.IdObjectValidatorConstant;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ROOT_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

@Component
public class IdRepoServiceHelper {

    Logger mosipLogger = IdRepoLogger.getLogger(IdRepoServiceHelper.class);

    private static final String ID_REPO_SERVICE_HELPER = "IdRepoServiceHelper";

    private static final String REQUEST = "request";

    /** The schema map. */
    private static Map<String, String> schemaMap = new HashMap<>();
    private static Map<String, List<String>> supportedHandlesInSchema = new HashMap<>();

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RestRequestBuilder restBuilder;

    @Autowired
    private RestHelper restHelper;

    @Autowired
    private IdRepoSecurityManager securityManager;

    @Autowired
    private UinHashSaltRepo uinHashSaltRepo;

    @Value("${mosip.identity.mapping-file}")
    private String identityMappingJson;

    @Value("#{${mosip.identity.fieldid.handle-postfix.mapping}}")
    private Map<String, String> fieldIdHandlePostfixMapping;

    private IdentityMapping identityMapping;


    @PostConstruct
    private void initialize() throws IOException {
        try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
            identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
                    IdentityMapping.class);;
        }
    }

    public IdentityMapping getIdentityMapping() {
        return this.identityMapping;
    }


    /**
     * Convert to map.
     *
     * @param identity the identity
     * @return the map
     * @throws IdRepoAppException the id repo app exception
     */
    public Map<String, Object> convertToMap(Object identity) throws IdRepoAppException {
        try {
            return mapper.readValue(mapper.writeValueAsBytes(identity), new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "convertToMap", "\n" + e.getMessage());
            throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
                    String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), REQUEST), e);
        }
    }

    /**
     * Gets the schema.
     *
     * @param schemaVersion the schema version
     * @return the schema
     */
    public String getSchema(String schemaVersion) {
        if (Objects.isNull(schemaVersion) || schemaVersion.contentEquals("null")) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "getSchema",
                    "\n" + "schemaVersion is null");
            throw new IdRepoAppUncheckedException(MISSING_INPUT_PARAMETER.getErrorCode(),
                    String.format(MISSING_INPUT_PARAMETER.getErrorMessage(),
                            ROOT_PATH + IdObjectValidatorConstant.PATH_SEPERATOR + "IDSchemaVersion"));
        }
        try {
            if (schemaMap.containsKey(schemaVersion)) {
                return schemaMap.get(schemaVersion);
            } else {
                RestRequestDTO restRequest;
                restRequest = restBuilder.buildRequest(RestServicesConstants.SYNCDATA_SERVICE, null, ResponseWrapper.class);
                restRequest.setUri(restRequest.getUri().concat("?schemaVersion=" + schemaVersion));
                ResponseWrapper<Map<String, String>> response = restHelper.requestSync(restRequest);
                schemaMap.put(schemaVersion, response.getResponse().get("schemaJson"));
                supportedHandlesInSchema.put(schemaVersion, getSupportedHandles(response.getResponse().get("schemaJson")));
                return getSchema(schemaVersion);
            }
        } catch (IdRepoDataValidationException | RestServiceException e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "getSchema", "\n" + e.getMessage());
            throw new IdRepoAppUncheckedException(IdRepoErrorConstants.SCHEMA_RETRIEVE_ERROR);
        }
    }

	/**
	 * @param identity it should contain the data for attributes coming as part of
	 *                 selected handles.
	 * @return handles map
	 * @throws IdRepoAppException
	 */
	public Map<String, List<HandleDto>> getSelectedHandles(Object identity, Map<String, List<HandleDto>> existingSelectedHandlesMap) throws IdRepoAppException {
		Map<String, Object> requestMap = convertToMap(identity);
		if (requestMap.containsKey(ROOT_PATH) && Objects.nonNull(requestMap.get(ROOT_PATH))) {
			Map<String, Object> identityMap = (Map<String, Object>) requestMap.get(ROOT_PATH);
			String schemaVersion = String
					.valueOf(identityMap.get(identityMapping.getIdentity().getIDSchemaVersion().getValue()));
			String selectedHandlesFieldId = identityMapping.getIdentity().getSelectedHandles().getValue();
//			if 'selectedHandles' is not coming in the request body itself then will execute else if block.
			if (identityMap.containsKey(selectedHandlesFieldId)
					&& Objects.nonNull(identityMap.get(selectedHandlesFieldId))) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "getSelectedHandles",
						identityMap.get(selectedHandlesFieldId));
				List<String> selectedHandleFieldIds = (List<String>) identityMap.get(selectedHandlesFieldId);
				return selectedHandleFieldIds.stream()
						.filter(handleFieldId -> supportedHandlesInSchema.get(schemaVersion).contains(handleFieldId))
						.collect(Collectors.toMap(handleName -> handleName,
                                handleFieldId -> buildHandleDto(identityMap.get(handleFieldId), handleFieldId)));
			} else if (existingSelectedHandlesMap != null && !existingSelectedHandlesMap.isEmpty()) {
                Set<String> existingSelectedHandleKeys = existingSelectedHandlesMap.keySet();
				return existingSelectedHandleKeys.stream()
						.filter(handleFieldId -> {
							if (identityMap.containsKey(handleFieldId) && Objects.nonNull(identityMap.get(handleFieldId))) {
								if (supportedHandlesInSchema.get(schemaVersion).contains(handleFieldId)) {
									return true;
								} else {
									return false;
								}
							}
							existingSelectedHandlesMap.remove(handleFieldId);
							return false;
						})
						.collect(Collectors.toMap(handleName -> handleName,
                                handleFieldId -> buildHandleDto(identityMap.get(handleFieldId), handleFieldId)));
			}
		}
		return null;
	}

    private List<HandleDto> buildHandleDto(Object value, String fieldId) {
        if(StringUtils.isEmpty(value))
            return new ArrayList<>();

        if(value instanceof String) {
            String handle = ((String) value).concat(getHandlePostfix(fieldId)).toLowerCase(Locale.ROOT);
            List<HandleDto> handleDtos = new ArrayList<>();
            handleDtos.add(new HandleDto(handle, getHandleHash(handle)));
            return handleDtos;
        }

        if(value instanceof List) {
            List<HandleDto> handleDtos = new ArrayList<>();
            for(Object val : getFilteredHandleValues((List<Object>) value)) {
                handleDtos.addAll(buildHandleDto(val, fieldId));
            }
            return handleDtos;
        }

        if(value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            return buildHandleDto(map.get("value"), fieldId);
        }
        return new ArrayList<>();
    }

    //if none of the values are tagged as "handle" , then consider all the values as handles
    private List<Object> getFilteredHandleValues(List<Object> list) {
        List<Object> filteredList = list.stream().filter( obj -> obj instanceof Map &&
                ((Map<String, Object>) obj).containsKey("tags") &&
                ((Map<String, Object>) obj).get("tags") != null &&
                ((List<String>) ((Map<String, Object>) obj).get("tags")).contains("handle")).collect(Collectors.toList());

        mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "getFilteredHandleValues size :", filteredList.size());
        return filteredList.isEmpty() ? list : filteredList;
    }

    public String getHandleHash(String handle) {
        //handle is converted to lowercase. It is language neutral conversion.
        int saltId = securityManager.getSaltKeyForHashOfId(handle.toLowerCase(Locale.ROOT));
        String salt = uinHashSaltRepo.retrieveSaltById(saltId);
        String saltedHash = securityManager.hashwithSalt(handle.getBytes(StandardCharsets.UTF_8),
                CryptoUtil.decodePlainBase64(salt));
        return saltId + SPLITTER + saltedHash;
    }

    private List<String> getSupportedHandles(String schema) {
        List<String> supportedHandles = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        try {
            paths = JsonPath.using(Configuration.builder().options(Option.AS_PATH_LIST).build())
                    .parse(schema)
                    .read("$['properties']['identity']['properties'][*][?(@['handle']==true)]");
        } catch (PathNotFoundException ex) { /*ignore this exception*/ }

        paths.forEach( path -> {
            // returns in below format, so need to remove parent paths
            //Eg: "$['properties']['identity']['properties']['phone']"
            supportedHandles.add(path.replace("$['properties']['identity']['properties']['", "")
                    .replace("']", ""));
        });
        return supportedHandles;
    }

    private String getHandlePostfix(String fieldId) {
        if(CollectionUtils.isEmpty(fieldIdHandlePostfixMapping)) {
            return "@".concat(fieldId);
        }
        String postfix = fieldIdHandlePostfixMapping.get(fieldId);
        if(postfix == null) {
            return "@".concat(fieldId);
        }
        return postfix;
    }
}
