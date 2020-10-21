package io.mosip.credential.request.generator.dto;

import io.mosip.credential.request.generator.dto.Event;

import lombok.Data;
@Data
public class CredentialStatusEvent {
	private String publisher;
	private String topic;
	private String publishedOn;
	private Event event;
}
