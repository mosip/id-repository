package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class Event {
	//uuid event id to be create and put in loggers
	private String id;
	
	//request id
	private String transactionId;
	
	private Type type;
	
	private String timestamp;
	
	private String dataShareUri;
	
	private Map<String,Object> data;
	
}
