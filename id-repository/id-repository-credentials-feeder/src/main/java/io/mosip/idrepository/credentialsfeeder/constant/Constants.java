package io.mosip.idrepository.credentialsfeeder.constant;

/**
 * The Enum Constants 
 *
 * @author Manoj SP
 */
public enum Constants {
	
	/** The package to scan. */
	PACKAGE_TO_SCAN("io.mosip.idrepository.credentialsfeeder.*"),
	
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
}
