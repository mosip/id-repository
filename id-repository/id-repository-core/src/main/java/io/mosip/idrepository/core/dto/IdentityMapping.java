package io.mosip.idrepository.core.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IdentityMapping {
	private Identity identity;
	private MetaInfo metaInfo;
	private Audits audits;
	private Documents documents;

	@Data
	@NoArgsConstructor
	public static class Identity {
		@JsonProperty("IDSchemaVersion")
		private IDSchemaVersion iDSchemaVersion;
		private Name name;
		private Gender gender;
		private LocationHierarchyForProfiling locationHierarchyForProfiling;
		private Dob dob;
		private Age age;
		private PreferredLanguages preferredLanguage;
		private IntroducerRID introducerRID;
		private IntroducerUIN introducerUIN;
		private IntroducerVID introducerVID;
		private IntroducerName introducerName;
		private Phone phone;
		private Email email;
		private Uin uin;
		private IndividualBiometrics individualBiometrics;
		private IntroducerBiometrics introducerBiometrics;
		private IndividualAuthBiometrics individualAuthBiometrics;
		private OfficerBiometricFileName officerBiometricFileName;
		private SupervisorBiometricFileName supervisorBiometricFileName;
		private ResidenceStatus residenceStatus;
		private FullAddress fullAddress;
		private SelectedHandles selectedHandles;
	}

	@Data
	@NoArgsConstructor
	public static class PreferredLanguages {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class LocationHierarchyForProfiling {
		private String value;

		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
	}

	@Data
	@NoArgsConstructor
	public static class MetaInfo {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Audits {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Poa {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Poi {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Por {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Pob {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Poe {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Documents {
		private Poa poa;
		private Poi poi;
		private Por por;
		private Pob pob;
		private Poe poe;

		public List<String> getValueList() {
			return List.of(poa.getValue(), poi.getValue(), por.getValue(), pob.getValue(), poe.getValue());
		}
	}

	@Data
	@NoArgsConstructor
	public static class IDSchemaVersion {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Name {
		private String value;		
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
		
	}

	@Data
	@NoArgsConstructor
	public static class Gender {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Dob {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Age {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerRID {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerUIN {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerVID {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerName {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Phone {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Email {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Uin {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IndividualBiometrics {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerBiometrics {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IndividualAuthBiometrics {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class OfficerBiometricFileName {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class SupervisorBiometricFileName {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ResidenceStatus {
		private String value;
	}
	
	@Data
	@NoArgsConstructor
	public static class FullAddress {
		private String value;
		
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
	}

	@Data
	@NoArgsConstructor
	public static class SelectedHandles {
		private String value;
	}
}
