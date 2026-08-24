package io.mosip.testrig.apirig.idrepo.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalMethods;

/**
 * Formats {@link GlobalMethods#serverEndpoints} for the emailable report.
 * apitest-commons {@code GlobalMethods.getComponentDetails()} only matches {@code https://}
 * URLs, so local compose runs ({@code http://localhost:8082/...}) render an empty
 * "End Points used" cell. This formatter accepts both http and https.
 */
final class HttpAwareEndpointFormatter {

	static final String DEFAULT_MODULE_NAME = "(mimoto|certify|signup|partnermanager|preregistration|resident|"
			+ "residentmobileapp|masterdata|esignet|idgenerator|policymanager|idauthentication|idrepository|"
			+ "auditmanager|authmanager|keymanager|mock-identity-system|datashare|credentialservice|credentialrequest)";

	private HttpAwareEndpointFormatter() {
	}

	static String format(Set<String> serverEndpoints) {
		if (serverEndpoints == null || serverEndpoints.isEmpty()) {
			return "";
		}

		String moduleName = ConfigManager.getproperty("moduleNamePattern");
		if (moduleName == null || moduleName.isBlank()) {
			moduleName = DEFAULT_MODULE_NAME;
		}

		Pattern pattern1 = Pattern.compile("https?://([^/]+)/(v[0-9]+)?/" + moduleName + "/([^,]+)");
		Pattern pattern2 = Pattern.compile("https?://([^/]+)/" + moduleName + "/(v[0-9]+)/([^,]+)");

		Set<String> uniqueResults = new HashSet<>();
		for (String url : serverEndpoints) {
			Matcher matcher1 = pattern1.matcher(url);
			if (matcher1.find()) {
				String domain = matcher1.group(1);
				String version = matcher1.group(2) != null ? matcher1.group(2) : "";
				String module = matcher1.group(3);
				String endpoint = version + "/" + module + "/" + matcher1.group(4);
				uniqueResults.add("Domain: " + domain + " ---- Module: " + module + " ---- End Point: "
						+ GlobalMethods.removeNumerics(endpoint));
				continue;
			}

			Matcher matcher2 = pattern2.matcher(url);
			if (matcher2.find()) {
				String domain = matcher2.group(1);
				String module = matcher2.group(2) != null ? matcher2.group(2) : "";
				String version = matcher2.group(3);
				String endpoint = module + "/" + version + "/" + matcher2.group(4);
				uniqueResults.add("Domain: " + domain + " ---- Module: " + module + " ---- End Point: "
						+ GlobalMethods.removeNumerics(endpoint));
				continue;
			}

			uniqueResults.add("End Point: " + GlobalMethods.removeNumerics(url));
		}

		List<String> uniqueList = new ArrayList<>(uniqueResults);
		StringBuilder stringBuilder = new StringBuilder();
		for (String result : uniqueList) {
			stringBuilder.append("\n").append(result);
		}
		return stringBuilder.toString();
	}
}
