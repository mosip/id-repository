package io.mosip.idrepository.core.constant;

/**
 * MOSIP audit module identifiers for ID Repository subsystems.
 *
 * <p>
 * Each constant maps to a short module code ({@link #getModuleId()}) sent to the
 * kernel audit manager via {@link io.mosip.idrepository.core.builder.AuditRequestBuilder}.
 * Module names ({@link #getModuleName()}) match the enum constant name.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Groups audit events by subsystem (core, VID, credential-request, credential-service,
 * auth-type status) so audit queries and dashboards can filter by module without parsing
 * event IDs alone.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * Module codes are internal to ID Repository auditing. IDA does not reference these
 * constants. Changing {@code IDR-*} module IDs affects audit classification only.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * auditHelper.audit(AuditModules.ID_REPO_VID_SERVICE, AuditEvents.CREATE_VID, ...);
 * </pre>
 * <p>
 * Pair with {@link AuditEvents} when building audit requests through
 * {@link io.mosip.idrepository.core.helper.AuditHelper}.
 * </p>
 *
 * @author Manoj SP
 * @see AuditEvents
 * @see io.mosip.idrepository.core.builder.AuditRequestBuilder
 * @see io.mosip.idrepository.core.helper.AuditHelper
 */
public enum AuditModules {

	/** Shared core logic (security, pipeline, jobs). */
	ID_REPO_CORE_SERVICE("IDR-IDS"),

	/** VID lifecycle service. */
	ID_REPO_VID_SERVICE("IDR-VID"),

	/** Credential-request-generator module. */
	ID_REPO_CREDENTIAL_REQUEST_GENERATOR("IDR-CRG"),

	/** Credential issuance (credential-service) module. */
	ID_REPO_CREDENTIAL_SERVICE("IDR-CRS"),

	/** Auth-type status update flows. */
	AUTH_TYPE_STATUS("IDR-ATS");

	/** Short module code sent to the audit manager (e.g. {@code IDR-IDS}). */
	private final String moduleId;

	/**
	 * Returns the short module code sent to the kernel audit manager.
	 *
	 * @return the audit module code (e.g. {@code IDR-IDS})
	 */
	public String getModuleId() {
		return moduleId;
	}

	/**
	 * Returns the enum constant name as the audit module name.
	 *
	 * @return the enum constant name as the module name
	 */
	public String getModuleName() {
		return this.name();
	}

	/**
	 * Creates an audit module constant with its short module code.
	 *
	 * @param moduleId audit module code (e.g. {@code IDR-IDS})
	 */
	private AuditModules(String moduleId) {
		this.moduleId = moduleId;
	}
}
