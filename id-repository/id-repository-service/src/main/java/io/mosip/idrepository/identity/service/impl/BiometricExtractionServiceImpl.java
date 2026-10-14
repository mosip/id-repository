package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.DOT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
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
 * Extracts ISO biometric templates from CBEFF for credential and auth flows.
 * <p>
 * Implements {@link io.mosip.idrepository.core.spi.BiometricExtractionService}.
 * </p>
 */
@Service
public class BiometricExtractionServiceImpl implements BiometricExtractionService {

	/** Method name used in logs and the performance log of both extract methods. */
	private static final String EXTRACT_TEMPLATE = "extractTemplate";

	/** Logger for extraction; also receives the per-method performance log. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(BiometricExtractionServiceImpl.class);

	/** Reads and writes extracted template files, on the live or draft path. */
	@Autowired
	private ObjectStoreHelper objectStoreHelper;

	/**
	 * Suffix of the SDK format flag. Replaces {@code ExtractionFormat} in the query parameter,
	 * so {@code fingerExtractionFormat} becomes {@code finger.format}.
	 */
	private static final String FORMAT_FLAG_SUFFIX = ".format";

	/** Calls the biometric SDK to extract templates. */
	@Autowired
	private BioExtractionHelper bioExractionHelper;

	/** Parses stored extracted CBEFF and writes newly extracted templates as CBEFF. */
	@Autowired
	private CbeffUtil cbeffUtil;

