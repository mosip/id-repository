package io.mosip.idrepository.identity.service.impl;

import io.mosip.kernel.core.util.DateUtils2;
import static io.mosip.idrepository.core.constant.IdRepoConstants.*;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RECORD_EXISTS;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;

import io.mosip.idrepository.core.constant.*;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.IdVidMetadataResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UinBiometric;
import io.mosip.idrepository.identity.entity.UinDocument;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.repository.UinHistoryRepo;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.idrepository.manager.CredentialStatusManager;
import io.mosip.idrepository.pipeline.InProcessVidClient;
import io.mosip.idrepository.pipeline.PendingCredentialSync;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * Proxy retrieve implementation routing reads through policy and partner filters.
 */
@Service
public class IdRepoProxyServiceImpl implements IdRepoService<IdRequestDTO, IdResponseDTO> {

	/** Logger for identity API calls; also receives the per-method performance log. */
	public static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoProxyServiceImpl.class);

	@Autowired
	/** Reads encrypted biometric and document files, and extracted templates, from the object store. */
	private ObjectStoreHelper objectStoreHelper;

	/** Converts identity JSON bytes into objects for the response. */
	@Autowired
	private ObjectMapper mapper;

	/** API ids by operation (for example {@code create}, {@code read}, {@code update}), used as the response {@code id}. */
	@Resource
	private Map<String, String> id;

	/**
	 * Biometric file categories that a retrieve may return (for example {@code individualBiometrics}).
	 * Biometric files of any other category are left out of the response.
	 */
	@Resource
	private List<String> allowedBioAttributes;

	/** Repository of live {@code uin} rows; used to find RIDs and UIN hashes. */
	@Autowired
	private UinRepo uinRepo;

	/** Repository of {@code uin_draft}; an add or update is rejected while a draft exists for the RID. */
	@Autowired
	private UinDraftRepo uinDraftRepo;

	/** Repository of {@code uin_h}; used to find an identity by an older RID. */
	@Autowired
	private UinHistoryRepo uinHistoryRepo;

	/** Database-level identity service ({@link IdRepoServiceImpl}) that this class delegates to. */
	@Autowired
	private IdRepoService<IdRequestDTO, Uin> service;

	/**
	 * Runs credential issuance queued by an add or update once its transaction has committed.
	 * Lazy to break a bean cycle.
	 */
	@Autowired
	@Lazy
	private CredentialStatusManager credentialStatusManager;

	/** Hashes UINs and file content for lookups and integrity checks. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** Repository of {@code uin_hash_salt}; salts for the UIN hash. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	/** Parses and writes CBEFF XML when combining extracted templates. */
	@Autowired
	private CbeffUtil cbeffUtil;

	@Autowired
	/** Extracts biometric templates in the formats a caller asks for. */
	private BiometricExtractionService biometricExtractionService;

	/** Publishes identity created and updated events to WebSub for IDA. */
	@Autowired
	private IdRepoWebSubHelper websubHelper;

	@Autowired
	/** Spring environment; supplies settings such as the active status. */
	private Environment env;

	@Autowired
	/** Repository of {@code handle}; used to find an identity by handle. */
	private HandleRepo handleRepo;

	@Autowired
	/** Builds handle hashes for lookups. */
	private IdRepoServiceHelper idRepoServiceHelper;

	/** Resolves a VID to its UIN inside this JVM (no HTTP call). Lazy to break a bean cycle. */
	@Autowired
	@Lazy
	private InProcessVidClient inProcessVidClient;

	/** Key of the registration id in identity event data. */
	private static final String REGISTRATION_ID = "registration_id";

	@Value("${" + IDA_EVENT_TYPE_NAMESPACE + ":" + IDA_EVENT_TYPE_NAMESPACE_DEFAULT + "}")
	/** Namespace set as the type of identity events sent to IDA. */
	private String idaEventTypeNamespace;

	@Value("${" + IDA_EVENT_TYPE_NAME + ":" + IDA_EVENT_TYPE_NAME_DEFAULT + "}")
	/** Name set as the type of identity events sent to IDA. */
	private String idaEventTypeName;

	@Value("${mosip.idrepo.bio.extraction.timeout-seconds:30}")
	/**
	 * Maximum seconds to wait for the template extractions of one CBEFF file
	 * ({@code mosip.idrepo.bio.extraction.timeout-seconds}, default 30). On timeout the
	 * retrieve fails.
	 */
	private long extractionTimeoutSeconds;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.kernel.core.idrepo.spi.IdRepoService#addIdentity(java.lang.Object)
	 */
	@Override
	/**
	 * Add identity.
	 * @param request request
	 * @param uin uin
	 * @return id response dto
	 */
	public IdResponseDTO addIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			String uinHash = retrieveUinHash(uin);
			if (uinRepo.existsByUinHash(uinHash)
					|| uinDraftRepo.existsByRegId(request.getRequest().getRegistrationId())
					|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
						RECORD_EXISTS.getErrorMessage());
				throw new IdRepoAppException(RECORD_EXISTS);
			}

			Uin uinEntity = service.addIdentity(request, uin);

			notify(uin, false, request.getRequest().getRegistrationId());
			return constructIdResponse(this.id.get(CREATE), uinEntity, null);

		} catch (IdRepoAppException e) {
			PendingCredentialSync.clear();
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getErrorText());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			PendingCredentialSync.clear();
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			PendingCredentialSync.clear();
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			credentialStatusManager.runPendingAfterIdentityCommit();
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl", ADD_IDENTITY, startNanos);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.kernel.core.idrepo.spi.IdRepoService#retrieveIdentity(java.lang.
	 * String)
	 */
	/**
	 * Retrieve identity.
	 * <p>
	 * Must run in a single idrepo transaction so lazy {@code Uin.biometrics} / {@code Uin.documents}
	 * collections remain initialized while biometric and demographic files are loaded (in-process
	 * credential issuance has no HTTP open-session-in-view).
	 * </p>
	 *
	 * @param id                  individual identifier
	 * @param idType              UIN, VID, RID, or handle
	 * @param type                identity slice (bio, demo, all)
	 * @param extractionFormats   partner biometric format map
	 * @return identity response DTO
	 */
	@Override
	@Transactional(rollbackFor = { IdRepoAppException.class, IdRepoAppUncheckedException.class })
	public IdResponseDTO retrieveIdentity(String id, IdType idType, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"RETRIEVE_IDENTITY START idType=" + idType + ", type=" + type + ", id=" + id
							+ ", extractionFormatKeys="
							+ (extractionFormats == null || extractionFormats.isEmpty() ? "[]"
									: extractionFormats.keySet()));
			switch (idType) {
				case HANDLE:
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
							"RETRIEVE_IDENTITY route=retrieveIdentityByHandle");
					return retrieveIdentityByHandle(id, type, extractionFormats);
				case VID:
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
							"RETRIEVE_IDENTITY route=retrieveIdentityByVid");
					return retrieveIdentityByVid(id, type, extractionFormats);
				case ID:
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
							"RETRIEVE_IDENTITY route=retrieveIdentityByRid");
					return retrieveIdentityByRid(id, type, extractionFormats);
				case UIN:
				default:
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
							"RETRIEVE_IDENTITY route=retrieveIdentityByUin");
					return retrieveIdentityByUin(id, type, extractionFormats);
			}
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl", RETRIEVE_IDENTITY, startNanos);
		}
	}

	/**
	 * Retrieves an identity by plaintext UIN.
	 *
	 * @param uin               plaintext UIN
	 * @param type              which files to return: {@code bio}, {@code demo}, {@code all}, or none
	 * @param extractionFormats requested extraction format by modality; may be {@code null}
	 * @return the identity response
	 * @throws IdRepoAppException {@code DATABASE_ACCESS_ERROR} on a database failure; any other
	 *                            failure keeps its error code and message
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
	 * Retrieves an identity by VID: resolves the VID to its UIN in-process, then retrieves by UIN.
	 *
	 * @param vid               the VID
	 * @param type              which files to return: {@code bio}, {@code demo}, {@code all}, or none
	 * @param extractionFormats requested extraction format by modality; may be {@code null}
	 * @return the identity response
	 * @throws IdRepoAppException when the VID is invalid or inactive, or the retrieve fails
	 */
	private IdResponseDTO retrieveIdentityByVid(String vid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		String uin = getUinByVid(vid);
		return retrieveIdentityByUin(uin, type, extractionFormats);
	}

	/**
	 * Builds the stored form of a UIN hash, {@code saltId_hash}, using the salt from
	 * {@code uin_hash_salt} picked by the modulo rule.
	 *
	 * @param uin plaintext UIN
	 * @return the UIN hash as stored in {@code uin.uin_hash}
	 * @throws IdRepoAppUncheckedException {@code INVALID_INPUT_PARAMETER} for {@code id} when the
	 *                                     UIN is not numeric
	 */
	protected String retrieveUinHash(String uin) {
		try {
			int saltId = securityManager.getSaltKeyForId(uin);
			String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
			String hashwithSalt = securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
			return saltId + SPLITTER + hashwithSalt;
		} catch (NumberFormatException e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id"), e);
		}
	}

	/**
	 * Retrieves an identity by its stored UIN hash and adds the requested files.
	 * <p>
	 * When both biometric and demographic files are wanted, the two sets load in parallel.
	 * Biometric files come first in the response.
	 * </p>
	 *
	 * @param type              which files to return: contains {@code bio}, {@code demo} or {@code all}
	 *                          (case-insensitive), or none for identity data only
	 * @param uinHash           stored UIN hash ({@code saltId_hash})
	 * @param extractionFormats requested extraction format by modality; may be {@code null}
	 * @return the identity response with the read API id
	 * @throws IdRepoAppException when the identity is not found or a file fails to load
	 */
	private IdResponseDTO retrieveIdentityByUinHash(String type, String uinHash, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		List<DocumentsDTO> documents = new ArrayList<>();
		Uin uinObject = service.retrieveIdentity(uinHash, IdType.UIN, type, null);
		boolean wantBio = StringUtils.containsIgnoreCase(type, BIO) || StringUtils.containsIgnoreCase(type, ALL);
		boolean wantDemo = StringUtils.containsIgnoreCase(type, DEMO) || StringUtils.containsIgnoreCase(type, ALL);
		if (wantBio && wantDemo) {
			List<UinBiometric> bios = new ArrayList<>(uinObject.getBiometrics());
			List<UinDocument> demos = new ArrayList<>(uinObject.getDocuments());
			String objectUinHash = uinObject.getUinHash().split("_")[1];
			CompletableFuture<List<DocumentsDTO>> bioFuture = CompletableFuture
					.supplyAsync(() -> loadBiometricFiles(objectUinHash, bios, extractionFormats));
			CompletableFuture<List<DocumentsDTO>> demoFuture = CompletableFuture
					.supplyAsync(() -> loadDemographicFiles(objectUinHash, demos));
			try {
				documents.addAll(bioFuture.join());
				documents.addAll(demoFuture.join());
			} catch (CompletionException e) {
				throw unwrapIdRepoFailure(e);
			}
		} else {
			if (wantBio) {
				getFiles(uinObject, documents, extractionFormats, BIOMETRICS);
			}
			if (wantDemo) {
				getFiles(uinObject, documents, null, DEMOGRAPHICS);
			}
		}
		return constructIdResponse(this.id.get(READ), uinObject, documents);
	}

	/**
	 * Turns a failure from the parallel biometric and demographic loads into the checked
	 * exception thrown by retrieve. The original error code and message are kept.
	 *
	 * @param e failure from {@link CompletableFuture#join()}
	 * @return the cause as an {@link IdRepoAppException}; any other cause becomes {@code DATABASE_ACCESS_ERROR}
	 */
	private IdRepoAppException unwrapIdRepoFailure(CompletionException e) {
		Throwable cause = e.getCause() != null ? e.getCause() : e;
		if (cause instanceof IdRepoAppUncheckedException unchecked) {
			return new IdRepoAppException(unchecked.getErrorCode(), unchecked.getErrorText(), unchecked);
		} else if (cause instanceof IdRepoAppException idRepoAppException) {
			return idRepoAppException;
		}
		return new IdRepoAppException(DATABASE_ACCESS_ERROR, cause);
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
	public IdResponseDTO retrieveIdentityByRid(String rid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
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
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl", "retrieveIdentityByRid", startNanos);
		}
	}

	/**
	 * Loads one kind of file for an identity and appends it to {@code documents}.
	 *
	 * @param uinObject         the identity with its file rows
	 * @param documents         response documents; changed in place
	 * @param extractionFormats requested extraction format by modality; used only for biometrics
	 * @param type              {@code BIOMETRICS} or {@code DEMOGRAPHICS}; any other value loads nothing
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
	 * Loads all demographic documents of an identity and appends them to {@code documents}.
	 * The object-store folder is the part of the stored UIN hash after the salt-id prefix.
	 *
	 * @param uinObject the identity with its document rows
	 * @param documents response documents; changed in place
	 */
	private void getDemographicFiles(Uin uinObject, List<DocumentsDTO> documents) {
		String uinHash = uinObject.getUinHash().split("_")[1];
		documents.addAll(loadDemographicFiles(uinHash, new ArrayList<>(uinObject.getDocuments())));
	}

	/**
	 * Loads all demographic documents of an identity. A single document is loaded on the
	 * calling thread; several are loaded in parallel and returned in their original order.
	 *
	 * @param uinHash  UIN hash without the salt-id prefix; the identity's object-store folder
	 * @param demoDocs document rows of the identity; may be {@code null}
	 * @return documents with Base64 content; empty when there are none
	 * @throws IdRepoAppUncheckedException if any document fails to load (see {@link #loadDemographicFile})
	 */
	private List<DocumentsDTO> loadDemographicFiles(String uinHash, List<UinDocument> demoDocs) {
		List<DocumentsDTO> documents = new ArrayList<>();
		if (demoDocs == null || demoDocs.isEmpty()) {
			return documents;
		}
		if (demoDocs.size() == 1) {
			documents.add(loadDemographicFile(uinHash, demoDocs.get(0)));
			return documents;
		}
		List<CompletableFuture<DocumentsDTO>> futures = demoDocs.stream()
				.map(demo -> CompletableFuture.supplyAsync(() -> loadDemographicFile(uinHash, demo)))
				.toList();
		for (CompletableFuture<DocumentsDTO> future : futures) {
			try {
				documents.add(future.join());
			} catch (CompletionException e) {
				throw wrapUnchecked(e);
			}
		}
		return documents;
	}

	/**
	 * Reads and decrypts one demographic document and checks it against the hash stored on its row.
	 *
	 * @param uinHash UIN hash without the salt-id prefix
	 * @param demo    document row; supplies the file id, category and expected hash
	 * @return the document category with URL-safe Base64 content
	 * @throws IdRepoAppUncheckedException {@code DOCUMENT_HASH_MISMATCH} when the content does not
	 *                                     match the hash, or the object-store error when the read fails
	 */
	private DocumentsDTO loadDemographicFile(String uinHash, UinDocument demo) {
		try {
			byte[] data = objectStoreHelper.getDemographicObject(uinHash, demo.getDocId());
			if (demo.getDocHash().equals(securityManager.hash(data))) {
				return new DocumentsDTO(demo.getDoccatCode(), CryptoUtil.encodeToURLSafeBase64(data));
			}
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
					DOCUMENT_HASH_MISMATCH.getErrorMessage());
			throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
					"\n" + e.getMessage());
			throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Loads all biometric files of an identity and appends them to {@code documents}.
	 * The object-store folder is the part of the stored UIN hash after the salt-id prefix.
	 *
	 * @param uinObject         the identity with its biometric rows
	 * @param documents         response documents; changed in place
	 * @param extractionFormats requested extraction format by modality; may be {@code null}
	 */
	private void getBiometricFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats) {
		String uinHash = uinObject.getUinHash().split("_")[1];
		documents.addAll(loadBiometricFiles(uinHash, new ArrayList<>(uinObject.getBiometrics()), extractionFormats));
	}

	/**
	 * Loads all biometric files of an identity one after another, in their original order.
	 *
	 * @param uinHash           UIN hash without the salt-id prefix
	 * @param bios              biometric rows of the identity; may be {@code null}
	 * @param extractionFormats requested extraction format by modality; {@code null} or empty returns the stored CBEFF
	 * @return loaded files; files skipped by {@link #loadBiometricFile} are left out
	 * @throws IdRepoAppUncheckedException if any file fails to load
	 */
	private List<DocumentsDTO> loadBiometricFiles(String uinHash, List<UinBiometric> bios,
			Map<String, String> extractionFormats) {
		List<DocumentsDTO> documents = new ArrayList<>();
		if (bios == null || bios.isEmpty()) {
			return documents;
		}
		for (UinBiometric bio : bios) {
			DocumentsDTO loaded = loadBiometricFile(uinHash, bio, extractionFormats);
			if (loaded != null) {
				documents.add(loaded);
			}
		}
		return documents;
	}

	/**
	 * Reads and decrypts one biometric CBEFF file and, when formats are requested, replaces it
	 * with extracted templates.
	 * <p>
	 * With extraction formats, the result of {@link #getBiometricsForRequestedFormats} is
	 * returned and the stored hash is not checked. Without them, the stored CBEFF is returned
	 * after its hash is checked.
	 * </p>
	 *
	 * @param uinHash           UIN hash without the salt-id prefix
	 * @param bio               biometric row; supplies the file id, category and expected hash
	 * @param extractionFormats requested extraction format by modality; may be {@code null}
	 * @return the file category with URL-safe Base64 content, or {@code null} when the category is
	 *         not in {@link #allowedBioAttributes}, the object is missing, or extraction returned nothing
	 * @throws IdRepoAppUncheckedException {@code DOCUMENT_HASH_MISMATCH} on a hash mismatch, or the
	 *                                     object-store or extraction error
	 */
	private DocumentsDTO loadBiometricFile(String uinHash, UinBiometric bio, Map<String, String> extractionFormats) {
		if (!allowedBioAttributes.contains(bio.getBiometricFileType())) {
			return null;
		}
		try {
			byte[] data = objectStoreHelper.getBiometricObject(uinHash, bio.getBioFileId());
			if (Objects.isNull(data)) {
				return null;
			}
			if (Objects.nonNull(extractionFormats) && !extractionFormats.isEmpty()) {
				byte[] extractedData = getBiometricsForRequestedFormats(uinHash, bio.getBioFileId(),
						extractionFormats, data);
				return Objects.nonNull(extractedData)
						? new DocumentsDTO(bio.getBiometricFileType(), CryptoUtil.encodeToURLSafeBase64(extractedData))
						: null;
			}
			if (StringUtils.equals(bio.getBiometricFileHash(), securityManager.hash(data))) {
				return new DocumentsDTO(bio.getBiometricFileType(), CryptoUtil.encodeToURLSafeBase64(data));
			}
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
					DOCUMENT_HASH_MISMATCH.getErrorMessage());
			throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
			throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Turns a failure from a parallel document load into an unchecked exception that can cross
	 * the stream and future boundary. The original error code and message are kept.
	 *
	 * @param e failure from {@link CompletableFuture#join()}
	 * @return the cause as an {@link IdRepoAppUncheckedException}; any other cause becomes {@code DATABASE_ACCESS_ERROR}
	 */
	private static IdRepoAppUncheckedException wrapUnchecked(CompletionException e) {
		Throwable cause = e.getCause() != null ? e.getCause() : e;
		if (cause instanceof IdRepoAppUncheckedException unchecked) {
			return unchecked;
		} else if (cause instanceof IdRepoAppException idRepoAppException) {
			return new IdRepoAppUncheckedException(idRepoAppException.getErrorCode(),
					idRepoAppException.getErrorText(), idRepoAppException);
		}
		return new IdRepoAppUncheckedException(DATABASE_ACCESS_ERROR, cause);
	}

	/**
	 * Get biometrics for requested formats.
	 * @param uinHash uin hash
	 * @param fileName file name
	 * @param extractionFormats extraction formats
	 * @param originalData original data
	 * @return byte[]
	 */
	protected byte[] getBiometricsForRequestedFormats(String uinHash, String fileName,
													  Map<String, String> extractionFormats, byte[] originalData) throws IdRepoAppException {
		try {
			List<BIR> originalBirs = cbeffUtil.getBIRDataFromXML(originalData);
			List<BIR> finalBirs = new ArrayList<>();

			List<CompletableFuture<List<BIR>>> extractionFutures = new ArrayList<>();

			for (BiometricType modality : SUPPORTED_MODALITIES) {
				List<BIR> birTypesForModality = originalBirs.stream()
						.filter(bir -> {
							List<BiometricType> types = bir.getBdbInfo().getType();
							return !types.isEmpty() && types.get(0).value().equalsIgnoreCase(modality.value());
						})
						.filter(bir -> {
							Map<String, String> others = bir.getOthers();
							return others == null || "false".equalsIgnoreCase(others.get("EXCEPTION"));
						})
						.collect(Collectors.toList());

				Optional<Entry<String, String>> extractionFormatForModality = extractionFormats.entrySet().stream()
						.filter(ent -> ent.getKey().toLowerCase().contains(modality.value().toLowerCase())).findAny();

				if (!extractionFormatForModality.isEmpty()&& !birTypesForModality.isEmpty()) {
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

			originalBirs.clear();

			try {
				CompletableFuture.allOf(extractionFutures.toArray(new CompletableFuture<?>[0]))
						.get(extractionTimeoutSeconds, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				extractionFutures.forEach(f -> f.cancel(true));
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
						"Biometric extraction timed out after " + extractionTimeoutSeconds + "s");
				throw new IdRepoAppUncheckedException(BIO_EXTRACTION_ERROR, e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof IdRepoAppUncheckedException) {
					throw (IdRepoAppUncheckedException) cause;
				}
				throw new IdRepoAppUncheckedException(BIO_EXTRACTION_ERROR, cause);
			}
			for (CompletableFuture<List<BIR>> future : extractionFutures) {
				finalBirs.addAll(future.getNow(Collections.emptyList()));
			}

			return cbeffUtil.createXML(finalBirs);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (InterruptedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			Thread.currentThread().interrupt();
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	/**
	 * V2 draft variant of {@code getBiometricsForRequestedFormats}. Builds a combined CBEFF
	 * in which each supported modality (finger, iris, face) is replaced by its extracted
	 * templates when a format was requested for it, or kept as stored otherwise. BIRs marked
	 * as exceptions are left out.
	 * <p>
	 * Extraction runs in parallel through
	 * {@link io.mosip.idrepository.core.spi.BiometricExtractionService#extractTemplateDraft},
	 * which stores results under {@code _draft/{ridHash}/Biometrics/}. Publishing the draft
	 * then moves them to the live path with the rest of the draft files. The wait for all
	 * extractions is bounded by {@code mosip.idrepo.bio.extraction.timeout-seconds}.
	 * </p>
	 *
	 * @param ridHash           rid hash of the draft
	 * @param fileName          file id of the draft CBEFF
	 * @param extractionFormats modality key to SDK format
	 * @param originalData      decrypted draft CBEFF XML
	 * @return combined CBEFF XML
	 * @throws IdRepoAppException {@code BIO_EXTRACTION_ERROR} when extraction fails, times out
	 *         or is interrupted
	 */
	protected byte[] getBiometricsForRequestedFormatsDraft(String ridHash, String fileName,
													  Map<String, String> extractionFormats, byte[] originalData) throws IdRepoAppException {
		try {
			List<BIR> originalBirs = cbeffUtil.getBIRDataFromXML(originalData);
			List<BIR> finalBirs = new ArrayList<>();

			List<CompletableFuture<List<BIR>>> extractionFutures = new ArrayList<>();

			for (BiometricType modality : SUPPORTED_MODALITIES) {
				List<BIR> birTypesForModality = originalBirs.stream()
						.filter(bir -> {
							List<BiometricType> types = bir.getBdbInfo().getType();
							return !types.isEmpty() && types.get(0).value().equalsIgnoreCase(modality.value());
						})						.filter(bir -> {
							Map<String, String> others = bir.getOthers();
							return others == null || "false".equalsIgnoreCase(others.get("EXCEPTION"));
						})
						.collect(Collectors.toList());
				Optional<Entry<String, String>> extractionFormatForModality = extractionFormats.entrySet().stream()
						.filter(ent -> ent.getKey().toLowerCase().contains(modality.value().toLowerCase())).findAny();

				if (!extractionFormatForModality.isEmpty()&& !birTypesForModality.isEmpty()) {
					Entry<String, String> format = extractionFormatForModality.get();
					CompletableFuture<List<BIR>> extractTemplateFuture = biometricExtractionService.extractTemplateDraft(
							ridHash, fileName, format.getKey(), format.getValue(), birTypesForModality);
					extractionFutures.add(extractTemplateFuture);

				} else {
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
							"GETTING NON EXTRACTED FORMAT for Modality: " + modality.name());
					finalBirs.addAll(birTypesForModality);
				}
			}

			originalBirs.clear(); // release parsed BIR list before blocking on futures - reduces live set during wait

			try {
				CompletableFuture.allOf(extractionFutures.toArray(new CompletableFuture<?>[0]))
						.get(extractionTimeoutSeconds, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				extractionFutures.forEach(f -> f.cancel(true));
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
						"Biometric extraction timed out after " + extractionTimeoutSeconds + "s");
				throw new IdRepoAppUncheckedException(BIO_EXTRACTION_ERROR, e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof IdRepoAppUncheckedException)
					throw (IdRepoAppUncheckedException) cause;
				throw new IdRepoAppUncheckedException(BIO_EXTRACTION_ERROR, cause);
			}
			for (CompletableFuture<List<BIR>> future : extractionFutures) {
				finalBirs.addAll(future.getNow(Collections.emptyList()));
			}

			return cbeffUtil.createXML(finalBirs);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (InterruptedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			Thread.currentThread().interrupt();
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
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
	/**
	 * Update identity.
	 * @param request request
	 * @param uin uin
	 * @return id response dto
	 */
	public IdResponseDTO updateIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		long startNanos = System.nanoTime();
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
				Uin uinObject=service.updateIdentity(request, uin);
				mosipLogger.info("Uin updated");
				String activeStatus = env.getProperty(ACTIVE_STATUS);
				if (activeStatus != null && activeStatus.equalsIgnoreCase(uinObject.getStatusCode())) {
					mosipLogger.info("Uin is in active status");
					notify(uin, true, request.getRequest().getRegistrationId());
				}
				return constructIdResponse(MOSIP_ID_UPDATE, uinObject, null);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						NO_RECORD_FOUND.getErrorMessage());
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (IdRepoAppException e) {
			PendingCredentialSync.clear();
			throw e;
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			PendingCredentialSync.clear();
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, UPDATE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} finally {
			credentialStatusManager.runPendingAfterIdentityCommit();
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl", UPDATE_IDENTITY, startNanos);
		}
	}

	/**
	 * This function takes an individualId and an IdType as input and returns the
	 * RID in the
	 * form of a ResponseWrapper object
	 *
	 * @param individualId The ID of the individual whose RID is to be retrieved.
	 * @param idType       The type of ID that you're passing in.
	 * @return String
	 */
	@Override
	/**
	 * Get rid by individual id.
	 * @param individualId individual id
	 * @param idType id type
	 * @return string
	 */
	public String getRidByIndividualId(String individualId, IdType idType) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			switch (idType) {
				case VID:
					individualId = getUinByVid(individualId);
				case UIN:
					individualId = retrieveRidByUin(individualId);
					return individualId;
				case ID:
					if (uinRepo.existsByRegId(individualId)) {
						return individualId;
					}
				default:
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getRidByIndividualId",
							"NO_RECORD_FOUND");
					throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl", "getRidByIndividualId", startNanos);
		}
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
	 * Retrieves the id vid metadata information for a given individual
	 * based on the provided ID and its type.
	 *
	 * @param individualId the identifier of the individual whose metadata is to be fetched.
	 * @param idType The type of ID that you're passing in.
	 * @return an {@code IdVidMetaDataResponseDTO} containing rid, createdOn, and updatedOn
	 * @throws IdRepoAppException if there is an error during retrieval
	 */
	public IdVidMetadataResponseDTO getIdVidMetadata(String individualId, IdType idType) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			switch (idType) {
				case VID:
					individualId = getUinByVid(individualId);
				case UIN:
					String uinHash = retrieveUinHash(individualId);
					Optional<Uin> uin = uinRepo.findByUinHash(uinHash);
					if (uin.isPresent()) {
						return getIdVidMetadataResponseDTO(uin.get());
					}
					break;
				case ID:
					Optional<Uin> uinOptional = uinRepo.findByRegId(individualId);
					if (uinOptional.isPresent()) {
						return getIdVidMetadataResponseDTO(uinOptional.get());
					}
					break;
			}
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getIdVidMetadata",
					"NO_RECORD_FOUND");
			throw new IdRepoAppException(NO_RECORD_FOUND);
		} catch (DataAccessException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL,
					"getIdVidMetadata", "DATABASE_ERROR");
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR);
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl", "getIdVidMetadata", startNanos);
		}
	}

	/**
	 * It takes a VID as input and returns the corresponding UIN
	 *
	 * @param vid Virtual ID
	 * @return The response is a map of key value pairs.
	 */
	protected String getUinByVid(String vid) throws IdRepoDataValidationException, IdRepoAppException {
		return inProcessVidClient.getUinByVid(vid);
	}

	/**
	 * This function is used to get the maximum allowed update count of an attribute
	 * for the given individual id
	 *
	 * @param individualId  The UIN of the individual
	 * @param idType        The type of the ID. For example, UIN, RID, VID, etc.
	 * @param attributeList List of attributes for which the update count is to be
	 *                      retrieved.
	 * @return A map of attribute name and the maximum allowed update count for that
	 *         attribute.
	 */
	@Override
	public Map<String, Integer> getRemainingUpdateCountByIndividualId(String individualId, IdType idType,
																	  List<String> attributeList) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			String uinHash = getUinHash(individualId, idType);
			return service.getRemainingUpdateCountByIndividualId(uinHash, idType,
					Objects.isNull(attributeList) ? List.of() : attributeList);
		} catch (NumberFormatException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "id"), e);
		} catch (IdRepoAppUncheckedException e) {
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, "IdRepoProxyServiceImpl",
					"getRemainingUpdateCountByIndividualId", startNanos);
		}
	}


	/**
	 * It takes in an individualId and an IdType, and returns the UIN hash of the
	 * individualId
	 *
	 * @param individualId The ID of the individual.
	 * @param idType       This is the type of the id that you are passing. It can
	 *                     be UIN, VID or RID.
	 * @return The UIN hash is being returned.
	 */
	private String getUinHash(String individualId, IdType idType)
			throws IdRepoDataValidationException, IdRepoAppException {
		switch (idType) {
			case VID:
				individualId = getUinByVid(individualId);
			case UIN:
				return retrieveUinHash(individualId);
			case ID:
				if (uinRepo.existsByRegId(individualId)) {
					return uinRepo.getUinHashByRid(individualId);
				}
			default:
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getRidByIndividualId",
						"NO_RECORD_FOUND");
				throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	/**
	 * Builds the API response for an identity.
	 * <p>
	 * Every response carries the status. Only a read response also carries the identity JSON,
	 * the documents and the verified attributes, which are moved out of the identity JSON
	 * into their own field.
	 * </p>
	 *
	 * @param id        API id for the response; the read id turns on the identity and documents
	 * @param uin       the identity
	 * @param documents loaded files; may be {@code null}
	 * @return the response
	 * @throws IdRepoAppException {@code ID_OBJECT_PROCESSING_FAILED} when the stored identity JSON cannot be read
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
			response.setIdentity(mapper.convertValue(identityObject, Map.class));
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

	/**
	 * Sends the identity created or updated event to IDA. A failure is logged and not thrown,
	 * so the add or update still succeeds.
	 *
	 * @param uin      plaintext UIN of the identity
	 * @param isUpdate {@code true} for {@code IDENTITY_UPDATED}, {@code false} for {@code IDENTITY_CREATED}
	 * @param txnId    registration id; used as the event's transaction id
	 */
	private void notify(String uin,boolean isUpdate, String txnId) {
		try {
			sendGenericIdentityEvents(uin, isUpdate, txnId);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", e.getMessage());
		}
	}

	/**
	 * Builds a WebSub event published by ID Repository, with a new event id, the current UTC
	 * time and the configured IDA event type.
	 *
	 * @param topic         WebSub topic, which is the event type name
	 * @param eventData     event payload
	 * @param transactionId transaction id stored on the event
	 * @return the event model ready to publish
	 */
	private EventModel createEventModel(String topic, Map<String, Object> eventData, String transactionId) {
		EventModel model = new EventModel();
		model.setPublisher(ID_REPO);
		String dateTime = DateUtils2.formatToISOString(DateUtils2.getUTCCurrentDateTime());
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
		event.setData(eventData);
		model.setEvent(event);
		model.setTopic(topic);
		return model;
	}

	/**
	 * Publishes an event to its WebSub topic.
	 *
	 * @param model event to publish
	 */
	private void sendEventToWebsub(EventModel model) {
		mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendEventToWebsub",
				"Publising event to topic: " + model.getTopic());
		websubHelper.publishEvent(model);
	}

	/**
	 * Builds and publishes the {@code IDENTITY_CREATED} or {@code IDENTITY_UPDATED} event.
	 * The payload holds the salted UIN hash ({@code id_hash}) and the registration id; the
	 * plaintext UIN is never sent.
	 *
	 * @param uin            plaintext UIN; only its hash is sent
	 * @param isUpdate       chooses the event type
	 * @param registrationId registration id; sent in the payload and as the transaction id
	 */
	private void sendGenericIdentityEvents(String uin, boolean isUpdate, String registrationId) {
		mosipLogger.info("Inside sendGenericIdentityEvents");
		EventType eventType = isUpdate ? IDAEventType.IDENTITY_UPDATED : IDAEventType.IDENTITY_CREATED;
		Map<String, Object> eventData = new HashMap<>();
		eventData.put(ID_HASH, getIdHash(uin));
		eventData.put(REGISTRATION_ID, registrationId);
		String topic = eventType.toString();
		EventModel eventModel = createEventModel(topic, eventData, registrationId);
		sendEventToWebsub(eventModel);
	}

	/**
	 * Hashes the UIN with its salt from {@code uin_hash_salt}. The salt id is picked from the UIN
	 * by the modulo rule. Unlike the stored {@code uin_hash}, the result has no salt-id prefix.
	 *
	 * @param uin plaintext UIN
	 * @return salted hash of the UIN
	 */
	private String getIdHash(String uin) {
		int saltId = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		return securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
	}

	/**
	 * Retrieves an identity by one of its handles (for example a phone-number handle).
	 *
	 * @param handle            handle value as sent by the caller
	 * @param type              which files to return: {@code bio}, {@code demo}, {@code all}, or none
	 * @param extractionFormats requested extraction format by modality; may be {@code null}
	 * @return the identity response
	 * @throws IdRepoAppException {@code NO_RECORD_FOUND} when no identity has the handle,
	 *                            {@code DATABASE_ACCESS_ERROR} on a database failure, or the retrieve error
	 */
	private IdResponseDTO retrieveIdentityByHandle(String handle, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String handleHash = idRepoServiceHelper.getHandleHash(handle);
			Optional<Handle> entity = handleRepo.findByHandleHash(handleHash);
			if (entity.isPresent() && (entity.get().getStatus() == null
					|| HandleStatusLifecycle.ACTIVATED.name().equals(entity.get().getStatus()))) {
				return retrieveIdentityByUinHash(type, entity.get().getUinHash(), extractionFormats);
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
	 * Builds the metadata response for an identity: its RID and its created and updated times
	 * in ISO format. Times that are not set are left out.
	 *
	 * @param uin the identity
	 * @return the metadata response
	 */
	private IdVidMetadataResponseDTO getIdVidMetadataResponseDTO(Uin uin) {
		IdVidMetadataResponseDTO metadataResponseDTO = new IdVidMetadataResponseDTO();
		metadataResponseDTO.setRid(uin.getRegId());
		if (uin.getUpdatedDateTime() != null) {
			metadataResponseDTO.setUpdatedOn(DateUtils2.formatToISOString(uin.getUpdatedDateTime()));
		}
		if (uin.getCreatedDateTime() != null) {
			metadataResponseDTO.setCreatedOn(DateUtils2.formatToISOString(uin.getCreatedDateTime()));
		}
		return metadataResponseDTO;
	}
}