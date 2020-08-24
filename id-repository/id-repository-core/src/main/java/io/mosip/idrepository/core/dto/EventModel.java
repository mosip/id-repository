package io.mosip.idrepository.core.dto;

import lombok.Data;

@Data
public class EventModel {

	private String publisher;
	private String topic;
	private String published_on;
	private Event event;
}
