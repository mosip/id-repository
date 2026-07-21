package io.mosip.kernel.websub.api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.config.publisher.RestTemplateHelper;
import io.mosip.kernel.websub.api.constants.HubMode;
import io.mosip.kernel.websub.api.constants.WebSubClientConstants;
import io.mosip.kernel.websub.api.constants.WebSubClientErrorCode;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.HubResponse;
import io.mosip.kernel.websub.api.util.ParseUtil;

/**
 * Spring Framework 7 / Boot 4 replacement for {@code kernel-websubclient-api}
 * {@code PublisherClientImpl}.
 * <p>
 * Loaded from {@code id-repository-service} before {@code kernel-websubclient-api.jar}. See
 * {@link SubscriberClientImpl} for rationale (stale {@code 1.4.0-SNAPSHOT} vs commons develop).
 * </p>
 */
public class PublisherClientImpl<P> implements PublisherClient<String, P, HttpHeaders> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PublisherClientImpl.class);

	@Autowired
	private RestTemplateHelper restTemplateHelper;

	@Override
	public void registerTopic(String topic, String hubURL) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add(WebSubClientConstants.HUB_MODE, HubMode.REGISTER.gethubModeValue());
		map.add(WebSubClientConstants.HUB_TOPIC, topic);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplateHelper.getRestTemplate().exchange(hubURL, HttpMethod.POST, entity, String.class);
		}
		catch (HttpClientErrorException | HttpServerErrorException exception) {
			String responseBody = exception.getResponseBodyAsString();
			if (isTopicAlreadyRegistered(responseBody)) {
				LOGGER.debug("WebSub topic already registered: topic={}, hubUrl={}", topic, hubURL);
				return;
			}
			logRegisterFailure(topic, hubURL, responseBody, exception.getStatusCode().value());
			throw new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(),
					WebSubClientErrorCode.REGISTER_ERROR.getErrorMessage() + responseBody);
		}
		if (response.getStatusCode() == HttpStatus.ACCEPTED) {
			LOGGER.info("WebSub topic registered: topic={}, hubUrl={}", topic, hubURL);
		}
		else if (response.getStatusCode() == HttpStatus.OK) {
			HubResponse hubResponse = ParseUtil.parseHubResponse(response.getBody());
			if (hubResponse.getHubResult().equals("accepted")) {
				LOGGER.info("WebSub topic registered: topic={}, hubUrl={}", topic, hubURL);
			}
			else {
				String denialReason = hubResponse.getErrorReason();
				if (isTopicAlreadyRegistered(denialReason) || isTopicAlreadyRegistered(response.getBody())) {
					LOGGER.debug("WebSub topic already registered: topic={}, hubUrl={}", topic, hubURL);
					return;
				}
				logRegisterFailure(topic, hubURL, denialReason != null ? denialReason : response.getBody(),
						response.getStatusCode().value());
				throw new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(),
						WebSubClientErrorCode.REGISTER_ERROR.getErrorMessage() + denialReason);
			}
		}
		else {
			logRegisterFailure(topic, hubURL, response.getBody(), response.getStatusCode().value());
			throw new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(),
					WebSubClientErrorCode.REGISTER_ERROR.getErrorMessage() + response.getBody());
		}
	}

	@Override
	public void unregisterTopic(String topic, String hubURL) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add(WebSubClientConstants.HUB_MODE, HubMode.UNREGISTER.gethubModeValue());
		map.add(WebSubClientConstants.HUB_TOPIC, topic);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplateHelper.getRestTemplate().exchange(hubURL, HttpMethod.POST, entity, String.class);
		}
		catch (HttpClientErrorException | HttpServerErrorException exception) {
			throw new WebSubClientException(WebSubClientErrorCode.UNREGISTER_ERROR.getErrorCode(),
					WebSubClientErrorCode.UNREGISTER_ERROR.getErrorMessage() + exception.getResponseBodyAsString());
		}
		if (response.getStatusCode() == HttpStatus.ACCEPTED) {
			LOGGER.info("topic {} unregistered at hub", topic);
		}
		else if (response.getStatusCode() == HttpStatus.OK) {
			HubResponse hubResponse = ParseUtil.parseHubResponse(response.getBody());
			if (hubResponse.getHubResult().equals("accepted")) {
				LOGGER.info("topic {} unregistered at hub", topic);
			}
			else {
				LOGGER.error(WebSubClientErrorCode.UNREGISTER_ERROR.getErrorMessage() + response.getBody());
				throw new WebSubClientException(WebSubClientErrorCode.UNREGISTER_ERROR.getErrorCode(),
						WebSubClientErrorCode.UNREGISTER_ERROR.getErrorMessage() + hubResponse.getErrorReason());
			}
		}
		else {
			throw new WebSubClientException(WebSubClientErrorCode.UNREGISTER_ERROR.getErrorCode(),
					WebSubClientErrorCode.UNREGISTER_ERROR.getErrorMessage() + response.getBody());
		}
	}

	@Override
	public void publishUpdate(String topic, P payload, String contentType, HttpHeaders headers, String hubURL) {
		HttpHeaders requestHeaders = headers != null ? headers : new HttpHeaders();
		requestHeaders.setContentType(MediaType.parseMediaType(contentType));

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(hubURL)
				.queryParam(WebSubClientConstants.HUB_MODE, HubMode.PUBLISH.gethubModeValue())
				.queryParam(WebSubClientConstants.HUB_TOPIC, topic);

		HttpEntity<P> entity = new HttpEntity<>(payload, requestHeaders);
		ResponseEntity<String> response;
		try {
			response = restTemplateHelper.getRestTemplate().exchange(builder.toUriString(), HttpMethod.POST, entity,
					String.class);
		}
		catch (HttpClientErrorException | HttpServerErrorException exception) {
			throw new WebSubClientException(WebSubClientErrorCode.PUBLISH_ERROR.getErrorCode(),
					WebSubClientErrorCode.PUBLISH_ERROR.getErrorMessage() + exception.getResponseBodyAsString());
		}
		if (response.getStatusCode() == HttpStatus.ACCEPTED) {
			LOGGER.info("published topic {} update at hub", topic);
		}
		else if (response.getStatusCode() == HttpStatus.OK) {
			HubResponse hubResponse = ParseUtil.parseHubResponse(response.getBody());
			if (hubResponse.getHubResult().equals("accepted")) {
				LOGGER.info("published topic {} update at hub", topic);
			}
			else {
				LOGGER.error(WebSubClientErrorCode.PUBLISH_ERROR.getErrorMessage() + response.getBody());
				throw new WebSubClientException(WebSubClientErrorCode.PUBLISH_ERROR.getErrorCode(),
						WebSubClientErrorCode.PUBLISH_ERROR.getErrorMessage() + hubResponse.getErrorReason());
			}
		}
		else {
			throw new WebSubClientException(WebSubClientErrorCode.PUBLISH_ERROR.getErrorCode(),
					WebSubClientErrorCode.PUBLISH_ERROR.getErrorMessage() + response.getBody());
		}
	}

	@Override
	public void notifyUpdate(String topic, HttpHeaders headers, String hubURL) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(hubURL)
				.queryParam(WebSubClientConstants.HUB_MODE, HubMode.PUBLISH.gethubModeValue())
				.queryParam(WebSubClientConstants.HUB_TOPIC, topic);

		HttpEntity<Void> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response;
		try {
			response = restTemplateHelper.getRestTemplate().exchange(builder.toUriString(), HttpMethod.POST, entity,
					String.class);
		}
		catch (HttpClientErrorException | HttpServerErrorException exception) {
			throw new WebSubClientException(WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorCode(),
					WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorMessage() + exception.getResponseBodyAsString());
		}
		if (response.getStatusCode() == HttpStatus.ACCEPTED) {
			LOGGER.info("notify topic {} update at hub", topic);
		}
		else if (response.getStatusCode() == HttpStatus.OK) {
			HubResponse hubResponse = ParseUtil.parseHubResponse(response.getBody());
			if (hubResponse.getHubResult().equals("accepted")) {
				LOGGER.info("notify topic {} update at hub", topic);
			}
			else {
				LOGGER.error(WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorMessage() + response.getBody());
				throw new WebSubClientException(WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorCode(),
						WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorMessage() + hubResponse.getErrorReason());
			}
		}
		else {
			throw new WebSubClientException(WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorCode(),
					WebSubClientErrorCode.NOTIFY_UPDATE_ERROR.getErrorMessage() + response.getBody());
		}
	}

	private static boolean isTopicAlreadyRegistered(String hubResponse) {
		return hubResponse != null && hubResponse.toLowerCase().contains("already registered");
	}

	private static void logRegisterFailure(String topic, String hubURL, String hubResponse, int httpStatus) {
		String reason = hubResponse != null ? hubResponse : "<empty hub response>";
		LOGGER.error("WebSub topic registration denied: topic={}, hubUrl={}, httpStatus={}, hubResponse={}",
				topic, hubURL, httpStatus, reason);
		if (isPublisherNotAuthorized(reason)) {
			LOGGER.error(
					"WebSub publisher not authorized for topic={}. Verify websub.publish.url, IAM client credentials "
							+ "(mosip.iam.adapter.clientid / clientsecret), and hub publisher ACL for publisher id ID_REPO",
					topic);
		}
	}

	private static boolean isPublisherNotAuthorized(String hubResponse) {
		return hubResponse != null && hubResponse.toLowerCase().contains("publisher is not authorized");
	}

}
