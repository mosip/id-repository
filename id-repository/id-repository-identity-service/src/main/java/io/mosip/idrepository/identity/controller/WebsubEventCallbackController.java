package io.mosip.idrepository.identity.controller;

import static io.mosip.idrepository.core.constant.IdRepoConstants.REMOVE_ID_STATUS_EVENT_CALLBACK_RELATIVE_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.REMOVE_ID_STATUS_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.REMOVE_ID_STATUS_EVENT_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_TOPIC;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * @author Manoj SP
 *
 */
@RestController
@Tag(name = "websub-event-callback-controller", description = "Websub Event Callback Controller")
public class WebsubEventCallbackController {

	@Autowired
	private CredentialStatusManager statusManager;

	@Autowired
	private ObjectMapper mapper;

	@SuppressWarnings("unchecked")
	@PostMapping(path = "/callback/vid_credential_status_update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "handleVidEvent", description = "handleVidEvent", tags = { "websub-event-callback-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + VID_EVENT_SECRET
			+ "}", callback = "/idrepository/v1/identity/callback/vid_credential_status_update", topic = "${" + VID_EVENT_TOPIC + "}")
	public void handleVidEvent(@RequestBody EventModel eventModel) {
		if (((String) eventModel.getEvent().getData().get("status")).contentEquals(EnvUtil.getVidActiveStatus())) {
			statusManager.credentialRequestResponseConsumer(
					mapper.convertValue(eventModel.getEvent().getData().get("request"), CredentialIssueRequestWrapperDto.class),
					mapper.convertValue(eventModel.getEvent().getData().get("response"), Map.class));
		} else {
			statusManager.idaEventConsumer(mapper.convertValue(eventModel.getEvent().getData().get("idaEvent"), EventModel.class));
		}
	}

	@PostMapping(path = "/callback/remove_id_status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "handleRemoveIdStatusEvent", description = "handleRemoveIdStatusEvent", tags = {
			"websub-event-callback-controller" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully"),
			@ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + REMOVE_ID_STATUS_EVENT_SECRET + "}", callback = "${"
			+ REMOVE_ID_STATUS_EVENT_CALLBACK_RELATIVE_URL + "}", topic = "${" + REMOVE_ID_STATUS_EVENT_TOPIC + "}")
	public void handleRemoveIdStatusEvent(@RequestBody EventModel eventModel) {
		statusManager.handleRemoveIdStatusEvent(eventModel);
	}
}
