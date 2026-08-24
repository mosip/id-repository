package io.mosip.testrig.apirig.idrepo.report;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.testng.internal.Utils;

import io.mosip.testrig.apirig.report.EmailableReport;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;

/**
 * Emailable report that fills "End Points used" for {@code http://} local runs.
 * apitest-commons only parses {@code https://} URLs when writing that cell.
 */
public class IdRepoEmailableReport extends EmailableReport {

	private static final Logger LOGGER = Logger.getLogger(IdRepoEmailableReport.class);
	private static final String END_POINTS_PRE_OPEN = "End Points used</span></th><td colspan=\"8\"><pre>";
	private static final String PRE_CLOSE = "</pre></td>";

	@Override
	public void generateOutputFile(boolean skipPassed) {
		try {
			fillEndPointsUsedIfEmpty();
		} catch (Exception e) {
			LOGGER.error("Failed to fill End Points used in the report: " + e.getMessage(), e);
		}
		super.generateOutputFile(skipPassed);
	}

	private void fillEndPointsUsedIfEmpty() throws Exception {
		String details = HttpAwareEndpointFormatter.format(GlobalMethods.serverEndpoints);
		if (details == null || details.isBlank()) {
			LOGGER.info("No collected endpoints to add to End Points used.");
			return;
		}

		File reportFile = resolveOriginalReportFile();
		if (reportFile == null || !reportFile.exists()) {
			LOGGER.warn("Could not find emailable report to fill End Points used.");
			return;
		}

		Charset charset = Charset.defaultCharset();
		String html = Files.readString(reportFile.toPath(), charset);
		int openAt = html.indexOf(END_POINTS_PRE_OPEN);
		if (openAt < 0) {
			LOGGER.warn("End Points used cell not found in report HTML.");
			return;
		}

		int contentStart = openAt + END_POINTS_PRE_OPEN.length();
		int contentEnd = html.indexOf(PRE_CLOSE, contentStart);
		if (contentEnd < 0) {
			LOGGER.warn("End Points used cell is malformed in report HTML.");
			return;
		}

		String existing = html.substring(contentStart, contentEnd);
		if (!existing.isBlank()) {
			LOGGER.info("End Points used already populated by apitest-commons; leaving as-is.");
			return;
		}

		String filled = html.substring(0, contentStart) + Utils.escapeHtml(details) + html.substring(contentEnd);
		Files.writeString(reportFile.toPath(), filled, charset);
		LOGGER.info("Filled End Points used for local/http URLs in " + reportFile.getName());
	}

	private static File resolveOriginalReportFile() {
		String outputDir = System.getProperty("testng.outpur.dir");
		String reportName = System.getProperty(GlobalConstants.EMAILABLEREPORT2NAME);
		if (outputDir == null || outputDir.isBlank() || reportName == null || reportName.isBlank()) {
			return null;
		}
		return new File(System.getProperty("user.dir") + "/" + outputDir + "/" + reportName);
	}
}
