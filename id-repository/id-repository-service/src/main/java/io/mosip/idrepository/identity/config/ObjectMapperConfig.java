package io.mosip.idrepository.identity.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.kernel.core.http.RequestWrapper;
import jakarta.annotation.PostConstruct;

/**
 * Registers Jackson modules on the shared application {@link ObjectMapper}.
 * <p>
 * Enables {@code Optional} and Java 8 date/time types for identity JSON serialization.
 * </p>
 */
@Configuration
public class ObjectMapperConfig {

	/** Application-wide Jackson mapper bean. */
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Adds {@link Jdk8Module} and {@link JavaTimeModule} after bean construction.
	 */
	@PostConstruct
	/**
	 * Init.
	 */
	public void init() {
		mapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
		mapper.addMixIn(RequestWrapper.class, RequestWrapperDateTimeMixin.class);
	}

	/** MOSIP envelope {@code requesttime} must end with literal {@code Z}. */
	private abstract static class RequestWrapperDateTimeMixin {
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		abstract LocalDateTime getRequesttime();
	}

}