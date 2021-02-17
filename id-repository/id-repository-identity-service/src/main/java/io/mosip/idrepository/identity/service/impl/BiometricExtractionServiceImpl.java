package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.identity.helper.BioExtractionHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class BiometricExtractionServiceImpl.
 * 
 * @author Loganathan Sekar
 */
@Service
public class BiometricExtractionServiceImpl implements BiometricExtractionService {
	
	/** The Constant EXTRACT_TEMPLATE. */
	private static final String EXTRACT_TEMPLATE = "extractTemplate";

	/** The Constant mosipLogger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(BiometricExtractionServiceImpl.class);
	
	/** The object store helper. */
	@Autowired
	private ObjectStoreHelper objectStoreHelper;
	
	/** The Constant DOT. */
	private static final String DOT = ".";
	
	/** The Constant FORMAT_FLAG_SUFFIX. */
	private static final String FORMAT_FLAG_SUFFIX = ".format";

	/** The Constant EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX. */
	private static final String EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX = "ExtractionFormat";

	/** The bio exraction helper. */
	@Autowired
	private BioExtractionHelper bioExractionHelper;
	
	/** The cbeff util. */
	@Autowired
	private CbeffUtil cbeffUtil;
	
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
	@Async("withSecurityContext")
	public CompletableFuture<List<BIR>> extractTemplate(String uinHash, String fileName,
			String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException {
		try {
			String extractionFileName = fileName.split("\\.")[0] + DOT + getModalityForFormat(extractionType) + DOT + extractionFormat;
			// TODO need to remove AmazonS3Exception handling
			try {
				if (objectStoreHelper.biometricObjectExists(uinHash, extractionFileName)) {
					mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
							"RETURNING EXISTING EXTRACTED BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat);
					byte[] xmlBytes = objectStoreHelper.getBiometricObject(uinHash, extractionFileName);
					List<BIR> existingBirs = cbeffUtil.convertBIRTypeToBIR(cbeffUtil.getBIRDataFromXML(xmlBytes));
					return CompletableFuture.completedFuture(existingBirs);
				}
			} catch (AmazonS3Exception e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
						e.getMessage());
			}
			
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"EXTRATCING BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat);
			Map<String, String> formatFlag = Map.of(getFormatFlag(extractionType), extractionFormat);
			List<BIR> extractedBiometrics = extractBiometricTemplate(formatFlag, birsForModality);
		
			objectStoreHelper.putBiometricObject(uinHash, extractionFileName, cbeffUtil.createXML(extractedBiometrics));
			return CompletableFuture.completedFuture(extractedBiometrics);
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} catch (BiometricExtractionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppUncheckedException(FILE_STORAGE_ACCESS_ERROR, e);
		} catch (AmazonS3Exception e) {
			// TODO need to remove AmazonS3Exception handling
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppUncheckedException(FILE_NOT_FOUND, e);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
	}
	
	/**
	 * Gets the format flag.
	 *
	 * @param formatQueryParam the format query param
	 * @return the format flag
	 */
	private String getFormatFlag(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, FORMAT_FLAG_SUFFIX);
	}
	
	/**
	 * Gets the modality for format.
	 *
	 * @param formatQueryParam the format query param
	 * @return the modality for format
	 */
	private String getModalityForFormat(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, "");
	}

	/**
	 * Extract biometric template.
	 *
	 * @param extractionFormats the extraction formats
	 * @param birs the birs
	 * @return the list
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	private List<BIR> extractBiometricTemplate(Map<String, String> extractionFormats, List<BIR> birs)
			throws BiometricExtractionException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "extractBiometricTemplate",
				"INVOKING BIOMETRIC EXTRACTION FOR THE FORMAT: " + extractionFormats);
		
		BioExtractRequestDTO bioExtractReq = new BioExtractRequestDTO();
		bioExtractReq.setBiometrics(birs);
		bioExtractReq.setExtractionFormats(extractionFormats);
		
		BioExtractResponseDTO bioExtractResponseDTO = extractBiometrics(bioExtractReq);
		return bioExtractResponseDTO.getExtractedBiometrics();
	}

	/**
	 * Extract biometrics.
	 *
	 * @param bioExtractRequestDTO the bio extract request DTO
	 * @return the bio extract response DTO
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	private BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO)
			throws BiometricExtractionException {
		BioExtractResponseDTO bioExtractPromiseResponseDTO = new BioExtractResponseDTO();
		List<BIR> birs = bioExtractRequestDTO.getBiometrics();
		List<BIR> encodedExtractedBiometrics = doBioExtraction(birs, bioExtractRequestDTO.getExtractionFormats());
		bioExtractPromiseResponseDTO.setExtractedBiometrics(encodedExtractedBiometrics);
		return bioExtractPromiseResponseDTO;
	}

	/**
	 * Do bio extraction.
	 *
	 * @param birs the birs
	 * @param extractionFormats the extraction formats
	 * @return the list
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	private List<BIR> doBioExtraction(List<BIR> birs, Map<String, String> extractionFormats)
			throws BiometricExtractionException {
		return bioExractionHelper.extractTemplates(birs, extractionFormats);
	}

}
