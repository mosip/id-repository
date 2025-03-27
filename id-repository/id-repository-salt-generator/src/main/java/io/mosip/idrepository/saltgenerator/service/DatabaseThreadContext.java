package io.mosip.idrepository.saltgenerator.service;

/*
@author kamesh Shekhar Prasad
 */
public class DatabaseThreadContext {

    private static final ThreadLocal<Database> current = new ThreadLocal<>();

    public static void setCurrentDatabase(Database database) {
        current.set(database);
    }

    public static Object getCurrentDatabase() {
        return current.get();
    }

    public static void clear(){
        current.remove();
    }

}