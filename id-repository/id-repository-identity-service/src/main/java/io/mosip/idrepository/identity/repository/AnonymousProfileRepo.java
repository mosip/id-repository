package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;

/**
 * @author Manoj SP
 *
 */
public interface AnonymousProfileRepo extends JpaRepository<AnonymousProfileEntity, String> {

}