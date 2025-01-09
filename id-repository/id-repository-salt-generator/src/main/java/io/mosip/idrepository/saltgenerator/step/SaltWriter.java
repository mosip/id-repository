package io.mosip.idrepository.saltgenerator.step;

import java.util.stream.Collectors;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.transaction.Transactional;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;
import io.mosip.idrepository.saltgenerator.logger.SaltGeneratorLogger;
import io.mosip.idrepository.saltgenerator.repository.idmap.VidEncryptSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idmap.VidHashSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idrepo.IdentityEncryptSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idrepo.IdentityHashSaltRepository;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.batch.item.Chunk;

/**
 * The Class SaltWriter - Class to write salt entities to DB in batch.
 * Implements {@code ItemWriter}.
 *
 * @author Manoj SP
 */
@Component
public class SaltWriter implements ItemWriter<IdRepoSaltEntitiesComposite> {

	Logger mosipLogger = SaltGeneratorLogger.getLogger(SaltWriter.class);

	@Autowired
	private IdentityHashSaltRepository identityHashSaltRepo;
	
	@Autowired
	private VidHashSaltRepository vidHashSaltRepo;
	
	@Autowired
	private IdentityEncryptSaltRepository identityEncryptSaltRepo;
	
	@Autowired
	private VidEncryptSaltRepository vidEncryptSaltRepo;

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Bean
	@Transactional
	public void write(Chunk<? extends IdRepoSaltEntitiesComposite> entitiesCompositeList) throws Exception {
		if (identityHashSaltRepo.countByIdIn(StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(entities -> entities.getIdentityHashSaltEntity().getId()).collect(Collectors.toList())) == 0l
				&& vidHashSaltRepo.countByIdIn(StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(entities -> entities.getVidHashSaltEntity().getId()).collect(Collectors.toList())) == 0l
				&& identityEncryptSaltRepo.countByIdIn(StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(entities -> entities.getIdentityEncryptSaltEntity().getId()).collect(Collectors.toList())) == 0l
				&& vidEncryptSaltRepo.countByIdIn(StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(entities -> entities.getVidEncryptSaltEntity().getId()).collect(Collectors.toList())) == 0l) {

			List<IdentityHashSaltEntity> idRepoHashSaltEntitiesList = StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(IdRepoSaltEntitiesComposite::getIdentityHashSaltEntity).collect(Collectors.toList());
			identityHashSaltRepo.saveAll(idRepoHashSaltEntitiesList);
			mosipLogger.debug("SALT_GENERATOR", "SaltWriter", "IdRepo Hash Salt Entities written", String.valueOf(idRepoHashSaltEntitiesList.size()));

			List<VidHashSaltEntity> vidHashSaltEntitiesList = StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(IdRepoSaltEntitiesComposite::getVidHashSaltEntity).collect(Collectors.toList());
			vidHashSaltRepo.saveAll(vidHashSaltEntitiesList);
			mosipLogger.debug("SALT_GENERATOR", "SaltWriter", "IdMap Hash Salt Entities Entities written", String.valueOf(vidHashSaltEntitiesList.size()));

			List<IdentityEncryptSaltEntity> idRepoEncryptSaltEntitiesList = StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(IdRepoSaltEntitiesComposite::getIdentityEncryptSaltEntity).collect(Collectors.toList());
			identityEncryptSaltRepo.saveAll(idRepoEncryptSaltEntitiesList);
			mosipLogger.debug("SALT_GENERATOR", "SaltWriter", "IdRepo Encrypt Salt Entities written", String.valueOf(idRepoEncryptSaltEntitiesList.size()));

			List<VidEncryptSaltEntity> vidEncryptSaltEntitiesList = StreamSupport.stream(entitiesCompositeList.spliterator(), true).map(IdRepoSaltEntitiesComposite::getVidEncryptSaltEntity).collect(Collectors.toList());
			vidEncryptSaltRepo.saveAll(vidEncryptSaltEntitiesList);
			mosipLogger.debug("SALT_GENERATOR", "SaltWriter", "IdMap Encrypt Salt Entities written", String.valueOf(vidEncryptSaltEntitiesList.size()));

		} else {
			mosipLogger.error("SALT_GENERATOR", "SaltWriter", "write", "Records already exists in IdRepo/Vid Salt Table");
		}
		
	}
}
