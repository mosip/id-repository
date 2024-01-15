package io.mosip.idrepository.core.repository;

import io.mosip.idrepository.core.entity.Handle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(name = { "idRepoDataSource" })
public interface HandleRepo extends JpaRepository<Handle, String> {

    boolean existsByHandleHash(String handleHash);

    List<Handle> findByUinHash(String uinHash);

    Handle findByHandleHash(String handleHash);
}
