package io.mosip.kernel.saltgenerator.step.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;
import io.mosip.idrepository.saltgenerator.repository.idmap.VidEncryptSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idmap.VidHashSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idrepo.IdentityEncryptSaltRepository;
import io.mosip.idrepository.saltgenerator.repository.idrepo.IdentityHashSaltRepository;
import io.mosip.idrepository.saltgenerator.step.SaltWriter;
import org.springframework.batch.item.Chunk;

@RunWith(MockitoJUnitRunner.class)
public class SaltWriterTest {

	@InjectMocks
	SaltWriter writer;

	@Mock
	private IdentityHashSaltRepository identityHashSaltRepo;

	@Mock
	private VidHashSaltRepository vidHashSaltRepo;

	@Mock
	private IdentityEncryptSaltRepository identityEncryptSaltRepo;

	@Mock
	private VidEncryptSaltRepository vidEncryptSaltRepo;

	@SuppressWarnings("unchecked")
	@Test
	public void testWriter() throws Exception {
		identityHashSaltRepo.countByIdIn(Mockito.any());
		IdRepoSaltEntitiesComposite idRepoSaltEntitiesComposite = new IdRepoSaltEntitiesComposite();
		IdentityHashSaltEntity entity = new IdentityHashSaltEntity();
		entity.setId(1l);
		VidHashSaltEntity vidEntity = new VidHashSaltEntity();
		entity.setId(1l);
		IdentityEncryptSaltEntity idEncryptEntity = new IdentityEncryptSaltEntity();
		entity.setId(1l);
		VidEncryptSaltEntity vidEncryptEntity = new VidEncryptSaltEntity();
		entity.setId(1l);
		idRepoSaltEntitiesComposite.setIdentityHashSaltEntity(entity);
		idRepoSaltEntitiesComposite.setVidHashSaltEntity(vidEntity);
		idRepoSaltEntitiesComposite.setIdentityEncryptSaltEntity(idEncryptEntity);
		idRepoSaltEntitiesComposite.setVidEncryptSaltEntity(vidEncryptEntity);
		writer.write(Chunk.of(idRepoSaltEntitiesComposite));
		ArgumentCaptor<List<IdentityEncryptSaltEntity>> argCapture = ArgumentCaptor.forClass(List.class);
		verify(identityEncryptSaltRepo).saveAll(argCapture.capture());
		assertEquals(new IdentityEncryptSaltEntity(), argCapture.getValue().get(0));
	}

	@Test
	public void testWriterRecordExists() throws Exception {
		identityHashSaltRepo.countByIdIn(Mockito.any());
		identityEncryptSaltRepo.countByIdIn(Mockito.any());
		IdRepoSaltEntitiesComposite idRepoSaltEntitiesComposite = new IdRepoSaltEntitiesComposite();
		IdentityHashSaltEntity entity = new IdentityHashSaltEntity();
		entity.setId(1l);
		IdentityEncryptSaltEntity identityEncryptSaltEntity = new IdentityEncryptSaltEntity();
		idRepoSaltEntitiesComposite.setIdentityEncryptSaltEntity(identityEncryptSaltEntity);
		idRepoSaltEntitiesComposite.setIdentityHashSaltEntity(entity);
		VidHashSaltEntity vidHashSaltEntity = new VidHashSaltEntity();
		idRepoSaltEntitiesComposite.setVidHashSaltEntity(vidHashSaltEntity);
		VidEncryptSaltEntity vidEncryptSaltEntity = new VidEncryptSaltEntity();
		idRepoSaltEntitiesComposite.setVidEncryptSaltEntity(vidEncryptSaltEntity);
		when(identityHashSaltRepo.countByIdIn(Mockito.any())).thenReturn(1l);
		writer.write(Chunk.of(idRepoSaltEntitiesComposite));
	}

}