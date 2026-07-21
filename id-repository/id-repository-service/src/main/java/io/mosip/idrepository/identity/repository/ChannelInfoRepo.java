package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.ChannelInfo;

/**
 * Spring Data repository for {@link ChannelInfo} ({@code idrepo.channel_info}).
 */
public interface ChannelInfoRepo extends JpaRepository<ChannelInfo, String> {

}
