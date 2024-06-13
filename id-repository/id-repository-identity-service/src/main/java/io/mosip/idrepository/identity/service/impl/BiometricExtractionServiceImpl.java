package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.DOT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_BIOMETRIC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
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
	
	/** The Constant FORMAT_FLAG_SUFFIX. */
	private static final String FORMAT_FLAG_SUFFIX = ".format";

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
	 * @param cbeffBirsForModality the birs for modality
	 * @return the completable future
	 * @throws IdRepoAppException the id repo app exception
	 */
	@Async
	public CompletableFuture<List<BIR>> extractTemplate(String uinHash, String fileName,
			String extractionType, String extractionFormat, List<BIR> cbeffBirsForModality) throws IdRepoAppException {
		try {
			String extractionFileName = fileName.split("\\.")[0] + DOT + getModalityForFormat(extractionType) + DOT + extractionFormat;
			Map<String, String> formatFlag = Map.of(getFormatFlag(extractionType), extractionFormat);
			try {
				if (objectStoreHelper.biometricObjectExists(uinHash, extractionFileName)) {
					mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
							"RETURNING EXISTING EXTRACTED BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat);
					byte[] xmlBytes = objectStoreHelper.getBiometricObject(uinHash, extractionFileName);
					List<BIR> existingBirs;
					try {
						existingBirs = cbeffUtil.getBIRDataFromXML(xmlBytes);
					} catch (Exception e) {
						existingBirs = List.of();
						mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
								EXTRACT_TEMPLATE, e.getMessage());
					}
					if (!validateCbeff(existingBirs, cbeffBirsForModality)) {
						return extractBiometricTemplateData(uinHash, extractionFileName, formatFlag,
								cbeffBirsForModality);
					}
					return CompletableFuture.completedFuture(existingBirs);
				}
			} catch (ObjectStoreAdapterException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
						e.getMessage());
			}
			
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
					"EXTRATCING BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat);
			return extractBiometricTemplateData(uinHash, extractionFileName, formatFlag, cbeffBirsForModality);
		} catch (BiometricExtractionException e) {
			if(e.getErrorCode().equalsIgnoreCase(INVALID_BIOMETRIC.getErrorCode())) {
				throw e;
			}
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		}
	}

	private CompletableFuture<List<BIR>> extractBiometricTemplateData(String uinHash, String extractionFileName,
			Map<String, String> formatFlag, List<BIR> cbeffBirsForModality)
			throws BiometricExtractionException, IdRepoAppException, Exception {
		List<BIR> extractedBiometrics = extractBiometricTemplate(formatFlag, cbeffBirsForModality);
		if (!extractedBiometrics.isEmpty()) {
			objectStoreHelper.putBiometricObject(uinHash, extractionFileName, cbeffUtil.createXML(extractedBiometrics));
		}
		return CompletableFuture.completedFuture(extractedBiometrics);
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
	 * @param cbeffBirsForModality the birs
	 * @return the list
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	private List<BIR> extractBiometricTemplate(Map<String, String> extractionFormats, List<BIR> cbeffBirsForModality)
			throws BiometricExtractionException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "extractBiometricTemplate",
				"INVOKING BIOMETRIC EXTRACTION FOR THE FORMAT: " + extractionFormats);
		
		BioExtractRequestDTO bioExtractReq = new BioExtractRequestDTO();
		bioExtractReq.setBiometrics(cbeffBirsForModality);
		bioExtractReq.setExtractionFormats(extractionFormats);
		
		BioExtractResponseDTO bioExtractResponseDTO = extractBiometrics(bioExtractReq);
		if (!validateCbeff(bioExtractResponseDTO.getExtractedBiometrics(), cbeffBirsForModality)) {
			throw new BiometricExtractionException(INVALID_BIOMETRIC.getErrorCode(),
					String.format(INVALID_BIOMETRIC.getErrorMessage()));
		}
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
		List<BIR> encodedExtractedBiometrics = doBioExtraction(bioExtractRequestDTO.getBiometrics(), bioExtractRequestDTO.getExtractionFormats());
		bioExtractPromiseResponseDTO.setExtractedBiometrics(encodedExtractedBiometrics);
		return bioExtractPromiseResponseDTO;
	}

	/**
	 * Do bio extraction.
	 *
	 * @param cbeffBirsForModality the birs
	 * @param extractionFormats the extraction formats
	 * @return the list
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	private List<BIR> doBioExtraction(List<BIR> cbeffBirsForModality, Map<String, String> extractionFormats)
			throws BiometricExtractionException {
		return bioExractionHelper.extractTemplates(cbeffBirsForModality, extractionFormats);
	}

	private boolean validateCbeff(List<BIR> extractedBirs, List<BIR> cbeffBirsForModality) {
		return extractedBirs != null && cbeffBirsForModality != null
				&& Integer.valueOf(extractedBirs.size()).equals(Integer.valueOf(cbeffBirsForModality.size()));
	}
}
