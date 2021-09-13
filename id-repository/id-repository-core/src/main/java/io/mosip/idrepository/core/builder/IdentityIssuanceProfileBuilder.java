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
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.stream.Streams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.AnonymousProfile;
import io.mosip.idrepository.core.dto.BiometricInfo;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.Exceptions;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.Entry;
import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Data;

@Data
public class IdentityIssuanceProfileBuilder {

	private String processName;
	private JsonNode oldIdentity;
	private JsonNode newIdentity;
	private List<DocumentsDTO> oldDocuments;
	private List<DocumentsDTO> newDocuments;
	private List<String> verifiedAttributes;
	private AnonymousProfile oldProfile;
	private AnonymousProfile newProfile;
	private static String filterLanguage;
	private static IdentityMapping identityMapping;
	private static ObjectMapper mapper = new ObjectMapper();
	private static String dateFormat;

	public IdentityIssuanceProfile build() {
		if (StringUtils.isBlank(filterLanguage))
			filterLanguage = this.getPreferredLanguage(oldIdentity);
		buildOldProfile();
		buildNewProfile();
		IdentityIssuanceProfile profile = new IdentityIssuanceProfile();
		profile.setProcessName(processName);
		profile.setDate(LocalDate.now());
		profile.setOldProfile(oldProfile);
		profile.setNewProfile(newProfile);
		return profile;
	}

	private void buildOldProfile() {
		Optional<BIR> birListOpt = buildBirList(oldDocuments);
		if (Objects.nonNull(oldIdentity))
			this.setOldProfile(buildProfile(oldIdentity, birListOpt.isPresent() ? birListOpt.get().getBirs() : null));

	}

	private void buildNewProfile() {
		Optional<BIR> birListOpt = buildBirList(newDocuments);
		if (Objects.nonNull(newIdentity))
			this.setNewProfile(buildProfile(newIdentity, birListOpt.isPresent() ? birListOpt.get().getBirs() : null));

	}

	private Optional<BIR> buildBirList(List<DocumentsDTO> documents) {
		return Streams.stream(documents)
				.filter(doc -> Objects.nonNull(doc.getCategory()) && doc.getCategory()
						.contentEquals(identityMapping.getIdentity().getIndividualBiometrics().getValue()))
				.map(doc -> CbeffValidator.getBIRFromXML(CryptoUtil.decodeBase64(doc.getValue()))).stream().findAny();
	}

	private AnonymousProfile buildProfile(JsonNode identity, List<BIR> bioData) {
		return AnonymousProfile.builder()
				.yearOfBirth(this.getYearOfBirth(identity))
				.gender(this.getGender(identity))
				.location(this.getLocation(identity))
				.preferredLanguage(this.getPreferredLanguage(identity))
				.channel(this.getChannel(identity))
				.exceptions(this.getExceptions(bioData))
				.verified(this.getVerified())
				.biometricInfo(this.getBiometricInfo(bioData))
				.documents(this.getDocuments(identity))
				.build();
	}

	private String getYearOfBirth(JsonNode identity) {
		if (Objects.nonNull(identityMapping.getIdentity().getDob().getValue())) {
			Optional<String> dobObj = extractValue(identity.get(identityMapping.getIdentity().getDob().getValue()));
			if (dobObj.isPresent()) {
				return String.valueOf(LocalDate.parse(dobObj.get(), DateTimeFormatter.ofPattern(dateFormat)).getYear());
			}
		}
		return null;
	}

	private String getGender(JsonNode identity) {
		if (Objects.nonNull(identityMapping.getIdentity().getGender().getValue())) {
			return extractValue(identity.get(identityMapping.getIdentity().getGender().getValue())).orElse(null);
		}
		return null;
	}

	private List<String> getLocation(JsonNode identity) {
		if (Objects.nonNull(identityMapping.getIdentity())
				&& Objects.nonNull(identityMapping.getIdentity().getLocationHierarchyForProfiling())
				&& Objects.nonNull(identityMapping.getIdentity().getLocationHierarchyForProfiling().getValue()))
			return identityMapping.getIdentity().getLocationHierarchyForProfiling().getValueList().stream()
					.map(value -> extractValue(identity.get(value)).orElse("")).filter(StringUtils::isNotBlank)
					.collect(Collectors.toList());
		return null;
	}

	private String getPreferredLanguage(JsonNode identity) {
		if (Objects.nonNull(identityMapping.getIdentity())
				&& Objects.nonNull(identityMapping.getIdentity().getPreferredLanguage())
				&& Objects.nonNull(identityMapping.getIdentity().getPreferredLanguage().getValue())
				&& Objects.nonNull(identity.get(identityMapping.getIdentity().getPreferredLanguage().getValue())))
			return extractValue(identity.get(identityMapping.getIdentity().getPreferredLanguage().getValue()))
					.orElse(null);
		return null;
	}

