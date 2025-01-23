package io.mosip.idrepository.saltgenerator.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/*
@author kamesh Shekhar Prasad
 */
@Configuration
public class DatabaseRouter {

    @Bean
    @ConfigurationProperties(prefix="datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix="datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        Map<Object, Object> targetDatasources = new HashMap<Object, Object>(){{
            put(Database.SECONDARY, secondaryDataSource());
            put(Database.PRIMARY, primaryDataSource());
        }};
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(primaryDataSource());
        routingDataSource.setTargetDataSources(targetDatasources);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

}