package io.mosip.idrepository.saltgenerator.service;

import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;
import io.mosip.idrepository.saltgenerator.logger.SaltGeneratorLogger;
import io.mosip.idrepository.saltgenerator.step.SaltReader;
import io.mosip.idrepository.saltgenerator.step.SaltWriter;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
@author kamesh Shekhar Prasad
 */
@Component
public class SaltGenerator {

    Logger mosipLogger = SaltGeneratorLogger.getLogger(SaltGenerator.class);

    @Autowired
    private SaltReader saltReader;

    @Autowired
    private SaltWriter saltWriter;

    public void start() throws Exception {
        mosipLogger.info("Starting Salt generator");
        DatabaseThreadContext.setCurrentDatabase(Database.PRIMARY);
        List<IdRepoSaltEntitiesComposite> entitiesList = new ArrayList<>();
        // Read records using SaltReader
        IdRepoSaltEntitiesComposite entity;
        while ((entity = saltReader.read()) != null) {
            entitiesList.add(entity);
        }

        if (!entitiesList.isEmpty()) {
            // Write records using SaltWriter
            saltWriter.write(new Chunk<>(entitiesList));
            mosipLogger.info("Salt data successfully processed.");
        } else {
            mosipLogger.info("No salt data found for processing.");
        }

        mosipLogger.info("Salt generation job Completed.");
    }
}
