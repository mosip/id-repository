package io.mosip.idrepository.core.builder;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * @author Manoj SP
 *
 */
//@Component
//@ConditionalOnBean(name = { "idRepoDataSource" })
public class CredentialRequestStatusInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = -8634328573011711898L;

	private transient Logger mosipLogger = IdRepoLogger.getLogger(CredentialRequestStatusInterceptor.class);

	@Value("${mosip.idrepo.crypto.refId.uin}")
	private String uinRefId;

	@Autowired
	private transient IdRepoSecurityManager securityManager;

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		try {
			if (entity instanceof CredentialRequestStatus) {
				CredentialRequestStatus credentialEntity = (CredentialRequestStatus) entity;
				List<String> idvIdList = Arrays.asList(credentialEntity.getIndividualId().split(SPLITTER));
				byte[] encryptedIdvIdByteWithSalt = securityManager.encryptWithSalt(idvIdList.get(1).getBytes(),
						CryptoUtil.decodeBase64(idvIdList.get(2)), uinRefId);
				String encryptedIdvIdWithSalt = idvIdList.get(0) + SPLITTER + new String(encryptedIdvIdByteWithSalt);
				credentialEntity.setIndividualId(encryptedIdvIdWithSalt);
				List<String> propertyNamesList = Arrays.asList(propertyNames);
				int indexOfIndividualId = propertyNamesList.indexOf("individualId");
				state[indexOfIndividualId] = encryptedIdvIdWithSalt;
				return super.onSave(credentialEntity, id, state, propertyNames, types);
			}
		} catch (IdRepoAppException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		try {
			if (entity instanceof CredentialRequestStatus) {
				if (entity instanceof CredentialRequestStatus) {
					CredentialRequestStatus credentialEntity = (CredentialRequestStatus) entity;
					List<String> propertyNamesList = Arrays.asList(propertyNames);
					int indexOfData = propertyNamesList.indexOf("individualId");
					state[indexOfData] = securityManager.decrypt((byte[]) state[indexOfData], uinRefId);
				}
			}
		} catch (IdRepoAppException e) {
			e.printStackTrace();
		}
		return super.onLoad(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		if (entity instanceof CredentialRequestStatus) {

		}
		return false;
	}
}
