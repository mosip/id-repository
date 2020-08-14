package io.mosip.credentialstore.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import io.mosip.credentialstore.constants.CredentialFormatter;
import io.mosip.credentialstore.constants.CredentialType;

public class CredentialFormatterMapperUtil {
	private static EnumMap<CredentialType, CredentialFormatter> formatterMap = new EnumMap<>(CredentialType.class);

	/** The unmodifiable map. */
	private static Map<CredentialType, CredentialFormatter> unmodifiableMap = Collections
			.unmodifiableMap(formatterMap);


	public CredentialFormatterMapperUtil() {
		super();
	}

	/**
	 * Status mapper.
	 *
	 * @return the map
	 */
	private static Map<CredentialType, CredentialFormatter> formatterMapper() {
		formatterMap.put(CredentialType.AUTH, CredentialFormatter.idAuthProvider);
		formatterMap.put(CredentialType.MOSIP, CredentialFormatter.credentialDefaultProvider);
		formatterMap.put(CredentialType.PIN, CredentialFormatter.pinBasedProvider);

		return unmodifiableMap;
	}

	public String getCredentialFormatterCode(CredentialType credentialType) {
		Map<CredentialType, CredentialFormatter> mapStatus = CredentialFormatterMapperUtil
				.formatterMapper();

		return mapStatus.get(CredentialType.valueOf(credentialType.toString())).toString();
	}
}
