package io.mosip.testrig.apirig.report;

//import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.collections.Lists;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;

import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.DependencyResolver;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.S3Adapter;
import io.mosip.testrig.apirig.utils.SlackChannelIntegration;

/**
 * Reporter that generates a single-page HTML report of the test results.
 */
public class EmailableReport implements IReporter {
	private static final Logger LOG = Logger.getLogger(EmailableReport.class);

	protected PrintWriter writer;

	protected final List<SuiteResult> suiteResults = Lists.newArrayList();

	protected final boolean reportIgnoredTestCases = ConfigManager.reportIgnoredTestCases();
	protected final boolean reportKnownIssueTestCases = ConfigManager.reportKnownIssueTestCases();

	// Reusable buffer
	private final StringBuilder buffer = new StringBuilder();

	private String fileName = "emailable-report.html";
	private String mountPathForReport = ConfigManager.getMountPathForReport();

	private static final String JVM_ARG = GlobalConstants.EMAILABLEREPORT2NAME;

	private int totalPassedTests = 0;
	private static int totalSkippedTests = 0;
	private int totalIgnoredTests = 0;
	private int totalKnownIssueTests = 0;
	private static int totalFailedTests = 0;
	private long totalDuration = 0;

	private int suiteCount = 0;
	private int prerequisiteFailedCount = 0;
	private int mainSuiteFailedCount = 0;

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public static int getFailedCount() {
		return totalFailedTests;
	}

