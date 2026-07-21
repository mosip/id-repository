package io.mosip.idrepository.saltgenerator.constant;

import lombok.Getter;

/**
 * Spring property keys that control salt sequence bounds and JDBC batch size.
 *
 * <p>
 * Resolved from Spring Cloud Config (or local overrides) via
 * {@link org.springframework.core.env.Environment} inside
 * {@link io.mosip.idrepository.saltgenerator.service.SaltGenerator}.
 * </p>
 *
 * <table border="1" summary="Salt-generator config keys">
 *   <tr><th>Constant</th><th>Property key</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@link #START_SEQ}</td>
 *     <td>{@code mosip.kernel.salt-generator.start-sequence}</td>
 *     <td>First salt id to generate (inclusive)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #END_SEQ}</td>
 *     <td>{@code mosip.kernel.salt-generator.end-sequence}</td>
 *     <td>Last salt id to generate (inclusive)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #CHUNK_SIZE}</td>
 *     <td>{@code mosip.kernel.salt-generator.chunk-size}</td>
 *     <td>JDBC batch size (default 500 when unset or &lt; 1)</td>
 *   </tr>
 * </table>
 *
 * @author MOSIP
 * @see io.mosip.idrepository.saltgenerator.service.SaltGenerator
 */
@Getter
public enum SaltGeneratorConstant {

	/** First salt bucket id (inclusive). Required. */
	START_SEQ("mosip.kernel.salt-generator.start-sequence"),

	/** Last salt bucket id (inclusive). Required. */
	END_SEQ("mosip.kernel.salt-generator.end-sequence"),

	/** Rows per JDBC batch commit. Optional; defaults to 500. */
	CHUNK_SIZE("mosip.kernel.salt-generator.chunk-size");

    /**
     * -- GETTER --
     *
     * @return the Spring property key string
     */
    private final String value;

	SaltGeneratorConstant(String value) {
		this.value = value;
	}

}
