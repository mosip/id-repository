package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MODULO_VALUE;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import io.mosip.idrepository.core.dto.VidInfoResponsDTO;
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
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.fsadapter.spi.FileSystemAdapter;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.fsadapter.hdfs.constant.HDFSAdapterErrorCode;

/**
 * The Class IdRepoServiceImpl - Service implementation for Identity service.
 *
 * @author Manoj SP
 */
@Service
public class IdRepoProxyServiceImpl implements IdRepoService<IdRequestDTO, IdResponseDTO> {

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

	/** The dfs provider. */
	@Autowired
	private FileSystemAdapter fsAdapter;

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
	public IdResponseDTO retrieveIdentity(String id, IdType idType, String type, Set<String> extractionFormats)
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
	private IdResponseDTO retrieveIdentityByUin(String uin, String type, Set<String> extractionFormats)
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
	private IdResponseDTO retrieveIdentityByVid(String vid, String type, Set<String> extractionFormats)
			throws IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_UIN_BY_VID, null,
					ResponseWrapper.class);
			request.setUri(request.getUri().replace("{vid}", vid));
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(request);
			String uin = response.getResponse().get("uin");
			return retrieveIdentityByUin(uin, type, null);
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
	private IdResponseDTO retrieveIdentityByUinHash(String type, String uinHash, Set<String> extractionFormats)
			throws IdRepoAppException {
		List<DocumentsDTO> documents = new ArrayList<>();
		Uin uinObject = service.retrieveIdentity(uinHash, IdType.UIN, type, null);
		if (Objects.isNull(type)) {
			mosipLogger.info(IdRepoSecurityManager.getUser(), RETRIEVE_IDENTITY, "method - " + RETRIEVE_IDENTITY,
					"filter - null");
			return constructIdResponse(this.id.get(READ), uinObject, null);
		} else if (type.equalsIgnoreCase(BIO)) {
			getFiles(uinObject, documents, extractionFormats, BIOMETRICS);
			mosipLogger.info(IdRepoSecurityManager.getUser(), RETRIEVE_IDENTITY, "filter - bio",
					"bio documents  --> " + documents);
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
	private IdResponseDTO retrieveIdentityByRid(String rid, String type, Set<String> extractionFormats)
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
	private void getFiles(Uin uinObject, List<DocumentsDTO> documents, Set<String> extractionFormats, String type) {
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
				byte[] data = securityManager
						.decrypt(IOUtils.toByteArray(fsAdapter.getFile(uinObject.getUinHash(), fileName)));
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
			} catch (FSAdapterException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						"\n" + e.getMessage());
				throw new IdRepoAppUncheckedException(
						e.getErrorCode().equals(HDFSAdapterErrorCode.FILE_NOT_FOUND_EXCEPTION.getErrorCode())
								? FILE_NOT_FOUND
								: FILE_STORAGE_ACCESS_ERROR,
						e);
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
	private void getBiometricFiles(Uin uinObject, List<DocumentsDTO> documents, Set<String> extractionFormats) {
		uinObject.getBiometrics().stream().forEach(bio -> {
			if (allowedBioAttributes.contains(bio.getBiometricFileType())) {
				try {
					String fileName = BIOMETRICS + SLASH + bio.getBioFileId();
					byte[] data = null;
					if (Objects.nonNull(extractionFormats) && !extractionFormats.isEmpty()) {
						data = extractTemplates(uinObject.getUinHash(), fileName, extractionFormats);
					} else {
						data = securityManager
								.decrypt(IOUtils.toByteArray(fsAdapter.getFile(uinObject.getUinHash(), fileName)));
					}
					if (Objects.nonNull(data)) {
						if (StringUtils.equals(bio.getBiometricFileHash(), securityManager.hash(data))) {
							documents.add(new DocumentsDTO(bio.getBiometricFileType(), CryptoUtil.encodeBase64(data)));
						} else {
							mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
									DOCUMENT_HASH_MISMATCH.getErrorMessage());
							throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
						}
					}
				} catch (IdRepoAppException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				} catch (FSAdapterException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
					throw new IdRepoAppUncheckedException(
							e.getErrorCode().equals(HDFSAdapterErrorCode.FILE_NOT_FOUND_EXCEPTION.getErrorCode())
									? FILE_NOT_FOUND
									: FILE_STORAGE_ACCESS_ERROR,
							e);
				} catch (IOException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
					throw new IdRepoAppUncheckedException(FILE_STORAGE_ACCESS_ERROR, e);
				}
			}
		});
	}

	private byte[] extractTemplates(String uinHash, String fileName, Set<String> extractionFormats)
			throws IdRepoAppException {
		try {
			ExecutorService executor = Executors.newWorkStealingPool();
			List<Callable<byte[]>> callables = extractionFormats.stream()
					.map(format -> (Callable<byte[]>) () -> extractTemplate(uinHash, fileName, format))
					.collect(Collectors.toList());
			List<byte[]> extractedTemplates = executor.invokeAll(callables).stream().map(future -> {
				try {
					return future.get();
				} catch (InterruptedException | ExecutionException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
							e.getMessage());
					throw new IdRepoAppUncheckedException(BIO_EXTRACTION_ERROR, e);
				}
			}).peek(System.out::println).collect(Collectors.toList());
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
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	private byte[] extractTemplate(String uinHash, String fileName, String extractionFormat) throws IdRepoAppException {
		try {
			String extractionFileName = fileName.split(".")[0] + DOT + extractionFormat;
			if (fsAdapter.checkFileExistence(uinHash, extractionFileName)) {
				return securityManager.decrypt(IOUtils.toByteArray(fsAdapter.getFile(uinHash, fileName)));
			}
			if (fsAdapter.checkFileExistence(uinHash, fileName)) {
				RequestWrapper<BioExtractRequestDTO> request = new RequestWrapper<>();
				BioExtractRequestDTO bioExtractReq = new BioExtractRequestDTO();
				bioExtractReq.setBiometrics(CryptoUtil.encodeBase64(
						securityManager.decrypt(IOUtils.toByteArray(fsAdapter.getFile(uinHash, fileName)))));
				request.setRequest(bioExtractReq);
				RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.BIO_EXTRACTOR_SERVICE, null,
						ResponseWrapper.class);
				restRequest.setUri(restRequest.getUri().replace("{extractionFormat}", extractionFormat));
				ResponseWrapper<Map<String, String>> response = restHelper.requestSync(restRequest);
				byte[] extractedBiometrics = CryptoUtil.decodeBase64(response.getResponse().get("extractedBiometrics"));
				fsAdapter.storeFile(uinHash, fileName,
						new ByteArrayInputStream(securityManager.encrypt(extractedBiometrics)));
				return extractedBiometrics;
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
			List<VidInfoDTO> vidInfoDtos = null;
			if (isUpdate) {
				RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null,
						ResponseWrapper.class);
				restRequest.setUri(restRequest.getUri().replace("{uin}", uin));
				VidInfoResponsDTO response = restHelper.requestSync(restRequest);
				vidInfoDtos = mapper.convertValue(response.getResponse(), List.class);
			}
			
			List<String> partnerIds = getPartnerIds();
			
			if(isUpdate && (!ACTIVE.equals(status) || expiryTimestamp != null)) {
				//Event to be sent to IDA for deactivation/blocked uin state
				sendEventToIDA(uin, expiryTimestamp, status, vidInfoDtos, partnerIds, txnId);
			} else {
				//For create uin, or update uin with null expiry (active status), send event to credential service.
				sendEventsToCredService(uin, expiryTimestamp, isUpdate, vidInfoDtos, partnerIds);
			}
			
			//restHelper.requestSync(restBuilder.buildRequest(RestServicesConstants.ID_AUTH_SERVICE, request, Void.class));
		} catch (IdRepoDataValidationException | RestServiceException e) {
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

	private void sendEventToIDA(String uin, LocalDateTime expiryTimestamp, String status, List<VidInfoDTO> vidInfoDtos, List<String> partnerIds, String txnId) {
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
			sendEventToIDA(eventDto);
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

	private void sendEventToIDA(EventModel model) {
		pb.registerTopic(model.getTopic(), webSubHubUrl);
		pb.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, new HttpHeaders(), webSubHubUrl);
	}

	private void sendEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate, List<VidInfoDTO> vidInfoDtos, List<String> partnerIds) {
		List<CredentialIssueRequestDto> eventRequestsList = new ArrayList<>();
		String token = tokenIDGenerator.generateTokenID(uin, PARTNER);
		eventRequestsList.addAll(partnerIds.stream().map(partnerId -> createCredReqDto(uin, partnerId, expiryTimestamp, null, token)).collect(Collectors.toList()));
		
		if(vidInfoDtos != null) {
			List<CredentialIssueRequestDto> vidRequests = vidInfoDtos.stream()
					.flatMap(vidInfoDTO -> {
						LocalDateTime vidExpiryTime = Objects.isNull(expiryTimestamp) ? vidInfoDTO.getExpiryTimestamp() : expiryTimestamp;
						return partnerIds.stream()
								.map(partnerId -> createCredReqDto(vidInfoDTO.getVid(), partnerId, 
														vidExpiryTime,
														vidInfoDTO.getTransactionLimit(), token));
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

	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp, Integer transactionLimit, String token) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(retrieveIdHashWithAttributes(id));
		data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(expiryTimestamp));
		data.put(TRANSACTION_LIMIT, Optional.ofNullable(transactionLimit).map(String::valueOf).orElse(null));
		data.put(TOKEN, token);

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
