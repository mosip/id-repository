package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Credential type metadata returned by {@code GET /v1/credentialservice/types}.
 */
@Data
public class Type {

	/** Credential type code used in issuance requests. */
	private String id;

	/** Display name of the credential type. */
	private String name;

	/** Human-readable description of partner use case. */
	private String description;

	/** Issuing authorities that may produce this credential type. */
	private List<Issuer> issuers;
}
