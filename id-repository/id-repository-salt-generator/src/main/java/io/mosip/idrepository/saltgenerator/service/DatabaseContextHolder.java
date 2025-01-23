package io.mosip.idrepository.saltgenerator.service;

import io.mosip.idrepository.saltgenerator.constant.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseContextHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseContextHolder.class);
    private static final ThreadLocal<DatabaseType> current = new ThreadLocal<>();

    public static void setCurrentDatabase(DatabaseType database) {
        LOGGER.info("🔄 Switching Database to: {}", database);
        current.set(database);
    }

    public static DatabaseType getCurrentDatabase() {
        DatabaseType db = current.get();
        LOGGER.info("📌 Current Database: {}", db);
        return db;
    }

    public static void clear() {
        LOGGER.info("❌ Clearing Database Context");
        current.remove();
    }
}

