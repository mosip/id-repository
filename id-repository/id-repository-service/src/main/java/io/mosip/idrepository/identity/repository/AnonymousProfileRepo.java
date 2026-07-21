package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;

/**
 * Spring Data repository for {@link AnonymousProfileEntity} ({@code idrepo.anonymous_profile}).
 */
public interface AnonymousProfileRepo extends JpaRepository<AnonymousProfileEntity, String> {

}
