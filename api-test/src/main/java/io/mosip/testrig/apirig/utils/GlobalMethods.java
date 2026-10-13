package io.mosip.testrig.apirig.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.testng.Reporter;

import io.restassured.response.Response;

/**
 * Local override of apitest-commons {@code GlobalMethods}.
 * <p>
 * Commons hard-codes {@code https://} in the endpoint regexes used by
 * {@link #getUpdatedEndPointURL(String)} and {@link #getComponentDetails()}.
 * Local runs use {@code http://localhost:...}, so "End Points used" in the
 * EmailableReport stayed empty. This copy accepts {@code http} and {@code https}.
 * Remove once apitest-commons ships {@code https?://} support.
 */
public class GlobalMethods {
	private static final Logger logger = Logger.getLogger(GlobalMethods.class);
	public static Map<Object, Object> serverFailuresMapS = Collections.synchronizedMap(new HashMap<>());
	public static Map<Object, Object> captchaStatusMap = Collections.synchronizedMap(new HashMap<>());
	public static Set<String> serverEndpoints = new HashSet<>();
	private static String module_name = "(mimoto|certify|signup|partnermanager|preregistration|resident|residentmobileapp|masterdata|esignet|idgenerator|policymanager|idauthentication|idrepository|auditmanager|authmanager|keymanager|mock-identity-system|credentialservice|credentialrequest)";
	// https?:// so local http://localhost endpoints appear in the report
	private static final String URL_SCHEME = "https?://";
	private static String regex_1 = URL_SCHEME + "([^/]+)/(v[0-9]+)?/" + module_name + "/([^,]+)";
	private static String regex_2 = URL_SCHEME + "([^/]+)/" + module_name + "/(v[0-9]+)/([^,]+)";
	private static Pattern pattern_1 = Pattern.compile(regex_1);
	private static Pattern pattern_2 = Pattern.compile(regex_2);
	public static String runContext = null;

	public static boolean isXSSProtectionCheckEnabled() {
		return ConfigManager.getproperty("xssProtectionCheck").equalsIgnoreCase("yes");
	}

	public static void checkXSSProtectionHeader(Response response, String url) throws SecurityXSSException {
		String xssHeader = response.getHeader("X-Xss-Protection");
		if (isXSSProtectionCheckEnabled() && (xssHeader == null || !xssHeader.equalsIgnoreCase("1; mode=block"))) {
			reportResponseHeader(response.getHeaders().asList().toString(), url);
			throw new SecurityXSSException("Response Header does not have X-XSS-Protection");
		}
	}

	public static void setModuleNameAndReCompilePattern(String value) {
		if (value == null || value.trim().isEmpty()) {
			return;
		}
		module_name = value;
		regex_1 = URL_SCHEME + "([^/]+)/(v[0-9]+)?/" + module_name + "/([^,]+)";
		regex_2 = URL_SCHEME + "([^/]+)/" + module_name + "/(v[0-9]+)/([^,]+)";
		pattern_1 = Pattern.compile(regex_1);
		pattern_2 = Pattern.compile(regex_2);
	}

	public static String getRunContext() {
		runContext = UUID.randomUUID().toString().replaceAll("-", "").toLowerCase().substring(0, 3) + "_";
		logger.info("RUN_CONTEXT set to: " + runContext);
		return runContext;
	}

	public static void main(String[] arg) {
	}

	public static String getUpdatedEndPointURL(String url) {
		Matcher matcher = pattern_1.matcher(url);
		if (matcher.find()) {
			String domain = matcher.group(1);
			String module = matcher.group(3);
			String newBaseURL = ConfigManager.getComponentBaseURL(module);
			if (newBaseURL != null && !newBaseURL.isEmpty()) {
				return url.replace(domain, newBaseURL);
			}
			return url;
		}
		Matcher matcher2 = pattern_2.matcher(url);
		if (matcher2.find()) {
			String domain = matcher2.group(1);
			String module = matcher2.group(2) != null ? matcher2.group(2) : "";
			String newBaseURL = ConfigManager.getComponentBaseURL(module);
			if (newBaseURL != null && !newBaseURL.isEmpty()) {
				return url.replace(domain, newBaseURL);
			}
			return url;
		}
		logger.debug("Needs RegEx revisit...url is:" + url);
		return url;
	}

	public static String addToServerEndPointMap(String url) {
		String updatedURL = getUpdatedEndPointURL(url);
		serverEndpoints.add(updatedURL);
		return updatedURL;
	}

