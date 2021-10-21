package io.mosip.credential.request.generator.batch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;


/**
 * The Class SchedulingConfigurerConfiguration.
 */
	@Configuration
	public class SchedulingConfigurerConfiguration implements SchedulingConfigurer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.scheduling.annotation.SchedulingConfigurer#configureTasks
	 * (org.springframework.scheduling.config.ScheduledTaskRegistrar) This will
	 * config 2 schedular to run
	 */
	@Override
	    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
	        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(2);
	        taskScheduler.initialize();
	        taskRegistrar.setTaskScheduler(taskScheduler);
	    }

}