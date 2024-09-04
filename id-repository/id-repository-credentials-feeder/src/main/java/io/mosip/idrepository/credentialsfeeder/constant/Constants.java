package io.mosip.idrepository.credentialsfeeder.constant;

/**
 * The Enum Constants 
 *
 * @author Manoj SP
 */
public enum Constants {
	
	/** The package to scan. */
	PACKAGE_TO_SCAN("io.mosip.idrepository.saltgenerator.*"),

	/** The db schema name. */
	DB_SCHEMA_NAME("mosip.kernel.salt-generator.schemaName"),

	/** The db table name. */
	DB_TABLE_NAME("mosip.kernel.salt-generator.tableName"),

	/** The chunk size. */
	CHUNK_SIZE("mosip.kernel.salt-generator.chunk-size"),

	/** The start seq. */
	START_SEQ("mosip.kernel.salt-generator.start-sequence"),

	/** The end seq. */
	END_SEQ("mosip.kernel.salt-generator.end-sequence"),

	DATASOURCE_ALIAS("mosip.kernel.salt-generator.db.key-alias"),

	/** The datasource url. */
	DATASOURCE_URL("%s.url"),

	/** The datasource username. */
	DATASOURCE_USERNAME("%s.username"),

	/** The datasource password. */
	DATASOURCE_PASSWORD("%s.password"),

	/** The datasource driverclassname. */
	DATASOURCE_DRIVERCLASSNAME("%s.driverClassName"),

	DATASOURCE_SCHEMA("%s.schema");

	/** The value. */
	private String value;

	/**
	 * Instantiates a new salt generator constant.
	 *
	 * @param value the value
	 */
	Constants(String value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	
	public static final int DEFAULT_CHUNCK_SIZE = 10;
	
	public static final String MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED = "mosip.idrepo.identity.uin-status.registered";

	public static final String IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE = "idrepo-credential-feeder-chunk-size";
	
	public static final String MOSIP_IDREPO_VID_ACTIVE_STATUS = "mosip.idrepo.vid.active-status";

	public static final String PROP_ONLINE_VERIFICATION_PARTNER_IDS = "online-verification-partner-ids";
	
	public static final String UNLOCK_EXP_TIMESTAMP = "unlockExpiryTimestamp";
}
