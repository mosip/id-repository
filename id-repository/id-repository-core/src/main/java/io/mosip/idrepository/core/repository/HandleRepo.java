package io.mosip.idrepository.core.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.core.entity.Handle;

@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface HandleRepo extends JpaRepository<Handle, String> {

	List<Handle> findByUinHash(String uinHash);

	@Query("SELECT uinHash FROM Handle WHERE handleHash=:handleHash")
	String findUinHashByHandleHash(@Param("handleHash") String handleHash);

	@Query("SELECT uinHash FROM Handle WHERE handleHash IN :hashes")
	List<String> findUinHashByHandleHashes(@Param("hashes") List<String> hashes);

	void deleteByHandleHash(String handleHash);

	@Modifying
	@Transactional
	@Query("UPDATE Handle SET status=:status WHERE handleHash=:handleHash")
	int updateStatusByHandleHash(@Param("handleHash") String handleHash, @Param("status") String status);

	@Modifying
	@Transactional
	@Query("UPDATE Handle SET status=:status WHERE uinHash=:uinHash")
	int updateStatusByUinHash(@Param("uinHash") String uinHash, @Param("status") String status);

	@Modifying
	@Transactional
	@Query("UPDATE Handle SET status=:newStatus WHERE uinHash=:uinHash AND status=:currentStatus")
	int updateStatusByUinHashAndStatus(@Param("uinHash") String uinHash, @Param("currentStatus") String currentStatus,
			@Param("newStatus") String newStatus);
}
