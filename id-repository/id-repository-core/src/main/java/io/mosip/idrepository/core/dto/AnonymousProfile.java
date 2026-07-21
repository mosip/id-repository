package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Anonymized demographic and biometric summary for identity issuance profiling.
 *
 * <p>
 * Captures year of birth, gender, location hierarchy, channels, biometric
 * exceptions, and document categories without exposing full PII. Used as the
 * {@code oldProfile} / {@code newProfile} snapshots inside
 * {@link IdentityIssuanceProfile}.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Built by {@link io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder}
 * from raw identity JSON using {@link IdentityMapping} path configuration.
 * Lombok {@code @Builder} constructs old/new profile snapshots for WebSub or
 * analytics consumers.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link IdentityIssuanceProfile}</li>
 *   <li>{@code IdentityIssuanceProfileBuilder}</li>
 *   <li>Downstream analytics / WebSub subscribers of issuance profiles</li>
 * </ul>
 *
 * @see IdentityIssuanceProfile
 * @see Exceptions
 * @see BiometricInfo
 * @see io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder
 */
@Builder(toBuilder = true)
@Data
public class AnonymousProfile {

	/** Year of birth derived from identity date-of-birth. */
	private String yearOfBirth;

	/** Gender code or value from the identity record. */
	private String gender;

	/** Hierarchical location codes (region, province, etc.). */
	private List<String> location;

	/** Preferred language code for the individual. */
	private String preferredLanguage;

	/** Communication channels (phone, email, etc.). */
	private List<String> channel;

	/** Biometric capture exceptions recorded during registration. */
	private List<Exceptions> exceptions;

	/** Verified attribute names present on the identity. */
	private List<String> verified;

	/** Summarized biometric capture metadata per modality. */
	private List<BiometricInfo> biometricInfo;

	/** Document category identifiers attached to the identity. */
	private List<String> documents;
}
