package io.mosip.idrepository.saltgenerator.service;

import io.mosip.idrepository.saltgenerator.constant.DatabaseType;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/*
@author Kamesh Shekhar Prasad
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DatabaseType databaseType = DatabaseContextHolder.get();
        System.out.println("Switching to Database: " + databaseType);
        return databaseType;
    }
}


