package io.mosip.credential.request.generator.constants;

/**
 * 
 * @author Sowmya
 *
 */
public enum ApiName {

	CRDENTIALSERVICE("CRDENTIALSERVICE"),
	ENCRYPTION("ENCRYPTION"),
	DECRYPTION("DECRYPTION"),
	KERNELAUDITMANAGER("mosip.idrepo.audit.rest.uri");

	private final String serviceName;

	private ApiName(String serviceName) {
		this.serviceName = serviceName;
	}

	private ApiName() {
		this.serviceName = "";
	}

	public String getServiceName() {
		return serviceName;
	}

}
