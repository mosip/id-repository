package io.mosip.idrepository.saltgenerator.service;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/*
@author kamesh Shekhar Prasad
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DatabaseThreadContext.getCurrentDatabase();
    }

}
