package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * API response listing credential types exposed by credential store.
 *
 * @see io.mosip.idrepository.credential.store.service.CredentialStoreService#getCredentialTypes()
 */
@Data
public class CredentialTypeResponse {

	/** Supported credential type definitions with issuers and descriptions. */
	List<Type> credentialTypes;
}
