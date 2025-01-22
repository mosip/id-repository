package io.mosip.idrepository.saltgenerator.service;

import io.mosip.idrepository.saltgenerator.constant.DatabaseType;
import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;
import io.mosip.idrepository.saltgenerator.step.SaltReader;
import io.mosip.idrepository.saltgenerator.step.SaltWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class SaltGenerator {

    @Autowired
    private SaltReader saltReader;

    @Autowired
    private SaltWriter saltWriter;

    @Transactional
    public void start() throws Exception {
        System.out.println("Starting Salt generator");

        List<IdRepoSaltEntitiesComposite> entitiesList = new ArrayList<>();
        DatabaseContextHolder.set(DatabaseType.IDENTITY);
        // Read records using SaltReader
        IdRepoSaltEntitiesComposite entity;
        while ((entity = saltReader.read()) != null) {
            entitiesList.add(entity);
        }

        if (!entitiesList.isEmpty()) {
            // Write records using SaltWriter
            saltWriter.write(new Chunk<>(entitiesList));
            System.out.println("Salt data successfully processed.");
        } else {
            System.out.println("No salt data found for processing.");
        }

        System.out.println("ReEncryption Utility Completed.");
    }
}
