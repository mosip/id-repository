package io.mosip.idrepository.credentialsfeeder.constant;

/**
 * The Enum Constants 
 *
 * @author Manoj SP
 */
public class Constants {
	
	private Constants() {
	}
	
	/** The package to scan. */
	public static final String PACKAGE_TO_SCAN = "io.mosip.idrepository.credentialsfeeder.*";
	
	/** The datasource url. */
	public static final String DATASOURCE_URL = "%s.url";
	
	/** The datasource username. */
	public static final String DATASOURCE_USERNAME = "%s.username";
	
	/** The datasource password. */
	public static final String DATASOURCE_PASSWORD = "%s.password";
	
	/** The datasource driverclassname. */
	public static final String DATASOURCE_DRIVERCLASSNAME = "%s.driverClassName";
	
	public static final String DATASOURCE_SCHEMA = "%s.schema";
	
	public static final int DEFAULT_CHUNCK_SIZE = 10;
	
	public static final String MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED = "mosip.idrepo.identity.uin-status.registered";

	public static final String IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE = "idrepo-credential-feeder-chunk-size";
	
	public static final String MOSIP_IDREPO_VID_ACTIVE_STATUS = "mosip.idrepo.vid.active-status";

	public static final String PROP_ONLINE_VERIFICATION_PARTNER_IDS = "online-verification-partner-ids";
	
	public static final String UNLOCK_EXP_TIMESTAMP = "unlockExpiryTimestamp";
}