	public static String removeNumerics(String url) {
		String modifiedString = url;
		modifiedString = Pattern.compile("/\\d+/").matcher(modifiedString).replaceAll("/");
		modifiedString = Pattern.compile("/\\d+$").matcher(modifiedString).replaceAll("/");
		modifiedString = Pattern.compile("/mosip_[a-zA-Z0-9_]+/").matcher(modifiedString).replaceAll("/");
		return modifiedString;
	}

	public static String getComponentDetails() {
		Pattern localPattern1 = Pattern.compile(URL_SCHEME + "([^/]+)/(v[0-9]+)?/" + module_name + "/([^,]+)");
		Pattern localPattern2 = Pattern.compile(URL_SCHEME + "([^/]+)/" + module_name + "/(v[0-9]+)/([^,]+)");
		Set<String> uniqueResults = new HashSet<>();
		for (String url : serverEndpoints) {
			Matcher matcher1 = localPattern1.matcher(url);
			if (matcher1.find()) {
				String domain = matcher1.group(1);
				String version = matcher1.group(2) != null ? matcher1.group(2) : "";
				String module = matcher1.group(3);
				String endpoint = version + "/" + module + "/" + matcher1.group(4);
				uniqueResults.add("Domain: " + domain + " ---- Module: " + module + " ---- End Point: "
						+ removeNumerics(endpoint));
				continue;
			}
			Matcher matcher2 = localPattern2.matcher(url);
			if (!matcher2.find()) {
				continue;
			}
			String domain = matcher2.group(1);
			String module = matcher2.group(2) != null ? matcher2.group(2) : "";
			String version = matcher2.group(3);
			String endpoint = module + "/" + version + "/" + matcher2.group(4);
			uniqueResults.add("Domain: " + domain + " ---- Module: " + module + " ---- End Point: "
					+ removeNumerics(endpoint));
		}
		List<String> uniqueList = new ArrayList<>(uniqueResults);
		StringBuilder stringBuilder = new StringBuilder();
		for (String result : uniqueList) {
			stringBuilder.append("\n").append(result);
		}
		return stringBuilder.toString();
	}

	public static String getTestCaseVariableMapping() {
		VariableDependencyMapper mapper = new VariableDependencyMapper(AdminTestUtil.generators, AdminTestUtil.consumers);
		StringBuilder variableMappingBuilder = new StringBuilder();
		if (!mapper.getConsumerToGeneratorsMap().isEmpty()) {
			variableMappingBuilder.append("Consumer to Generators Mapping:\n");
			mapper.getConsumerToGeneratorsMap()
					.forEach((k, v) -> variableMappingBuilder.append(k + " → " + String.valueOf(v)).append("\n"));
		}
		String impactSummary = mapper.getImpactSummary();
		if (impactSummary != null && !impactSummary.trim().isEmpty()) {
			variableMappingBuilder.append("\nImpact Summary:\n");
			variableMappingBuilder.append(impactSummary);
		}
		String impactByGenerator = mapper.getImpactSummaryBasedOnGenerator();
		if (impactByGenerator != null && !impactByGenerator.trim().isEmpty()) {
			variableMappingBuilder.append("\nImpact Summary Based On Generator:\n");
			variableMappingBuilder.append(impactByGenerator);
		}
		String impactByConsumer = mapper.getImpactSummaryBasedOnConsumer();
		if (impactByConsumer != null && !impactByConsumer.trim().isEmpty()) {
			variableMappingBuilder.append("\nImpact Summary Based On Consumer:\n");
			variableMappingBuilder.append(impactByConsumer);
		}
		return variableMappingBuilder.toString();
	}

	public static void reportServerError(Object code, Object errorMessage) {
		serverFailuresMapS.put(code, errorMessage);
	}

	public static String getServerErrors() {
		if (serverFailuresMapS.isEmpty()) {
			return "No server errors";
		}
		return serverFailuresMapS.toString();
	}

	public static void reportCaptchaStatus(Object code, Object captchaMessage) {
		captchaStatusMap.put(code, captchaMessage);
	}

	public static boolean getCaptchaStatus() {
		Object value = captchaStatusMap.get("Captcha_enabled");
		return Boolean.parseBoolean(String.valueOf(value));
	}

