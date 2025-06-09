package io.mosip.idrepository.saltgenerator.service;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${mosip.idrepo.identity.db.url}")
    private String identityJdbcUrl;

    @Value("${mosip.idrepo.identity.db.username}")
    private String identityUserName;

    @Value("${mosip.idrepo.identity.db.password}")
    private String identityPassword;

    @Value("${mosip.idrepo.identity.db.driverClassName}")
    private String identityDriverClassName;

    @Value("${mosip.idrepo.vid.db.url}")
    private String vidJdbcUrl;

    @Value("${mosip.idrepo.vid.db.username}")
    private String vidUserName;

    @Value("${mosip.idrepo.vid.db.password}")
    private String vidPassword;

    @Value("${mosip.idrepo.vid.db.driverClassName}")
    private String vidDriverClassName;

    @Bean
    public DataSource primaryDataSource() {

        return DataSourceBuilder.create()
                .url(identityJdbcUrl)
                .username(identityUserName)
                .password(identityPassword)
                .driverClassName(identityDriverClassName)
                .build();
    }

    @Bean
    public DataSource secondaryDataSource() {

        return DataSourceBuilder.create()
                .url(vidJdbcUrl)
                .username(vidUserName)
                .password(vidPassword)
                .driverClassName(vidDriverClassName)
                .build();
    }


    @Bean
    @Primary
    public DataSource dataSource() {
        Map<Object, Object> targetDatasources = new HashMap<Object, Object>();
        targetDatasources.put(Database.SECONDARY, secondaryDataSource());
        targetDatasources.put(Database.PRIMARY, primaryDataSource());
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(primaryDataSource());
        routingDataSource.setTargetDataSources(targetDatasources);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

}