package io.mosip.idrepository.saltgenerator.service;

import io.mosip.idrepository.saltgenerator.constant.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        DatabaseType dbType = DatabaseContextHolder.getCurrentDatabase();
        LOGGER.info("📌 Using DataSource: {}", dbType);
        return dbType;
    }
}

