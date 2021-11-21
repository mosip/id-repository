package io.mosip.credential.request.generator.api;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class AfterAppStart implements ApplicationListener<ApplicationReadyEvent> {
	
	@Autowired
	private DataSource dataSource;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
	}

}
