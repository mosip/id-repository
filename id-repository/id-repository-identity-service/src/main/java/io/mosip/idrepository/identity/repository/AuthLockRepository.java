package io.mosip.idrepository.identity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.identity.entity.AuthtypeLock;

/**
 * 
 * @author Dinesh Karuppiah.T
 *
 */
@Repository
public interface AuthLockRepository extends JpaRepository<AuthtypeLock, Integer> {

	@Query(value = "select " + 
			"        t.auth_type_code, " + 
			"        t.status_code  " + 
			"    from " + 
			"        idrepo.uin_auth_lock t  " + 
			"    inner join " + 
			"        ( " + 
			"            select " + 
			"                auth_type_code, " + 
			"                MAX(cr_dtimes) as crd " + 
			"            from " + 
			"                idrepo.uin_auth_lock      " + 
			"            where " + 
			"                uin_hash = :uin_hash " + 
			"            group by " + 
			"                uin_hash, " + 
			"                auth_type_code  " + 
			"        ) tm  " + 
			"            on t.auth_type_code = tm.auth_type_code  " + 
			"            and t.cr_dtimes = tm.crd  " + 
			"    where " + 
			"        t.uin_hash = :uin_hash", 
			nativeQuery = true)
	public List<Object[]> findByUinHash(@Param("uin_hash") String hashedUin);

}	
