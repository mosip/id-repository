package io.mosip.idrepository.saltgenerator.service;

import io.mosip.idrepository.saltgenerator.constant.DatabaseType;

/*
@author Kamesh Shekhar Prasad
 */
public class DatabaseContextHolder {
    private static final ThreadLocal<DatabaseType> CONTEXT = new ThreadLocal<>();

    public static void set(DatabaseType databaseType) {
        CONTEXT.set(databaseType);
    }

    public static DatabaseType get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

