package io.mosip.idrepository.saltgenerator.service;

/*
@author Kamesh Shekhar Prasad
 */
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DatabaseContextHolder.get();
    }
}

