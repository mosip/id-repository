package io.mosip.idrepository.credential.request.constant;

/**
 * Outbound REST service identifiers for credential-request integrations.
 * <p>
 * Property keys are resolved via {@link io.mosip.idrepository.core.util.EnvUtil}.
 * </p>
 */
public enum ApiName {

	/** Consolidated or standalone credential store {@code /issue} endpoint. */
	CRDENTIALSERVICE("CRDENTIALSERVICE"),

	/** Kernel audit manager REST URI. */
	KERNELAUDITMANAGER("mosip.idrepo.audit.rest.uri"),

	/** Cryptomanager encrypt endpoint for queue payload storage. */
	ENCRYPTION("ENCRYPTION"),

	/** Cryptomanager decrypt endpoint for queue payload reads. */
	DECRYPTION("DECRYPTION");

	/** Spring property key suffix or full key for URL lookup. */
	private final String serviceName;

	ApiName(String serviceName) {
		this.serviceName = serviceName;
	}

	ApiName() {
		this.serviceName = "";
	}

	/**
	 * Returns the config property key associated with this API.
	 *
	 * @return service property name
	 */
	public String getServiceName() {
		return serviceName;
	}
}
