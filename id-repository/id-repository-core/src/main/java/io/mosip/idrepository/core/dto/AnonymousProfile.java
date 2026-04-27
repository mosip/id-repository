package io.mosip.idrepository.core.dto;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class AnonymousProfile {
	private String yearOfBirth;
	private String gender;
	private List<String> location;
	private String preferredLanguage;
	private List<String> channel;
	private List<Exceptions> exceptions;
	private JsonNode verified;
	private List<BiometricInfo> biometricInfo;
	private List<String> documents;
}
