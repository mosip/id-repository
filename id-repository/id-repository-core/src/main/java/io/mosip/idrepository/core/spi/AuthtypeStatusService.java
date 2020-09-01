package io.mosip.idrepository.core.spi;

import java.util.List;

import org.springframework.stereotype.Service;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * The Interface AuthtypeStatusService - Service to check whether the
 * Auth type requested in Locked/Unlocked for authentication.
 *
 * @author Manoj SP
 */
@Service
public interface AuthtypeStatusService {
	
	/**
	 * Fetch authtype status.
	 *
	 * @param individualId the individual id
	 * @param individualIdType the individual id type
	 * @return the list
	 * @throws IdAuthenticationBusinessException the id authentication business exception
	 */
	public List<AuthtypeStatus> fetchAuthTypeStatus(String individualId, IdType idType) throws IdRepoAppException;

	public IdResponseDTO updateAuthTypeStatus(String individualId, IdType idType, List<AuthtypeStatus> authTypeStatus)
			throws IdRepoAppException;
	
}
