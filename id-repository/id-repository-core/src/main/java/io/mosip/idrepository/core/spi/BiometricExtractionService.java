package io.mosip.idrepository.core.spi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.kernel.biometrics.entities.BIR;

/**
 * The Interface BiometricExtractionService.
 * 
 * @author Loganathan Sekar
 */
public interface BiometricExtractionService {

	 /**
 	 * Extract template.
 	 *
 	 * @param uinHash the uin hash
 	 * @param fileName the file name
 	 * @param extractionType the extraction type
 	 * @param extractionFormat the extraction format
 	 * @param birsForModality the birs for modality
 	 * @return the completable future
 	 * @throws IdRepoAppException the id repo app exception
 	 */
 	CompletableFuture<List<BIR>> extractTemplate(String uinHash, String fileName,
				String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException;

	 /**
	 	 * V2: same as {@link #extractTemplate}, but persists to and reads from the
	 	 * draft object-store path ({@code _draft/{ridHash}/Biometrics/...}) instead
	 	 * of the live path, so the extracted file lands where publishDraftV2 expects it.
	 	 *
	 	 * @param ridHash the rid hash
	 	 * @param fileName the file name
	 	 * @param extractionType the extraction type
	 	 * @param extractionFormat the extraction format
	 	 * @param birsForModality the birs for modality
	 	 * @return the completable future
	 	 * @throws IdRepoAppException the id repo app exception
	 	 */
	 	CompletableFuture<List<BIR>> extractTemplateDraft(String ridHash, String fileName,
				String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException;
	 
}