	public static String maskOutSensitiveInfo(String strInput) {
		if (strInput == null || strInput.isBlank()) {
			return strInput;
		}
		String maskedInput = strInput;
		if (!ConfigManager.IsDebugEnabled().booleanValue()) {
			String[] sensitiveKeys = new String[] { "password", "secret", "token", "key", "private", "client_secret",
					"authclientsecret" };
			for (String key : sensitiveKeys) {
				String regex = "(?i)(\"[^\"]*" + key + "[^\"]*\"\\s*:\\s*\")(.*?)(\")";
				maskedInput = maskedInput.replaceAll(regex, "$1***** MASKED *****$3");
			}
			Pattern individualBiometricsPattern = Pattern
					.compile("\"category\"\\s*:\\s*\"individualBiometrics\"\\s*,\\s*\"value\"\\s*:\\s*\"(.*?)\"");
			maskedInput = individualBiometricsPattern.matcher(maskedInput)
					.replaceAll("\"category\": \"individualBiometrics\", \"value\": \"***** MASKED *****\"");
		}
		maskedInput = maskedInput.replaceAll("\"value\"\\s*:\\s*\"([^\"]{200,})\"", "\"value\": \"***** MASKED *****\"");
		maskedInput = maskedInput.replaceAll("\"data\"\\s*:\\s*\"([^\"]{200,})\"", "\"data\": \"***** MASKED *****\"");
		return maskedInput;
	}

	public static void ReportRequestAndResponse(String reqHeader, String resHeader, String url, String requestBody,
			String response, boolean formatResponse) {
		reportRequest(reqHeader, requestBody, url);
		reportResponse(resHeader, url, response, formatResponse);
	}

	public static void ReportRequestAndResponse(String reqHeader, String resHeader, String url, String requestBody,
			String response) {
		reportRequest(reqHeader, requestBody, url);
		reportResponse(resHeader, url, response);
	}

	public static void reportRequest(String requestHeader, String request) {
		reportRequest(requestHeader, request, "");
	}

	public static void reportRequest(String requestHeader, String request, String url) {
		if (url != null && !url.isBlank()) {
			url = addToServerEndPointMap(url);
		}
		String formattedHeader = ReportUtil.getTextAreaForHeaders(requestHeader);
		if (request != null && !request.equals("{}")) {
			Reporter.log("<b><u>Request: </u></b>(End Point URL: " + url + ") <pre>" + formattedHeader
					+ ReportUtil.getTextAreaJsonMsgHtml(maskOutSensitiveInfo(request)) + "</pre>");
		} else {
			Reporter.log("<b><u>Request: </u></b>(End Point URL: " + url + ") <pre>" + formattedHeader
					+ ReportUtil.getTextAreaJsonMsgHtml("No request body") + "</pre>");
		}
	}

	public static void reportResponse(String responseHeader, String url, Response response) {
		String formattedHeader = ReportUtil.getTextAreaForHeaders(responseHeader);
		String contentType = response.getHeader("Content-Type");
		if (contentType != null && contentType.contains("application/pdf")) {
			Reporter.log("<b><u>Response: </u></b><pre>" + formattedHeader + "</pre>");
		} else {
			Reporter.log("<b><u>Response: </u></b><pre>" + formattedHeader
					+ ReportUtil.getTextAreaJsonMsgHtml(maskOutSensitiveInfo(response.asString())) + "</pre>");
		}
	}

	public static void reportResponseHeader(String responseHeader, String url) {
		String formattedHeader = ReportUtil.getTextAreaForHeaders(responseHeader);
		Reporter.log("<b><u>Response: </u></b><pre>" + formattedHeader + "</pre>");
	}

	public static void reportResponse(String responseHeader, String url, String response) {
		reportResponse(responseHeader, url, response, false);
	}

	public static void reportResponse(String responseHeader, String url, String response, boolean formatResponse) {
		String formattedHeader = ReportUtil.getTextAreaForHeaders(responseHeader);
		String maskedResponse = maskOutSensitiveInfo(response);
		if (formatResponse) {
			Reporter.log("<b><u>Response: </u></b><pre>" + formattedHeader + ReportUtil.getTextAreaJsonMsgHtml(maskedResponse)
					+ "</pre>");
		} else {
			Reporter.log("<b><u>Response: </u></b><pre>" + responseHeader + maskedResponse + "</pre>");
		}
	}

	public static String sha256(String input) {
		String returnString = "";
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes());
			StringBuilder hexStringBuilder = new StringBuilder(2 * hashBytes.length);
			for (byte hashByte : hashBytes) {
				hexStringBuilder.append(String.format("%02x", hashByte));
			}
			returnString = hexStringBuilder.toString();
		} catch (NoSuchAlgorithmException e) {
			logger.error("Failed while hashing SHA256 for VCI code challenge " + e.getMessage());
		}
		return returnString;
	}
}
