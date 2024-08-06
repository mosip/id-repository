package io.mosip.credential.request.generator.repositary;

import java.time.LocalDateTime;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;



// TODO: Auto-generated Javadoc
/**
 * The Interface CredentialRepositary.
 *
 * @author Sowmya
 * 
 *         The Interface CredentialRepositary.
 * @param <T> the generic type
 * @param <E> the element type
 */
@Repository
public interface CredentialRepositary<T extends CredentialEntity, E> extends BaseRepository<T, E> {



	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode= :statusCode")
	Page<CredentialEntity> findByStatusCode(@Param("statusCode") String statusCode, Pageable pageable);


	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode= :statusCode and crdn.updateDateTime>= :effectiveDTimes")
	Page<CredentialEntity> findByStatusCodeWithEffectiveDtimes(@Param("statusCode") String statusCode,
			@Param("effectiveDTimes") LocalDateTime effectiveDTimes,
			Pageable pageable);
	
	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE) 
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "1") })
	@Query("select c from CredentialEntity c where c.statusCode=:statusCode")
	Page<CredentialEntity> findCredentialByStatusCode(@Param("statusCode")String statusCode, Pageable pageable);

	/**
	 * Find credential by status codes.
	 *
	 * @param statusCodes the status codes
	 * @param type        the type
	 * @param pageable    the pageable
	 * @return the page
	 */
	@Transactional
	@Lock(value = LockModeType.PESSIMISTIC_WRITE) // adds 'FOR UPDATE' statement
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "1") })
	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode in :statusCodes")
	Page<CredentialEntity> findCredentialByStatusCodes(@Param("statusCodes") String[] statusCodes, Pageable pageable);
}
