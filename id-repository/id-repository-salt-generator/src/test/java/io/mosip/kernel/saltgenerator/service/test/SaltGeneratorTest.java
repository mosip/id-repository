package io.mosip.kernel.saltgenerator.service.test;

import io.mosip.idrepository.saltgenerator.SaltGeneratorBootApplication;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;
import io.mosip.idrepository.saltgenerator.repository.idmap.VidEncryptSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idmap.VidHashSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idrepo.IdentityEncryptSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idrepo.IdentityHashSaltRepository;
import io.mosip.idrepository.saltgenerator.service.SaltGenerator;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
@author Kamesh Shekhar Prasad
 */
@SpringBootTest(classes = SaltGeneratorBootApplication.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")  // Uses application-test.properties
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.saltgenerator.repository")
@EntityScan(basePackages = "io.mosip.idrepository.saltgenerator.entity")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SaltGeneratorTest {

    @Autowired
    private SaltGenerator saltGenerator;

    @Autowired
    private IdentityHashSaltRepository identityHashSaltRepo;

    @Autowired
    private VidHashSaltRepository vidHashSaltRepo;

    @Autowired
    private IdentityEncryptSaltRepository identityEncryptSaltRepo;

    @Autowired
    private VidEncryptSaltRepository vidEncryptSaltRepo;

    @BeforeEach
    public void setup() {
        identityHashSaltRepo.deleteAll();
        vidHashSaltRepo.deleteAll();
        identityEncryptSaltRepo.deleteAll();
        vidEncryptSaltRepo.deleteAll();
    }

    @Test
    public void testSaltGeneration() throws Exception {
        // Run the salt generator process
        saltGenerator.start();

        // Wait briefly to avoid unique constraint issues due to timestamp conflicts
        Thread.sleep(10); // Small delay ensures unique timestamps in a fast loop

        // Fetch actual inserted record count
        long identityHashCount = identityHashSaltRepo.count();
        long vidHashCount = vidHashSaltRepo.count();
        long identityEncryptCount = identityEncryptSaltRepo.count();
        long vidEncryptCount = vidEncryptSaltRepo.count();

        // Verify expected record counts dynamically instead of assuming 10
        assertTrue(identityHashCount == 5000, "Expected records in identityHashSaltRepo");
        assertTrue(vidHashCount == 5000, "Expected records in vidHashSaltRepo");
        assertTrue(identityEncryptCount == 5000, "Expected records in identityEncryptSaltRepo");
        assertTrue(vidEncryptCount == 5000, "Expected records in vidEncryptSaltRepo");

        // Retrieve and validate a record
        Optional<IdentityHashSaltEntity> savedEntity = identityHashSaltRepo.findById(1L);
        assertTrue(savedEntity.isPresent(), "Record with ID 1 should be present");
        assertNotNull(savedEntity.get().getSalt(), "Salt should not be null");
    }

}
