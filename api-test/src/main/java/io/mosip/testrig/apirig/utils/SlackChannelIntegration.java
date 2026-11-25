package io.mosip.testrig.apirig.utils;

import org.apache.log4j.Logger;

import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Local override of apitest-commons {@code SlackChannelIntegration}.
 * <p>
 * Commons always POSTs to {@code slack-webhook-url}. Local runs leave that property blank, which
 * produces {@code Failed to send Slack notification: Expected URL scheme 'http' or 'https'...} on
 * every suite finish. Skip the call when the webhook is unset or not an http(s) URL.
 * </p>
 */
public class SlackChannelIntegration extends BaseTestCase {

	private static final Logger LOGGER = Logger.getLogger(SlackChannelIntegration.class);

	private static final String SLACK_WEBHOOK_URL = ConfigManager.getSlackWebHookUrl();

	/**
	 * Posts {@code message} to Slack when a webhook URL is configured; otherwise logs and returns.
	 *
	 * @param message summary text for the channel
	 */
	public static void sendMessageToSlack(String message) {
		if (SLACK_WEBHOOK_URL == null || SLACK_WEBHOOK_URL.isBlank()
				|| !(SLACK_WEBHOOK_URL.startsWith("http://") || SLACK_WEBHOOK_URL.startsWith("https://"))) {
			LOGGER.info("Slack webhook not configured; skipping notification.");
			return;
		}
		LOGGER.info("Sending Slack notification to configured webhook.");
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse("application/json");
		String json = "{\"text\":\"" + message.replace("\"", "\\\"") + "\"}";
		RequestBody body = RequestBody.create(json, mediaType);
		Request request = new Request.Builder().url(SLACK_WEBHOOK_URL).post(body).build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, java.io.IOException e) {
				LOGGER.warn("Slack notification failed: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) {
				response.close();
			}
		});
	}
}
