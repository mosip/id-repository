package io.mosip.idrepository.core.spi;

import java.util.List;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * SPI for reading and updating per-individual authentication-type lock status.
 * <p>
 * Each {@link AuthtypeStatus} entry represents whether a specific auth factor
 * (demographic, biometric modality, etc.) is locked or unlocked for IDA
 * authentication requests. Lock state is persisted in {@code idrepo.authtype_lock}
 * and changes are published to IDA via WebSub.
 * </p>
 * <p>
 * <b>Implementor:</b> {@code AuthTypeStatusImpl} in {@code id-repository-service}.
 * </p>
 * <p>
 * <b>Caller:</b> identity REST controller ({@code IdRepoController}).
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.dto.AuthtypeStatus
 */
public interface AuthtypeStatusService {

	/**
	 * Fetches the current lock/unlock status for all auth types of an individual.
	 * <p>
	 * Resolves the individual via {@link IdType} (UIN, VID, etc.), loads rows from
	 * {@code authtype_lock}, and maps them to {@link AuthtypeStatus} DTOs for the
	 * caller.
	 * </p>
	 *
	 * @param individualId plain individual identifier (UIN, VID, etc.)
	 * @param idType       identifier type used for lookup and validation
	 * @return list of auth-type status entries (may be empty when no locks exist)
	 * @throws IdRepoAppException if the individual is not found or lookup fails
	 */
	List<AuthtypeStatus> fetchAuthTypeStatus(String individualId, IdType idType) throws IdRepoAppException;

	/**
	 * Updates lock/unlock status for one or more auth types of an individual.
	 * <p>
	 * Persists lock rows, applies optional unlock expiry timestamps, and notifies IDA
	 * through {@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper} when
	 * configured.
	 * </p>
	 *
	 * @param individualId   plain individual identifier
	 * @param idType         identifier type
	 * @param authTypeStatus list of auth-type statuses to apply
	 * @return identity response confirming the update
	 * @throws IdRepoAppException if validation fails or the individual is not found
	 */
	IdResponseDTO updateAuthTypeStatus(String individualId, IdType idType, List<AuthtypeStatus> authTypeStatus)
			throws IdRepoAppException;
}