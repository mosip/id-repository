package io.mosip.idrepository.core.spi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.kernel.biometrics.entities.BIR;

/**
 * SPI for asynchronous biometric template extraction from stored BIR records.
 * <p>
 * Used during identity retrieval when the caller requests biometric data in a
 * specific extraction format (e.g. fingerprint minutiae vs. image). Extraction
 * runs on a dedicated thread pool configured in the service module
 * ({@code IdRepoConfig}).
 * </p>
 * <p>
 * <b>Implementor:</b> {@code BiometricExtractionServiceImpl} in {@code id-repository-service}.
 * </p>
 * <p>
 * <b>Caller:</b> {@code IdRepoProxyServiceImpl} during {@code retrieveIdentity}
 * with extraction format parameters.
 * </p>
 *
 * @author Loganathan Sekar
 */
public interface BiometricExtractionService {

	/**
	 * Extracts biometric templates for a single modality asynchronously.
	 * <p>
	 * The returned {@link CompletableFuture} completes on the extraction thread
	 * pool with the list of extracted {@link BIR} records, or completes
	 * exceptionally on extraction failure.
	 * </p>
	 *
	 * @param uinHash          salted UIN hash (for audit/logging context)
	 * @param fileName         CBEFF file name being processed
	 * @param extractionType   biometric modality (finger, face, iris)
	 * @param extractionFormat target template format identifier
	 * @param birsForModality  input BIR records for the modality
	 * @return future completing with extracted BIR list
	 * @throws IdRepoAppException if extraction cannot be initiated
	 */
 	CompletableFuture<List<BIR>> extractTemplate(String uinHash, String fileName,
				String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException;


	/**
	 * V2 draft variant of {@link #extractTemplate}. The extracted file
	 * {@code {fileName without extension}.{modality}.{extractionFormat}} is read from and
	 * written to the draft path {@code _draft/{ridHash}/Biometrics/} instead of the live
	 * path, so it is moved to the live identity when the draft is published.
	 * <p>
	 * When a previously extracted file exists it is returned without calling the SDK;
	 * otherwise the templates are extracted and a non-empty result is stored for later calls.
	 * </p>
	 *
	 * @param ridHash          rid hash of the draft (the draft path segment)
	 * @param fileName         file id of the source CBEFF in the draft
	 * @param extractionType   modality key ({@code fingerExtractionFormat},
	 *                         {@code irisExtractionFormat} or {@code faceExtractionFormat})
	 * @param extractionFormat target template format identifier
	 * @param birsForModality  BIR records of the modality from the draft CBEFF
	 * @return future completing with the extracted BIR list
	 * @throws IdRepoAppException {@code BIO_EXTRACTION_ERROR} when the SDK fails;
	 *         {@code UNKNOWN_ERROR} for any other failure
	 */
	CompletableFuture<List<BIR>> extractTemplateDraft(String ridHash, String fileName,
			String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException;
}