	private List<String> getChannel(JsonNode identity) {
		List<String> channelList = new ArrayList<>();
		channelList.add(
				extractValue(identity.get(identityMapping.getIdentity().getPhone().getValue())).isPresent() ? "PHONE"
						: null);
		channelList.add(
				extractValue(identity.get(identityMapping.getIdentity().getEmail().getValue())).isPresent() ? "EMAIL"
						: null);
		channelList.removeIf(Objects::isNull);
		return channelList;
	}

	private List<Exceptions> getExceptions(List<BIR> bioData) {
		if (Objects.nonNull(bioData))
			return bioData.stream()
					.filter(bir -> Objects.nonNull(bir.getOthers()))
					.filter(bir -> bir.getOthers().stream().filter(others -> others.getKey().contentEquals("EXCEPTION"))
							.findAny().isPresent())
					.filter(bir -> bir.getOthers().stream().filter(others -> others.getKey().contentEquals("EXCEPTION"))
							.findAny().get().getValue().contentEquals("true"))
					.map(bir -> Exceptions.builder()
							.type(bir.getBdbInfo().getType().stream().map(type -> type.value())
									.collect(Collectors.joining(" ")))
							.subType(String.join(" ", bir.getBdbInfo().getSubtype())).build())
					.collect(Collectors.toList());
		return null;
	}

	private List<String> getVerified() {
		return verifiedAttributes;
	}

	private List<BiometricInfo> getBiometricInfo(List<BIR> biometrics) {
		if (Objects.nonNull(biometrics))
			return Streams.stream(biometrics)
				.map(bir -> {
						Optional<Entry> payload = Optional.ofNullable(bir.getOthers()).stream()
								.flatMap(birs -> birs.stream())
								.filter(others -> others.getKey().contentEquals("PAYLOAD")).findAny();
						String digitalId = null;
						if (payload.isPresent()) {
							Map<String, String> digitalIdEncoded = mapper.readValue(payload.get().getValue(),
									new TypeReference<Map<String, String>>() {
									});
							digitalId = new String(
									CryptoUtil.decodeBase64(digitalIdEncoded.get("digitalId").split("\\.")[1]));
						}
						return BiometricInfo.builder()
								.type(bir.getBdbInfo().getType().stream().map(type -> type.value())
										.collect(Collectors.joining(" ")))
								.subType(String.join(" ", bir.getBdbInfo().getSubtype()))
								.qualityScore(bir.getBdbInfo().getQuality().getScore())
								.attempts(Objects.nonNull(bir.getOthers()) ? bir.getOthers().stream()
										.filter(others -> others.getKey().contentEquals("RETRIES"))
										.findAny()
										.orElseGet(() -> new Entry()).getValue() : null)
								.digitalId(digitalId).build();
					}).collect(Collectors.toList());
		return null;
	}

	private List<String> getDocuments(JsonNode identity) {
		return identityMapping.getDocuments().getValueList().stream()
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

	public static void setFilterLanguage(String filterLanguage) {
		IdentityIssuanceProfileBuilder.filterLanguage = filterLanguage;
	}

	public IdentityIssuanceProfileBuilder setProcessName(String processName) {
		this.processName = processName;
		return this;
	}

	public IdentityIssuanceProfileBuilder setOldIdentity(byte[] identity) {
		try {
			this.oldIdentity = mapper.readTree(identity);
		} catch (IOException e) {
			// this block should never be executed
			e.printStackTrace();
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
			e.printStackTrace();
		}
		return this;
	}

	public IdentityIssuanceProfileBuilder setNewDocuments(List<DocumentsDTO> newDocuments) {
		this.newDocuments = newDocuments;
		return this;
	}

	public IdentityIssuanceProfileBuilder setVerifiedAttributes(List<String> verifiedAttributes) {
		this.verifiedAttributes = verifiedAttributes;
		return this;
	}

	private Optional<String> extractValue(JsonNode jsonNode) {
		if (Objects.isNull(jsonNode)) {
			return Optional.empty();
		}
		if (jsonNode.isValueNode()) {
			return Optional.of(jsonNode.asText());
		} else if (jsonNode.isArray()) {
			Iterator<JsonNode> iterator = jsonNode.iterator();
			while (iterator.hasNext()) {
				Map<String, String> valueMap = mapper.convertValue(iterator.next(),
						new TypeReference<Map<String, String>>() {
						});
				if (valueMap.get("language").contentEquals(filterLanguage)) {
					return Optional.of(valueMap.get("value"));
				}
			}
		} else if (jsonNode.isObject()) {
			Map<String, String> valueMap = mapper.convertValue(jsonNode, new TypeReference<Map<String, String>>() {
			});
			if (valueMap.get("language").contentEquals(filterLanguage)) {
				return Optional.of(valueMap.get("value"));
			}
		}
		return Optional.empty();
	}

	public static void setDateFormat(String dateFormat) {
		IdentityIssuanceProfileBuilder.dateFormat = dateFormat;
	}
}
