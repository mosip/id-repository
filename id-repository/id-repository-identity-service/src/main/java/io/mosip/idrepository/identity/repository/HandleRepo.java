package io.mosip.idrepository.identity.repository;

import io.mosip.idrepository.identity.entity.Handle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HandleRepo extends JpaRepository<Handle, String> {

    boolean existsByHandleHash(String handleHash);

    List<Handle> findByUinHash(String uinHash);

    Handle findByHandleHash(String handleHash);
}
