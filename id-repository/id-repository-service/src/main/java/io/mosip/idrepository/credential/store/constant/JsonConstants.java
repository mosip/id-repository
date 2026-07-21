package io.mosip.idrepository.credential.store.constant;

/**
 * JSON property names used when assembling MOSIP legacy credential envelopes and W3C Verifiable
 * Credential ({@code VC-V1}) documents during credential-store issuance.
 * <p>
 * Credential providers ({@link io.mosip.idrepository.credential.store.provider.impl.VerCredProvider},
 * QR, PDF, ID-auth, etc.) build a canonical JSON structure before signing and Datashare upload.
 * Constants here avoid string drift between proof generation, selective-disclosure blocks, and
 * partner-facing credential schemas. W3C-prefixed keys map directly to the VC data model;
 * unprefixed keys belong to the legacy MOSIP credential wrapper still consumed by some partners.
 * </p>
 */
public class JsonConstants {

	/** Top-level credential identifier in the legacy MOSIP credential envelope. */
	public static final String ID = "id";

	/** Credential type discriminator in the legacy envelope. */
	public static final String TYPE = "type";

	/** Issuing authority block in the legacy envelope. */
	public static final String ISSUER = "issuer";

	/** Timestamp when the credential was issued (legacy envelope). */
	public static final String ISSUANCEDATE = "issuanceDate";

	/** Subject (resident) the credential was issued to in the legacy envelope. */
	public static final String ISSUEDTO = "issuedTo";

	/** Resident consent metadata block in the legacy envelope. */
	public static final String CONSENT = "consent";

	/** Claims about the credential subject (demographics, biometrics references). */
	public static final String CREDENTIALSUBJECT = "credentialSubject";

	/** Nested credential object inside the legacy wrapper. */
	public static final String CREDENTIAL = "credential";

	/** Digital signature or JWT proof container in the legacy envelope. */
	public static final String PROOF = "proof";

	/** Raw signature value property inside the proof block. */
	public static final String SIGNATURE = "signature";

	/** Attributes encrypted or selectively disclosed in VC flows. */
	public static final String PROTECTEDATTRIBUTES = "protectedAttributes";

	/** Partner-specific credential type label in policy-driven issuance. */
	public static final String CREDENTIALTYPE = "credentialType";

	/** Partner or resident protection key reference for encrypted attributes. */
	public static final String PROTECTIONKEY = "protectionKey";

	/** W3C Verifiable Credentials {@code @context} JSON-LD property. */
	public static final String VC_AT_CONTEXT = "@context";

	/** W3C VC {@code type} property (for example, {@code VerifiableCredential}). */
	public static final String VC_TYPE = "type";

	/** W3C VC document identifier URI. */
	public static final String VC_ID = "id";

	/** W3C VC issuer DID or URI. */
	public static final String VC_ISSUER = "issuer";

	/** W3C VC issuance timestamp (ISO-8601). */
	public static final String VC_ISSUANCE_DATE = "issuanceDate";

	/** W3C proof {@code created} timestamp for the JWT linked data proof. */
	public static final String VC_PROOF_CREATED = "created";

	/** W3C proof purpose (for example, {@code assertionMethod}). */
	public static final String VC_PROOF_PURPOSE = "proofPurpose";

	/** W3C proof type (for example, {@code JsonWebSignature2020}). */
	public static final String VC_PROOF_TYPE = "type";

	/** W3C verification method reference used to validate the proof. */
	public static final String VC_PROOF_VERIFICATION_METHOD = "verificationMethod";

	/** Default JWS algorithm ({@code PS256}) for VC JWT proofs via Keymanager. */
	public static final String VC_SIGN_ALGO = "PS256";

	/** MOSIP internal label for W3C Verifiable Credential format version 1. */
	public static final String VC_VERSION_1 = "VC-V1";

	/** Internal JSON key storing the VC format version constant in issued documents. */
	public static final String VC_VERSION_CONST = "vcVer";
}
