package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MODULO_VALUE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RECORD_EXISTS;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.Event;
import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.Type;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.repository.UinHashSaltRepo;
import io.mosip.idrepository.identity.repository.UinHistoryRepo;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.spi.PublisherClient;

/**
 * The Class IdRepoServiceImpl - Service implementation for Identity service.
 *
 * @author Manoj SP
 */
@Service
public class IdRepoProxyServiceImpl implements IdRepoService<IdRequestDTO, IdResponseDTO> {

	private static final String COOKIE = "Cookie";

	private static final String ID_TYPE = "idType";

	private static final String TOKEN = "TOKEN";

	private static final String PARTNER = "PARTNER";

	private static final String SALT = "SALT";

	private static final String MODULO = "MODULO";

	private static final String ID_HASH = "id_hash";

	private static final String EXPIRY_TIMESTAMP = "expiry_timestamp";

	private static final String TRANSACTION_LIMIT = "transaction_limit";

	private static final String ID_REPO = "ID_REPO";
	
	private static final String IDA = "IDA";

	private static final String PARNER_ACTIVE_STATUS = "Active";

	private static final String AUTH = "AUTH";

	private static final String ACTIVE = "ACTIVE";

	private static final String BLOCKED = "BLOCKED";

	/** The Constant GET_FILES. */
	private static final String GET_FILES = "getFiles";

	/** The Constant UPDATE_IDENTITY. */
	private static final String UPDATE_IDENTITY = "updateIdentity";

	/** The Constant MOSIP_ID_UPDATE. */
	private static final String MOSIP_ID_UPDATE = "mosip.id.update";

