package io.mosip.idrepository.saltgenerator.entity;

import io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdRepoSaltEntitiesComposite {
	
	private IdentityHashSaltEntity identityHashSaltEntity;
	
	private VidHashSaltEntity vidHashSaltEntity;
	
	private IdentityEncryptSaltEntity identityEncryptSaltEntity;
	
	private VidEncryptSaltEntity vidEncryptSaltEntity;

}
