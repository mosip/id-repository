package io.mosip.credential.request.generator.dto;

import lombok.Data;

@Data
public class Event {

		private String id;
	
		private String requestId;
		
		private String timestamp;
		
		private String status;
		
		private String url;
}
