package io.mosip.idrepository.credentialsfeeder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.credentialsfeeder.entity.AuthtypeLock;

/**
 * 
 * @author Dinesh Karuppiah.T
 *
 */
@Repository
public interface AuthLockRepository extends JpaRepository<AuthtypeLock, Integer> {

	List<AuthtypeLock> findByHashedUin(String hashedUin);
}	
