package io.mosip.idrepository.core.constant;

/**
 * The Enum AuditModules - Contains all the modules in Id Repository for Audit
 * purpose.
 *
 * @author Manoj SP
 */
public enum AuditModules {
	
	ID_REPO_CORE_SERVICE("IDR-IDS"),
	
	ID_REPO_VID_SERVICE("IDR-VID"),
	
	ID_REPO_CREDENTIAL_REQUEST_GENERATOR("IDR-CRG"),
	
	ID_REPO_CREDENTIAL_SERVICE("IDR-CRS"),
	
	AUTH_TYPE_STATUS("IDR-ATS");
	

	/** The module id. */
	private final String moduleId;

	/**
	 * Gets the module id.
	 *
	 * @return the module id
	 */
	public String getModuleId() {
		return moduleId;
	}

	/**
	 * Gets the module name.
	 *
	 * @return the module name
	 */
	public String getModuleName() {
		return this.name();
	}

	/**
	 * Instantiates a new audit contants.
	 *
	 * @param moduleId the moduleId
	 */
	private AuditModules(String moduleId) {
		this.moduleId = moduleId;
	}
}
