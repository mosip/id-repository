package io.mosip.idrepository.saltgenerator.step;

import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseLogger implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Connected to Database: " + dataSource.getConnection().getMetaData().getURL());
    }
}