	/**
	 * {@inheritDoc}
	 * <p>
	 * Live-identity variant; runs on the {@code withSecurityContext} executor.
	 * </p>
	 * <ol>
	 * <li>Builds the extracted file name
	 * {@code {fileName without extension}.{modality}.{extractionFormat}}.</li>
	 * <li>Returns the file from {@code {uinHash}/Biometrics/} when it already exists. A missing
	 * file is expected and not logged; other read failures are logged and extraction continues.</li>
	 * <li>Otherwise extracts the templates with the biometric SDK and, when the result is not
	 * empty, stores it so later calls can reuse it.</li>
	 * </ol>
	 *
	 * @param uinHash          UIN hash without the salt-id prefix; the identity's object-store folder
	 * @param fileName         file id of the source CBEFF
	 * @param extractionType   modality key, for example {@code fingerExtractionFormat}
	 * @param extractionFormat target template format identifier
	 * @param birsForModality  BIR records of the modality from the source CBEFF
	 * @return completed future with the extracted (or previously stored) BIR list
	 * @throws IdRepoAppException {@code BIO_EXTRACTION_ERROR} when the SDK fails;
	 *         {@code UNKNOWN_ERROR} for any other failure
	 */
	@Async("withSecurityContext")
	public CompletableFuture<List<BIR>> extractTemplate(String uinHash, String fileName,
			String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			try {
				String extractionFileName = fileName.split("\\.")[0] + DOT + getModalityForFormat(extractionType) + DOT + extractionFormat;
				try {
					byte[] xmlBytes = objectStoreHelper.getBiometricObject(uinHash, extractionFileName);
					mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
							"RETURNING EXISTING EXTRACTED BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat);
					List<BIR> existingBirs = cbeffUtil.getBIRDataFromXML(xmlBytes);
					return CompletableFuture.completedFuture(existingBirs);
				} catch (IdRepoAppException e) {
					if (!IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode().equals(e.getErrorCode())) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
								e.getMessage());
					}
				} catch (ObjectStoreAdapterException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
							e.getMessage());
				}

				mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
						"EXTRATCING BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat);
				Map<String, String> formatFlag = Map.of(getFormatFlag(extractionType), extractionFormat);
				List<BIR> extractedBiometrics = extractBiometricTemplate(formatFlag, birsForModality);
				// add loggers for extractedBiometrics
				mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
						"EXTRACTED BIOMETRICS FOR FORMAT: " + extractionType +" : "+ extractionFormat+" : "+extractedBiometrics.size() +
						extractedBiometrics.isEmpty());
				if (!extractedBiometrics.isEmpty()) {
					objectStoreHelper.putBiometricObject(uinHash, extractionFileName, cbeffUtil.createXML(extractedBiometrics));
				}
				return CompletableFuture.completedFuture(extractedBiometrics);
			} catch (BiometricExtractionException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
				throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
			} catch (Exception e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
				throw new IdRepoAppException(UNKNOWN_ERROR, e);
			}
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, this.getClass().getSimpleName(), EXTRACT_TEMPLATE, startNanos);
		}
	}

	/**
	 * V2 draft variant of {@link #extractTemplate}. Runs on the {@code withSecurityContext}
	 * executor.
	 * <ol>
	 * <li>Builds the extracted file name
	 * {@code {fileName without extension}.{modality}.{extractionFormat}}.</li>
	 * <li>Returns the file from {@code _draft/{ridHash}/Biometrics/} when it already exists.
	 * A missing file is expected and not logged; other read failures are logged and
	 * extraction continues.</li>
	 * <li>Otherwise extracts the templates with the biometric SDK and, when the result is
	 * not empty, stores it on the draft path so publish moves it to the live identity.</li>
	 * </ol>
	 *
	 * @param ridHash          rid hash of the draft (the draft path segment)
	 * @param fileName         file id of the source CBEFF in the draft
	 * @param extractionType   modality key, for example {@code fingerExtractionFormat}
	 * @param extractionFormat target template format identifier
	 * @param birsForModality  BIR records of the modality from the draft CBEFF
	 * @return completed future with the extracted (or previously stored) BIR list
	 * @throws IdRepoAppException {@code BIO_EXTRACTION_ERROR} when the SDK fails;
	 *         {@code UNKNOWN_ERROR} for any other failure
	 */
	@Override
	@Async("withSecurityContext")
	public CompletableFuture<List<BIR>> extractTemplateDraft(String ridHash, String fileName,
			String extractionType, String extractionFormat, List<BIR> birsForModality) throws IdRepoAppException {
		long startNanos = System.nanoTime();
		try {
			String extractionFileName = fileName.split("\\.")[0] + DOT + getModalityForFormat(extractionType) + DOT + extractionFormat;
			try {
				byte[] xmlBytes = objectStoreHelper.getDraftBiometricObject(ridHash, extractionFileName);
				mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
						"RETURNING EXISTING DRAFT EXTRACTED BIOMETRICS FOR FORMAT: " + extractionType + " : " + extractionFormat);
				return CompletableFuture.completedFuture(cbeffUtil.getBIRDataFromXML(xmlBytes));
			} catch (IdRepoAppException e) {
				if (!IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode().equals(e.getErrorCode())) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
							e.getMessage());
				}
			} catch (ObjectStoreAdapterException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE,
						e.getMessage());
			}

			Map<String, String> formatFlag = Map.of(getFormatFlag(extractionType), extractionFormat);
			List<BIR> extractedBiometrics = extractBiometricTemplate(formatFlag, birsForModality);
			if (!extractedBiometrics.isEmpty()) {
				objectStoreHelper.putDraftBiometricObject(ridHash, extractionFileName, cbeffUtil.createXML(extractedBiometrics));
			}
			return CompletableFuture.completedFuture(extractedBiometrics);
		} catch (BiometricExtractionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), EXTRACT_TEMPLATE, e.getMessage());
			throw new IdRepoAppException(UNKNOWN_ERROR, e);
		} finally {
			IdentityServicePerformanceLog.log(mosipLogger, this.getClass().getSimpleName(), EXTRACT_TEMPLATE, startNanos);
		}
	}

	/**
	 * Turns a format query parameter into the flag the biometric SDK reads, for example
	 * {@code fingerExtractionFormat} into {@code finger.format}.
	 *
	 * @param formatQueryParam query parameter name ending in {@code ExtractionFormat}
	 * @return the SDK format flag
	 */
	private String getFormatFlag(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, FORMAT_FLAG_SUFFIX);
	}

	/**
	 * Gets the modality name from a format query parameter, for example {@code finger} from
	 * {@code fingerExtractionFormat}. The name is part of the extracted file name.
	 *
	 * @param formatQueryParam query parameter name ending in {@code ExtractionFormat}
	 * @return the modality name
	 */
	private String getModalityForFormat(String formatQueryParam) {
		return formatQueryParam.replace(EXTRACTION_FORMAT_QUERY_PARAM_SUFFIX, "");
	}

	/**
	 * Wraps BIRs and format flags in an extraction request and returns the extracted templates.
	 *
	 * @param extractionFormats SDK format flag to format, for example {@code finger.format} to a format id
	 * @param birs              BIR records of one modality
	 * @return extracted BIRs; may be empty
	 * @throws BiometricExtractionException if the SDK fails
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
	 * Runs an extraction request and wraps the result in a response.
	 *
	 * @param bioExtractRequestDTO BIRs and SDK format flags
	 * @return response holding the extracted BIRs
	 * @throws BiometricExtractionException if the SDK fails
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
	 * Hands the BIRs to the biometric SDK through {@link BioExtractionHelper}.
	 *
	 * @param birs              BIR records to extract from
	 * @param extractionFormats SDK format flag to format
	 * @return extracted BIRs
	 * @throws BiometricExtractionException if the SDK fails
	 */
	private List<BIR> doBioExtraction(List<BIR> birs, Map<String, String> extractionFormats)
			throws BiometricExtractionException {
		return bioExractionHelper.extractTemplates(birs, extractionFormats);
	}

}