	/** The Constant ADD_IDENTITY. */
	private static final String ADD_IDENTITY = "addIdentity";

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoProxyServiceImpl.class);

	/** The Constant TYPE. */
	private static final String TYPE = "type";

	/** The Constant SLASH. */
	private static final String SLASH = "/";

	/** The Constant RETRIEVE_IDENTITY. */
	private static final String RETRIEVE_IDENTITY = "retrieveIdentity";

	/** The Constant BIOMETRICS. */
	private static final String BIOMETRICS = "Biometrics";

	/** The Constant BIO. */
	private static final String BIO = "bio";

	/** The Constant DEMO. */
	private static final String DEMO = "demo";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String ID_REPO_SERVICE_IMPL = "IdRepoServiceImpl";

	/** The Constant CREATE. */
	private static final String CREATE = "create";

	/** The Constant READ. */
	private static final String READ = "read";

	/** The Constant ALL. */
	private static final String ALL = "all";

	/** The Constant DEMOGRAPHICS. */
	private static final String DEMOGRAPHICS = "Demographics";

	/** The Constant DOT. */
	private static final String DOT = ".";
	
	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	private String objectStoreAccountName;
	
	@Autowired
	@Qualifier("S3Adapter")
	private ObjectStoreAdapter objectStore;

	/** The env. */
	@Autowired
	private Environment env;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** The id. */
	@Resource
	private Map<String, String> id;

	/** The allowed bio types. */
	@Resource
	private List<String> allowedBioAttributes;

	/** The uin repo. */
	@Autowired
	private UinRepo uinRepo;

	/** The uin history repo. */
	@Autowired
	private UinHistoryRepo uinHistoryRepo;

	/** The service. */
	@Autowired
	private IdRepoService<IdRequestDTO, Uin> service;

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private RestHelper restHelper;

	@Autowired
	private RestRequestBuilder restBuilder;
	
	@Value("${id-repo-ida-event-type-namespace:mosip}")
	private  String idaEventTypeNamespace;
	
	@Value("${id-repo-ida-event-type-name:ida}")
	private  String idaEventTypeName;
	
	@Value("${id-repo-websub-hub-url}")
	private String webSubHubUrl;
	
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb; 
	
	@Autowired
	private TokenIDGenerator tokenIDGenerator;
	
	/** The cbeff util. */
	@Autowired
	private CbeffUtil cbeffUtil;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.kernel.core.idrepo.spi.IdRepoService#addIdentity(java.lang.Object)
	 */
	@Override
	public IdResponseDTO addIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		try {
			if (uinRepo.existsByUinHash(retrieveUinHash(uin))
					|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
						RECORD_EXISTS.getErrorMessage());
				throw new IdRepoAppException(RECORD_EXISTS);
			} else {
				Uin uinEntity = service.addIdentity(request, uin);
				notify(uin, null, null, false, request.getRequest().getRegistrationId());
				return constructIdResponse(this.id.get(CREATE), uinEntity, null);
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getErrorText());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.kernel.core.idrepo.spi.IdRepoService#retrieveIdentity(java.lang.
	 * String)
	 */
	@Override
	public IdResponseDTO retrieveIdentity(String id, IdType idType, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		switch (idType) {
		case UIN:
			return retrieveIdentityByUin(id, type, extractionFormats);
		case VID:
			return retrieveIdentityByVid(id, type, extractionFormats);
		case REG_ID:
			return retrieveIdentityByRid(id, type, extractionFormats);
		default:
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getIdType", "Invalid ID");
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					INVALID_INPUT_PARAMETER.getErrorMessage(), "id");
		}
	}

	/**
	 * Retrieve identity by uin.
	 *
	 * @param uin
	 *            the uin
	 * @param type
	 *            the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByUin(String uin, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String uinHash = retrieveUinHash(uin);
			if (uinRepo.existsByUinHash(uinHash)) {
				return retrieveIdentityByUinHash(type, uinHash, extractionFormats);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
						NO_RECORD_FOUND.getErrorMessage());
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Retrieve identity by vid.
	 *
	 * @param vid
	 *            the vid
	 * @param type
	 *            the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByVid(String vid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_UIN_BY_VID, null,
					ResponseWrapper.class);
			request.setUri(request.getUri().replace("{vid}", vid));
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(request);
			String uin = response.getResponse().get("uin");
			return retrieveIdentityByUin(uin, type, extractionFormats);
		} catch (RestServiceException e) {
			if (e.getResponseBodyAsString().isPresent()) {
				List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString().get());
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
						"\n" + errorList);
				throw new IdRepoAppException(errorList.get(0).getErrorCode(), errorList.get(0).getMessage());
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
						"\n" + e.getMessage());
				throw new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR);
			}
		}
	}

	/**
	 * Retrieve uin hash.
	 *
	 * @param uin
	 *            the uin
	 * @return the string
	 */
	private String retrieveUinHash(String uin) {
		Integer moduloValue = env.getProperty(MODULO_VALUE, Integer.class);
		int modResult = (int) (Long.parseLong(uin) % moduloValue);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		return modResult + SPLITTER + securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
	}
	
	private Map<String, String> retrieveIdHashWithAttributes(String id) {
		Map<String, String> hashWithAttributes = new HashMap<>();
		Integer moduloValue = env.getProperty(MODULO_VALUE, Integer.class);
		int modResult = (int) (Long.parseLong(id) % moduloValue);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		String hash =  modResult + SPLITTER + securityManager.hashwithSalt(id.getBytes(), hashSalt.getBytes());
		hashWithAttributes.put(ID_HASH, hash);
		hashWithAttributes.put(MODULO, String.valueOf(modResult));
		hashWithAttributes.put(SALT, hashSalt);
		return hashWithAttributes;
	}

	/**
	 * Retrieve identity by uin hash.
	 *
	 * @param type the type
	 * @param uinHash the uin hash
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByUinHash(String type, String uinHash, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		List<DocumentsDTO> documents = new ArrayList<>();
		Uin uinObject = service.retrieveIdentity(uinHash, IdType.UIN, type, null);
		if (Objects.isNull(type)) {
			mosipLogger.info(IdRepoSecurityManager.getUser(), RETRIEVE_IDENTITY, "method - " + RETRIEVE_IDENTITY,
					"filter - null");
			return constructIdResponse(this.id.get(READ), uinObject, null);
		} else if (type.equalsIgnoreCase(BIO)) {
			getFiles(uinObject, documents, extractionFormats, BIOMETRICS);
			return constructIdResponse(this.id.get(READ), uinObject, documents);
		} else if (type.equalsIgnoreCase(DEMO)) {
			getFiles(uinObject, documents, null, DEMOGRAPHICS);
			mosipLogger.info(IdRepoSecurityManager.getUser(), RETRIEVE_IDENTITY, "filter - demo",
					"docs documents  --> " + documents);
			return constructIdResponse(this.id.get(READ), uinObject, documents);
		} else if (type.equalsIgnoreCase(ALL)) {
			getFiles(uinObject, documents, extractionFormats, BIOMETRICS);
			getFiles(uinObject, documents, null, DEMOGRAPHICS);
			mosipLogger.info(IdRepoSecurityManager.getUser(), RETRIEVE_IDENTITY, "filter - all",
					"docs documents  --> " + documents);
			return constructIdResponse(this.id.get(READ), uinObject, documents);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE));
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE));
		}
	}

	/**
	 * Retrieve identity by rid.
	 *
	 * @param rid
	 *            the rid
	 * @param type
	 *            the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByRid(String rid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String uinHash = uinRepo.getUinHashByRid(rid);
			if (Objects.isNull(uinHash)) {
				uinHash = uinHistoryRepo.getUinHashByRid(rid);
			}
			if (Objects.nonNull(uinHash)) {
				return retrieveIdentityByUinHash(type, uinHash, extractionFormats);
			} else {
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY, "\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Gets the files.
	 *
	 * @param uinObject the uin object
	 * @param documents the documents
	 * @param type      the type
	 * @return the files
	 */
	private void getFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats, String type) {
		if (type.equals(BIOMETRICS)) {
			getBiometricFiles(uinObject, documents, extractionFormats);
		}

		if (type.equals(DEMOGRAPHICS)) {
			getDemographicFiles(uinObject, documents);
		}
	}

	/**
	 * Gets the demographic files.
	 *
	 * @param uinObject the uin object
	 * @param documents the documents
	 * @return the demographic files
	 */
	private void getDemographicFiles(Uin uinObject, List<DocumentsDTO> documents) {
		uinObject.getDocuments().stream().forEach(demo -> {
			try {
				String fileName = DEMOGRAPHICS + SLASH + demo.getDocId();
				if (!objectStore.exists(objectStoreAccountName, uinObject.getUinHash().substring(4, 67).toLowerCase(),
						fileName)) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getDemographicFiles",
							"FILE NOT FOUND IN OBJECT STORE");
					throw new IdRepoAppUncheckedException(FILE_NOT_FOUND);
				}
				byte[] data = securityManager.decrypt(IOUtils.toByteArray(objectStore.getObject(objectStoreAccountName,
						uinObject.getUinHash().substring(4, 67).toLowerCase(), fileName)));
				if (demo.getDocHash().equals(securityManager.hash(data))) {
					documents.add(new DocumentsDTO(demo.getDoccatCode(), CryptoUtil.encodeBase64(data)));
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
							DOCUMENT_HASH_MISMATCH.getErrorMessage());
					throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
				}
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						"\n" + e.getMessage());
				throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
			} catch (IOException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						"\n" + e.getMessage());
				throw new IdRepoAppUncheckedException(FILE_STORAGE_ACCESS_ERROR, e);
			}
		});
	}

	/**
	 * Gets the biometric files.
	 *
	 * @param uinObject
	 *            the uin object
	 * @param documents
	 *            the documents
	 * @param extractionFormats
	 * @return the biometric files
	 */
	private void getBiometricFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats) {
		uinObject.getBiometrics().stream().forEach(bio -> {
			if (allowedBioAttributes.contains(bio.getBiometricFileType())) {
				try {
					String fileName = BIOMETRICS + SLASH + bio.getBioFileId();
					String uinHash = uinObject.getUinHash().substring(4, 67).toLowerCase();
					if (!objectStore.exists(objectStoreAccountName,
							uinHash, fileName)) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getBiometricFiles",
								"FILE NOT FOUND IN OBJECT STORE");
						throw new IdRepoAppUncheckedException(FILE_NOT_FOUND);
					}
					byte[] data = null;
					if (Objects.nonNull(extractionFormats) && !extractionFormats.isEmpty()) {
						data = extractTemplates(uinHash, fileName, extractionFormats);
						if (Objects.nonNull(data)) {
							documents.add(new DocumentsDTO(bio.getBiometricFileType(), CryptoUtil.encodeBase64(data)));
						}
					} else {
						data = securityManager.decrypt(IOUtils.toByteArray(
								objectStore.getObject(objectStoreAccountName,
										uinHash, fileName)));
						if (Objects.nonNull(data)) {
							if (StringUtils.equals(bio.getBiometricFileHash(), securityManager.hash(data))) {
								documents.add(new DocumentsDTO(bio.getBiometricFileType(), CryptoUtil.encodeBase64(data)));
							} else {
								mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
										DOCUMENT_HASH_MISMATCH.getErrorMessage());
								throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
							}
						}
					}
				} catch (IdRepoAppException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				} catch (IOException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
					throw new IdRepoAppUncheckedException(FILE_STORAGE_ACCESS_ERROR, e);
				}
			}
		});
	}

	private byte[] extractTemplates(String uinHash, String fileName, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
					"EXTRACTING FORMAT: " + extractionFormats.toString());
			
			List<byte[]> extractedTemplates = new ArrayList<>();
			int i = 0;
			CompletableFuture<?>[] extractTemplateFuture = new CompletableFuture<?>[extractionFormats.size()];
			for (Entry<String, String> extractionFormat : extractionFormats.entrySet()) {
				extractTemplateFuture[i++] = extractTemplate(uinHash, fileName, extractionFormat.getKey(),
						extractionFormat.getValue());
			}
			CompletableFuture.allOf(extractTemplateFuture).join();
			for (int j = 0; j < extractTemplateFuture.length; j++) {
				extractedTemplates.add((byte[]) extractTemplateFuture[j].get());
			}
			extractedTemplates.remove(null);
			List<BIRType> birTypeList = new ArrayList<>();
			if (!extractedTemplates.isEmpty()) {
				for (byte[] template : extractedTemplates) {
					birTypeList.addAll(cbeffUtil.getBIRDataFromXML(template));
				}
			}
			return cbeffUtil.createXML(cbeffUtil.convertBIRTypeToBIR(birTypeList));
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (Exception e) {
			ExceptionUtils.getStackTrace(e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	@Async
	private CompletableFuture<byte[]> extractTemplate(String uinHash, String fileName, String extractionType, String extractionFormat) throws IdRepoAppException {
		try {
			String extractionFileName = fileName.split("\\.")[0] + DOT + extractionType;
			//TODO need to remove AmazonS3Exception handling
			try {
				if (objectStore.exists(objectStoreAccountName, uinHash, extractionFileName)) {
					return CompletableFuture.completedFuture(securityManager.decrypt(IOUtils
							.toByteArray(objectStore.getObject(objectStoreAccountName, uinHash, extractionFileName))));
				}
			} catch (AmazonS3Exception e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
						e.getMessage());
			}
			if (objectStore.exists(objectStoreAccountName, uinHash, fileName)) {
				RequestWrapper<BioExtractRequestDTO> request = new RequestWrapper<>();
				BioExtractRequestDTO bioExtractReq = new BioExtractRequestDTO();
				byte[] data = securityManager
						.decrypt(IOUtils.toByteArray(objectStore.getObject(objectStoreAccountName, uinHash, fileName)));
				bioExtractReq.setBiometrics(CryptoUtil.encodeBase64(data));
				request.setRequest(bioExtractReq);
				RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.BIO_EXTRACTOR_SERVICE, request,
						ResponseWrapper.class);
				restRequest.setUri(restRequest.getUri().replace("{extractionFormat}", extractionFormat));
				ResponseWrapper<Map<String, String>> response = restHelper.requestSync(restRequest);
				byte[] extractedBiometrics = CryptoUtil.decodeBase64(response.getResponse().get("extractedBiometrics"));
				objectStore.putObject(objectStoreAccountName, uinHash, extractionFileName,
						new ByteArrayInputStream(securityManager.encrypt(extractedBiometrics)));
				return CompletableFuture.completedFuture(extractedBiometrics);
			} else {
				return null;
			}
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
			throw new IdRepoAppUncheckedException(FILE_STORAGE_ACCESS_ERROR, e);
		} catch (AmazonS3Exception e) {
			//TODO need to remove AmazonS3Exception handling
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
			throw new IdRepoAppUncheckedException(FILE_NOT_FOUND, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.core.idrepo.spi.IdRepoService#updateIdentity(java.lang.
	 * Object, java.lang.String)
	 */
	@Override
	public IdResponseDTO updateIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		String regId = request.getRequest().getRegistrationId();
		try {
			String uinHash = retrieveUinHash(uin);
			if (uinRepo.existsByUinHash(uinHash)) {
				if (uinRepo.existsByRegId(regId)
						|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
							RECORD_EXISTS.getErrorMessage());
					throw new IdRepoAppException(RECORD_EXISTS);
				}

				Uin uinObject = service.updateIdentity(request, uin);
				if (Objects.nonNull(request.getRequest().getStatus())
						&& !env.getProperty(ACTIVE_STATUS).equalsIgnoreCase(request.getRequest().getStatus())) {
					notify(uin, uinObject.getUpdatedDateTime(), request.getRequest().getStatus(), true, request.getRequest().getRegistrationId());
				} else {
					notify(uin, null, null, true, request.getRequest().getRegistrationId());
				}
				return constructIdResponse(MOSIP_ID_UPDATE, service.retrieveIdentity(uinHash, IdType.UIN, null, null),
						null);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						NO_RECORD_FOUND.getErrorMessage());
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, UPDATE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * Construct id response.
	 *
	 * @param id        the id
	 * @param uin       the uin
	 * @param documents the documents
	 * @return the id response DTO
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private IdResponseDTO constructIdResponse(String id, Uin uin, List<DocumentsDTO> documents)
			throws IdRepoAppException {
		IdResponseDTO idResponse = new IdResponseDTO();
		idResponse.setId(id);
		idResponse.setVersion(env.getProperty(APPLICATION_VERSION));
		ResponseDTO response = new ResponseDTO();
		response.setStatus(uin.getStatusCode());
		if (id.equals(this.id.get(READ))) {
			if (!Objects.isNull(documents)) {
				response.setDocuments(documents);
			}
			response.setIdentity(convertToObject(uin.getUinData(), Object.class));
		}
		idResponse.setResponse(response);
		return idResponse;
	}

	/**
	 * Convert Identity to object.
	 *
	 * @param identity the identity
	 * @param clazz    the clazz
	 * @return the object
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private Object convertToObject(byte[] identity, Class<?> clazz) throws IdRepoAppException {
		try {
			return mapper.readValue(identity, clazz);
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "convertToObject", e.getMessage());
			throw new IdRepoAppException(ID_OBJECT_PROCESSING_FAILED, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void notify(String uin, LocalDateTime expiryTimestamp, String status, boolean isUpdate, String txnId) {
		try {
			Optional<String> authToken = RestHelper.getAuthToken();
			SecurityContextHolder.getContext().getAuthentication().getDetails();
			List<VidInfoDTO> vidInfoDtos = null;
			if (isUpdate) {
				RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null,
						ResponseWrapper.class);
				restRequest.setUri(restRequest.getUri().replace("{uin}", uin));
				ResponseWrapper<Map<String, Object>> response = restHelper.requestSync(restRequest);
				vidInfoDtos = mapper.convertValue(response.getResponse().get("events"), List.class);
			}
			
			List<String> partnerIds = getPartnerIds();
			
			if(isUpdate && (!ACTIVE.equals(status) || expiryTimestamp != null)) {
				//Event to be sent to IDA for deactivation/blocked uin state
				sendEventToIDA(uin, expiryTimestamp, status, vidInfoDtos, partnerIds, txnId, authToken);
			} else {
				//For create uin, or update uin with null expiry (active status), send event to credential service.
				sendEventsToCredService(uin, expiryTimestamp, isUpdate, vidInfoDtos, partnerIds);
			}
			
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> getPartnerIds() {
		try {
			Map<String, Object> responseWrapperMap = restHelper.requestSync(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class));
			Object response = responseWrapperMap.get("response");
			if(response instanceof Map) {
				Object partners = responseWrapperMap.get("partners");
				if(partners instanceof List) {
					List<Map<String, Object>> partnersList = (List<Map<String, Object>>) partners;
					return partnersList.stream()
								.filter(partner -> PARNER_ACTIVE_STATUS.equalsIgnoreCase((String)partner.get("status")))
								.map(partner -> (String)partner.get("partnerID"))
								.collect(Collectors.toList());
				}
			}
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getPartnerIds", e.getMessage());
		}
		return Collections.emptyList();
	}

	private void sendEventToIDA(String uin, LocalDateTime expiryTimestamp, String status, List<VidInfoDTO> vidInfoDtos, List<String> partnerIds, String txnId, Optional<String> authToken) {
		List<EventModel> eventList = new ArrayList<>();
		EventType eventType = BLOCKED.equals(status) ? IDAEventType.REMOVE_ID : IDAEventType.DEACTIVATE_ID;
		eventList.addAll(createIdaEventModel(eventType, uin, expiryTimestamp, null, partnerIds, txnId).collect(Collectors.toList()));
		
		if(vidInfoDtos != null) {
			List<EventModel> idaEvents = vidInfoDtos.stream()
					.flatMap(vidInfoDTO -> createIdaEventModel(eventType,
							vidInfoDTO.getVid(), 
							expiryTimestamp,
							vidInfoDTO.getTransactionLimit(),
							partnerIds, txnId))
					.collect(Collectors.toList());
			eventList.addAll(idaEvents);
		}
		
		eventList.forEach(eventDto -> {
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", "notifying IDA for event" + eventType.toString());
			sendEventToIDA(eventDto, authToken);
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", "notified IDA for event" + eventType.toString());
		});
	}

	private Stream<EventModel> createIdaEventModel(EventType eventType, String id, LocalDateTime expiryTimestamp, Integer transactionLimit, List<String> partnerIds, String transactionId) {
		return partnerIds.stream().map(partner -> createEventModel(eventType, id, expiryTimestamp, transactionLimit, transactionId, partner));
	}

	private EventModel createEventModel(EventType eventType, String id, LocalDateTime expiryTimestamp, Integer transactionLimit, String transactionId, String partner) {
		EventModel model = new EventModel();
		model.setPublisher(ID_REPO);
		String dateTime = DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime());
		model.setPublishedOn(dateTime);
		Event event = new Event();
		event.setTimestamp(dateTime);
		String eventId = UUID.randomUUID().toString();
		event.setId(eventId);
		event.setTransactionId(transactionId);
		Type type = new Type();
		type.setNamespace(idaEventTypeNamespace);
		type.setName(idaEventTypeName);
		event.setType(type);
		Map<String, Object> data = new HashMap<>();
		data.put(ID_HASH, retrieveUinHash(id));
		data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(expiryTimestamp));
		data.put(TRANSACTION_LIMIT, transactionLimit);
		event.setData(data);
		model.setEvent(event);
		model.setTopic(partner + "/" + eventType.toString());
		return model;
	}

	private void sendEventToIDA(EventModel model, Optional<String> authToken) {
		pb.registerTopic(model.getTopic(), webSubHubUrl);
		HttpHeaders headers = new HttpHeaders();
		if(authToken.isPresent()) {
			headers.add(COOKIE, authToken.get());
		}
		pb.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, headers, webSubHubUrl);
	}

	private void sendEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate, List<VidInfoDTO> vidInfoDtos, List<String> partnerIds) {
		List<CredentialIssueRequestDto> eventRequestsList = new ArrayList<>();
		String token = tokenIDGenerator.generateTokenID(uin, PARTNER);
		eventRequestsList.addAll(partnerIds.stream().map(partnerId -> createCredReqDto(uin, partnerId, expiryTimestamp, null, token, IdType.UIN.getIdType())).collect(Collectors.toList()));
		
		if(vidInfoDtos != null) {
			List<CredentialIssueRequestDto> vidRequests = vidInfoDtos.stream()
					.flatMap(vidInfoDTO -> {
						LocalDateTime vidExpiryTime = Objects.isNull(expiryTimestamp) ? vidInfoDTO.getExpiryTimestamp() : expiryTimestamp;
						return partnerIds.stream()
								.map(partnerId -> createCredReqDto(vidInfoDTO.getVid(), partnerId, 
														vidExpiryTime,
														vidInfoDTO.getTransactionLimit(), token, IdType.VID.getIdType()));
					}).collect(Collectors.toList());
			eventRequestsList.addAll(vidRequests);
		}
		
		eventRequestsList.forEach(reqDto -> {
			CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
			requestWrapper.setRequest(reqDto);
			requestWrapper.setRequesttime(DateUtils.getUTCCurrentDateTime());
			String eventTypeDisplayName = isUpdate? "Update ID" : "Create ID";
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", "notifying Credential Service for event " + eventTypeDisplayName);
			sendRequestToCredService(requestWrapper);
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", "notified Credential Service for event" + eventTypeDisplayName);
		});
	}

	private void sendRequestToCredService(CredentialIssueRequestWrapperDto requestWrapper) {
		try {
			restHelper.requestSync(restBuilder.buildRequest(RestServicesConstants.CREDENTIAL_REQUEST_SERVICE, requestWrapper, Map.class));
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendRequestToCredService", e.getMessage());
		}
	}

	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp, Integer transactionLimit, String token, String idType) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(retrieveIdHashWithAttributes(id));
		data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(expiryTimestamp));
		data.put(TRANSACTION_LIMIT, Optional.ofNullable(transactionLimit).map(String::valueOf).orElse(null));
		data.put(TOKEN, token);
		data.put(ID_TYPE, idType);

		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setId(id);
		credentialIssueRequestDto.setCredentialType(AUTH);
		credentialIssueRequestDto.setIssuer(partnerId);
		credentialIssueRequestDto.setRecepiant(IDA);
		credentialIssueRequestDto.setUser(IdRepoSecurityManager.getUser());
		credentialIssueRequestDto.setAdditionalData(data);
		return credentialIssueRequestDto;
	}
}
