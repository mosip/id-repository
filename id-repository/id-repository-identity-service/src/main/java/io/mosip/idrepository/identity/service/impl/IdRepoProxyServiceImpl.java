package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RECORD_EXISTS;
import static io.mosip.kernel.biometrics.constant.BiometricType.FACE;
import static io.mosip.kernel.biometrics.constant.BiometricType.FINGER;
import static io.mosip.kernel.biometrics.constant.BiometricType.IRIS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.repository.UinHistoryRepo;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * The Class IdRepoServiceImpl - Service implementation for Identity service.
 *
 * @author Manoj SP
 */
@Service
public class IdRepoProxyServiceImpl implements IdRepoService<IdRequestDTO, IdResponseDTO> {

	private static final List<BiometricType> SUPPORTED_MODALITIES = List.of(FINGER, IRIS, FACE);

	/** The Constant GET_FILES. */
	private static final String GET_FILES = "getFiles";

	/** The Constant UPDATE_IDENTITY. */
	private static final String UPDATE_IDENTITY = "updateIdentity";

	/** The Constant MOSIP_ID_UPDATE. */
	private static final String MOSIP_ID_UPDATE = "mosip.id.update";

	/** The Constant ADD_IDENTITY. */
	private static final String ADD_IDENTITY = "addIdentity";

	/** The mosip logger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoProxyServiceImpl.class);

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

	@Autowired
	private ObjectStoreHelper objectStoreHelper;

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

	@Autowired
	private UinDraftRepo uinDraftRepo;

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

	/** The cbeff util. */
	@Autowired
	private CbeffUtil cbeffUtil;

