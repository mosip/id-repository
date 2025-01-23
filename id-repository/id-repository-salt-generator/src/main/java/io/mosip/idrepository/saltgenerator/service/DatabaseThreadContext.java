package io.mosip.idrepository.saltgenerator.service;

public class DatabaseThreadContext {

    private static final ThreadLocal<Database> current = new ThreadLocal<>();

    public static void setCurrentDatabase(Database database) {
        current.set(database);
    }

    public static Object getCurrentDatabase() {
        return current.get();
    }

}