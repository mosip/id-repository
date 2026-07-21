package io.mosip.idrepository.core.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-path mapping configuration for building {@link IdentityIssuanceProfile} snapshots.
 *
 * <p>
 * Loaded from the identity-mapping JSON referenced by
 * {@code mosip.identity.mapping-file-url} (see {@code IdRepoConstants.IDENTITY_MAPPING_JSON}).
 * Each nested class holds a {@code value} field: comma-separated JSON paths into raw
 * identity documents. {@link io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder}
 * resolves these paths when anonymizing old and new identity payloads.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Top-level sections: {@link #identity} (demographics / biometrics), {@link #metaInfo},
 * {@link #audits}, and {@link #documents} (POA/POI/POR/POB/POE). Nested types with
 * {@code getValueList()} split comma-separated paths into trimmed tokens for multi-field
 * extraction (name, address, location hierarchy).
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code IdentityIssuanceProfileBuilder} — primary consumer</li>
 *   <li>Config loaders that deserialize the mapping file into this type</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <p>
 * Jackson {@code @JsonProperty("IDSchemaVersion")} maps the schema-version key.
 * Do not change nested JSON property names without updating the mapping file and
 * builder path resolution together.
 * </p>
 *
 * @see IdentityIssuanceProfile
 * @see AnonymousProfile
 * @see io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder
 */
@Data
@NoArgsConstructor
public class IdentityMapping {

	/** Mapping entries for core identity demographic and biometric fields. */
	private Identity identity;

	/** Mapping entry for meta-information block paths. */
	private MetaInfo metaInfo;

	/** Mapping entry for audit trail paths. */
	private Audits audits;

	/** Mapping entries for proof-of-address, identity, residence, birth, and employment documents. */
	private Documents documents;

	/**
	 * Identity field path mappings used when extracting anonymized profile attributes.
	 */
	@Data
	@NoArgsConstructor
	public static class Identity {

		/** JSON path to the ID schema version field. */
		@JsonProperty("IDSchemaVersion")
		private IDSchemaVersion iDSchemaVersion;

		/** JSON path to the full name field. */
		private Name name;

		/** JSON path to the gender field. */
		private Gender gender;

		/** JSON path to location hierarchy used for profiling. */
		private LocationHierarchyForProfiling locationHierarchyForProfiling;

		/** JSON path to the date-of-birth field. */
		private Dob dob;

		/** JSON path to the age field. */
		private Age age;

		/** JSON path to preferred language. */
		private PreferredLanguages preferredLanguage;

		/** JSON path to introducer registration id. */
		private IntroducerRID introducerRID;

		/** JSON path to introducer UIN. */
		private IntroducerUIN introducerUIN;

		/** JSON path to introducer VID. */
		private IntroducerVID introducerVID;

		/** JSON path to introducer name. */
		private IntroducerName introducerName;

		/** JSON path to phone number. */
		private Phone phone;

		/** JSON path to email address. */
		private Email email;

		/** JSON path to UIN (typically excluded from anonymous output). */
		private Uin uin;

		/** JSON path to individual biometrics. */
		private IndividualBiometrics individualBiometrics;

		/** JSON path to introducer biometrics. */
		private IntroducerBiometrics introducerBiometrics;

		/** JSON path to individual authentication biometrics. */
		private IndividualAuthBiometrics individualAuthBiometrics;

		/** JSON path to officer biometric file name. */
		private OfficerBiometricFileName officerBiometricFileName;

		/** JSON path to supervisor biometric file name. */
		private SupervisorBiometricFileName supervisorBiometricFileName;

		/** JSON path to residence status. */
		private ResidenceStatus residenceStatus;

		/** JSON path to full address used for location profiling. */
		private FullAddress fullAddress;

		/** JSON path to selected handles on the identity. */
		private SelectedHandles selectedHandles;
	}

	/** Mapping entry for preferred language JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class PreferredLanguages {

		/** Comma-separated JSON path(s) to preferred language in identity JSON. */
		private String value;
	}

	/** Mapping entry for location hierarchy used in anonymous location profiling. */
	@Data
	@NoArgsConstructor
	public static class LocationHierarchyForProfiling {

		/** Comma-separated JSON path(s) to location hierarchy fields. */
		private String value;

		/**
		 * Splits {@link #value} into trimmed, non-blank path tokens.
		 *
		 * @return list of JSON path segments
		 */
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
	}

	/** Mapping entry for meta-information JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class MetaInfo {

		/** Comma-separated JSON path(s) to meta-info block. */
		private String value;
	}

	/** Mapping entry for audit JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Audits {

		/** Comma-separated JSON path(s) to audit records. */
		private String value;
	}

	/** Mapping entry for proof-of-address document path(s). */
	@Data
	@NoArgsConstructor
	public static class Poa {

		/** JSON path(s) to proof-of-address document reference. */
		private String value;
	}

	/** Mapping entry for proof-of-identity document path(s). */
	@Data
	@NoArgsConstructor
	public static class Poi {

		/** JSON path(s) to proof-of-identity document reference. */
		private String value;
	}

	/** Mapping entry for proof-of-residence document path(s). */
	@Data
	@NoArgsConstructor
	public static class Por {

		/** JSON path(s) to proof-of-residence document reference. */
		private String value;
	}

	/** Mapping entry for proof-of-birth document path(s). */
	@Data
	@NoArgsConstructor
	public static class Pob {

		/** JSON path(s) to proof-of-birth document reference. */
		private String value;
	}

	/** Mapping entry for proof-of-employment document path(s). */
	@Data
	@NoArgsConstructor
	public static class Poe {

		/** JSON path(s) to proof-of-employment document reference. */
		private String value;
	}

	/** Container for all document category path mappings. */
	@Data
	@NoArgsConstructor
	public static class Documents {

		/** Proof-of-address mapping. */
		private Poa poa;

		/** Proof-of-identity mapping. */
		private Poi poi;

		/** Proof-of-residence mapping. */
		private Por por;

		/** Proof-of-birth mapping. */
		private Pob pob;

		/** Proof-of-employment mapping. */
		private Poe poe;

		/**
		 * Collects document path values from all proof categories in fixed order.
		 *
		 * @return list of document path strings (poa, poi, por, pob, poe)
		 */
		public List<String> getValueList() {
			return List.of(poa.getValue(), poi.getValue(), por.getValue(), pob.getValue(), poe.getValue());
		}
	}

	/** Mapping entry for ID schema version JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IDSchemaVersion {

		/** JSON path to ID schema version in identity JSON. */
		private String value;
	}

	/** Mapping entry for full name JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Name {

		/** Comma-separated JSON path(s) to name components. */
		private String value;

		/**
		 * Splits {@link #value} into trimmed, non-blank path tokens.
		 *
		 * @return list of JSON path segments
		 */
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}

	}

	/** Mapping entry for gender JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Gender {

		/** JSON path to gender in identity JSON. */
		private String value;
	}

	/** Mapping entry for date-of-birth JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Dob {

		/** JSON path to date of birth in identity JSON. */
		private String value;
	}

	/** Mapping entry for age JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Age {

		/** JSON path to age in identity JSON. */
		private String value;
	}

	/** Mapping entry for introducer RID JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IntroducerRID {

		/** JSON path to introducer registration id. */
		private String value;
	}

	/** Mapping entry for introducer UIN JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IntroducerUIN {

		/** JSON path to introducer UIN. */
		private String value;
	}

	/** Mapping entry for introducer VID JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IntroducerVID {

		/** JSON path to introducer VID. */
		private String value;
	}

	/** Mapping entry for introducer name JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IntroducerName {

		/** JSON path to introducer name. */
		private String value;
	}

	/** Mapping entry for phone JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Phone {

		/** JSON path to phone number. */
		private String value;
	}

	/** Mapping entry for email JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Email {

		/** JSON path to email address. */
		private String value;
	}

	/** Mapping entry for UIN JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class Uin {

		/** JSON path to UIN field in identity JSON. */
		private String value;
	}

	/** Mapping entry for individual biometrics JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IndividualBiometrics {

		/** JSON path to individual biometrics CBEFF reference. */
		private String value;
	}

	/** Mapping entry for introducer biometrics JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IntroducerBiometrics {

		/** JSON path to introducer biometrics CBEFF reference. */
		private String value;
	}

	/** Mapping entry for individual authentication biometrics JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class IndividualAuthBiometrics {

		/** JSON path to authentication biometrics CBEFF reference. */
		private String value;
	}

	/** Mapping entry for officer biometric file name JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class OfficerBiometricFileName {

		/** JSON path to officer biometric file name. */
		private String value;
	}

	/** Mapping entry for supervisor biometric file name JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class SupervisorBiometricFileName {

		/** JSON path to supervisor biometric file name. */
		private String value;
	}

	/** Mapping entry for residence status JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class ResidenceStatus {

		/** JSON path to residence status. */
		private String value;
	}

	/** Mapping entry for full address JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class FullAddress {

		/** Comma-separated JSON path(s) to address components. */
		private String value;

		/**
		 * Splits {@link #value} into trimmed, non-blank path tokens.
		 *
		 * @return list of JSON path segments
		 */
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
	}

	/** Mapping entry for selected handles JSON path(s). */
	@Data
	@NoArgsConstructor
	public static class SelectedHandles {

		/** JSON path to selected handles on the identity. */
		private String value;
	}
}