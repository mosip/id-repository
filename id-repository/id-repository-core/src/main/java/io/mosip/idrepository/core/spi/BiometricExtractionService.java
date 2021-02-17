package io.mosip.idrepository.core.spi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.kernel.core.cbeffutil.entity.BIR;

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
	 
}
