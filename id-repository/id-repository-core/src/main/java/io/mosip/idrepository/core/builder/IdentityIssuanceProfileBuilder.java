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
import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Data;

@Data
public class IdentityIssuanceProfileBuilder {

	private static final String VALUE = "value";
	private static final String VERIFIED_ATTRIBUTES = "verifiedAttributes";
	private String processName;
	private JsonNode oldIdentity;
	private JsonNode newIdentity;
	private List<DocumentsDTO> oldDocuments;
	private List<DocumentsDTO> newDocuments;
	private AnonymousProfile oldProfile;
	private AnonymousProfile newProfile;
	private String filterLanguage;
	private static IdentityMapping identityMapping;
	private static ObjectMapper mapper = new ObjectMapper();
	private static String dateFormat;

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
			IdRepoLogger.getLogger(IdentityIssuanceProfileBuilder.class)
					.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));
			return new IdentityIssuanceProfile();
		}
	}

	private void buildOldProfile() {
		if (Objects.nonNull(oldIdentity)) {
			Optional<BIR> birListOpt = buildBirList(oldDocuments);
			this.setOldProfile(buildProfile(oldIdentity, birListOpt.isPresent() ? birListOpt.get().getBirs() : null));
		}

	}

	private void buildNewProfile() {
		if (Objects.nonNull(newIdentity)) {
			Optional<BIR> birListOpt = buildBirList(newDocuments);
			this.setNewProfile(buildProfile(newIdentity, birListOpt.isPresent() ? birListOpt.get().getBirs() : null));
		}
	}

	private Optional<BIR> buildBirList(List<DocumentsDTO> documents) {
		try {
			if (Objects.isNull(documents) || documents.isEmpty())
				return Optional.empty();
			else
				return Streams.stream(documents)
						.filter(doc -> Objects.nonNull(doc.getCategory()) && doc.getCategory()
								.contentEquals(getIdentityMapping().getIdentity().getIndividualBiometrics().getValue()))
						.map(doc -> CbeffValidator.getBIRFromXML(CryptoUtil.decodeURLSafeBase64(doc.getValue())))
						.stream().findAny();
		} catch (Exception e) {
			IdRepoLogger.getLogger(IdentityIssuanceProfileBuilder.class)
			.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));
			return Optional.empty();
		}
	}

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

	private String getYearOfBirth(JsonNode identity) {
		if (Objects.nonNull(getIdentityMapping().getIdentity().getDob().getValue())) {
			Optional<String> dobObj = extractValue(identity.get(getIdentityMapping().getIdentity().getDob().getValue()));
			if (dobObj.isPresent()) {
				return String.valueOf(LocalDate.parse(dobObj.get(), DateTimeFormatter.ofPattern(dateFormat)).getYear());
			}
		}
		return null;
	}

	private String getGender(JsonNode identity) {
		if (Objects.nonNull(getIdentityMapping().getIdentity().getGender().getValue())) {
			return extractValue(identity.get(getIdentityMapping().getIdentity().getGender().getValue())).orElse(null);
		}
		return null;
	}

	private List<String> getLocation(JsonNode identity) {
		if (Objects.nonNull(getIdentityMapping().getIdentity())
				&& Objects.nonNull(getIdentityMapping().getIdentity().getLocationHierarchyForProfiling())
				&& Objects.nonNull(getIdentityMapping().getIdentity().getLocationHierarchyForProfiling().getValue()))
			return getIdentityMapping().getIdentity().getLocationHierarchyForProfiling().getValueList().stream()
					.map(value -> extractValue(identity.get(value)).orElse("")).filter(StringUtils::isNotBlank)
					.collect(Collectors.toList());
		return List.of();
	}

	private String getPreferredLanguage(JsonNode identity) {
		if (Objects.nonNull(getIdentityMapping().getIdentity())
				&& Objects.nonNull(getIdentityMapping().getIdentity().getPreferredLanguage())
				&& Objects.nonNull(getIdentityMapping().getIdentity().getPreferredLanguage().getValue())
				&& Objects.nonNull(identity)
				&& Objects.nonNull(identity.get(getIdentityMapping().getIdentity().getPreferredLanguage().getValue())))
			return extractValue(identity.get(getIdentityMapping().getIdentity().getPreferredLanguage().getValue()))
					.orElse(null);
		return null;
	}

	private List<String> getChannel(JsonNode identity) {
		List<String> channelList = new ArrayList<>();
		channelList.add(
				extractValue(identity.get(getIdentityMapping().getIdentity().getPhone().getValue())).isPresent() ? "PHONE"
						: null);
		channelList.add(
				extractValue(identity.get(getIdentityMapping().getIdentity().getEmail().getValue())).isPresent() ? "EMAIL"
						: null);
		channelList.removeIf(Objects::isNull);
		return channelList;
	}

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

	private JsonNode getVerified(JsonNode identity) {
		return Objects.isNull(identity.get(VERIFIED_ATTRIBUTES)) || identity.get(VERIFIED_ATTRIBUTES).isNull() ? mapper.createObjectNode()
				: identity.get(VERIFIED_ATTRIBUTES);
	}

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
							Map<String, String> digitalIdEncoded = mapper.readValue(payload.get(),
									new TypeReference<Map<String, String>>() {
									});
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

	private List<String> getDocuments(JsonNode identity) {
		return getIdentityMapping().getDocuments().getValueList().stream()
				.filter(docCategory -> Objects.nonNull(identity.get(docCategory)))
				.filter(docCategory -> Objects.nonNull(identity.get(docCategory).get("type")))
				.map(docCategory -> identity.get(docCategory).get("type").asText()).filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());
	}

	public static IdentityMapping getIdentityMapping() {
		return identityMapping;
	}
	
	public static void setIdentityMapping(IdentityMapping identityMapping) {
		IdentityIssuanceProfileBuilder.identityMapping = identityMapping;
	}

	public IdentityIssuanceProfileBuilder setFilterLanguage(String filterLanguage) {
		this.filterLanguage = filterLanguage;
		return this;
	}

	public IdentityIssuanceProfileBuilder setProcessName(String processName) {
		this.processName = processName;
		return this;
	}

	public IdentityIssuanceProfileBuilder setOldIdentity(byte[] identity) {
		try {
			if (Objects.nonNull(identity))
				this.oldIdentity = mapper.readTree(identity);
		} catch (IOException e) {
			// this block should never be executed
			IdRepoLogger.getLogger(IdentityIssuanceProfileBuilder.class)
				.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));			
		}
		return this;
	}

	public IdentityIssuanceProfileBuilder setOldDocuments(List<DocumentsDTO> oldDocuments) {
		this.oldDocuments = oldDocuments;
		return this;
	}

	public IdentityIssuanceProfileBuilder setNewIdentity(byte[] identity) {
		try {
			if (Objects.nonNull(identity))
				this.newIdentity = mapper.readTree(identity);
		} catch (IOException e) {
			// this block should never be executed
			IdRepoLogger.getLogger(IdentityIssuanceProfileBuilder.class)
				.warn("EXCEPTION --->>> " + ExceptionUtils.getStackTrace(e));
		}
		return this;
	}

	public IdentityIssuanceProfileBuilder setNewDocuments(List<DocumentsDTO> newDocuments) {
		this.newDocuments = newDocuments;
		return this;
	}

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
				Map<String, Object> valueMap = mapper.convertValue(iterator.next(),
						new TypeReference<Map<String, Object>>() {
						});
				if (StringUtils.isNotBlank(filterLanguage) &&  valueMap.containsKey("language") && ((String)valueMap.get("language")).contentEquals(filterLanguage)) {
					valueOpt = Optional.of((String) valueMap.get(VALUE));
				}
			}
			if (valueOpt.isEmpty())
				valueOpt = Optional.ofNullable(jsonNode.iterator().next().get(VALUE).asText());
		} else if (jsonNode.isObject()) {
			Map<String, String> valueMap = mapper.convertValue(jsonNode, new TypeReference<Map<String, String>>() {
			});
			if (StringUtils.isNotBlank(filterLanguage) && valueMap.get("language").contentEquals(filterLanguage)) {
				valueOpt = Optional.of(valueMap.get(VALUE));
			}
			if (valueOpt.isEmpty())
				valueOpt = Optional.ofNullable(valueMap.get(VALUE));
		}
		return valueOpt;
	}

	public static void setDateFormat(String dateFormat) {
		IdentityIssuanceProfileBuilder.dateFormat = dateFormat;
	}
}
