package io.mosip.idrepository.identity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;

@Configuration
public class ObjectMapperConfig {
	
	@Autowired
	private ObjectMapper mapper;

	@PostConstruct
	public void init() {
		mapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
	}

}
