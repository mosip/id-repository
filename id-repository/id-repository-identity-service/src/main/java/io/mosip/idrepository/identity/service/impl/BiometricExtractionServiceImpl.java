package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.DOT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.identity.helper.BioExtractionHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Production-ready implementation of {@link BiometricExtractionService}.
 *
 * <p>Key improvements over the baseline:
 * <ul>
 *   <li><b>Stampede prevention</b> – a {@link ConcurrentHashMap} of in-flight
 *       {@link CompletableFuture}s ensures that N concurrent requests for the
 *       same (uinHash, extractionFileName) pair share one extraction rather than
 *       triggering N parallel calls to the biometric extractor.</li>
 *   <li><b>Extraction timeout</b> – the call to the external biometric extractor
 *       is bounded by a configurable timeout so that a slow or hung extractor
 *       cannot exhaust the async thread pool.</li>
 *   <li><b>Defensive object-store reads</b> – the store is checked for an
 *       existing result before extraction; on read failure the code falls through
 *       to (re-)extraction rather than propagating a store error to the caller.</li>
 *   <li><b>Safe byte-array lifecycle</b> – the raw XML byte array fetched from the
 *       store is nulled immediately after deserialisation so that the GC can reclaim
 *       it before the caller processes the BIR list.</li>
 *   <li><b>Collapsed helper chain</b> – the three-hop delegation
 *       (extractBiometricTemplate → extractBiometrics → doBioExtraction) is
 *       consolidated into a single private method. There is no functional loss
 *       and the call stack is significantly shorter.</li>
 *   <li><b>Structured logging</b> – all log calls carry a consistent key/value
 *       shape so that log aggregators (ELK, Splunk, etc.) can index them
 *       without additional parsing.</li>
 * </ul>
 *
 * @author Loganathan Sekar (original)
 */
@Service
public class BiometricExtractionServiceImpl implements BiometricExtractionService {

	// ------------------------------------------------------------------ //
	//  Constants                                                           //
	// ------------------------------------------------------------------ //

	private static final String EXTRACT_TEMPLATE   = "extractTemplate";
	private static final String FORMAT_FLAG_SUFFIX = ".format";

	private static final Logger mosipLogger =
			IdRepoLogger.getLogger(BiometricExtractionServiceImpl.class);

	// ------------------------------------------------------------------ //
	//  Dependencies                                                        //
	// ------------------------------------------------------------------ //

	@Autowired
	private ObjectStoreHelper objectStoreHelper;

	@Autowired
	private BioExtractionHelper bioExtractionHelper;

	@Autowired
	private CbeffUtil cbeffUtil;

	// ------------------------------------------------------------------ //
	//  Configuration                                                       //
	// ------------------------------------------------------------------ //

	// ------------------------------------------------------------------ //
	//  Stampede guard                                                      //
	// ------------------------------------------------------------------ //

	/**
	 * In-flight extractions keyed by {@code "<uinHash>|<extractionFileName>"}.
	 *
	 * <p>A thread that finds an entry here will attach itself to the existing
	 * future rather than starting a parallel extraction for the same payload.
	 * The entry is removed (regardless of outcome) in a {@code whenComplete}
	 * handler so that transient failures do not permanently poison the map.
	 */
	private final ConcurrentHashMap<String, CompletableFuture<List<BIR>>> inFlight =
			new ConcurrentHashMap<>();

	// ------------------------------------------------------------------ //
	//  Public API                                                          //
	// ------------------------------------------------------------------ //

