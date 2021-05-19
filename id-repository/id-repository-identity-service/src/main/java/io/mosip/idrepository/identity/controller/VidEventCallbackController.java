package io.mosip.idrepository.identity.controller;

import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_TOPIC;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.manager.CredentialStatusManager;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Manoj SP
 *
 */
@RestController
public class VidEventCallbackController {

	@Autowired
	private CredentialStatusManager statusManager;

	@Autowired
	private Environment env;

	@Autowired
	private ObjectMapper mapper;

	@SuppressWarnings("unchecked")
	@PostMapping(path = "/callback/vid_credential_status_update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully") })
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + VID_EVENT_SECRET
			+ "}", callback = "/idrepository/v1/identity/callback/vid_credential_status_update", topic = "${" + VID_EVENT_TOPIC + "}")
	public void handleVidEvent(@RequestBody EventModel eventModel) {
		if (((String) eventModel.getEvent().getData().get("status")).contentEquals(env.getProperty(VID_ACTIVE_STATUS))) {
			statusManager.credentialRequestResponseHandler(
					mapper.convertValue(eventModel.getEvent().getData().get("request"), CredentialIssueRequestWrapperDto.class),
					mapper.convertValue(eventModel.getEvent().getData().get("response"), Map.class));
		} else {
			statusManager.idaEventHandler(mapper.convertValue(eventModel.getEvent().getData().get("idaEvent"), EventModel.class));
		}
	}
}
