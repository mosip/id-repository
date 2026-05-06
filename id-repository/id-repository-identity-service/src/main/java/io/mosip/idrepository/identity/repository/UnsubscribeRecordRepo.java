package io.mosip.idrepository.identity.repository;

import io.mosip.idrepository.identity.entity.UnsubscribeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnsubscribeRecordRepo extends JpaRepository<UnsubscribeRecord, String> {
    boolean existsByEmail(String email);
}