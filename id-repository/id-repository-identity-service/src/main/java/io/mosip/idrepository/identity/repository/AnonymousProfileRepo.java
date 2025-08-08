package io.mosip.idrepository.identity.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AnonymousProfileRepo extends JpaRepository<AnonymousProfileEntity, String> {

    /**
     * Inserts or updates anonymous profile in a single DB call using PostgreSQL ON CONFLICT.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO anonymous_profile (id, profile, created_by, cr_dtimes)
        VALUES (:id, :profile, :createdBy, :createdTime)
        ON CONFLICT (id) DO UPDATE
          SET profile = EXCLUDED.profile,
              created_by = EXCLUDED.created_by,
              cr_dtimes = EXCLUDED.cr_dtimes
        """, nativeQuery = true)
    void upsertAnonymousProfile(@Param("id") String id,
                                @Param("profile") String profile,
                                @Param("createdBy") String createdBy,
                                @Param("createdTime") LocalDateTime createdTime);
}