	@Autowired
	private BiometricExtractionService biometricExtractionService;

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
					|| uinDraftRepo.existsByRegId(request.getRequest().getRegistrationId())
					|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
						RECORD_EXISTS.getErrorMessage());
				throw new IdRepoAppException(RECORD_EXISTS);
			} else {
				Uin uinEntity = service.addIdentity(request, uin);
				return constructIdResponse(this.id.get(CREATE), uinEntity, null);
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getErrorText());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
					"\n" + e.getMessage());
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
		case VID:
			return retrieveIdentityByVid(id, type, extractionFormats);
		case ID:
			return retrieveIdentityByRid(id, type, extractionFormats);
		case UIN:
		default:
			return retrieveIdentityByUin(id, type, extractionFormats);
		}
	}

	/**
	 * Retrieve identity by uin.
	 *
	 * @param uin               the uin
	 * @param type              the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByUin(String uin, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String uinHash = retrieveUinHash(uin);
			return retrieveIdentityByUinHash(type, uinHash, extractionFormats);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException | IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			String errorCode = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorCode()
					: ((IdRepoAppUncheckedException) e).getErrorCode();
			String errorMsg = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorText()
					: ((IdRepoAppUncheckedException) e).getErrorText();
			throw new IdRepoAppException(errorCode, errorMsg, e);
		}
	}

	/**
	 * Retrieve identity by vid.
	 *
	 * @param vid               the vid
	 * @param type              the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByVid(String vid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		String uin = getUinByVid(vid);
		return retrieveIdentityByUin(uin, type, extractionFormats);
	}

	/**
	 * Retrieve uin hash.
	 *
	 * @param uin the uin
	 * @return the string
	 */
	private String retrieveUinHash(String uin) {
		int saltId = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String hashwithSalt = securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
		return saltId + SPLITTER + hashwithSalt;
	}

	/**
	 * Retrieve identity by uin hash.
	 *
	 * @param type    the type
	 * @param uinHash the uin hash
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByUinHash(String type, String uinHash, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		List<DocumentsDTO> documents = new ArrayList<>();
		Uin uinObject = service.retrieveIdentity(uinHash, IdType.UIN, type, null);
		if (StringUtils.containsIgnoreCase(type, BIO) || StringUtils.containsIgnoreCase(type, ALL)) {
			getFiles(uinObject, documents, extractionFormats, BIOMETRICS);
		}
		if (StringUtils.containsIgnoreCase(type, DEMO) || StringUtils.containsIgnoreCase(type, ALL)) {
			getFiles(uinObject, documents, null, DEMOGRAPHICS);
		}
		return constructIdResponse(this.id.get(READ), uinObject, documents);
	}

	/**
	 * Retrieve identity by rid.
	 *
	 * @param rid               the rid
	 * @param type              the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	IdResponseDTO retrieveIdentityByRid(String rid, String type, Map<String, String> extractionFormats)
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
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException | IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			String errorCode = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorCode()
					: ((IdRepoAppUncheckedException) e).getErrorCode();
			String errorMsg = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorText()
					: ((IdRepoAppUncheckedException) e).getErrorText();
			throw new IdRepoAppException(errorCode, errorMsg, e);
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
	private void getFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats,
			String type) {
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
				String uinHash = uinObject.getUinHash().split("_")[1];
				byte[] data = objectStoreHelper.getDemographicObject(uinHash, demo.getDocId());
				if (demo.getDocHash().equals(securityManager.hash(data))) {
					documents.add(new DocumentsDTO(demo.getDoccatCode(), CryptoUtil.encodeToURLSafeBase64(data)));
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
							DOCUMENT_HASH_MISMATCH.getErrorMessage());
					throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
				}
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						"\n" + e.getMessage());
				throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
			}
		});
	}

	/**
	 * Gets the biometric files.
	 *
	 * @param uinObject         the uin object
	 * @param documents         the documents
	 * @param extractionFormats
	 * @return the biometric files
	 */
	private void getBiometricFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats) {
		uinObject.getBiometrics().stream().forEach(bio -> {
			if (allowedBioAttributes.contains(bio.getBiometricFileType())) {
				try {
					String uinHash = uinObject.getUinHash().split("_")[1];
					byte[] data = objectStoreHelper.getBiometricObject(uinHash, bio.getBioFileId());
					if (Objects.nonNull(data)) {
						if (Objects.nonNull(extractionFormats) && !extractionFormats.isEmpty()) {
							byte[] extractedData = getBiometricsForRequestedFormats(uinHash, bio.getBioFileId(),
									extractionFormats, data);
							if (Objects.nonNull(extractedData)) {
								documents.add(new DocumentsDTO(bio.getBiometricFileType(),
										CryptoUtil.encodeToURLSafeBase64(extractedData)));
							}

						} else {
							if (StringUtils.equals(bio.getBiometricFileHash(), securityManager.hash(data))) {
								documents.add(
										new DocumentsDTO(bio.getBiometricFileType(),
												CryptoUtil.encodeToURLSafeBase64(data)));
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
				}
			}
		});
	}

	protected byte[] getBiometricsForRequestedFormats(String uinHash, String fileName,
			Map<String, String> extractionFormats, byte[] originalData) throws IdRepoAppException {
		try {
			List<BIR> originalBirs = cbeffUtil.getBIRDataFromXML(originalData);
			List<BIR> finalBirs = new ArrayList<>();

			List<CompletableFuture<List<BIR>>> extractionFutures = new ArrayList<>();

			for (BiometricType modality : SUPPORTED_MODALITIES) {
				List<BIR> birTypesForModality = originalBirs.stream()
						.filter(bir -> bir.getBdbInfo().getType().get(0).value().equalsIgnoreCase(modality.value()))
						.collect(Collectors.toList());

				Optional<Entry<String, String>> extractionFormatForModality = extractionFormats.entrySet().stream()
						.filter(ent -> ent.getKey().toLowerCase().contains(modality.value().toLowerCase())).findAny();

				if (!extractionFormatForModality.isEmpty()) {
					Entry<String, String> format = extractionFormatForModality.get();
					CompletableFuture<List<BIR>> extractTemplateFuture = biometricExtractionService.extractTemplate(
							uinHash, fileName, format.getKey(), format.getValue(), birTypesForModality);
					extractionFutures.add(extractTemplateFuture);

				} else {
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
							"GETTING NON EXTRACTED FORMAT for Modality: " + modality.name());
					finalBirs.addAll(birTypesForModality);
				}
			}

			CompletableFuture.allOf(extractionFutures.toArray(new CompletableFuture<?>[extractionFutures.size()]))
					.join();
			for (CompletableFuture<List<BIR>> future : extractionFutures) {
				finalBirs.addAll(future.get());
			}

			return cbeffUtil.createXML(finalBirs);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
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
						|| uinDraftRepo.existsByRegId(request.getRequest().getRegistrationId())
						|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
							RECORD_EXISTS.getErrorMessage());
					throw new IdRepoAppException(RECORD_EXISTS);
				}

				service.updateIdentity(request, uin);
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
	 * This function takes an individualId and an IdType as input and returns the
	 * RID in the
	 * form of a ResponseWrapper object
	 * 
	 * @param individualId The ID of the individual whose RID is to be retrieved.
	 * @param idType       The type of ID that you're passing in.
	 * @return ResponseWrapper<String>
	 */
	@Override
	public ResponseWrapper<String> getRidByIndividualId(String individualId, IdType idType) throws IdRepoAppException {
		ResponseWrapper<String> responseWrapper = new ResponseWrapper<>();
		switch (idType) {
		case VID:
			individualId = getUinByVid(individualId);
		case UIN:
			individualId = retrieveRidByUin(individualId);
			responseWrapper.setResponse(individualId);
			break;
		case ID:
			if (uinRepo.existsByRegId(individualId)) {
				responseWrapper.setResponse(individualId);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getRidByIndividualId",
						"NO_RECORD_FOUND");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			break;
		}
		return responseWrapper;
	}

	/**
	 * It retrieves the RID of an individual by their UIN
	 * 
	 * @param individualId The UIN of the individual
	 * @return The RID is being returned.
	 */
	private String retrieveRidByUin(String individualId) throws IdRepoAppException {
		String uinHash = retrieveUinHash(individualId);
		if (uinRepo.existsByUinHash(uinHash)) {
			return uinRepo.getRidByUinHash(uinHash);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "retrieveRidByUin",
					"NO_RECORD_FOUND");
			throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	/**
	 * It takes a VID as input and returns the corresponding UIN
	 * 
	 * @param vid Virtual ID
	 * @return The response is a map of key value pairs.
	 */
	private String getUinByVid(String vid) throws IdRepoDataValidationException, IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_UIN_BY_VID, null,
					ResponseWrapper.class);
			request.setUri(request.getUri().replace("{vid}", vid));
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(request);
			return response.getResponse().get("UIN");
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
	 * Construct id response.
	 *
	 * @param id               the id
	 * @param uin              the uin
	 * @param documents        the documents
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	@SuppressWarnings("unchecked")
	private IdResponseDTO constructIdResponse(String id, Uin uin, List<DocumentsDTO> documents)
			throws IdRepoAppException {
		IdResponseDTO idResponse = new IdResponseDTO();
		idResponse.setId(id);
		idResponse.setVersion(EnvUtil.getAppVersion());
		ResponseDTO response = new ResponseDTO();
		response.setStatus(uin.getStatusCode());
		if (id.equals(this.id.get(READ))) {
			if (!Objects.isNull(documents)) {
				response.setDocuments(documents);
			}
			ObjectNode identityObject = convertToObject(uin.getUinData(), ObjectNode.class);
			response.setVerifiedAttributes(mapper.convertValue(identityObject.get("verifiedAttributes"), List.class));
			identityObject.remove("verifiedAttributes");
			response.setIdentity(identityObject);
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
	 * @throws IdRepoAppException the id repo app exception
	 */
	private <T> T convertToObject(byte[] identity, Class<T> clazz) throws IdRepoAppException {
		try {
			return mapper.readValue(identity, clazz);
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "convertToObject", e.getMessage());
			throw new IdRepoAppException(ID_OBJECT_PROCESSING_FAILED, e);
		}
	}

}