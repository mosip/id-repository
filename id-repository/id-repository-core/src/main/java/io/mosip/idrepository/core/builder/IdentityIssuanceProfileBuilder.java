package io.mosip.idrepository.core.builder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.stream.Streams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.AnonymousProfile;
import io.mosip.idrepository.core.dto.BiometricInfo;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.Exceptions;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Data;

/**
 * Fluent builder for anonymized {@link IdentityIssuanceProfile} snapshots used by
 * identity-issuance observability (IOV / anonymous profile).
 *
 * <p>
 * Given old and/or new identity JSON plus optional document lists, this builder extracts
 * non-PII profiling attributes (year of birth, gender, location hierarchy, channels,
 * verified attributes, document types, and biometric metadata) into
 * {@link AnonymousProfile} instances. Raw UIN, full DOB, and biometric templates are
 * never copied into the profile.
 * </p>
 *
 * <h2>Static configuration (process-wide)</h2>
 * <p>
 * Call once at application startup (typically from identity config):
 * </p>
 * <ul>
 *   <li>{@link #setIdentityMapping(IdentityMapping)} — schema field paths; rebuilds
 *       cached {@link MappingFields}</li>
 *   <li>{@link #setDateFormat(String)} — DOB parse pattern (e.g. {@code uuuu/MM/dd})</li>
 * </ul>
 *
 * <h2>Instance usage</h2>
 * <pre>
 * IdentityIssuanceProfile profile = new IdentityIssuanceProfileBuilder()
 *     .setProcessName("Update")
 *     .setFilterLanguage("eng")
 *     .setOldIdentity(oldJsonBytes)
 *     .setOldDocuments(oldDocs)
 *     .setNewIdentity(newJsonBytes)
 *     .setNewDocuments(newDocs)
 *     .build();
 * </pre>
 *
 * <h2>Failure policy</h2>
 * <p>
 * {@link #build()} catches unexpected exceptions, logs a warning, and returns an empty
 * {@link IdentityIssuanceProfile} so anonymous-profile generation never fails the parent
 * identity create/update transaction. Document/CBEFF parse errors similarly yield empty
 * biometric sections rather than throwing.
 * </p>
 *
 * <h2>Multilingual fields</h2>
 * <p>
 * {@link #extractValue(JsonNode)} prefers entries matching {@link #filterLanguage}. When
 * blank at build time, the preferred language is taken from the new identity (if mapped).
 * If no language match exists, the first array/object value is used.
 * </p>
 *
 * @see IdentityIssuanceProfile
 * @see AnonymousProfile
 * @see IdentityMapping
 */
@Data
public class IdentityIssuanceProfileBuilder {

	/** JSON property name for localized attribute values. */
	private static final String VALUE = "value";

	/** Identity JSON key holding the list of verified attribute names. */
	private static final String VERIFIED_ATTRIBUTES = "verifiedAttributes";

	/** Structured logger for parse and build failures (never rethrown from {@link #build()}). */
	private static Logger mosipLogger = IdRepoLogger.getLogger(IdentityIssuanceProfileBuilder.class);

	/** Shared, thread-safe Jackson mapper for identity JSON parsing. */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Reusable type token for verified-attributes list deserialization. */
	private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {
	};

	/** Reusable type token for language/value map deserialization. */
	private static final TypeReference<Map<String, String>> MAP_STRING_STRING_TYPE = new TypeReference<>() {
	};

	/** Registration or update process name (e.g. {@code New}, {@code Update}). */
	private String processName;

	/** Parsed JSON of the identity before the change; may be {@code null}. */
	private JsonNode oldIdentity;

	/** Parsed JSON of the identity after the change; may be {@code null}. */
	private JsonNode newIdentity;

	/** Document list associated with the old identity. */
	private List<DocumentsDTO> oldDocuments;

	/** Document list associated with the new identity. */
	private List<DocumentsDTO> newDocuments;

	/** Built anonymous profile for the old identity; set during {@link #build()}. */
	private AnonymousProfile oldProfile;

	/** Built anonymous profile for the new identity; set during {@link #build()}. */
	private AnonymousProfile newProfile;

	/**
	 * Preferred language code used when extracting multilingual identity fields.
	 * Defaults from new identity when blank at build time.
	 */
	private String filterLanguage;

	/** Active ID schema to JSON field mapping configuration. */
	private static IdentityMapping identityMapping;

