package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * @author Manoj SP
 *
 */
@Builder(toBuilder = true)
@Data
public class AnonymousProfile {
	private String yearOfBirth;
	private String gender;
	private List<String> location;
	private String preferredLanguage;
	private List<String> channel;
	private List<Exceptions> exceptions;
	private List<String> verified;
	private List<BiometricInfo> biometricInfo;
	private List<String> documents;
}