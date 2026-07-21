package io.mosip.idrepository.credential.store.constant;

/**
 * Domain literals and Spring configuration keys used when filtering identity data and formatting
 * partner credentials in credential-store.
 * <p>
 * During issuance, credential-store retrieves the resident identity, applies the partner's PMS
 * policy (allowed KYC attributes, masking, biometric extraction formats), and dispatches to a
 * {@link io.mosip.idrepository.credential.store.provider.CredentialProvider} implementation.
 * Constants here name biometric modalities, demographic field keys, policy-driven operation labels,
 * and {@code application.properties} keys for MVEL format/mask functions consumed by
 * {@link io.mosip.idrepository.credential.store.provider.CredentialProvider} and
 * {@link io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl}.
 * </p>
 */
public class CredentialConstants {

	/** Partner or credential lifecycle status value indicating an active, issuable credential type. */
	public static final String ACTIVE_STATUS = "active";

	/** Biometric modality label for face capture in CBEFF extraction policy matching. */
	public static final String FACE = "face";

	/** Biometric modality label for fingerprint capture in CBEFF extraction policy matching. */
	public static final String FINGER = "finger";

	/** Biometric modality label for iris capture in CBEFF extraction policy matching. */
	public static final String IRIS = "iris";

	/** Identity JSON key for the consolidated individual biometrics CBEFF blob. */
	public static final String INDIVIDUAL_BIOMETRICS = "individualBiometrics";

	/** WebSub / audit event label emitted after successful credential issuance. */
	public static final String CREDENTIAL_ISSUED = "credentialIssued";

	/** PMS allowed-KYC group name for ISO CBEFF biometric segments. */
	public static final String CBEFF = "CBEFF";

	/** PMS allowed-KYC format indicating partner-specific biometric template extraction. */
	public static final String EXTRACTION = "extraction";

	/** Policy operation indicating demographic or biometric attribute masking before share. */
	public static final String MASK = "mask";

	/** Identity demographic attribute key for date of birth. */
	public static final String DATEOFBIRTH = "dateOfBirth";

	/** Identity demographic attribute key for full name. */
	public static final String FULLNAME = "fullName";

	/** JSON property name for partner-supplied encryption key material in VC flows. */
	public static final String ENCRYPTIONKEY = "encryptionKey";

	/** Identifier type label for Virtual ID in VID generate/retrieve operations. */
	public static final String VID = "VID";

	/** VID service operation to create a new virtual identifier. */
	public static final String GENERATE = "GENERATE";

	/** VID service operation to fetch an existing virtual identifier. */
	public static final String RETRIEVE = "RETRIEVE";

	/** Identity attribute key for best-two-fingers biometric subset. */
	public static final String BESTTWOFINGERS = "bestTwoFingers";

	/** PMS policy section key listing attributes subject to masking. */
	public static final String MASKING_ATTRIBUTES = "maskingAttributes";

	/** PMS policy section key listing attributes subject to display formatting. */
	public static final String FORMATTING_ATTRIBUTES = "formatingAttributes";

	/** Generic identity attribute key for person name (used in format functions). */
	public static final String NAME = "name";

	/** Identity address component key for postal code. */
	public static final String POSTALCODE = "postalCode";

	/** Default ISO-639 language code applied when a single-locale attribute is emitted. */
	public static final String LANGUAGE = "eng";

	/** JSON/policy key describing output format for an allowed KYC attribute. */
	public static final String FORMAT = "format";

	/** Identity demographic attribute key for full postal address. */
	public static final String FULLADDRESS = "fullAddress";

	/** JSON key for biometric sub-type inside best-finger selection metadata. */
	public static final String BF_SUB_TYPE = "subType";

	/** JSON key for biometric rank inside best-finger selection metadata. */
	public static final String BF_RANK = "rank";

	/** Image or photo segment MIME subtype for extracted biometric photos. */
	public static final String JPEG = "jpeg";

	/** Spring property key for datetime pattern used when formatting credential timestamps. */
	public static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** Spring property key listing identity attribute names eligible for mask functions. */
	public static final String IDENTITY_ATTRIBUTES = "mosip.mask.function.identityAttributes";

	/** Spring property key for MVEL date-mask function script location or expression. */
	public static final String DATE_FORMAT_FUNCTION = "mosip.mask.function.date";

	/** Spring property key for MVEL address-format function script location or expression. */
	public static final String ADDRESS_FORMAT_FUNCTION = "mosip.format.function.address";

	/** Spring property key for MVEL name-format function script location or expression. */
	public static final String NAME_FORMAT_FUNCTION = "mosip.format.function.name";

	/** Spring property key for MVEL date-time format function script location or expression. */
	public static final String DATE_TIME_FORMAT_FUNCTION = "mosip.format.function.dateTimeFormat";

	/** Spring property key listing identity attribute names treated as photo sources. */
	public static final String CREDENTIAL_PHOTO_ATTRIBUTE_NAMES = "mosip.credential.photo.attribute.names";

	/** Spring property key listing identity attribute names treated as name sources. */
	public static final String CREDENTIAL_NAME_ATTRIBUTE_NAMES = "mosip.credential.name.attribute.names";

	/** Spring property key listing identity attribute names treated as address sources. */
	public static final String CREDENTIAL_ADDRESS_ATTRIBUTE_NAMES = "mosip.credential.address.attribute.names";
}
