package io.mosip.idrepository.saltgenerator.constant;

/**
 * The Enum SaltGeneratorConstant - contains constants for SaltGenerator.
 *
 * @author Manoj SP
 */
public enum SaltGeneratorConstant {
	
	/** The start seq. */
	START_SEQ("mosip.kernel.salt-generator.start-sequence"),
	
	/** The end seq. */
	END_SEQ("mosip.kernel.salt-generator.end-sequence");

	/** The value. */
	private String value;
	
	/**
	 * Instantiates a new salt generator constant.
	 *
	 * @param value the value
	 */
	SaltGeneratorConstant(String value) {
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
