package io.mosip.credential.request.generator.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PartnerManageUtil {
	@Value("${mosip.formatter}")
	private String formatter;

	public String getFormatter(String issuer) {
		// TODO call partnermanagement util by providing issuer(partner id) to get
		// formatters
		return formatter;
	}
}