	/** Pre-resolved field paths derived from {@link #identityMapping}. */
	private static MappingFields mappingFields;

	/** Configured date-of-birth format pattern string (e.g. {@code uuuu/MM/dd}). */
	private static String dateFormat;

	/** Cached formatter for {@link #dateFormat}; rebuilt in {@link #setDateFormat(String)}. */
	private static DateTimeFormatter dateFormatter;

	/**
	 * Builds the issuance profile containing anonymized old and new snapshots.
	 * <p>
	 * When {@link #filterLanguage} is blank, it is set from the preferred language of
	 * {@link #newIdentity} (if available). On any unexpected error returns an empty
	 * {@link IdentityIssuanceProfile} rather than failing the parent transaction.
	 * </p>
	 *
	 * @return populated profile with {@code processName}, {@code date}, and optional
	 *         old/new profiles; or an empty profile when building fails
	 */
	public IdentityIssuanceProfile build() {
		try {
			if (StringUtils.isBlank(filterLanguage))
				this.setFilterLanguage(this.getPreferredLanguage(newIdentity));
			buildOldProfile();
			buildNewProfile();
			IdentityIssuanceProfile profile = new IdentityIssuanceProfile();
			profile.setProcessName(processName);
			profile.setDate(LocalDate.now());
			profile.setOldProfile(oldProfile);
			profile.setNewProfile(newProfile);
			return profile;
		} catch (Exception e) {
			mosipLogger.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));
			return new IdentityIssuanceProfile();
		}
	}

	/**
	 * Populates {@link #oldProfile} when {@link #oldIdentity} is present.
	 * <p>
	 * Biometric BIR data is decoded from the individual-biometrics CBEFF document in
	 * {@link #oldDocuments} when mapping and documents are available.
	 * </p>
	 */
	private void buildOldProfile() {
		if (Objects.nonNull(oldIdentity)) {
			Optional<BIR> birListOpt = buildBirList(oldDocuments);
			this.setOldProfile(buildProfile(oldIdentity, birListOpt.isPresent() ? birListOpt.get().getBirs() : null));
		}
	}

	/**
	 * Populates {@link #newProfile} when {@link #newIdentity} is present.
	 * <p>
	 * Same biometric extraction rules as {@link #buildOldProfile()} using
	 * {@link #newDocuments}.
	 * </p>
	 */
	private void buildNewProfile() {
		if (Objects.nonNull(newIdentity)) {
			Optional<BIR> birListOpt = buildBirList(newDocuments);
			this.setNewProfile(buildProfile(newIdentity, birListOpt.isPresent() ? birListOpt.get().getBirs() : null));
		}
	}

	/**
	 * Extracts the first individual-biometrics CBEFF document from the document list.
	 * <p>
	 * Matches {@link DocumentsDTO#getCategory()} against the mapped
	 * {@code individualBiometrics} category, URL-safe Base64-decodes the value, and
	 * parses CBEFF via {@link CbeffValidator#getBIRFromXML(byte[])}.
	 * </p>
	 *
	 * @param documents identity documents; may be {@code null} or empty
	 * @return parsed {@link BIR} wrapper, or empty when none found or on parse failure
	 */
	private Optional<BIR> buildBirList(List<DocumentsDTO> documents) {
		try {
			if (Objects.isNull(documents) || documents.isEmpty())
				return Optional.empty();
			MappingFields fields = mappingFields;
			if (fields == null || fields.individualBiometrics == null) {
				return Optional.empty();
			}
			return Streams.stream(documents)
					.filter(doc -> Objects.nonNull(doc.getCategory())
							&& doc.getCategory().contentEquals(fields.individualBiometrics))
					.map(doc -> CbeffValidator.getBIRFromXML(CryptoUtil.decodeURLSafeBase64(doc.getValue())))
					.stream().findAny();
		} catch (Exception e) {
			mosipLogger.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));
			return Optional.empty();
		}
	}

	/**
	 * Assembles one {@link AnonymousProfile} from identity JSON and optional biometric records.
	 *
	 * @param identity parsed identity JSON node
	 * @param bioData  decoded BIR entries; may be {@code null}
	 * @return anonymized profile snapshot (year of birth, gender, location, channels,
	 *         exceptions, verified attributes, biometric info, document types)
	 */
	private AnonymousProfile buildProfile(JsonNode identity, List<BIR> bioData) {
		return AnonymousProfile.builder()
				.yearOfBirth(this.getYearOfBirth(identity))
				.gender(this.getGender(identity))
				.location(this.getLocation(identity))
				.preferredLanguage(this.getPreferredLanguage(identity))
				.channel(this.getChannel(identity))
				.exceptions(this.getExceptions(bioData))
				.verified(this.getVerified(identity))
				.biometricInfo(this.getBiometricInfo(bioData))
				.documents(this.getDocuments(identity))
				.build();
	}

	/**
	 * Derives year of birth from the mapped DOB field using {@link #dateFormatter}.
	 * <p>
	 * Only the year is retained for anonymity; full date of birth is never stored on the
	 * profile.
	 * </p>
	 *
	 * @param identity identity JSON node
	 * @return birth year as string, or {@code null} when DOB is absent, unmapped, or
	 *         unparseable
	 */
	private String getYearOfBirth(JsonNode identity) {
		MappingFields fields = mappingFields;
		if (fields != null && fields.dob != null && dateFormatter != null) {
			Optional<String> dobObj = extractValue(identity.get(fields.dob));
			if (dobObj.isPresent()) {
				return String.valueOf(LocalDate.parse(dobObj.get(), dateFormatter).getYear());
			}
		}
		return null;
	}

	/**
	 * Extracts the gender value using the mapped gender field path.
	 *
	 * @param identity identity JSON node
	 * @return gender value, or {@code null} when not mapped or absent
	 */
	private String getGender(JsonNode identity) {
		MappingFields fields = mappingFields;
		if (fields != null && fields.gender != null) {
			return extractValue(identity.get(fields.gender)).orElse(null);
		}
		return null;
	}

	/**
	 * Extracts ordered location hierarchy values used for profiling.
	 * <p>
	 * Blank values are filtered out. Order follows
	 * {@link IdentityMapping.Identity#getLocationHierarchyForProfiling()}.
	 * </p>
	 *
	 * @param identity identity JSON node
	 * @return ordered non-blank location values; empty list when unmapped
	 */
	private List<String> getLocation(JsonNode identity) {
		MappingFields fields = mappingFields;
		if (fields != null && !fields.locationHierarchy.isEmpty()) {
			return fields.locationHierarchy.stream()
					.map(value -> extractValue(identity.get(value)).orElse("")).filter(StringUtils::isNotBlank)
					.collect(Collectors.toList());
		}
		return List.of();
	}

	/**
	 * Extracts the preferred language code from the mapped field.
	 *
	 * @param identity identity JSON node; may be {@code null}
	 * @return preferred language code, or {@code null} when not present
	 */
	private String getPreferredLanguage(JsonNode identity) {
		MappingFields fields = mappingFields;
		if (fields != null && fields.preferredLanguage != null && Objects.nonNull(identity)
				&& Objects.nonNull(identity.get(fields.preferredLanguage))) {
			return extractValue(identity.get(fields.preferredLanguage)).orElse(null);
		}
		return null;
	}

	/**
	 * Detects notification channels present on the identity.
	 * <p>
	 * Presence of mapped phone / email fields yields {@code PHONE} and/or {@code EMAIL}
	 * markers; actual contact values are not included.
	 * </p>
	 *
	 * @param identity identity JSON node
	 * @return channel indicators ({@code PHONE}, {@code EMAIL}); empty when unmapped
	 */
	private List<String> getChannel(JsonNode identity) {
		MappingFields fields = mappingFields;
		if (fields == null) {
			return List.of();
		}
		List<String> channelList = new ArrayList<>();
		channelList.add(extractValue(identity.get(fields.phone)).isPresent() ? "PHONE" : null);
		channelList.add(extractValue(identity.get(fields.email)).isPresent() ? "EMAIL" : null);
		channelList.removeIf(Objects::isNull);
		return channelList;
	}

	/**
	 * Collects biometric exception markers declared in CBEFF {@code others} metadata.
	 * <p>
	 * Includes BIR entries where {@code others.EXCEPTION=true}, mapping type/subtype from
	 * {@link BIR} BDB info.
	 * </p>
	 *
	 * @param bioData decoded biometric records; may be {@code null}
	 * @return exception markers; empty list when {@code bioData} is null or none match
	 */
	private List<Exceptions> getExceptions(List<BIR> bioData) {
		if (Objects.nonNull(bioData))
			return bioData.stream().filter(bir -> Objects.nonNull(bir.getOthers()))
					.filter(bir -> bir.getOthers().keySet().stream()
							.anyMatch(key -> key.contentEquals("EXCEPTION")))
					.filter(bir -> bir.getOthers().get("EXCEPTION").contentEquals("true"))
					.map(bir -> Exceptions.builder()
							.type(bir.getBdbInfo().getType().stream().map(BiometricType::value)
									.collect(Collectors.joining(" ")))
							.subType(String.join(" ", bir.getBdbInfo().getSubtype())).build())
					.collect(Collectors.toList());
		return List.of();
	}

	/**
	 * Reads the {@code verifiedAttributes} array from identity JSON.
	 *
	 * @param identity identity JSON node
	 * @return list of verified attribute names, or empty when absent or null
	 */
	private List<String> getVerified(JsonNode identity) {
		return Objects.isNull(identity.get(VERIFIED_ATTRIBUTES)) || identity.get(VERIFIED_ATTRIBUTES).isNull() ? List.of()
				: MAPPER.convertValue(identity.get(VERIFIED_ATTRIBUTES), LIST_STRING_TYPE);
	}

	/**
	 * Builds anonymized biometric metadata (type, subtype, quality, retries, digital id).
	 * <p>
	 * Digital ID is decoded from JWT payload segment in CBEFF {@code others.PAYLOAD} when
	 * present. Biometric templates (BDB) are not copied.
	 * </p>
	 *
	 * @param biometrics decoded biometric records; may be {@code null}
	 * @return anonymized biometric info list; empty when {@code biometrics} is null
	 */
	private List<BiometricInfo> getBiometricInfo(List<BIR> biometrics) {
		if (Objects.nonNull(biometrics))
			return Streams.stream(biometrics)
				.map(bir -> {
						Optional<String> payload = Optional.ofNullable(bir.getOthers())
								.stream()
								.filter(othersMap -> othersMap.containsKey("PAYLOAD"))
								.map(othersMap -> othersMap.get("PAYLOAD"))
								.findFirst();
						
						String digitalId = null;
						if (payload.isPresent() && StringUtils.isNotBlank(payload.get())) {
							Map<String, String> digitalIdEncoded = MAPPER.readValue(payload.get(),
									MAP_STRING_STRING_TYPE);
							digitalId = new String(
									CryptoUtil.decodeURLSafeBase64(digitalIdEncoded.get("digitalId").split("\\.")[1]));
						}
						
						return BiometricInfo.builder()
								.type(bir.getBdbInfo().getType().stream().map(BiometricType::value)
										.collect(Collectors.joining(" ")))
								.subType(String.join(" ", bir.getBdbInfo().getSubtype()))
								.qualityScore(bir.getBdbInfo().getQuality().getScore())
								.attempts(Objects.nonNull(bir.getOthers()) && bir.getOthers().containsKey("RETRIES")
										? bir.getOthers().get("RETRIES")
										: null)
								.digitalId(digitalId)
								.build();
					}).collect(Collectors.toList());
		return List.of();
	}

	/**
	 * Collects document type strings for mapped document categories present on the identity.
	 *
	 * @param identity identity JSON node
	 * @return non-blank document type values; empty when categories are unmapped
	 */
	private List<String> getDocuments(JsonNode identity) {
		MappingFields fields = mappingFields;
		if (fields == null || fields.documentCategories.isEmpty()) {
			return List.of();
		}
		return fields.documentCategories.stream()
				.filter(docCategory -> Objects.nonNull(identity.get(docCategory)))
				.filter(docCategory -> Objects.nonNull(identity.get(docCategory).get("type")))
				.map(docCategory -> identity.get(docCategory).get("type").asText()).filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());
	}

	/**
	 * Returns the process-wide identity schema mapping.
	 *
	 * @return the active {@link IdentityMapping} configuration; may be {@code null} before
	 *         {@link #setIdentityMapping(IdentityMapping)} is called
	 */
	public static IdentityMapping getIdentityMapping() {
		return identityMapping;
	}

	/**
	 * Sets the identity schema mapping and rebuilds cached field paths.
	 * <p>
	 * Must be called before {@link #build()} for meaningful field extraction. Passing
	 * {@code null} clears cached {@link MappingFields}.
	 * </p>
	 *
	 * @param identityMapping mapping from schema field names to JSON keys; may be {@code null}
	 */
	public static void setIdentityMapping(IdentityMapping identityMapping) {
		IdentityIssuanceProfileBuilder.identityMapping = identityMapping;
		IdentityIssuanceProfileBuilder.mappingFields = MappingFields.from(identityMapping);
	}

	/**
	 * Sets the language filter used when extracting multilingual identity fields.
	 *
	 * @param filterLanguage ISO language code (e.g. {@code eng}); may be blank to auto-detect
	 *                       from new identity at {@link #build()} time
	 * @return this builder for chaining
	 */
	public IdentityIssuanceProfileBuilder setFilterLanguage(String filterLanguage) {
		this.filterLanguage = filterLanguage;
		return this;
	}

	/**
	 * Sets the registration/update process label stored on the profile.
	 *
	 * @param processName process label (e.g. {@code New}, {@code Update})
	 * @return this builder for chaining
	 */
	public IdentityIssuanceProfileBuilder setProcessName(String processName) {
		this.processName = processName;
		return this;
	}

	/**
	 * Parses and stores the pre-change identity JSON.
	 * <p>
	 * Parse failures are logged and leave {@link #oldIdentity} unchanged / null rather than
	 * throwing.
	 * </p>
	 *
	 * @param identity UTF-8 identity JSON bytes; may be {@code null}
	 * @return this builder for chaining
	 */
	public IdentityIssuanceProfileBuilder setOldIdentity(byte[] identity) {
		try {
			if (Objects.nonNull(identity))
				this.oldIdentity = MAPPER.readTree(identity);
		} catch (IOException e) {
			// this block should never be executed
			mosipLogger.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));			
		}
		return this;
	}

	/**
	 * Sets documents tied to the old identity (used for CBEFF biometric extraction).
	 *
	 * @param oldDocuments document list; may be {@code null} or empty
	 * @return this builder for chaining
	 */
	public IdentityIssuanceProfileBuilder setOldDocuments(List<DocumentsDTO> oldDocuments) {
		this.oldDocuments = oldDocuments;
		return this;
	}

	/**
	 * Parses and stores the post-change identity JSON.
	 * <p>
	 * Parse failures are logged and leave {@link #newIdentity} unchanged / null rather than
	 * throwing.
	 * </p>
	 *
	 * @param identity UTF-8 identity JSON bytes; may be {@code null}
	 * @return this builder for chaining
	 */
	public IdentityIssuanceProfileBuilder setNewIdentity(byte[] identity) {
		try {
			if (Objects.nonNull(identity))
				this.newIdentity = MAPPER.readTree(identity);
		} catch (IOException e) {
			// this block should never be executed
			mosipLogger.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));
		}
		return this;
	}

	/**
	 * Sets documents tied to the new identity (used for CBEFF biometric extraction).
	 *
	 * @param newDocuments document list; may be {@code null} or empty
	 * @return this builder for chaining
	 */
	public IdentityIssuanceProfileBuilder setNewDocuments(List<DocumentsDTO> newDocuments) {
		this.newDocuments = newDocuments;
		return this;
	}

	/**
	 * Extracts a scalar string from a JSON value node, object, or language-tagged array.
	 * <p>
	 * Resolution rules:
	 * </p>
	 * <ul>
	 *   <li>Value node — {@link JsonNode#asText()}</li>
	 *   <li>Array of {@code {language, value}} — prefer {@link #filterLanguage}, else first
	 *       entry</li>
	 *   <li>Object map — prefer matching language, else {@code value} key</li>
	 * </ul>
	 *
	 * @param jsonNode field node from identity JSON; may be {@code null}
	 * @return extracted value, or empty when the node is null / unmatched
	 */
	private Optional<String> extractValue(JsonNode jsonNode) {
		if (Objects.isNull(jsonNode)) {
			return Optional.empty();
		}
		Optional<String> valueOpt = Optional.empty();
		if (jsonNode.isValueNode()) {
			return Optional.of(jsonNode.asText());
		} else if (jsonNode.isArray()) {
			Iterator<JsonNode> iterator = jsonNode.iterator();
			while (iterator.hasNext()) {
				Map<String, String> valueMap = MAPPER.convertValue(iterator.next(), MAP_STRING_STRING_TYPE);
				if (StringUtils.isNotBlank(filterLanguage) && valueMap.get("language").contentEquals(filterLanguage)) {
					valueOpt = Optional.of(valueMap.get(VALUE));
				}
			}
			if (valueOpt.isEmpty())
				valueOpt = Optional.ofNullable(jsonNode.iterator().next().get(VALUE).asText());
		} else if (jsonNode.isObject()) {
			Map<String, String> valueMap = MAPPER.convertValue(jsonNode, MAP_STRING_STRING_TYPE);
			if (StringUtils.isNotBlank(filterLanguage) && valueMap.get("language").contentEquals(filterLanguage)) {
				valueOpt = Optional.of(valueMap.get(VALUE));
			}
			if (valueOpt.isEmpty())
				valueOpt = Optional.ofNullable(valueMap.get(VALUE));
		}
		return valueOpt;
	}

	/**
	 * Configures the date-of-birth parse pattern and rebuilds the cached formatter.
	 * <p>
	 * Blank patterns clear {@link #dateFormatter}, causing {@link #getYearOfBirth(JsonNode)}
	 * to return {@code null}.
	 * </p>
	 *
	 * @param dateFormat pattern compatible with {@link DateTimeFormatter#ofPattern(String)}
	 *                   (e.g. {@code uuuu/MM/dd}); may be blank
	 */
	public static void setDateFormat(String dateFormat) {
		IdentityIssuanceProfileBuilder.dateFormat = dateFormat;
		IdentityIssuanceProfileBuilder.dateFormatter = StringUtils.isNotBlank(dateFormat)
				? DateTimeFormatter.ofPattern(dateFormat)
				: null;
	}

	/**
	 * Pre-resolved JSON field paths from {@link IdentityMapping} for hot-path profile building.
	 * <p>
	 * Avoids repeated null-safe navigation of the mapping tree on every identity create/update.
	 * Rebuilt whenever {@link #setIdentityMapping(IdentityMapping)} is called.
	 * </p>
	 */
	private static final class MappingFields {

		/** JSON key for date of birth. */
		private final String dob;

		/** JSON key for gender. */
		private final String gender;

		/** Ordered JSON keys for location hierarchy profiling. */
		private final List<String> locationHierarchy;

		/** JSON key for preferred language. */
		private final String preferredLanguage;

		/** JSON key for phone channel. */
		private final String phone;

		/** JSON key for email channel. */
		private final String email;

		/** Document category value for individual biometrics CBEFF. */
		private final String individualBiometrics;

		/** Document category keys whose types are included in the profile. */
		private final List<String> documentCategories;

		/**
		 * @param dob                    date-of-birth field key
		 * @param gender                 gender field key
		 * @param locationHierarchy      location field keys in profiling order
		 * @param preferredLanguage      preferred-language field key
		 * @param phone                  phone field key
		 * @param email                  email field key
		 * @param individualBiometrics   biometrics document category
		 * @param documentCategories     document category keys for type extraction
		 */
		private MappingFields(String dob, String gender, List<String> locationHierarchy, String preferredLanguage,
				String phone, String email, String individualBiometrics, List<String> documentCategories) {
			this.dob = dob;
			this.gender = gender;
			this.locationHierarchy = locationHierarchy;
			this.preferredLanguage = preferredLanguage;
			this.phone = phone;
			this.email = email;
			this.individualBiometrics = individualBiometrics;
			this.documentCategories = documentCategories;
		}

		/**
		 * Builds cached field paths from a mapping configuration.
		 *
		 * @param mapping active identity mapping; may be {@code null}
		 * @return resolved fields, or {@code null} when mapping or identity section is absent
		 */
		private static MappingFields from(IdentityMapping mapping) {
			if (mapping == null || mapping.getIdentity() == null) {
				return null;
			}
			IdentityMapping.Identity identity = mapping.getIdentity();
			List<String> locationHierarchy = identity.getLocationHierarchyForProfiling() != null
					? identity.getLocationHierarchyForProfiling().getValueList()
					: List.of();
			List<String> documentCategories = mapping.getDocuments() != null
					? mapping.getDocuments().getValueList()
					: List.of();
			return new MappingFields(
					identity.getDob() != null ? identity.getDob().getValue() : null,
					identity.getGender() != null ? identity.getGender().getValue() : null,
					locationHierarchy,
					identity.getPreferredLanguage() != null ? identity.getPreferredLanguage().getValue() : null,
					identity.getPhone() != null ? identity.getPhone().getValue() : null,
					identity.getEmail() != null ? identity.getEmail().getValue() : null,
					identity.getIndividualBiometrics() != null ? identity.getIndividualBiometrics().getValue() : null,
					documentCategories);
		}
	}
}