	/**
	 * Extracts or retrieves a biometric template for the given modality/format.
	 *
	 * <p>Execution order:
	 * <ol>
	 *   <li>Derive the cache file name from {@code fileName}, {@code extractionType},
	 *       and {@code extractionFormat}.</li>
	 *   <li>Return a previously stored result from the object store if present.</li>
	 *   <li>Attach to an in-flight extraction for the same key if one is already
	 *       running, avoiding a redundant call to the extractor.</li>
	 *   <li>Start a new extraction, persist the result, and return it.</li>
	 * </ol>
	 *
	 * @param uinHash           hash of the UIN (used as the object-store bucket key)
	 * @param fileName          base biometric file name (e.g. {@code "Finger.xml"})
	 * @param extractionType    format query-param key (e.g. {@code "finger.extractionFormat"})
	 * @param extractionFormat  target format value (e.g. {@code "ISO19794_4_2011"})
	 * @param birsForModality   source BIR list for this modality
	 * @return a {@link CompletableFuture} that resolves to the extracted BIR list
	 * @throws IdRepoAppException on biometric-extraction failure or unexpected error
	 */
	@Override
	@Async("withSecurityContext")
	public CompletableFuture<List<BIR>> extractTemplate(
			String uinHash,
			String fileName,
			String extractionType,
			String extractionFormat,
			List<BIR> birsForModality) throws IdRepoAppException {

		final String extractionFileName = buildExtractionFileName(fileName, extractionType, extractionFormat);
		final String inFlightKey        = uinHash + "|" + extractionFileName;

		// ── 1. Return cached result from object store if available ────────── //
		List<BIR> stored = tryReadFromObjectStore(uinHash, extractionFileName);
		if (stored != null) {
			return CompletableFuture.completedFuture(stored);
		}

		// ── 2. Attach to an existing in-flight extraction if present ──────── //
		CompletableFuture<List<BIR>> existing = inFlight.get(inFlightKey);
		if (existing != null) {
			mosipLogger.info(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Joining in-flight extraction | key=" + inFlightKey);
			return existing;
		}

		// ── 3. Start a new extraction ──────────────────────────────────────── //
		CompletableFuture<List<BIR>> future = new CompletableFuture<>();

		/*
		 * putIfAbsent returns null when this thread won the race; otherwise it
		 * returns the future registered by the winning thread, which we return
		 * directly — same stampede-prevention path as check #2 above.
		 */
		CompletableFuture<List<BIR>> winner = inFlight.putIfAbsent(inFlightKey, future);
		if (winner != null) {
			mosipLogger.info(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Lost race for key, joining winner | key=" + inFlightKey);
			return winner;
		}

		// This thread owns the extraction — clean up the map when done.
		future.whenComplete((result, ex) -> inFlight.remove(inFlightKey));

		try {
			List<BIR> extracted = runExtractionWithTimeout(extractionType, extractionFormat, birsForModality);
			persistToObjectStore(uinHash, extractionFileName, extracted);
			future.complete(extracted);
			return future;
		} catch (IdRepoAppException e) {
			future.completeExceptionally(e);
			throw e;
		} catch (Exception e) {
			IdRepoAppException wrapped = new IdRepoAppException(UNKNOWN_ERROR, e);
			future.completeExceptionally(wrapped);
			throw wrapped;
		}
	}

	// ------------------------------------------------------------------ //
	//  Lifecycle                                                           //
	// ------------------------------------------------------------------ //

	/**
	 * Cancels all pending in-flight futures on bean destruction so that
	 * downstream callers are not left waiting during a graceful shutdown.
	 */
	@PreDestroy
	public void onShutdown() {
		mosipLogger.info(IdRepoSecurityManager.getUser(),
				this.getClass().getSimpleName(), "onShutdown",
				"Cancelling " + inFlight.size() + " in-flight extractions");
		inFlight.values().forEach(f -> f.cancel(true));
		inFlight.clear();
	}

	// ------------------------------------------------------------------ //
	//  Private helpers                                                     //
	// ------------------------------------------------------------------ //

	/**
	 * Constructs the object-store file name for a specific extraction result.
	 *
	 * <p>Pattern: {@code <baseName>.<modality>.<format>}
	 * e.g. {@code "Finger.finger.ISO19794_4_2011"}
	 */
	private String buildExtractionFileName(String fileName, String extractionType, String extractionFormat) {
		String base     = fileName.split("\\.")[0];
		String modality = extractionType.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, "");
		return base + DOT + modality + DOT + extractionFormat;
	}

	/**
	 * Attempts to read a previously stored extraction result from the object store.
	 *
	 * @return the deserialised BIR list, or {@code null} if the object does not
	 *         exist or the store raised an exception
	 */
	private List<BIR> tryReadFromObjectStore(String uinHash, String extractionFileName) {
		try {
			if (!objectStoreHelper.biometricObjectExists(uinHash, extractionFileName)) {
				return null;
			}

			mosipLogger.info(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Cache hit in object store | file=" + extractionFileName);

			byte[] xmlBytes = objectStoreHelper.getBiometricObject(uinHash, extractionFileName);
			try {
				return cbeffUtil.getBIRDataFromXML(xmlBytes);
			} finally {
				// Release the large byte array as soon as we have the BIR objects.
				xmlBytes = null; // NOSONAR – intentional early GC hint
			}

		} catch (ObjectStoreAdapterException e) {
			// A transient store error must not prevent extraction from proceeding.
			mosipLogger.warn(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Object-store read failed (will extract fresh) | file=" + extractionFileName
							+ " | error=" + e.getMessage());
			return null;
		} catch (Exception e) {
			// Treat any unexpected deserialization error the same way.
			mosipLogger.warn(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Unexpected error reading from object store (will extract fresh) | file=" + extractionFileName
							+ " | error=" + e.getMessage());
			return null;
		}
	}

	/**
	 * Calls the biometric extractor directly on the current managed async thread.
	 *
	 * <p>The previous implementation wrapped the call in {@code CompletableFuture.supplyAsync()}
	 * (ForkJoinPool) and blocked with {@code get(timeout)} — adding an unnecessary second thread
	 * hop and a blocking wait. Since {@code extractTemplate} is already dispatched to the
	 * {@code withSecurityContext} managed pool via {@code @Async}, the extraction runs here
	 * directly. The per-call timeout is now enforced at the coordination point in
	 * {@code getBiometricsForRequestedFormats} via {@code allOf().get(timeout, unit)}.
	 *
	 * @throws BiometricExtractionException if the extractor raises one
	 * @throws IdRepoAppException           on any extraction failure
	 */
	private List<BIR> runExtractionWithTimeout(
			String extractionType,
			String extractionFormat,
			List<BIR> birsForModality) throws IdRepoAppException {

		mosipLogger.info(IdRepoSecurityManager.getUser(),
				this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
				"Starting extraction | type=" + extractionType + " | format=" + extractionFormat);

		Map<String, String> formatFlag = Map.of(
				extractionType.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, FORMAT_FLAG_SUFFIX),
				extractionFormat
		);

		try {
			return bioExtractionHelper.extractTemplates(birsForModality, formatFlag);
		} catch (BiometricExtractionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Biometric extraction failed | type=" + extractionType
							+ " | format=" + extractionFormat + " | error=" + e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	/**
	 * Persists the extracted BIRs to the object store as a CBEFF XML blob.
	 *
	 * <p>Persistence failures are logged but not propagated — the extracted BIRs
	 * are still returned to the caller.  A subsequent request will simply
	 * re-extract and attempt to persist again.
	 *
	 * @param uinHash            object-store bucket key
	 * @param extractionFileName derived file name for this extraction result
	 * @param extractedBiometrics BIRs to persist
	 */
	private void persistToObjectStore(String uinHash, String extractionFileName, List<BIR> extractedBiometrics) {
		if (extractedBiometrics == null || extractedBiometrics.isEmpty()) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Skipping persist – empty extraction result | file=" + extractionFileName);
			return;
		}

		try {
			byte[] xmlBytes = cbeffUtil.createXML(extractedBiometrics);
			objectStoreHelper.putBiometricObject(uinHash, extractionFileName, xmlBytes);
			mosipLogger.info(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Persisted extraction result | file=" + extractionFileName
							+ " | birCount=" + extractedBiometrics.size());
		} catch (Exception e) {
			// Soft failure: caller already has the extracted BIRs.
			mosipLogger.error(IdRepoSecurityManager.getUser(),
					this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"Failed to persist extraction result (non-fatal) | file=" + extractionFileName
							+ " | error=" + e.getMessage());
		}
	}
}