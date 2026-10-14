package io.mosip.idrepository.identity.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoConstants;
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
import io.mosip.idrepository.core.util.IdRepositoryUtil;
import io.mosip.idrepository.identity.dto.HandleDto;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.idobjectvalidator.constant.IdObjectValidatorConstant;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.mosip.idrepository.core.constant.IdRepoConstants.GENERATE_UIN;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ROOT_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_GENERATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

@Component
public class IdRepoServiceHelper {

    /** Mosip logger. */
    private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoServiceHelper.class);

    /** Class name used as the logger component id. */
    private static final String ID_REPO_SERVICE_HELPER = "IdRepoServiceHelper";

    /** Field name reported in {@code INVALID_INPUT_PARAMETER} errors. */
    private static final String REQUEST = "request";

    /** Maximum number of schema versions kept in each cache below. */
    private static final int MAX_SCHEMA_CACHE_SIZE = 100;

    /**
     * Identity schema JSON by schema version, fetched from the sync-data service. Bounded
     * (least recently used entry evicted) and synchronized.
     */
    private static final Map<String, String> schemaMap = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_SCHEMA_CACHE_SIZE;
                }
            });

    /**
     * Field ids marked {@code "handle": true} in the identity schema, by schema version.
     * Filled together with {@link #schemaMap}; bounded and synchronized the same way.
     */
    private static final Map<String, List<String>> supportedHandlesInSchema = Collections.synchronizedMap(
            new LinkedHashMap<String, List<String>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
                    return size() > MAX_SCHEMA_CACHE_SIZE;
                }
            });

    /** Mapper. */
    @Autowired
    private ObjectMapper mapper;

    /** Rest builder for the sync-data (schema) and UIN generator calls. */
    @Autowired
    private RestRequestBuilder restBuilder;

    /** Rest helper for the sync-data (schema) and UIN generator calls. */
    @Autowired
    private RestHelper restHelper;

    /** Security manager used to salt and hash handles. */
    @Autowired
    private IdRepoSecurityManager securityManager;

    /** Uin hash salt repo; handle hashes use the same salts as UIN hashes. */
    @Autowired
    private UinHashSaltRepo uinHashSaltRepo;

    /** URL of the identity mapping JSON, loaded once at startup. */
    @Value("${" + IdRepoConstants.IDENTITY_MAPPING_JSON + "}")
    private String identityMappingJson;

    /**
     * Postfix appended to a handle value by field id (for example {@code @phone}); a field
     * without an entry uses {@code @<fieldId>}.
     */
    @Value("#{${" + IdRepoConstants.IDENTITY_FIELDID_HANDLE_POSTFIX_MAPPING + "}}")
    private Map<String, String> fieldIdHandlePostfixMapping;

    /** Identity mapping parsed from {@link #identityMappingJson}. */
    private IdentityMapping identityMapping;


    /**
     * Loads the identity mapping from {@link #identityMappingJson} at startup.
     *
     * @throws IOException when the mapping cannot be read or parsed
     */
    @PostConstruct
    private void initialize() throws IOException {
        try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
            identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
                    IdentityMapping.class);;
        }
    }

    /**
     * Returns the identity mapping loaded at startup (schema version, selected handles and
     * other mapped field ids).
     *
     * @return identity mapping
     */
    public IdentityMapping getIdentityMapping() {
        return this.identityMapping;
    }


    /**
     * Converts a request or identity object to a JSON map through the shared mapper.
     *
     * @param identity request, identity object or map
     * @return the object as a {@code Map<String, Object>}
     * @throws IdRepoAppException {@code INVALID_INPUT_PARAMETER} ({@code request}) when the
     *         object cannot be serialized or read back as a map
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
     * Returns the identity schema JSON of a schema version. On a cache miss the schema is
     * fetched from the sync-data service and cached together with its handle field ids
     * ({@link #supportedHandlesInSchema}).
     *
     * @param schemaVersion the schema version
     * @return the schema JSON
     * @throws IdRepoAppUncheckedException {@code MISSING_INPUT_PARAMETER}
     *         ({@code identity/IDSchemaVersion}) when the version is {@code null} or
     *         {@code "null"}; {@code SCHEMA_RETRIEVE_ERROR} when the fetch fails
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
                String schema = response.getResponse().get("schemaJson");
                schemaMap.put(schemaVersion, schema);
                supportedHandlesInSchema.put(schemaVersion, getSupportedHandles(schema));
                return schema; // return directly - no recursive call needed
            }
        } catch (IdRepoDataValidationException | RestServiceException e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "getSchema", "\n" + e.getMessage());
            throw new IdRepoAppUncheckedException(IdRepoErrorConstants.SCHEMA_RETRIEVE_ERROR);
        }
    }

    /**
     * Resolves selected handle field ids to one or more {@link HandleDto} values each.
     * <p>
     * Field values may be a string, a list (optionally tagged {@code handle}), or a
     * map with a {@code value} key. When {@code selectedHandles} is absent and
     * {@code existingSelectedHandlesMap} is provided, existing field ids still present
     * in the identity (or omitted from a partial update) are kept.
     * </p>
     *
     * @param identity                    request or identity wrapper containing {@code identity}
     * @param existingSelectedHandlesMap  currently stored handles keyed by field id; may be {@code null}
     * @return mutable map of field id → handle DTOs; empty when none apply
     * @throws IdRepoAppException when the identity payload cannot be converted to a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<HandleDto>> getSelectedHandles(Object identity,
            Map<String, List<HandleDto>> existingSelectedHandlesMap) throws IdRepoAppException {
        Map<String, Object> requestMap = convertToMap(identity);
        Map<String, List<HandleDto>> selectedHandlesMap = new HashMap<>();
        if (requestMap.containsKey(ROOT_PATH) && Objects.nonNull(requestMap.get(ROOT_PATH))) {
            Map<String, Object> identityMap = (Map<String, Object>) requestMap.get(ROOT_PATH);
            String schemaVersion = String.valueOf(
                    identityMap.get(identityMapping.getIdentity().getIDSchemaVersion().getValue()));
            if (!supportedHandlesInSchema.containsKey(schemaVersion)) {
                getSchema(schemaVersion);
            }
            List<String> schemaHandles = supportedHandlesInSchema.getOrDefault(schemaVersion, List.of());
            String selectedHandlesFieldId = identityMapping.getIdentity().getSelectedHandles().getValue();
            if (identityMap.containsKey(selectedHandlesFieldId) && Objects.nonNull(identityMap.get(selectedHandlesFieldId))) {
                mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER, "getSelectedHandles",
                        String.valueOf(identityMap.get(selectedHandlesFieldId)));
                List<String> selectedHandleFieldIds = (List<String>) identityMap.get(selectedHandlesFieldId);
                if (selectedHandleFieldIds != null) {
                    for (String handleFieldId : selectedHandleFieldIds) {
                        if (handleFieldId != null && schemaHandles.contains(handleFieldId)) {
                            buildHandleDto(identityMap.get(handleFieldId), handleFieldId)
                                    .ifPresent(dtos -> selectedHandlesMap.put(handleFieldId, dtos));
                        }
                    }
                }
            } else if (existingSelectedHandlesMap != null && !existingSelectedHandlesMap.isEmpty()) {
                for (Map.Entry<String, List<HandleDto>> existing : existingSelectedHandlesMap.entrySet()) {
                    String handleFieldId = existing.getKey();
                    if (!schemaHandles.contains(handleFieldId)) {
                        continue;
                    }
                    if (identityMap.containsKey(handleFieldId) && identityMap.get(handleFieldId) != null) {
                        buildHandleDto(identityMap.get(handleFieldId), handleFieldId)
                                .ifPresent(dtos -> selectedHandlesMap.put(handleFieldId, dtos));
                    } else {
                        selectedHandlesMap.put(handleFieldId, existing.getValue());
                    }
                }
            }
        }
        return selectedHandlesMap;
    }

    /**
     * Replaces {@code selectedHandles} in the identity map with the field ids that
     * were actually accepted as handles.
     *
     * @param identityObjectMap        identity JSON as a map
     * @param newSelectedHandlesMap    handles to persist; ignored when {@code null}
     */
    public void updateSelectedHandleFields(Map<String, Object> identityObjectMap,
            Map<String, List<HandleDto>> newSelectedHandlesMap) {
        if (newSelectedHandlesMap == null || identityObjectMap == null) {
            return;
        }
        String selectedHandlesFieldId = identityMapping.getIdentity().getSelectedHandles().getValue();
        if (identityObjectMap.containsKey(selectedHandlesFieldId)) {
            identityObjectMap.put(selectedHandlesFieldId, new ArrayList<>(newSelectedHandlesMap.keySet()));
        }
    }

    /**
     * Hashes a handle with a UIN hash salt. The handle is trimmed and lower-cased first, so
     * the same handle in a different case or with surrounding spaces gives the same hash.
     *
     * @param handle handle value including its postfix, for example {@code user@x.com@email}
     * @return {@code {saltId}_{salted hash}}
     */
    public String getHandleHash(String handle) {
        String normalized = IdRepositoryUtil.getTrimmedLowercaseValue(handle);
        int saltId = securityManager.getSaltKeyForHashOfId(normalized);
        String salt = uinHashSaltRepo.retrieveSaltById(saltId);
        String saltedHash = securityManager.hashwithSalt(normalized.getBytes(StandardCharsets.UTF_8),
                CryptoUtil.decodePlainBase64(salt));
        return saltId + SPLITTER + saltedHash;
    }

    /**
     * Calls the UIN generator service and returns a new UIN string.
     *
     * <p>Uses {@link Propagation#NOT_SUPPORTED} so the caller's database
     * transaction is suspended for the duration of the HTTP call.  This
     * prevents a HikariCP connection from being held idle while waiting
     * for the remote service, which would otherwise exhaust the pool
     * under concurrent load.
     *
     * @return a freshly generated UIN
     * @throws IdRepoAppException on any failure from the remote service
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateUin() throws IdRepoAppException {
        try {
            RestRequestDTO restRequest = restBuilder.buildRequest(
                    RestServicesConstants.UIN_GENERATOR_SERVICE, null, ResponseWrapper.class);
            ResponseWrapper<Map<String, String>> response = restHelper.requestSync(restRequest);
            return response.getResponse().get("uin");
        } catch (IdRepoDataValidationException e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER,
                    GENERATE_UIN, e.getMessage());
            throw new IdRepoAppException(UNKNOWN_ERROR, e);
        } catch (RestServiceException e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_HELPER,
                    GENERATE_UIN, e.getMessage());
            throw new IdRepoAppException(UIN_GENERATION_FAILED, e);
        }
    }

    /**
     * Builds the handles of one identity field. A string gives one handle; a list gives one
     * handle per value kept by {@link #getFilteredHandleValues}; a map gives one handle from
     * its {@code value} entry. Blank values are skipped.
     *
     * @param handleValue   value of the field in the identity
     * @param handleFieldId field id, used to choose the handle postfix
     * @return the handles, or empty when the value yields none
     */
    @SuppressWarnings("unchecked")
    private Optional<List<HandleDto>> buildHandleDto(Object handleValue, String handleFieldId) {
        if (handleValue instanceof String) {
            return buildSingleHandleDto((String) handleValue, handleFieldId).map(List::of);
        }
        if (handleValue instanceof List) {
            List<HandleDto> dtos = new ArrayList<>();
            for (String value : getFilteredHandleValues((List<Object>) handleValue)) {
                buildSingleHandleDto(value, handleFieldId).ifPresent(dtos::add);
            }
            return dtos.isEmpty() ? Optional.empty() : Optional.of(dtos);
        }
        if (handleValue instanceof Map) {
            Object value = ((Map<String, Object>) handleValue).get("value");
            if (value instanceof String) {
                return buildSingleHandleDto((String) value, handleFieldId).map(List::of);
            }
        }
        return Optional.empty();
    }

    /**
     * Builds one handle: the trimmed, lower-cased value plus the field's postfix, and its hash.
     *
     * @param handleValue   raw handle value
     * @param handleFieldId field id, used to choose the handle postfix
     * @return the handle, or empty when the value is blank
     */
    private Optional<HandleDto> buildSingleHandleDto(String handleValue, String handleFieldId) {
        if (!StringUtils.hasText(handleValue)) {
            return Optional.empty();
        }
        String handle = IdRepositoryUtil.getTrimmedLowercaseValue(handleValue).concat(getHandlePostfix(handleFieldId));
        return Optional.of(new HandleDto(handle, getHandleHash(handle)));
    }

    /**
     * From a list of identity attribute values, keeps strings and map entries tagged
     * as {@code handle} (or untagged maps, which are treated as handles).
     *
     * @param handleValues list value of a handle field; may be {@code null}
     * @return the string values to turn into handles
     */
    @SuppressWarnings("unchecked")
    private List<String> getFilteredHandleValues(List<Object> handleValues) {
        List<String> values = new ArrayList<>();
        if (handleValues == null) {
            return values;
        }
        for (Object item : handleValues) {
            if (item instanceof String) {
                values.add((String) item);
            } else if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                Object rawValue = map.get("value");
                if (!(rawValue instanceof String)) {
                    continue;
                }
                if (map.containsKey("tags") && map.get("tags") instanceof List) {
                    List<?> tags = (List<?>) map.get("tags");
                    if (tags.stream().anyMatch(tag -> "handle".equals(String.valueOf(tag)))) {
                        values.add((String) rawValue);
                    }
                } else {
                    values.add((String) rawValue);
                }
            }
        }
        return values;
    }

    /**
     * Lists the identity field ids marked {@code "handle": true} in a schema.
     *
     * @param schema identity schema JSON
     * @return handle field ids; empty when the schema marks none
     */
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

    /**
     * Returns the postfix appended to handles of a field.
     *
     * @param fieldId handle field id
     * @return configured postfix, or {@code @<fieldId>} when none is configured
     */
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
