package io.mosip.idrepository.core.repository;

import io.mosip.idrepository.core.entity.Handle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import javax.transaction.Transactional;

@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface HandleRepo extends JpaRepository<Handle, String> {

    boolean existsByHandleHash(String handleHash);

    List<Handle> findByUinHash(String uinHash);

    Handle findByHandleHash(String handleHash);

	void deleteByHandleHash(String handleHash);

	boolean existsByHandleHashAndUinHash(String handleHash, String uinHash);

	@Modifying
	@Transactional
	@Query("update Handle set is_deleted='true' where handle_hash=:handleHash")
	int updateIsDeleted(@Param("handleHash") String handleHash);
}
