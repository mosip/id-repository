package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * W3C Verifiable Credentials {@code proof} object attached after JWT signing.
 * <p>
 * Populated by {@link io.mosip.idrepository.credential.store.provider.impl.VerCredProvider}
 * using kernel signature service output.
 * </p>
 *
 * @see VerifiableCredential
 */
@Data
public class Proof {

	/** Proof type (e.g. {@code JsonWebSignature2020}). */
	private String type;

	/** ISO-8601 proof creation timestamp. */
	private String created;

	/** Proof purpose (e.g. {@code assertionMethod}). */
	private String proofPurpose;

	/** Verification method DID or key id used to sign. */
	private String verificationMethod;

	/** Compact JWS over the verifiable credential document. */
	private String jws;
}
