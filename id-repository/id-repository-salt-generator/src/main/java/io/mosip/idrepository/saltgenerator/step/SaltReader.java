package io.mosip.idrepository.saltgenerator.step;

import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.END_SEQ;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.START_SEQ;

import java.time.LocalDateTime;

import javax.annotation.PostConstruct;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idmap.VidHashSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityEncryptSaltEntity;
import io.mosip.idrepository.saltgenerator.entity.idrepo.IdentityHashSaltEntity;
import io.mosip.idrepository.saltgenerator.logger.SaltGeneratorLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * The Class SaltReader - Creates entities based on chunk size.
 * Start and end sequence for entity Id is provide via configuration.
 * Implements {@code ItemReader}.
 * Salt is provided by {@code HMACUtils.generateSalt()}
 *
 * @author Manoj SP
 */
@Component
public class SaltReader implements ItemReader<IdRepoSaltEntitiesComposite> {
	
	/** The mosip logger. */
	Logger mosipLogger = SaltGeneratorLogger.getLogger(SaltReader.class);

	/** The start seq. */
	private Long startSeq;

	/** The end seq. */
	private Long endSeq;

	/** The env. */
	@Autowired
	private Environment env;

	@PostConstruct
	public void initialize() {
		startSeq = env.getProperty(START_SEQ.getValue(), Long.class);
		endSeq = env.getProperty(END_SEQ.getValue(), Long.class);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	@Override
	public IdRepoSaltEntitiesComposite read() {
		if (startSeq <= endSeq) {
			String idHshSalt = CryptoUtil.encodeBase64String(HMACUtils2.generateSalt());
			String vidHshSalt = idHshSalt;
			LocalDateTime currentDateTime = DateUtils.getUTCCurrentDateTime();
			
			IdentityHashSaltEntity identityHashSalt = new IdentityHashSaltEntity();
			identityHashSalt.setId(startSeq);
			identityHashSalt.setSalt(idHshSalt);
			identityHashSalt.setCreatedBy("System");
			identityHashSalt.setCreateDtimes(currentDateTime);
			
			VidHashSaltEntity vidHashSalt = new VidHashSaltEntity();
			vidHashSalt.setId(startSeq);
			vidHashSalt.setSalt(vidHshSalt);
			vidHashSalt.setCreatedBy("System");
			vidHashSalt.setCreateDtimes(currentDateTime);
			
			String idEncSalt = CryptoUtil.encodeBase64String(HMACUtils2.generateSalt());

			IdentityEncryptSaltEntity identityEncryptSalt = new IdentityEncryptSaltEntity();
			identityEncryptSalt.setId(startSeq);
			identityEncryptSalt.setSalt(idEncSalt);
			identityEncryptSalt.setCreatedBy("System");
			identityEncryptSalt.setCreateDtimes(currentDateTime);
			
			String vidEncSalt = CryptoUtil.encodeBase64String(HMACUtils2.generateSalt());
			
			VidEncryptSaltEntity vidEncryptSalt = new VidEncryptSaltEntity();
			vidEncryptSalt.setId(startSeq);
			vidEncryptSalt.setSalt(vidEncSalt);
			vidEncryptSalt.setCreatedBy("System");
			vidEncryptSalt.setCreateDtimes(currentDateTime);
			
			mosipLogger.debug("SALT_GENERATOR", "SaltReader", "Entity with id created - ",
					String.valueOf(startSeq));
			startSeq = startSeq + 1;
			return new IdRepoSaltEntitiesComposite(identityHashSalt, vidHashSalt, identityEncryptSalt, vidEncryptSalt );
		} else {
			return null;
		}
	}

}
