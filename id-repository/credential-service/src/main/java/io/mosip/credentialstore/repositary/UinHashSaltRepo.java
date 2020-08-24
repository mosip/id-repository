package io.mosip.credentialstore.repositary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import io.mosip.credentialstore.entity.UinHashSalt;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;



/**
 * The Interface UinHashSaltRepo.
 *
 * @author Prem Kumar
 */
@Repository
public interface UinHashSaltRepo <T extends UinHashSalt, E> extends BaseRepository<T, E>{
	
	/**
	 * The Query to retrieve salt by passing id as parameter.
	 *
	 * @param id the id
	 * @return String salt
	 */
	@Query("select salt from UinHashSalt where id = :id")
	public String retrieveSaltById(@Param("id") int id);
}
