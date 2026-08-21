package io.mosip.idrepository.identity.service;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.identity.dto.UnsubscribeRequestDto;

public interface UnsubscribeService {

    /**
     * Processes an unsubscribe request for the given email.
     * Deactivates the associated UIN and records the unsubscription.
     *
     * @param request the unsubscribe request DTO
     * @throws IdRepoAppException if UIN lookup or deactivation fails
     */
    void processUnsubscribe(UnsubscribeRequestDto request) throws IdRepoAppException;

    /**
     * Checks whether the given email is already unsubscribed.
     *
     * @param email the email to check
     * @return true if already unsubscribed
     */
    boolean isAlreadyUnsubscribed(String email);
}