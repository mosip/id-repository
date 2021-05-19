package io.mosip.idrepository.vid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.vid.entity.UinHashSalt;

/**
 * The Repository for UinHashSalt table.
 * 
 * @author Prem Kumar
 *
 */
@Repository
public interface UinHashSaltRepo extends JpaRepository<UinHashSalt, Integer> {
	
	/**
	 * The Query to retrieve salt by passing id as parameter.
	 *
	 * @param id the id
	 * @return String salt
	 */
	@Query("select salt from UinHashSalt where id = :id")
	public String retrieveSaltById(@Param("id") int id);
}
