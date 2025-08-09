package io.mosip.idrepository.identity.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.ChannelInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChannelInfoRepo extends JpaRepository<ChannelInfo, String> {

    /**
     * Inserts a channel row or updates an existing one in a single atomic DB call.
     * If row exists, increments/decrements `no_of_records` by :delta (never below 0).
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO channel_info (hashed_channel, channel_type, no_of_records, cr_by, cr_dtimes)
        VALUES (:hashedChannel, :channelType, GREATEST(:initial, 0), :createdBy, :createdTime)
        ON CONFLICT (hashed_channel) DO UPDATE
          SET no_of_records = GREATEST(channel_info.no_of_records + :delta, 0),
              upd_by = :updatedBy,
              upd_dtimes = :updatedTime
        """, nativeQuery = true)
    void upsertAndDelta(@Param("hashedChannel") String hashedChannel,
                        @Param("channelType") String channelType,
                        @Param("initial") int initial,
                        @Param("delta") int delta,
                        @Param("createdBy") String createdBy,
                        @Param("createdTime") LocalDateTime createdTime,
                        @Param("updatedBy") String updatedBy,
                        @Param("updatedTime") LocalDateTime updatedTime);
}
