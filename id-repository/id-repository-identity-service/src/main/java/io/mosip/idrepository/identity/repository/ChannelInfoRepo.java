package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.ChannelInfo;

/**
 * @author Manoj SP
 *
 */
public interface ChannelInfoRepo extends JpaRepository<ChannelInfo, String> {

}