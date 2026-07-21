package io.mosip.idrepository.core.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application error codes and messages for ID Repository APIs.
 *
 * <p>
 * Codes follow the MOSIP {@code IDR-{component}-{number}} pattern. Messages may contain
 * {@code %s} placeholders filled at throw time via
 * {@link io.mosip.idrepository.core.exception.IdRepoAppException} and related types.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Provides a single catalogue of machine-readable error codes and human-readable message
 * templates for validation, identity, VID, biometric extraction, and credential-feeder
 * failures. Controllers and exception handlers expose {@link #getErrorCode()} /
 * {@link #getErrorMessage()} in the standard MOSIP error envelope.
 * </p>
 *
 * <h2>Component prefixes</h2>
 * <ul>
 *   <li>{@code IDC} — core / cross-cutting validation and infrastructure</li>
 *   <li>{@code IDS} — identity service</li>
 *   <li>{@code VID} — VID service</li>
 *   <li>{@code BIE} — biometric extraction</li>
 *   <li>{@code CFJ} — credential feeder job</li>
 * </ul>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * <strong>Critical:</strong> This enum is part of the published {@code id-repository-core}
 * API surface. ID Authentication and partner clients match on specific error codes
 * (for example credential-request codes such as {@code IDR-CRG-009} defined in related
 * modules, and core codes such as {@link #NO_RECORD_FOUND}, {@link #AUTHENTICATION_FAILED}).
 * </p>
 * <ul>
 *   <li>Do <strong>not</strong> change {@link #getErrorCode()} string values without an
 *       IDA / partner coordinated release</li>
 *   <li>Message text may be clarified carefully; avoid changing {@code %s} placeholder
 *       count or order without updating all throw sites</li>
 *   <li>Note: {@link #INVALID_CBEFF} and {@link #TECHNICAL_ERROR} historically share
 *       {@code IDR-BIE-001} — treat as legacy; do not “fix” without migration planning</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * throw new IdRepoAppException(
 *     IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
 *     String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "uin"));
 * </pre>
 * <p>
 * {@link #getAllErrorCodes()} returns an immutable list of every defined code for
 * validation or documentation tooling.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.exception.IdRepoAppException
 * @see io.mosip.idrepository.core.exception.IdRepoAppUncheckedException
 * @see AuthAdapterErrorCode
 */
public enum IdRepoErrorConstants {

	// ---- IdRepo Core (IDR-IDC) ----

	/** Required request field absent. Placeholder: parameter name. */
	MISSING_INPUT_PARAMETER("IDR-IDC-001", "Missing Input Parameter - %s"),

	/** Request field present but fails format/constraint validation. Placeholder: parameter name. */
	INVALID_INPUT_PARAMETER("IDR-IDC-002", "Invalid Input Parameter - %s"),

	/** Request body or headers fail structural validation. */
	INVALID_REQUEST("IDR-IDC-003", "Invalid Request"),

	/** Unclassified internal failure. */
	UNKNOWN_ERROR("IDR-IDC-004", "Unknown error occurred"),

	/** Identity JSON schema or business-rule validation failed. */
	DATA_VALIDATION_FAILED("IDR-IDC-005", "Input Data Validation Failed"),

	/** JPA / JDBC operation failed. */
	DATABASE_ACCESS_ERROR("IDR-IDC-006", "Error occured while performing DB operations"),

	/** No matching row in the database. */
	NO_RECORD_FOUND("IDR-IDC-007", "No Record(s) found"),

	/** Outbound REST call returned HTTP 4xx. */
	CLIENT_ERROR("IDR-IDC-008", "4XX - Client Error occured"),

	/** Outbound REST call returned HTTP 5xx. */
	SERVER_ERROR("IDR-IDC-009", "5XX - Server Error occured"),

	/** Outbound REST call timed out. */
	CONNECTION_TIMED_OUT("IDR-IDC-010", "Connection timed out"),

	/** Caller lacks required role or scope. */
	AUTHORIZATION_FAILED("IDR-IDC-011", "Authorization Failed"),

	/** Insert rejected because a unique key already exists. */
	RECORD_EXISTS("IDR-IDC-012", "Record already exists in DB"),

	/** Attribute update count exceeded configured limit. Placeholder: attribute name. */
	UPDATE_COUNT_LIMIT_EXCEEDED("IDR-IDC-013", "Update count limit for the attributes exceeded:- %s"),

	/** Handle value already registered. Placeholder: handle value. */
	HANDLE_RECORD_EXISTS("IDR-IDC-014", "%s : Handle record already exists in DB"),

	// ---- Identity Service (IDR-IDS) ----

	/** Computed identity element hash does not match stored hash. */
	IDENTITY_HASH_MISMATCH("IDR-IDS-001", "Identity Element hash does not match"),

	/** Biometric or document hash does not match stored hash. */
	DOCUMENT_HASH_MISMATCH("IDR-IDS-002", "Biometric/Document hash does not match"),

	/** Cryptomanager encrypt/decrypt call failed. */
	ENCRYPTION_DECRYPTION_FAILED("IDR-IDS-003", "Failed to either encrypt/decrypt message using Kernel Crypto Manager"),

	/** Object store read/write failed. */
	FILE_STORAGE_ACCESS_ERROR("IDR-IDS-004", "Failed to store/retrieve files in Object Store"),

	/** Kernel ID-object validator rejected the identity JSON. */
	ID_OBJECT_PROCESSING_FAILED("IDR-IDS-005", "Failed to process Id Object using kernel Id Object validator"),

	/** Requested biometric/document file not found in object store. */
	FILE_NOT_FOUND("IDR-IDS-006", "File(s) not found in Object Store"),

	/** Kernel masterdata service call failed. */
	MASTERDATA_RETRIEVE_ERROR("IDR-IDS-007", "Failed to retrieve data from kernel Masterdata"),

	/** Kernel syncdata identity-schema call failed. */
	SCHEMA_RETRIEVE_ERROR("IDR-IDS-008", "Failed to retrieve Identity Schema from kernel Syncdata service"),

	/** Biometric template extractor service call failed. */
	BIO_EXTRACTION_ERROR("IDR-IDS-009", "Failed to extract template from bio extractor service"),

	/** Outbound VID service call failed. */
	VID_SERVICE_RETRIEVAL_ERROR("IDR-IDS-010", "Failed to retrieve data from vid service"),

	/** UIN generator service returned an error. */
	UIN_GENERATION_FAILED("IDR-IDS-011", "Failed to generate UIN"),

	/** Keycloak / auth adapter rejected the bearer token. */
	AUTHENTICATION_FAILED("IDR-IDS-012", "Authentication Failed"),

	// ---- VID Service (IDR-VID) ----

	/** VID is in an invalid state for the requested operation. Placeholder: status. */
	INVALID_VID("IDR-VID-001", "VID is %s"),

	/** VID generator service returned an error. */
	VID_GENERATION_FAILED("IDR-VID-002", "Failed to generate VID"),

	/** VID policy constraints prevent generation/regeneration. */
	VID_POLICY_FAILED("IDR-VID-003", "Could not generate/regenerate VID as per policy"),

	/** UIN is invalid for VID operations. Placeholder: reason. */
	INVALID_UIN("IDR-VID-004", "%s UIN"),

	/** Identity retrieve call from VID service failed. */
	UIN_RETRIEVAL_FAILED("IDR-VID-005", "Failed to retrieve uin data using Identity Service"),

	/** UIN hash verification failed during VID crypto. */
	UIN_HASH_MISMATCH("IDR-VID-006", "Uin hash does not match"),

	// ---- Biometric Extraction (IDR-BIE) ----

	/** CBEFF XML failed validation or parsing. */
	INVALID_CBEFF("IDR-BIE-001", "Invalid CBEFF"),

	/** Biometric extractor returned an unexpected error. */
	TECHNICAL_ERROR("IDR-BIE-001", "Technical Error in Biometric Extraction"),

	// ---- Credential Feeder Job (IDR-CFJ) ----

	/** Credential feeder batch job failed. */
	JOB_FAILED("IDR-CFJ-001", "Credential Feeder job failed");

	/** MOSIP error code (e.g. {@code IDR-IDC-001}). */
	private final String errorCode;

	/** Error message template; may contain {@code %s} placeholders. */
	private final String errorMessage;

	/**
	 * Creates an error constant with its MOSIP code and message template.
	 *
	 * @param errorCode    MOSIP error code following {@code IDR-{component}-{number}}
	 * @param errorMessage human-readable message template (may contain {@code %s})
	 */
	private IdRepoErrorConstants(String errorCode, String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns the MOSIP error code string.
	 *
	 * @return the MOSIP error code (e.g. {@code IDR-IDC-001})
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Returns the error message template (may contain {@code %s} placeholders).
	 *
	 * @return the error message template
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Returns an immutable list of every defined MOSIP error code in this enum.
	 *
	 * @return immutable list of all defined error codes
	 */
	public static List<String> getAllErrorCodes() {
		return Collections.unmodifiableList(Arrays.asList(IdRepoErrorConstants.values()).parallelStream()
				.map(IdRepoErrorConstants::getErrorCode).collect(Collectors.toList()));
	}
}