	public static int getSkippedCount() {
		return totalSkippedTests;
	}



	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
	    // Sort suites in descending order by suite name
	    Collections.sort(suites, new Comparator<ISuite>() {
	        @Override
	        public int compare(ISuite suite1, ISuite suite2) {
	            return suite2.getName().compareTo(suite1.getName()); // Reverse the order for descending sort
	        }
	    });
		generateReport(xmlSuites, suites, outputDirectory, false);
	}

	private SuiteResult createFilteredSuiteResult(ISuite suite) {
	    ISuite filteredSuite = suite;
	    // Filter passed test results from suite
	    suite.getResults().forEach((testName, suiteResult) -> {
	        ITestContext testContext = suiteResult.getTestContext();
	        // Remove passed tests
	        testContext.getPassedTests().getAllResults().clear();
	    });
	    return new SuiteResult(filteredSuite);
	}

	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory, boolean skipPassed) {
		try {
			writer = createWriter(outputDirectory);
		} catch (IOException e) {
			LOG.error("Unable to create output file", e);
			return;
		}
		suiteResults.clear();
		for (ISuite suite : suites) {
			if (skipPassed) {
				suiteResults.add(createFilteredSuiteResult(suite));
			} else {
				suiteResults.add(new SuiteResult(suite));
			}
		}

		writeDocumentStart();
		writeHead();
		writeBody(skipPassed, suites.size());
		writeDocumentEnd();
		writer.close();

		generateOutputFile(skipPassed);

	}

	public void generateOutputFile(boolean skipPassed) {

		int totalTestCases = 0;

		totalTestCases = totalPassedTests + totalSkippedTests + totalFailedTests;

		if (reportIgnoredTestCases) {
			totalTestCases = totalTestCases + totalIgnoredTests;
		}
		if (reportKnownIssueTestCases) {
			totalTestCases = totalTestCases + totalKnownIssueTests;
		}

		String oldString = System.getProperty(GlobalConstants.EMAILABLEREPORT2NAME);
        // Remove unwanted random suffix (e.g., -5e9, -2b9, -abc123) before the module name
        oldString = oldString.replaceAll("-[a-z0-9]{3,}(?=_)", "");
		String temp = "";
		String reportContext = skipPassed == true ? "error-" : "full-";

		temp = "-" + reportContext + "report_T-" + totalTestCases + "_P-" + totalPassedTests + "_S-" + totalSkippedTests + "_F-"
				+ totalFailedTests;

		if (reportIgnoredTestCases && reportKnownIssueTestCases) {
			temp = "-" + reportContext + "report_T-" + totalTestCases + "_P-" + totalPassedTests + "_S-" + totalSkippedTests + "_F-"
					+ totalFailedTests + "_I-" + totalIgnoredTests + "_KI-" + totalKnownIssueTests;
		} else if (reportIgnoredTestCases && !(reportKnownIssueTestCases)) {
			temp = "-" + reportContext + "report_T-" + totalTestCases + "_P-" + totalPassedTests + "_S-" + totalSkippedTests + "_F-"
					+ totalFailedTests + "_I-" + totalIgnoredTests;
		} else if (reportKnownIssueTestCases && !(reportIgnoredTestCases)) {
			temp = "-" + reportContext + "report_T-" + totalTestCases + "_P-" + totalPassedTests + "_S-" + totalSkippedTests + "_F-"
					+ totalFailedTests + "_KI-" + totalKnownIssueTests;
		}

		if (skipPassed == true) {
			temp = temp.replaceAll("P-\\d+_", ""); // Removes passed count (P-*_) from the report title.
		}

		String newString = oldString.replace("-report", temp);

		StringBuilder slackNotification = new StringBuilder();
		slackNotification
		    .append("Completed apitestrig run --- ")
		    .append(BaseTestCase.currentModule).append(" --- ")
		    .append(BaseTestCase.environment.replace("api-internal.", ""))
		    .append(" env --- ").append(BaseTestCase.testLevel)
		    .append(" --- ").append(temp.replace("-full-report_", ""));

		File orignialReportFile = new File(
				System.getProperty("user.dir") + "/" + System.getProperty("testng.outpur.dir") + "/"
						+ System.getProperty(GlobalConstants.EMAILABLEREPORT2NAME));
		LOG.info("reportFile is::" + System.getProperty("user.dir") + "/" + System.getProperty("testng.outpur.dir")
				+ "/" + System.getProperty(GlobalConstants.EMAILABLEREPORT2NAME));

		File newReportFile = new File(
				System.getProperty("user.dir") + "/" + System.getProperty("testng.outpur.dir") + "/" + newString);
		LOG.info("New reportFile is::" + System.getProperty("user.dir") + "/" + System.getProperty("testng.outpur.dir")
				+ "/" + newString);

		if (orignialReportFile.exists()) {
			ReportTreeViewEnhancer.enhanceReportFileInPlace(orignialReportFile, LOG);
			if (orignialReportFile.renameTo(newReportFile)) {
				LOG.info("Report File re-named successfully!");
				if (ConfigManager.getPushReportsToS3().equalsIgnoreCase("yes")) {
					S3Adapter s3Adapter = new S3Adapter();
					boolean isStoreSuccess = false;
					try {
						isStoreSuccess = s3Adapter.putObject(ConfigManager.getS3Account(),
								System.getProperty("modules"), null, null, newString, newReportFile);
						LOG.info("isStoreSuccess:: " + isStoreSuccess);
					} catch (Exception e) {
						LOG.info("error occured while pushing the object" + e.getLocalizedMessage());
						LOG.error(e.getMessage());
					}
					if (isStoreSuccess) {
						LOG.info("Pushed file to S3");
						String reportLink = "https://minio."
						        + BaseTestCase.environment.replace("api-internal.", "")
						        + ".mosip.net/browser/"
						        + ConfigManager.getS3Account() + "/"
						        + BaseTestCase.currentModule + "%2F" + newString;
						slackNotification.append(" --- ").append(reportLink);
					} else {
						LOG.info("Failed while pushing file to S3");
					}
				}
			} else {
				LOG.error("Renamed report file doesn't exist");
			}
		} else {
			LOG.error("Original report File does not exist!");
		}

		try {
			SlackChannelIntegration.sendMessageToSlack(slackNotification.toString()); // NOSONAR
		} catch (Exception e) {
			LOG.error("Failed to send Slack notification: " + e.getMessage());
		}
	}

	private String getGitProperty(String key) {
		Properties properties = new Properties();
		try (InputStream is = EmailableReport.class.getClassLoader().getResourceAsStream("git.properties")) {
			if (is != null) {
				properties.load(is);
				String value = properties.getProperty(key);
				return value == null ? "" : value;
			}
		} catch (IOException e) {
			LOG.error("Error reading git.properties: " + e.getMessage());
		}
		return "";
	}

	private void writeExecutionDetails(boolean skipPassed, int suiteSize) {
		String commitId = getGitProperty("git.commit.id.abbrev");
		String branch = getGitProperty("git.branch");
		String repoWebUrl = gitHubWebUrl(getGitProperty("git.remote.origin.url"));
		String environmentUrl = environmentUrl();

		String host = "Unknown";
		try {
			host = java.net.InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			LOG.info("Unable to resolve host name: " + e.getMessage());
		}

		String dockerImage = System.getenv("DOCKER_IMAGE");
		if (dockerImage == null || dockerImage.isEmpty()) {
			dockerImage = "Local Execution";
		}

		writer.print("<table class='report-exec-table' style='width:100%; table-layout:fixed;'>");
		writer.print("<tr class='exec-title-row'><th colspan='7'>");
		writer.print("<div class=\"report-hero\">");
		writer.print("<h2 class=\"report-hero__title main-title\">API Test Report</h2>");
		writer.print("<p class=\"report-hero__meta\"></p>");
		writer.print("</div></th></tr>");

		writer.print("<tr><td colspan='7' class=\"exec-section-title section-title report-section-bar\">");
		writer.print("Execution Details");
		writer.print("</td></tr>");

		String[] runStarted = runStartedParts();
		writeExecRow("&#128197; Run Date", runStarted[0], false);
		writeExecRow("&#9200; Run Time", runStarted[1], false);
		if (environmentUrl.isEmpty()) {
			writeExecRow("&#127760; Environment", "NA", false);
		} else {
			writeExecRow("&#127760; Environment", execLink(environmentUrl, environmentUrl), false);
		}
		writeExecRow("&#128278; Commit Id", gitLink(repoWebUrl, "/commit/", commitId), false);
		writeExecRow("&#127807; Branch", gitLink(repoWebUrl, "/tree/", branch), false);
		writeExecRow("&#9749; Java", Utils.escapeHtml(System.getProperty("java.version", "")), false);
		writeExecRow("&#128187; OS", Utils.escapeHtml(System.getProperty("os.name", "")), false);
		writeExecRow("&#128421; Host", Utils.escapeHtml(host), false);
		writeExecRow("&#128051; Docker Image", Utils.escapeHtml(dockerImage), false);
		boolean captchaEnabled = GlobalMethods.getCaptchaStatus();
		String captchaValue = captchaEnabled
				? "<span class=\"exec-flag-alert\">true</span>"
				: "false";
		writeExecRow("&#128274; Captcha Enabled", captchaValue, true);
		writeResultSummary(skipPassed, suiteSize);
		writer.print("</table>");
	}

	private String[] runStartedParts() {
		Date earliest = null;
		for (SuiteResult suiteResult : suiteResults) {
			for (TestResult testResult : suiteResult.getTestResults()) {
				Date start = testResult.getStartDate();
				if (start == null) {
					continue;
				}
				if (earliest == null || start.before(earliest)) {
					earliest = start;
				}
			}
		}
		if (earliest == null) {
			return new String[] { "NA", "NA" };
		}
		var started = earliest.toInstant().atZone(ZoneId.systemDefault());
		return new String[] {
				Utils.escapeHtml(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(started)),
				Utils.escapeHtml(DateTimeFormatter.ofPattern("HH:mm:ss").format(started)) };
	}

	private void writeExecRow(String label, String valueHtml, boolean footer) {
		writer.print(footer ? "<tr class=\"exec-kv-row exec-kv-row--footer\">" : "<tr class=\"exec-kv-row\">");
		writer.print("<td colspan='3'><b>" + label + "</b></td>");
		writer.print("<td colspan='4'>" + valueHtml + "</td>");
		writer.print("</tr>");
	}

	private void writeResultSummary(boolean skipPassed, int suiteSize) {
		int passed = 0;
		int failed = 0;
		int skipped = 0;
		int ignored = 0;
		int known = 0;
		long duration = 0;
		int localSuiteCount = 0;
		int prerequisiteFailed = 0;
		int mainFailed = 0;

		for (SuiteResult suiteResult : suiteResults) {
			if (suiteResult.getSuiteName().contains("Master Suite")) {
				continue;
			}
			localSuiteCount++;
			if (suiteSize > 2 && skipPassed && ((localSuiteCount == 3 && prerequisiteFailed == 0)
					|| (localSuiteCount == 4 && mainFailed == 0))) {
				continue;
			}
			if (suiteSize == 2 && skipPassed && localSuiteCount == 2 && prerequisiteFailed == 0) {
				continue;
			}

			int suiteFailed = 0;
			for (TestResult testResult : suiteResult.getTestResults()) {
				int passedTests = testResult.getPassedTestCount();
				int ignoredTests = reportIgnoredTestCases ? testResult.getIgnoredTestCount() : 0;
				int knownTests = reportKnownIssueTestCases ? testResult.getKnownIssueTestCount() : 0;
				int skippedTests = testResult.getSkippedTestCount();
				int failedTests = testResult.getFailedTestCount();
				if (passedTests + skippedTests + failedTests + ignoredTests + knownTests < 1) {
					continue;
				}
				passed += passedTests;
				failed += failedTests;
				skipped += skippedTests;
				ignored += ignoredTests;
				known += knownTests;
				duration += testResult.getDuration();
				suiteFailed += failedTests;
			}
			if (!skipPassed && localSuiteCount == 1 && suiteFailed > 0) {
				prerequisiteFailed = suiteFailed;
			}
			if (suiteSize > 2 && !skipPassed && localSuiteCount == 2 && suiteFailed > 0) {
				mainFailed = suiteFailed;
			}
		}

		int total = passed + failed + skipped + ignored + known;
		writer.print("<tr><td colspan='7' class=\"exec-separator\"></td></tr>");
		writer.print("<tr id=\"report-exec-summary\">");
		writer.print("<td colspan='7' class=\"exec-section-title section-title\">Summary of Test Results</td>");
		writer.print("</tr>");
		writer.print("<tr><td colspan='7' class=\"exec-legend\"><div class=\"exec-legend-inner\">");
		writer.print("<p class=\"exec-legend-title\"><b>Result Legend:</b></p><ul class=\"exec-legend-list\">");
		writeLegendItem("&#9989;", "Passed",
				"API test executed and the response matched the expected result.");
		writeLegendItem("&#10060;", "Failed",
				"API test executed but the response failed validation or an exception was thrown.");
		writeLegendItem("&#9888;", "Known Issue",
				"API test skipped because it is tagged as a known issue.");
		writeLegendItem("&#9193;", "Skipped",
				"API test skipped because a prerequisite failed or the test could not be executed.");
		writeLegendItem("&#128683;", "Ignored",
				"API test skipped because the feature is not supported or the service is not deployed.");
		writer.print("</ul></div></td></tr>");
		writer.print("<tr class=\"exec-stats-header-row\">");
		writer.print("<th style='text-align:center;'>&#128202; Total</th>");
		writer.print("<th style='text-align:center;'>&#9989; Passed</th>");
		writer.print("<th style='text-align:center;'>&#128683; Ignored</th>");
		writer.print("<th style='text-align:center;'>&#9888; Known Issues</th>");
		writer.print("<th style='text-align:center;'>&#9193; Skipped</th>");
		writer.print("<th style='text-align:center;'>&#10060; Failed</th>");
		writer.print("<th style='text-align:center;'>&#9201; Time (HH:MM:SS)</th>");
		writer.print("</tr>");
		writer.print("<tr class=\"exec-stats-data-row\">");
		writeTableData(Integer.toString(total), "num num-center");
		writeTableData(countWithPercent(passed, total), "num green-bg num-center");
		writeTableData(countWithPercent(ignored, total), "num orange-bg num-center");
		writeTableData(countWithPercent(known, total), "num yellow-bg num-center");
		writeTableData(countWithPercent(skipped, total), "num blue-bg num-center");
		writeTableData(countWithPercent(failed, total), "num attn num-center");
		writeTableData(convertMillisToTime(duration), "num num-center");
		writer.print("</tr>");
	}

	private void writeLegendItem(String icon, String label, String description) {
		writer.print("<li class=\"exec-legend-item\"><span class=\"exec-legend-ico\">");
		writer.print(icon);
		writer.print("</span><span class=\"exec-legend-txt\"><b>");
		writer.print(label);
		writer.print("</b> – ");
		writer.print(description);
		writer.print("</span></li>");
	}

	private String countWithPercent(int count, int total) {
		double percent = total > 0 ? (count * 100.0) / total : 0;
		return count + " (" + String.format("%.0f", percent) + "%)";
	}

	private void writeRuntimeDetails() {
		String componentsRaw = AdminTestUtil.getServerComponentsDetails();
		String endpointsRaw = GlobalMethods.getComponentDetails();
		String serverErrors = GlobalMethods.getServerErrors();
		boolean hasServerErrors = serverErrors != null && !serverErrors.isBlank()
				&& !"No server errors".equals(serverErrors.trim());

		List<String> componentLines = runtimeNonEmptyLines(componentsRaw);
		List<String> endpointLines = runtimeNonEmptyLines(endpointsRaw);
		int componentCount = componentLines.size();
		int endpointCount = endpointLines.size();
		int errorCount = hasServerErrors ? Math.max(1, runtimeNonEmptyLines(serverErrors).size()) : 0;
		String defaultTab = hasServerErrors ? "errors" : "components";

		writer.print("<section class=\"runtime-panel\" id=\"report-runtime-panel\" data-runtime-panel=\"1\">");
		writer.print("<header class=\"runtime-panel__head\"><h3 class=\"runtime-panel__title\">Runtime details</h3></header>");
		writer.print("<div class=\"runtime-panel__tabs\" role=\"tablist\" aria-label=\"Runtime details\">");
		writeRuntimeTab("components", "Components", componentCount, false, "components".equals(defaultTab));
		writeRuntimeTab("endpoints", "Endpoints", endpointCount, false, "endpoints".equals(defaultTab));
		writeRuntimeTab("errors", "Errors", errorCount, hasServerErrors, "errors".equals(defaultTab));
		writer.print("</div>");

		writer.print("<div class=\"runtime-panel__panels\">");
		writer.print("<div class=\"runtime-panel__pane");
		if ("components".equals(defaultTab)) {
			writer.print(" is-active");
		}
		writer.print("\" role=\"tabpanel\" data-runtime-pane=\"components\"");
		if (!"components".equals(defaultTab)) {
			writer.print(" hidden");
		}
		writer.print(">");
		writeComponentCards(componentLines);
		writer.print("</div>");

		writer.print("<div class=\"runtime-panel__pane");
		if ("endpoints".equals(defaultTab)) {
			writer.print(" is-active");
		}
		writer.print("\" role=\"tabpanel\" data-runtime-pane=\"endpoints\"");
		if (!"endpoints".equals(defaultTab)) {
			writer.print(" hidden");
		}
		writer.print(">");
		writeEndpointList(endpointLines);
		writer.print("</div>");

		writer.print("<div class=\"runtime-panel__pane");
		if ("errors".equals(defaultTab)) {
			writer.print(" is-active");
		}
		writer.print("\" role=\"tabpanel\" data-runtime-pane=\"errors\"");
		if (!"errors".equals(defaultTab)) {
			writer.print(" hidden");
		}
		writer.print(">");
		writeErrorPane(serverErrors, hasServerErrors);
		writer.print("</div>");
		writer.print("</div></section>");
		writer.print("<script>(function(){var root=document.getElementById('report-runtime-panel');");
		writer.print("if(!root||root.getAttribute('data-tabs-ready'))return;root.setAttribute('data-tabs-ready','1');");
		writer.print("var tabs=root.querySelectorAll('[data-runtime-tab]');");
		writer.print("var panes=root.querySelectorAll('[data-runtime-pane]');");
		writer.print("function show(id){tabs.forEach(function(t){var on=t.getAttribute('data-runtime-tab')===id;");
		writer.print("t.classList.toggle('is-active',on);t.setAttribute('aria-selected',on?'true':'false');});");
		writer.print("panes.forEach(function(p){var on=p.getAttribute('data-runtime-pane')===id;");
		writer.print("p.classList.toggle('is-active',on);p.hidden=!on;});}");
		writer.print("root.addEventListener('click',function(e){var t=e.target.closest&&e.target.closest('[data-runtime-tab]');");
		writer.print("if(!t||!root.contains(t))return;e.preventDefault();show(t.getAttribute('data-runtime-tab'));});");
		writer.print("})();</script>");
	}

	private void writeRuntimeTab(String id, String label, int count, boolean alert, boolean active) {
		writer.print("<button type=\"button\" class=\"runtime-panel__tab");
		if (active) {
			writer.print(" is-active");
		}
		if (alert) {
			writer.print(" runtime-panel__tab--alert");
		}
		writer.print("\" role=\"tab\" data-runtime-tab=\"");
		writer.print(id);
		writer.print("\" aria-selected=\"");
		writer.print(active ? "true" : "false");
		writer.print("\"><span class=\"runtime-panel__tab-label\">");
		writer.print(Utils.escapeHtml(label));
		writer.print("</span><span class=\"runtime-panel__tab-count\">");
		writer.print(count);
		writer.print("</span></button>");
	}

	private void writeComponentCards(List<String> lines) {
		if (lines.isEmpty()) {
			writer.print("<p class=\"runtime-panel__empty\">No server component details</p>");
			return;
		}
		writer.print("<ul class=\"runtime-component-list\">");
		for (String line : lines) {
			java.util.Map<String, String> fields = parseRuntimeFields(line);
			String artifact = firstNonBlank(fields.get("Artifact"), fields.get("artifact"), "Component");
			String group = firstNonBlank(fields.get("Group"), fields.get("group"), "");
			String version = firstNonBlank(fields.get("version"), fields.get("Version"), "");
			String commit = firstNonBlank(fields.get("Commit ID"), fields.get("Commit"), fields.get("commit"), "");
			writer.print("<li class=\"runtime-component-card\">");
			writer.print("<div class=\"runtime-component-card__title\">");
			writer.print(Utils.escapeHtml(artifact));
			writer.print("</div><div class=\"runtime-component-card__meta\">");
			boolean first = true;
			if (!group.isEmpty()) {
				writer.print("<span>");
				writer.print(Utils.escapeHtml(group));
				writer.print("</span>");
				first = false;
			}
			if (!version.isEmpty()) {
				if (!first) {
					writer.print("<span class=\"runtime-dot\" aria-hidden=\"true\">·</span>");
				}
				writer.print("<span>v");
				writer.print(Utils.escapeHtml(version));
				writer.print("</span>");
				first = false;
			}
			if (!commit.isEmpty()) {
				if (!first) {
					writer.print("<span class=\"runtime-dot\" aria-hidden=\"true\">·</span>");
				}
				writer.print("<code>");
				writer.print(Utils.escapeHtml(commit));
				writer.print("</code>");
			}
			writer.print("</div></li>");
		}
		writer.print("</ul>");
	}

	private void writeEndpointList(List<String> lines) {
		if (lines.isEmpty()) {
			writer.print("<p class=\"runtime-panel__empty\">No endpoints recorded</p>");
			return;
		}
		writer.print("<ul class=\"runtime-endpoint-list\">");
		for (String line : lines) {
			java.util.Map<String, String> fields = parseRuntimeFields(line);
			String domain = firstNonBlank(fields.get("Domain"), fields.get("domain"), "");
			String module = firstNonBlank(fields.get("Module"), fields.get("module"), "");
			String endpoint = firstNonBlank(fields.get("End Point"), fields.get("Endpoint"), fields.get("endpoint"), line);
			writer.print("<li class=\"runtime-endpoint-item\">");
			writer.print("<code class=\"runtime-endpoint-item__path\">");
			writer.print(Utils.escapeHtml(endpoint));
			writer.print("</code>");
			if (!domain.isEmpty() || !module.isEmpty()) {
				writer.print("<div class=\"runtime-endpoint-item__meta\">");
				if (!module.isEmpty()) {
					writer.print("<span class=\"runtime-chip\">");
					writer.print(Utils.escapeHtml(module));
					writer.print("</span>");
				}
				if (!domain.isEmpty()) {
					writer.print("<span class=\"runtime-endpoint-item__domain\">");
					writer.print(Utils.escapeHtml(domain));
					writer.print("</span>");
				}
				writer.print("</div>");
			}
			writer.print("</li>");
		}
		writer.print("</ul>");
	}

	private void writeErrorPane(String serverErrors, boolean hasServerErrors) {
		if (!hasServerErrors) {
			writer.print("<p class=\"runtime-panel__empty runtime-panel__empty--ok\">No server errors</p>");
			return;
		}
		writer.print("<pre class=\"runtime-errors\">");
		writer.print(Utils.escapeHtml(serverErrors == null ? "" : serverErrors.trim()));
		writer.print("</pre>");
	}

	private static List<String> runtimeNonEmptyLines(String raw) {
		List<String> out = Lists.newArrayList();
		if (raw == null || raw.isBlank()) {
			return out;
		}
		for (String line : raw.split("\\R")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				out.add(trimmed);
			}
		}
		return out;
	}

	private static java.util.Map<String, String> parseRuntimeFields(String line) {
		java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
		if (line == null || line.isBlank()) {
			return map;
		}
		String[] parts = line.split("\\s*----\\s*");
		for (String part : parts) {
			int colon = part.indexOf(':');
			if (colon <= 0) {
				continue;
			}
			String key = part.substring(0, colon).trim();
			String value = part.substring(colon + 1).trim();
			if (!key.isEmpty()) {
				map.put(key, value);
			}
		}
		return map;
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return "";
		}
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return "";
	}

	private String gitLink(String repoWebUrl, String path, String value) {
		if (value == null || value.isEmpty()) {
			return "NA";
		}
		if (repoWebUrl == null || repoWebUrl.isEmpty()) {
			return Utils.escapeHtml(value);
		}
		return execLink(repoWebUrl + path + value, value);
	}

	private String execLink(String href, String text) {
		return "<a href=\"" + Utils.escapeHtml(href) + "\" target=\"_blank\" "
				+ "style=\"color:#2E86C1;text-decoration:none;font-weight:bold;\">" + Utils.escapeHtml(text) + "</a>";
	}

	private String environmentUrl() {
		String endpoint = System.getProperty("env.endpoint");
		if (endpoint == null || endpoint.isBlank()) {
			return "";
		}
		String cleaned = endpoint.replaceAll("https?://", "").replace("api-internal.", "");
		if (!cleaned.endsWith("/")) {
			cleaned = cleaned + "/";
		}
		return "https://" + cleaned;
	}

	private String gitHubWebUrl(String remote) {
		if (remote == null) {
			return "";
		}
		String url = remote.trim();
		if (url.isEmpty()) {
			return "";
		}
		if (url.startsWith("git@github.com:")) {
			url = "https://github.com/" + url.substring("git@github.com:".length());
		} else if (url.startsWith("ssh://git@github.com/")) {
			url = "https://github.com/" + url.substring("ssh://git@github.com/".length());
		}
		if (url.endsWith(".git")) {
			url = url.substring(0, url.length() - 4);
		}
		if (!url.startsWith("https://github.com/") && !url.startsWith("http://github.com/")) {
			return "";
		}
		return url;
	}

	private static String convertMillisToTime(long milliseconds) {
		// Round to 1 second if duration is between 1 ms and 999 ms
		if (milliseconds > 0 && milliseconds < 1000) {
	        milliseconds = 1000;
	    }
		long seconds = (milliseconds / 1000) % 60;
		long minutes = (milliseconds / (1000 * 60)) % 60;
		long hours = (milliseconds / (1000 * 60 * 60)) % 24;
		// Format time into HH:MM:SS
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	protected PrintWriter createWriter(String outdir) throws IOException {
		new File(outdir).mkdirs();
		String jvmArg = System.getProperty(JVM_ARG);
		if (jvmArg != null && !jvmArg.trim().isEmpty()) {
			fileName = jvmArg;
		}
		return new PrintWriter(new BufferedWriter(new FileWriter(new File(outdir, fileName))));
	}

	protected void writeDocumentStart() {
		writer.println(
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
		writer.print("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
	}

	protected void writeHead() {
		writer.print("<head>");
		writer.print("<title>API Test Report</title>");
		writeStylesheet();
		writer.print("</head>");
	}

	protected void writeStylesheet() {
	    writer.print("<style type=\"text/css\">");
	    writer.print("table {margin-bottom:10px;border-collapse:collapse;empty-cells:show;width: 100%;}");
	    writer.print("th,td {border:1px solid #009;padding:.25em .5em;width: 25%;}");  // Set a fixed width for uniform cell sizes
	    writer.print("th {vertical-align:bottom}");
	    writer.print("td {vertical-align:top}");
	    writer.print("table a {font-weight:bold}");
	    writer.print(".stripe td {background-color: #E6EBF9}");
	    writer.print(".num {text-align:center}");
	    writer.print(".orange-bg {background-color: #FFD28E}");
	    writer.print(".grey-bg {background-color: #808080}");
	    writer.print(".yellow-bg {background-color: #FFFF00}");
	    writer.print(".green-bg {background-color: #D0F0C0}");
	    writer.print(".count-total {background-color: #E2E8F0; color: #1e293b; text-align: center; font-weight: 700;}");
	    writer.print(".count-passed {background-color: #BBF7D0; color: #065f46; text-align: center; font-weight: 700;}");
	    writer.print(".count-skipped {background-color: #FDE68A; color: #92400e; text-align: center; font-weight: 700;}");
	    writer.print(".count-failed {background-color: #FECACA; color: #991b1b; text-align: center; font-weight: 700;}");
	    writer.print(".count-ignored {background-color: #E5E7EB; color: #374151; text-align: center; font-weight: 700;}");
	    writer.print(".count-known {background-color: #FEF08A; color: #713f12; text-align: center; font-weight: 700;}");
	    writer.print(".attn {background-color: #E74C3C}");
	    writer.print(".passedodd td {background-color: #D0F0C0}");
	    writer.print(".passedeven td {background-color: #D0F0C0}");
	    writer.print(".skippedodd td {background-color: #FFD28E}");
	    writer.print(".skippedeven td,.stripe {background-color: #FFD28E}");
	    writer.print(".failedodd td {background-color: #E74C3C}");
	    writer.print(".failedeven td,.stripe {background-color: #E74C3C}");
	    writer.print(".ignoredodd td {background-color: #808080}");
	    writer.print(".ignoredeven td {background-color: #808080}");
	    writer.print(".known_issuesodd td, .knownissueodd td {background-color: #CC5500}");
	    writer.print(".known_issueseven td, .knownissueeven td {background-color: #CC5500}");
	    writer.print(".stacktrace {white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-word;max-width:100%;font-family:monospace}");
		writer.print(".totop {font-size:85%;text-align:center;border-bottom:2px solid #000}");
	    writer.print(".exec-flag-alert {display:inline-block;padding:2px 8px;border-radius:4px;background:#dc2626;color:#fff;font-weight:700;}");
	    writer.print(".runtime-panel{margin:0 0 14px;border:1px solid #AED6F1;border-radius:12px;background:#fff;overflow:hidden;}");
	    writer.print(".runtime-panel__head{padding:10px 14px 0;}");
	    writer.print(".runtime-panel__title{margin:0;font-size:13px;font-weight:700;color:#0f172a;letter-spacing:.04em;text-transform:uppercase;}");
	    writer.print(".runtime-panel__tabs{display:flex;flex-wrap:wrap;gap:8px;padding:10px 14px;}");
	    writer.print(".runtime-panel__tab{appearance:none;border:1px solid #cbd5e1;background:#f8fafc;color:#334155;border-radius:999px;");
	    writer.print("padding:6px 12px;font:inherit;font-size:12px;font-weight:700;cursor:pointer;display:inline-flex;align-items:center;gap:8px;}");
	    writer.print(".runtime-panel__tab.is-active{background:#dbeafe;border-color:#93c5fd;color:#0f172a;}");
	    writer.print(".runtime-panel__tab--alert{border-color:#fca5a5;color:#991b1b;}");
	    writer.print(".runtime-panel__tab--alert.is-active{background:#fee2e2;border-color:#f87171;}");
	    writer.print(".runtime-panel__tab-count{display:inline-flex;min-width:1.4rem;justify-content:center;padding:1px 6px;border-radius:999px;");
	    writer.print("background:rgba(15,23,42,.08);font-size:11px;}");
	    writer.print(".runtime-panel__pane{padding:0 14px 14px;max-height:18rem;overflow:auto;}");
	    writer.print(".runtime-panel__empty{margin:0;padding:12px 14px;border-radius:10px;background:#f8fafc;color:#64748b;font-size:13px;}");
	    writer.print(".runtime-panel__empty--ok{background:#ecfdf5;color:#065f46;}");
	    writer.print(".runtime-component-list,.runtime-endpoint-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:8px;}");
	    writer.print(".runtime-component-card,.runtime-endpoint-item{border:1px solid #e2e8f0;border-radius:10px;padding:10px 12px;background:#f8fafc;}");
	    writer.print(".runtime-component-card__title{font-weight:700;color:#0f172a;margin:0 0 4px;}");
	    writer.print(".runtime-component-card__meta,.runtime-endpoint-item__meta{display:flex;flex-wrap:wrap;gap:6px;align-items:center;color:#64748b;font-size:12px;}");
	    writer.print(".runtime-dot{opacity:.7;}");
	    writer.print(".runtime-endpoint-item__path{display:block;font-family:Consolas,monospace;font-size:12px;color:#0f172a;word-break:break-word;}");
	    writer.print(".runtime-chip{display:inline-block;padding:2px 8px;border-radius:999px;background:#e2e8f0;color:#334155;font-size:11px;font-weight:700;}");
	    writer.print(".runtime-endpoint-item__domain{color:#64748b;font-size:12px;}");
	    writer.print(".runtime-errors{margin:0;padding:12px 14px;border-radius:10px;background:#fee2e2;color:#7f1d1d;white-space:pre-wrap;");
	    writer.print("word-break:break-word;font-family:Consolas,monospace;font-size:12px;}");
	    writer.print("</style>");
	}

	protected void writeBody(boolean skipPassed, int suiteSize) {
		writer.print("<body style='font-family: Arial, sans-serif;'>");
		writeExecutionDetails(skipPassed, suiteSize);
		writeRuntimeDetails();
		writeSuiteSummary(skipPassed, suiteSize);
		writeScenarioSummary(skipPassed);
		writeScenarioDetails(skipPassed);
		writer.print("</body>");
	}

	protected void writeDocumentEnd() {
		writer.print("</html>");
	}

	protected void writeSuiteSummary(boolean skipPassed, int suiteSize) {
		NumberFormat integerFormat = NumberFormat.getIntegerInstance();

		totalPassedTests = 0;
		totalSkippedTests = 0;
		totalIgnoredTests = 0;
		totalKnownIssueTests = 0;
		totalFailedTests = 0;
		totalDuration = 0;

		writer.print("<table>");

		int testIndex = 0;
		for (SuiteResult suiteResult : suiteResults) {

			int EverySuiteFailedTestCount = 0;

			if(suiteResult.getSuiteName().contains("Master Suite"))
				continue;

			suiteCount = suiteCount + 1;

			if (suiteSize > 2 && skipPassed == true) {
				if (suiteCount == 3 && prerequisiteFailedCount == 0) {
					continue;
				}
				if (suiteCount == 4 && mainSuiteFailedCount == 0) {
					continue;
				}
			}
			if (suiteSize == 2 && skipPassed == true) {
				if (suiteCount == 2 && prerequisiteFailedCount == 0) {
					continue;
				}
			}

			writer.print("<tr>");
			writer.print("<th>&#128203; Scenario</th>");
			writer.print("<th class=\"count-total\">&#128202; Total</th>");

			if (skipPassed == false)
				writer.print("<th class=\"count-passed\">&#9989; Passed</th>");

			writer.print("<th class=\"count-skipped\">&#9197; Skipped</th>");
			writer.print("<th class=\"count-failed\">&#10060; Failed</th>");
			if (reportIgnoredTestCases) {
				writer.print("<th class=\"count-ignored\">&#9208; Ignored</th>");
			}

			if (reportKnownIssueTestCases) {
				writer.print("<th class=\"count-known\">&#9888; Known</th>");
			}
			writer.print("<th>&#9201; Time</th>");
			writer.print(GlobalConstants.TR);

			for (TestResult testResult : suiteResult.getTestResults()) {
				int passedTests = testResult.getPassedTestCount();
				int ignoredTests = testResult.getIgnoredTestCount();
				int knownIssueTests = testResult.getKnownIssueTestCount();
				int skippedTests = testResult.getSkippedTestCount();
				int failedTests = testResult.getFailedTestCount();
				long duration = testResult.getDuration();
				int totalTests = 0;
				EverySuiteFailedTestCount = EverySuiteFailedTestCount + failedTests;

				totalTests = passedTests + skippedTests + failedTests;

				if (reportIgnoredTestCases) {
					totalTests = totalTests + ignoredTests;
				}
				if (reportKnownIssueTestCases) {
					totalTests = totalTests + knownIssueTests;
				}
				// All test cases are ignored. Hence don't print anything in the report.
				if (totalTests < 1)
					continue;

				writer.print("<tr");
				writer.print(">");

				buffer.setLength(0);



				writeTableData(buffer.append("<a href=\"#t").append(testIndex).append("\">")
						.append(Utils.escapeHtml(testResult.getTestName())).append("</a>").toString(), "num");

				writeTableData(integerFormat.format(totalTests), countClass("count-total", totalTests));

				if (skipPassed == false)
					writeTableData(integerFormat.format(passedTests), countClass("count-passed", passedTests));

				writeTableData(integerFormat.format(skippedTests), countClass("count-skipped", skippedTests));
				writeTableData(integerFormat.format(failedTests), countClass("count-failed", failedTests));
				if (reportIgnoredTestCases) {
					writeTableData(integerFormat.format(ignoredTests), countClass("count-ignored", ignoredTests));
				}

				if (reportKnownIssueTestCases) {
					writeTableData(integerFormat.format(knownIssueTests), countClass("count-known", knownIssueTests));
				}
				writeTableData(convertMillisToTime(duration), "num");
				writer.print(GlobalConstants.TR);

				totalPassedTests += passedTests;
				totalSkippedTests += skippedTests;
				totalFailedTests += failedTests;
				totalIgnoredTests += ignoredTests;
				totalKnownIssueTests += knownIssueTests;
				totalDuration += duration;

				testIndex++;
			}

			if (skipPassed == false && suiteCount == 1 && EverySuiteFailedTestCount > 0) {
				prerequisiteFailedCount = EverySuiteFailedTestCount;
				System.out.println("prerequisiteFailedCount = " + prerequisiteFailedCount);
			}if (suiteSize > 2 && skipPassed == false && suiteCount == 2 && EverySuiteFailedTestCount > 0) {
				mainSuiteFailedCount = EverySuiteFailedTestCount;
				System.out.println("mainSuiteFailedCount = " + mainSuiteFailedCount);
			}
		}

//		if (testIndex > 1) {
			writer.print("<tr>");
			writer.print("<th>Total</th>");

			if (reportIgnoredTestCases && reportKnownIssueTestCases) {
				writeTableHeader(integerFormat.format((long) totalPassedTests + totalSkippedTests + totalFailedTests
						+ totalIgnoredTests + totalKnownIssueTests), countClass("count-total",
								totalPassedTests + totalSkippedTests + totalFailedTests + totalIgnoredTests
										+ totalKnownIssueTests));
			} else if (reportIgnoredTestCases && !(reportKnownIssueTestCases)) {
				writeTableHeader(integerFormat.format((long) totalPassedTests + totalSkippedTests + totalFailedTests
						+ totalIgnoredTests), countClass("count-total",
								totalPassedTests + totalSkippedTests + totalFailedTests + totalIgnoredTests));
			} else if (reportKnownIssueTestCases && !(reportIgnoredTestCases)) {
				writeTableHeader(integerFormat.format((long) totalPassedTests + totalSkippedTests + totalFailedTests
						+totalKnownIssueTests), countClass("count-total",
								totalPassedTests + totalSkippedTests + totalFailedTests + totalKnownIssueTests));
			} else {
				writeTableHeader(integerFormat.format((long) totalPassedTests + totalSkippedTests + totalFailedTests),
						countClass("count-total", totalPassedTests + totalSkippedTests + totalFailedTests));
			}

			if (skipPassed == false)
				writeTableHeader(integerFormat.format(totalPassedTests), countClass("count-passed", totalPassedTests));

			writeTableHeader(integerFormat.format(totalSkippedTests), countClass("count-skipped", totalSkippedTests));
			writeTableHeader(integerFormat.format(totalFailedTests), countClass("count-failed", totalFailedTests));
			if (reportIgnoredTestCases) {
				writeTableHeader(integerFormat.format(totalIgnoredTests), countClass("count-ignored", totalIgnoredTests));
			}
			if (reportKnownIssueTestCases) {
				writeTableHeader(integerFormat.format(totalKnownIssueTests),
						countClass("count-known", totalKnownIssueTests));
			}
			writeTableHeader(convertMillisToTime(totalDuration), "num");
			writer.print(GlobalConstants.TR);
//		}

		writer.print(GlobalConstants.TABLE);
	}

	protected static Set<ITestResult> getResultsSubSet(Set<ITestResult> resultsSet, String subSetString) {
		List<ITestResult> testResultsSubList = Lists.newArrayList();
		if (!resultsSet.isEmpty()) {
			List<ITestResult> resultsList = Lists.newArrayList(resultsSet);
			Iterator<ITestResult> resultsIterator = resultsList.iterator();
			while (resultsIterator.hasNext()) {
				ITestResult result = resultsIterator.next();
				Throwable throwable = result.getThrowable();
				if (throwable != null) {
					if (subSetString.contains(GlobalConstants.FEATURE_NOT_SUPPORTED)
							|| subSetString.contains(GlobalConstants.SERVICE_NOT_DEPLOYED)
							|| subSetString.contains(GlobalConstants.NOT_IN_RUN_SCOPE)) {
						if (containsAny(throwable.getMessage(), subSetString)) {
							// Add only results which are skipped due to feature not supported
							testResultsSubList.add(result);
						} else {
							// Skip the test result
						}
					} else if (subSetString.contains(GlobalConstants.KNOWN_ISSUES_STRING)) {
						if (containsAny(throwable.getMessage(), subSetString)) {
							// Add only results which are skipped due to feature not supported
							testResultsSubList.add(result);
						} else {
							// Skip the test result
						}
					} else { // Service not deployed. Hence skipping the testcase // skipped
						if (!throwable.getMessage().contains(GlobalConstants.FEATURE_NOT_SUPPORTED)
								&& !throwable.getMessage().contains(GlobalConstants.SERVICE_NOT_DEPLOYED)
								&& !throwable.getMessage().contains(GlobalConstants.KNOWN_ISSUES)
								&& !throwable.getMessage().contains(GlobalConstants.NOT_IN_RUN_SCOPE)) {
							// Add only results which are not skipped due to feature not supported
							testResultsSubList.add(result);
						} else {
							// Skip the test result
						}
					}
				}
			}
		}
		Set<ITestResult> testResultsSubSet = Set.copyOf(testResultsSubList);
		return testResultsSubSet;
	}

	public static boolean containsAny(String stringToCheckIn, String delimitedString) {
	    // Split the input string into an array based on semicolons
        String[] stringsToCheckFor = delimitedString.split(";");

        for (String str : stringsToCheckFor) {
            if (stringToCheckIn.contains(str)) {
                return true; // If any string is found, return true
            }
        }
        return false; // If none of the strings are found, return false
    }

	/**
	 * Writes a summary of all the test scenarios.
	 */
	protected void writeScenarioSummary(boolean skipPassed) {
		writer.print("<table id='summary'>");
		writer.print("<thead>");
		writer.print("<tr>");
		writer.print("<th>TestCase Number</th>");
		writer.print("<th>TestCase Description</th>");
		writer.print("<th>Execution Time (HH:MM:SS)</th>");
		writer.print(GlobalConstants.TR);
		writer.print("</thead>");

		int testIndex = 0;
		int scenarioIndex = 0;
		for (SuiteResult suiteResult : suiteResults) {



			for (TestResult testResult : suiteResult.getTestResults()) {
				int passedTests = testResult.getPassedTestCount();
				int ignoredTests = testResult.getIgnoredTestCount();
				int knownIssueTests = testResult.getKnownIssueTestCount();
				int skippedTests = testResult.getSkippedTestCount();
				int failedTests = testResult.getFailedTestCount();
				int totalTests = 0;

				totalTests = passedTests + skippedTests + failedTests;

				if (reportIgnoredTestCases) {
					totalTests = totalTests + ignoredTests;
				}
				if (reportKnownIssueTestCases) {
					totalTests = totalTests + knownIssueTests;
				}
				// All test cases are ignored. Hence don't print anything in the report.
				if (totalTests < 1)
					continue;
				writer.print("<tbody id=\"t");
				writer.print(testIndex);
				writer.print("\">");

				String testName = Utils.escapeHtml(testResult.getTestName());

				if (reportIgnoredTestCases) {
					scenarioIndex += writeScenarioSummary(testName + " &#8212; Ignored",
							testResult.getIgnoredTestResults(), "ignored", scenarioIndex);
				}

				if (reportKnownIssueTestCases) {
					scenarioIndex += writeScenarioSummary(testName + " &#8212; known_issues",
							testResult.getKnownIssueTestResults(), "knownissue", scenarioIndex);
				}

				scenarioIndex += writeScenarioSummary(testName + " &#8212; Failed", testResult.getFailedTestResults(),
						"failed", scenarioIndex);
				scenarioIndex += writeScenarioSummary(testName + " &#8212; Skipped", testResult.getSkippedTestResults(),
						"skipped", scenarioIndex);

				if (skipPassed == false)
					scenarioIndex += writeScenarioSummary(testName + " &#8212; Passed", testResult.getPassedTestResults(),
						"passed", scenarioIndex);

				writer.print("</tbody>");

				testIndex++;
			}
		}

		writer.print(GlobalConstants.TABLE);
	}

	private String getTestCaseDescription(ITestResult result) {
		Object[] parameters = result.getParameters();
		if (parameters != null && parameters.length > 0 && parameters[0] instanceof TestCaseDTO) {
			TestCaseDTO testCase = (TestCaseDTO) parameters[0];
			if (testCase.getDescription() == null)
				return "";
			else
				return testCase.getDescription();
		}

		return "";
	}


	private String getTestCaseUniqueIdentifier(ITestResult result) {
		Object[] parameters = result.getParameters();
		if (parameters != null && parameters.length > 0 && parameters[0] instanceof TestCaseDTO) {
			TestCaseDTO testCase = (TestCaseDTO) parameters[0];
			if (testCase.getUniqueIdentifier()  == null)
				return "";
			else
				return testCase.getUniqueIdentifier();
		}

		return "";
	}

	/**
	 * Writes the scenario summary for the results of a given state for a single
	 * test.
	 */
	private int writeScenarioSummary(String description, List<ClassResult> classResults, String cssClassPrefix,
			int startingScenarioIndex) {
		int scenarioCount = 0;
		if (!classResults.isEmpty()) {
			writer.print("<tr><th colspan=\"3\">");
			writer.print(description);
			writer.print(GlobalConstants.TRTR);

			int scenarioIndex = startingScenarioIndex;
			int classIndex = 0;
			for (ClassResult classResult : classResults) {
				String cssClass = cssClassPrefix + ((classIndex % 2) == 0 ? "even" : "odd");

				buffer.setLength(0);

				for (MethodResult methodResult : classResult.getMethodResults()) {
					List<ITestResult> results = methodResult.getResults();
					int resultsCount = results.size();
					assert resultsCount > 0;

					for (ITestResult result : results) {
						String testCaseDescription = Utils.escapeHtml(getTestCaseDescription(result));
						String uniqueIdentifier = getTestCaseUniqueIdentifier(result);
						long duration = result.getEndMillis() - result.getStartMillis();
						String temp = uniqueIdentifier.isEmpty() ? getTestCaseKey(result) : uniqueIdentifier;

						buffer.append("<tr class=\"").append(cssClass).append("\">")
								.append("<td><a href=\"#m").append(scenarioIndex).append("\">")
								.append(Utils.escapeHtml(temp)).append("</a></td>")
								.append("<td class='scenario-desc-col'>").append(testCaseDescription).append("</td>")
								.append("<td class='bug-column'>").append(convertMillisToTime(duration))
								.append("</td></tr>");
						scenarioIndex++;
					}
				}

				writer.print(buffer);

				classIndex++;
			}
			scenarioCount = scenarioIndex - startingScenarioIndex;
		}
		return scenarioCount;
	}

	/**
	 * Writes the details for all test scenarios.
	 */
	protected void writeScenarioDetails(boolean skipPassed) {
		int scenarioIndex = 0;
		for (SuiteResult suiteResult : suiteResults) {
			for (TestResult testResult : suiteResult.getTestResults()) {

				if (reportIgnoredTestCases) {
					scenarioIndex += writeScenarioDetails(testResult.getIgnoredTestResults(), scenarioIndex);
				}

				if (reportKnownIssueTestCases) {
					scenarioIndex += writeScenarioDetails(testResult.getKnownIssueTestResults(), scenarioIndex);
				}
				scenarioIndex += writeScenarioDetails(testResult.getFailedConfigurationResults(), scenarioIndex);
				scenarioIndex += writeScenarioDetails(testResult.getFailedTestResults(), scenarioIndex);
				scenarioIndex += writeScenarioDetails(testResult.getSkippedConfigurationResults(), scenarioIndex);
				scenarioIndex += writeScenarioDetails(testResult.getSkippedTestResults(), scenarioIndex);

				if (skipPassed == false)
					scenarioIndex += writeScenarioDetails(testResult.getPassedTestResults(), scenarioIndex);

			}
		}
	}

	/**
	 * Writes the scenario details for the results of a given state for a single
	 * test.
	 */
	private int writeScenarioDetails(List<ClassResult> classResults, int startingScenarioIndex) {
		int scenarioIndex = startingScenarioIndex;
		String label = "";
		for (ClassResult classResult : classResults) {
			String className = classResult.getClassName();
			for (MethodResult methodResult : classResult.getMethodResults()) {
				List<ITestResult> results = methodResult.getResults();
				assert !results.isEmpty();
				if (ConfigManager.IsDebugEnabled())
					label = Utils.escapeHtml(className + "#" + results.iterator().next().getMethod().getMethodName());
				else
					label = Utils.escapeHtml(results.iterator().next().getMethod().getMethodName());
				for (ITestResult result : results) {
					writeScenario(scenarioIndex, label, result);
					scenarioIndex++;
				}
			}
		}

		return scenarioIndex - startingScenarioIndex;
	}

	/**
	 * Writes the details for an individual test scenario.
	 */
	private void writeScenario(int scenarioIndex, String label, ITestResult result) {
		writer.print("<h3 id=\"m");
		writer.print(scenarioIndex);
		writer.print("\">");

		Object[] parameters = result.getParameters();
		int parameterCount = (parameters == null ? 0 : parameters.length);

		String testCaseName = (String) result.getAttribute("TestCaseName");

		if (testCaseName == null || testCaseName.trim().isEmpty()) {
		    testCaseName = result.getMethod().getMethodName();
		}
		// Get the class name
		String className = result.getMethod().getTestClass().getRealClass().getSimpleName();

		String uniqueIdentifier = (parameterCount > 0 && parameters[0] instanceof TestCaseDTO)
				? (((TestCaseDTO) parameters[0]).getUniqueIdentifier() != null
						? ((TestCaseDTO) parameters[0]).getUniqueIdentifier()
						: "TestCase ID is not available")
				: "UNKNOWN";

		String description = (parameterCount > 0 && parameters[0] instanceof TestCaseDTO)
				? (((TestCaseDTO) parameters[0]).getDescription() != null
						? ((TestCaseDTO) parameters[0]).getDescription()
						: "No description available.")
				: "UNKNOWN";

		// Replace test case name with: TestcaseNumber # TestcaseDescription
		writer.print(Utils.escapeHtml(uniqueIdentifier + " # " + description));
		writer.print("</h3>");

		writer.print("<table class=\"result\">");

		// Add TestCaseName
		writer.print("<tr class=\"param\">");
		writer.print("<th>Testcase Name</th>");
		writer.print("</tr><tr class=\"param stripe\">");
		writer.print("<td>");
		writer.print(Utils.escapeHtml(testCaseName));
		writer.print("</td></tr>");

		// Add Dependency
		writer.print("<tr class=\"param\">");
		writer.print("<th>Testcase Dependency</th>");
		writer.print("</tr><tr class=\"param stripe\">");
		List<String> dependencies = DependencyResolver.getDependencies(uniqueIdentifier);
		dependencies.remove(uniqueIdentifier);
		writer.print("<td>");
		writer.print(Utils.escapeHtml(dependencies.toString()));
		writer.print("</td>");

		// Add ClassName
		writer.print("<tr class=\"param\">");
		writer.print("<th>Class Name</th>");
		writer.print("</tr><tr class=\"param stripe\">");
		writer.print("<td>");
		writer.print(Utils.escapeHtml(className));
		writer.print("</td></tr>");

		writer.print(GlobalConstants.TR);

		if (ConfigManager.IsDebugEnabled()) {
			if (parameterCount > 0) {
				writer.print("<tr class=\"param\">");
				for (int i = 1; i <= parameterCount; i++) {
					writer.print("<th>Testcase Input");
					writer.print("</th>");
				}
				writer.print("</tr><tr class=\"param stripe\">");
				for (Object parameter : parameters) {
					String testcaseDTO = Utils.toString(parameter).replace("TestCaseDTO(", "");
					writer.print("<td>");
					writer.print(Utils.escapeHtml(testcaseDTO.substring(0, testcaseDTO.length() - 1)));
					writer.print("</td>");
				}
				writer.print(GlobalConstants.TR);
			}
		}

		List<String> reporterMessages = Reporter.getOutput(result);
		if (!reporterMessages.isEmpty()) {
			writer.print("<tr><th");
			if (parameterCount > 1) {
				writer.print(GlobalConstants.colspan);
				writer.print(parameterCount);
				writer.print("\"");
			}
			writer.print(">Messages</th></tr>");

			writer.print("<tr><td");
			if (parameterCount > 1) {
				writer.print(GlobalConstants.colspan);
				writer.print(parameterCount);
				writer.print("\"");
			}
			writer.print(">");
			writeReporterMessages(reporterMessages);
			writer.print(GlobalConstants.TDTR);
		}

		Throwable throwable = result.getThrowable();
		if (throwable != null) {
			writer.print("<tr><th");
			if (parameterCount > 1) {
				writer.print(GlobalConstants.colspan);
				writer.print(parameterCount);
				writer.print("\"");
			}
			writer.print(">");
			writer.print(
					(result.getStatus() == ITestResult.SUCCESS ? "Expected Exception" : "Output Validation Exception"));
			writer.print(GlobalConstants.TRTR);

			writer.print("<tr><td");
			if (parameterCount > 1) {
				writer.print(GlobalConstants.colspan);
				writer.print(parameterCount);
				writer.print("\"");
			}
			writer.print(">");
			writeStackTrace(throwable);
			writer.print(GlobalConstants.TDTR);
		}

		writer.print(GlobalConstants.TABLE);
		writer.print("<p class=\"totop\"><a href=\"#summary\">back to summary</a></p>");
	}

	protected void writeReporterMessages(List<String> reporterMessages) {
		writer.print("<div class=\"Request and Response messages including headers\">");
		Iterator<String> iterator = reporterMessages.iterator();
		assert iterator.hasNext();
		if (Reporter.getEscapeHtml()) {
			writer.print(Utils.escapeHtml(iterator.next()));
		} else {
			writer.print(iterator.next());
		}
		while (iterator.hasNext()) {
			writer.print("<br/>");
			if (Reporter.getEscapeHtml()) {
				writer.print(Utils.escapeHtml(iterator.next()));
			} else {
				writer.print(iterator.next());
			}
		}
		writer.print("</div>");
	}

	protected void writeStackTrace(Throwable throwable) {
		writer.print("<div class=\"stacktrace\">");
		writer.print(Utils.shortStackTrace(throwable, true));
		writer.print("</div>");
	}

	private String countClass(String statusClass, int count) {
		return count > 0 ? "num " + statusClass : "num";
	}

	/**
	 * Writes a TH element with the specified contents and CSS class names.
	 *
	 * @param html       the HTML contents
	 * @param cssClasses the space-delimited CSS classes or null if there are no
	 *                   classes to apply
	 */
	protected void writeTableHeader(String html, String cssClasses) {
		writeTag("th", html, cssClasses);
	}

	/**
	 * Writes a TD element with the specified contents.
	 *
	 * @param html the HTML contents
	 */
	protected void writeTableData(String html) {
		writeTableData(html, null);
	}

	/**
	 * Writes a TD element with the specified contents and CSS class names.
	 *
	 * @param html       the HTML contents
	 * @param cssClasses the space-delimited CSS classes or null if there are no
	 *                   classes to apply
	 */
	protected void writeTableData(String html, String cssClasses) {
		writeTag("td", html, cssClasses);
	}

	/**
	 * Writes an arbitrary HTML element with the specified contents and CSS class
	 * names.
	 *
	 * @param tag        the tag name
	 * @param html       the HTML contents
	 * @param cssClasses the space-delimited CSS classes or null if there are no
	 *                   classes to apply
	 */
	protected void writeTag(String tag, String html, String cssClasses) {
		writer.print("<");
		writer.print(tag);
		if (cssClasses != null) {
			writer.print(" class=\"");
			writer.print(cssClasses);
			writer.print("\"");
		}
		writer.print(">");
		writer.print(html);
		writer.print("</");
		writer.print(tag);
		writer.print(">");
	}

	/**
	 * Groups {@link TestResult}s by suite.
	 */
	protected static class SuiteResult {
		private final String suiteName;
		private final List<TestResult> testResults = Lists.newArrayList();

		public SuiteResult(ISuite suite) {
			suiteName = suite.getName();
			for (ISuiteResult suiteResult : suite.getResults().values()) {
				testResults.add(new TestResult(suiteResult.getTestContext()));
			}
		}

		public String getSuiteName() {
			return suiteName;
		}

		/**
		 * @return the test results (possibly empty)
		 */
		public List<TestResult> getTestResults() {
			return testResults;
		}
	}

	/**
	 * Groups {@link ClassResult}s by test, type (configuration or test), and
	 * status.
	 */
	protected static class TestResult {
		/**
		 * Orders test results by class name and then by method name (in lexicographic
		 * order).
		 */
		protected static final Comparator<ITestResult> RESULT_COMPARATOR = new Comparator<ITestResult>() {
			@Override
			public int compare(ITestResult o1, ITestResult o2) {
				int result = o1.getTestClass().getName().compareTo(o2.getTestClass().getName());
				if (result == 0) {
					result = o1.getMethod().getMethodName().compareTo(o2.getMethod().getMethodName());
				}
				return result;
			}
		};

		private final String testName;
		private final Date startDate;
		private final List<ClassResult> failedConfigurationResults;
		private final List<ClassResult> failedTestResults;
		private final List<ClassResult> skippedConfigurationResults;
		private final List<ClassResult> skippedTestResults;
		private final List<ClassResult> passedTestResults;
		private final List<ClassResult> ignoredTestResults;
		private final List<ClassResult> knownIssueTestResults;
		private final int ignoredTestCount;
		private final int knownIssueTestCount;
		private final int failedTestCount;
		private final int skippedTestCount;
		private final int passedTestCount;
		private final long duration;
		private final String includedGroups;
		private final String excludedGroups;

		public TestResult(ITestContext context) {
			testName = context.getName();
			startDate = context.getStartDate();

			Set<ITestResult> failedConfigurations = context.getFailedConfigurations().getAllResults();
			Set<ITestResult> failedTests = context.getFailedTests().getAllResults();
			Set<ITestResult> skippedConfigurations = context.getSkippedConfigurations().getAllResults();
//			Set<ITestResult> skippedTests = context.getSkippedTests().getAllResults();
			Set<ITestResult> skippedTests = getResultsSubSet(context.getSkippedTests().getAllResults(), GlobalConstants.SKIPPED);
			Set<ITestResult> ignoredTests =  getResultsSubSet(context.getSkippedTests().getAllResults(), GlobalConstants.IGNORED_SUBSET_STRING);
			Set<ITestResult> knownIssueTests =  getResultsSubSet(context.getSkippedTests().getAllResults(), GlobalConstants.KNOWN_ISSUE_SUBSET_STRING);
			Set<ITestResult> passedTests = context.getPassedTests().getAllResults();

			failedConfigurationResults = groupResults(failedConfigurations);
			failedTestResults = groupResults(failedTests);
			skippedConfigurationResults = groupResults(skippedConfigurations);
			skippedTestResults = groupResults(skippedTests);
			ignoredTestResults = groupResults(ignoredTests);
			knownIssueTestResults = groupResults(knownIssueTests);
			passedTestResults = groupResults(passedTests);

			failedTestCount = failedTests.size();
			skippedTestCount = skippedTests.size();
			passedTestCount = passedTests.size();
			ignoredTestCount = ignoredTests.size();
			knownIssueTestCount = knownIssueTests.size();

			duration = context.getEndDate().getTime() - context.getStartDate().getTime();

			includedGroups = formatGroups(context.getIncludedGroups());
			excludedGroups = formatGroups(context.getExcludedGroups());
		}

		/**
		 * Groups test results by method and then by class.
		 */
		protected List<ClassResult> groupResults(Set<ITestResult> results) {
			List<ClassResult> classResults = Lists.newArrayList();
			if (!results.isEmpty()) {
				List<MethodResult> resultsPerClass = Lists.newArrayList();
				List<ITestResult> resultsPerMethod = Lists.newArrayList();

				List<ITestResult> resultsList = Lists.newArrayList(results);
				Collections.sort(resultsList, RESULT_COMPARATOR);
				Iterator<ITestResult> resultsIterator = resultsList.iterator();
				assert resultsIterator.hasNext();

				ITestResult result = resultsIterator.next();
				resultsPerMethod.add(result);

				String previousClassName = result.getTestClass().getName();
				String previousMethodName = getTestCaseKey(result);
				while (resultsIterator.hasNext()) {
					result = resultsIterator.next();

					String className = result.getTestClass().getName();
					if (!previousClassName.equals(className)) {
						assert !resultsPerMethod.isEmpty();
						resultsPerClass.add(new MethodResult(resultsPerMethod));
						resultsPerMethod = Lists.newArrayList();

						assert !resultsPerClass.isEmpty();
						classResults.add(new ClassResult(previousClassName, resultsPerClass));
						resultsPerClass = Lists.newArrayList();

						previousClassName = className;
						previousMethodName = getTestCaseKey(result);
					} else {
						String methodName = getTestCaseKey(result);
						if (!previousMethodName.equals(methodName)) {
							assert !resultsPerMethod.isEmpty();
							resultsPerClass.add(new MethodResult(resultsPerMethod));
							resultsPerMethod = Lists.newArrayList();

							previousMethodName = methodName;
						}
					}
					resultsPerMethod.add(result);
				}
				assert !resultsPerMethod.isEmpty();
				resultsPerClass.add(new MethodResult(resultsPerMethod));
				assert !resultsPerClass.isEmpty();
				classResults.add(new ClassResult(previousClassName, resultsPerClass));
			}
			return classResults;
		}

		public String getTestName() {
			return testName;
		}

		public Date getStartDate() {
			return startDate;
		}

		/**
		 * @return the results for failed configurations (possibly empty)
		 */
		public List<ClassResult> getFailedConfigurationResults() {
			return failedConfigurationResults;
		}

		/**
		 * @return the results for failed tests (possibly empty)
		 */
		public List<ClassResult> getFailedTestResults() {
			return failedTestResults;
		}

		public List<ClassResult> getIgnoredTestResults() {
			return ignoredTestResults;
		}

		public List<ClassResult> getKnownIssueTestResults() {
			return knownIssueTestResults;
		}

		/**
		 * @return the results for skipped configurations (possibly empty)
		 */
		public List<ClassResult> getSkippedConfigurationResults() {
			return skippedConfigurationResults;
		}

		/**
		 * @return the results for skipped tests (possibly empty)
		 */
		public List<ClassResult> getSkippedTestResults() {
			return skippedTestResults;
		}

		/**
		 * @return the results for passed tests (possibly empty)
		 */
		public List<ClassResult> getPassedTestResults() {
			return passedTestResults;
		}

		public int getFailedTestCount() {
			return failedTestCount;
		}

		public int getSkippedTestCount() {
			return skippedTestCount;
		}

		public int getPassedTestCount() {
			return passedTestCount;
		}

		public int getIgnoredTestCount() {
			return ignoredTestCount;
		}

		public int getKnownIssueTestCount() {
			return knownIssueTestCount;
		}

		public long getDuration() {
			return duration;
		}

		public String getIncludedGroups() {
			return includedGroups;
		}

		public String getExcludedGroups() {
			return excludedGroups;
		}

		/**
		 * Formats an array of groups for display.
		 */
		protected String formatGroups(String[] groups) {
			if (groups.length == 0) {
				return "";
			}

			StringBuilder builder = new StringBuilder();
			builder.append(groups[0]);
			for (int i = 1; i < groups.length; i++) {
				builder.append(", ").append(groups[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Groups {@link MethodResult}s by class.
	 */
	protected static class ClassResult {
		private final String className;
		private final List<MethodResult> methodResults;

		/**
		 * @param className     the class name
		 * @param methodResults the non-null, non-empty {@link MethodResult} list
		 */
		public ClassResult(String className, List<MethodResult> methodResults) {
			this.className = className;
			this.methodResults = methodResults;
		}

		public String getClassName() {
			return className;
		}

		/**
		 * @return the non-null, non-empty {@link MethodResult} list
		 */
		public List<MethodResult> getMethodResults() {
			return methodResults;
		}
	}

	/**
	 * Groups test results by method.
	 */
	protected static class MethodResult {
		private final List<ITestResult> results;

		/**
		 * @param results the non-null, non-empty result list
		 */
		public MethodResult(List<ITestResult> results) {
			this.results = results;
		}

		/**
		 * @return the non-null, non-empty result list
		 */
		public List<ITestResult> getResults() {
			return results;
		}
	}

	private static String getTestCaseKey(ITestResult result) {

		String testCaseName = (String) result.getAttribute("TestCaseName");
		if (testCaseName != null && !testCaseName.isEmpty()) {
			return testCaseName;
		}

		Object[] params = result.getParameters();
		if (params != null && params.length > 0 && params[0] instanceof TestCaseDTO dto) {
			String dtoTestcaseName = dto.getTestCaseName();
	        if (dtoTestcaseName != null && !dtoTestcaseName.trim().isEmpty()) {
	            return dtoTestcaseName;
	        }
		}

		return result.getMethod().getMethodName();
	}

}
