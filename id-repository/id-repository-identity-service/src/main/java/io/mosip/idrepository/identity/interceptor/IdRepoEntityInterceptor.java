package io.mosip.idrepository.identity.interceptor;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.IDENTITY_HASH_MISMATCH;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.mosip.idrepository.core.entity.HandleInfo;
import io.mosip.idrepository.core.entity.UinInfo;
import io.mosip.idrepository.identity.entity.*;
import org.apache.commons.codec.binary.StringUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * The Class IdRepoEntityInterceptor - Interceptor for repository calls and
 * allows to update/modify the entity data.
 *
 * @author Manoj SP
 */
@Component
public class IdRepoEntityInterceptor extends EmptyInterceptor {

	private static final String UIN = "uin";
	private static final String HANDLE = "handle";

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Value("${" + UIN_DATA_REFID + "}")
	private String uinDataRefId;

	/** The Constant ID_REPO_ENTITY_INTERCEPTOR. */
	private static final String ID_REPO_ENTITY_INTERCEPTOR = "IdRepoEntityInterceptor";

	/** The Constant UIN_DATA_HASH. */
	private static final String UIN_DATA_HASH = "uinDataHash";

	/** The Constant UIN_DATA. */
	private static final String UIN_DATA = "uinData";

	/** The mosip logger. */
	private transient Logger mosipLogger = IdRepoLogger.getLogger(IdRepoEntityInterceptor.class);

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985336846122302850L;

	/** The security manager. */
	@Autowired
	private transient IdRepoSecurityManager securityManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object,
	 * java.io.Serializable, java.lang.Object[], java.lang.String[],
	 * org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		try {
			List<String> propertyNamesList = Arrays.asList(propertyNames);
			if (entity instanceof HandleInfo) {
				encryptDataOnSave(id, state, propertyNamesList, types, (HandleInfo) entity);
			}
			else if (entity instanceof UinInfo) {
				encryptDataOnSave(id, state, propertyNamesList, types, (UinInfo) entity);
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_ENTITY_INTERCEPTOR, "onSave", "\n" + e.getMessage());
			throw new IdRepoAppUncheckedException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
		return super.onSave(entity, id, state, propertyNames, types);
	}

	private <T extends UinInfo> void encryptDataOnSave(Serializable id, Object[] state, List<String> propertyNamesList,
			Type[] types, T entity) throws IdRepoAppException {
		if (Objects.nonNull(entity.getUinData())) {
			byte[] encryptedData = securityManager.encrypt(entity.getUinData(), uinDataRefId);
			entity.setUinData(encryptedData);
			int indexOfData = propertyNamesList.indexOf(UIN_DATA);
			state[indexOfData] = encryptedData;
		}

		if (Objects.nonNull(entity.getUin()) && !(entity instanceof UinHistory)) {
			List<String> uinList = Arrays.asList(entity.getUin().split(SPLITTER));
			byte[] encryptedUinByteWithSalt = securityManager.encryptWithSalt(uinList.get(1).getBytes(),
					CryptoUtil.decodePlainBase64(uinList.get(2)), uinRefId);
			String encryptedUinWithSalt = uinList.get(0) + SPLITTER + new String(encryptedUinByteWithSalt);
			entity.setUin(encryptedUinWithSalt);
			int indexOfUin = propertyNamesList.indexOf(UIN);
			state[indexOfUin] = encryptedUinWithSalt;
		}

		if ((entity instanceof HandleInfo)) {
			List<String> parts = Arrays.asList(((HandleInfo) entity).getHandle().split(SPLITTER));
			byte[] encryptedHandleByteWithSalt = securityManager.encryptWithSalt(parts.get(1).getBytes(),
					CryptoUtil.decodePlainBase64(parts.get(2)), uinRefId);
			String encryptedHandleWithSalt = parts.get(0) + SPLITTER + new String(encryptedHandleByteWithSalt);
			((HandleInfo) entity).setHandle(encryptedHandleWithSalt);
			int indexOfUin = propertyNamesList.indexOf(HANDLE);
			state[indexOfUin] = encryptedHandleWithSalt;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.EmptyInterceptor#onLoad(java.lang.Object,
	 * java.io.Serializable, java.lang.Object[], java.lang.String[],
	 * org.hibernate.type.Type[])
	 */
	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		try {
			List<String> propertyNamesList = Arrays.asList(propertyNames);
			if (entity instanceof Uin || entity instanceof UinHistory || entity instanceof UinDraft) {
				int indexOfData = propertyNamesList.indexOf(UIN_DATA);
				if (Objects.nonNull(state[indexOfData])) {
					state[indexOfData] = securityManager.decrypt((byte[]) state[indexOfData], uinDataRefId);

					if (!StringUtils.equals(securityManager.hash((byte[]) state[indexOfData]),
							(String) state[propertyNamesList.indexOf(UIN_DATA_HASH)])) {
						throw new IdRepoAppUncheckedException(IDENTITY_HASH_MISMATCH);
					}
				}
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_ENTITY_INTERCEPTOR, "onLoad", "\n" + e.getMessage());
			throw new IdRepoAppUncheckedException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
		return super.onLoad(entity, id, state, propertyNames, types);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object,
	 * java.io.Serializable, java.lang.Object[], java.lang.Object[],
	 * java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		try {
			if (entity instanceof UinInfo) {
				UinInfo uinEntity = (UinInfo) entity;
				if (Objects.nonNull(uinEntity.getUinData())) {
					return encryptOnDirtyFlush(id, currentState, previousState, propertyNames, types, uinEntity);
				}
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_ENTITY_INTERCEPTOR, "onSave", "\n" + e.getMessage());
			throw new IdRepoAppUncheckedException(ENCRYPTION_DECRYPTION_FAILED, e);
		}
		return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}

	private <T extends UinInfo> boolean encryptOnDirtyFlush(Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types, T uinEntity) throws IdRepoAppException {
		byte[] encryptedData = securityManager.encrypt(uinEntity.getUinData(), uinDataRefId);
		List<String> propertyNamesList = Arrays.asList(propertyNames);
		int indexOfData = propertyNamesList.indexOf(UIN_DATA);
		currentState[indexOfData] = encryptedData;
		return super.onFlushDirty(uinEntity, id, currentState, previousState, propertyNames, types);
	}
}
