package io.mosip.idrepository.identity.service.impl;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.dto.UnsubscribeRequestDto;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UnsubscribeRecord;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.idrepository.identity.repository.UnsubscribeRecordRepo;
import io.mosip.idrepository.identity.service.UnsubscribeService;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;

@Service
@Transactional(rollbackFor = { IdRepoAppException.class })
public class UnsubscribeServiceImpl implements UnsubscribeService {

    private static final String CLASS_NAME = "UnsubscribeServiceImpl";

    Logger mosipLogger = IdRepoLogger.getLogger(UnsubscribeServiceImpl.class);

    @Autowired
    private UinRepo uinRepo;

    @Autowired
    private UnsubscribeRecordRepo unsubscribeRecordRepo;

    @Autowired
    private IdRepoServiceImpl idRepoServiceImpl;

    @Value("${mosip.idrepo.identity.uin-status.deactivated:DEACTIVATED}")
    private String deactivatedStatus;

    @Override
    public void processUnsubscribe(UnsubscribeRequestDto request) throws IdRepoAppException {
        String email = request.getEmail().toLowerCase().trim();




        //-------------------------- issue 1.0: UIN lookup by email --------------------------
        // 1. Find UIN(s) associated with this email from identity data
        List<Uin> uinList = uinRepo.findByEmailInIdentityData(email); // this one causing issue 1.0 
        // error 1.0:  The issue is that your database WHERE clause is trying to search for a
        //  plain-text email directly against the uin_data column, but because that column is
        //  cryptographically encrypted (BYTEA), the SQL engine cannot decrypt records on
        //  the fly to find a text match, resulting in failed lookups and hash mismatches.
        // for to correction i need follow:
         // as per issue i was understand that i cannot use original logic of passing a plain text email into the dtabase serach nor can just split the UIN 
         //string withput decrypting it
         /* fix for production:
         1. we must hash the incoming email in java using IdRepoSecurityManger and serach the database against a stored email_hash column
         2. decrypt properly : we must use IdRepoSecurityManager.decrypt() to extract the UIN instead of a simple string Split

         logic need to add :
            // 1. Hash the email to securely search the database
        // Because uin_data is highly encrypted, we cannot search plain-text emails using SQL.
        // We must search against an indexed hashed value of the email.
        String emailHash = securityManager.hash(email.getBytes());
        List<Uin> uinList = uinRepo.findByEmailHash(emailHash); // Ensure findByEmailHash is updated in UinRepo
         
         */

        if (uinList == null || uinList.isEmpty()) {
            mosipLogger.warn(IdRepoSecurityManager.getUser(), CLASS_NAME,
                    "processUnsubscribe", "No UIN found for email: [redacted]");
            throw new IdRepoAppException(NO_RECORD_FOUND.getErrorCode(),
                    "No identity record found for the provided email");
        }

        for (Uin uinEntity : uinList) {
            // Skip already deactivated UINs
            if (deactivatedStatus.equalsIgnoreCase(uinEntity.getStatusCode())) {
                mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NAME,
                        "processUnsubscribe", "UIN already deactivated, skipping");
                continue;
            }

            // 2. Build an update request to set status = DEACTIVATED
            IdRequestDTO updateRequest = buildDeactivationRequest(uinEntity, deactivatedStatus);

            // 3. Decrypt the UIN value to pass to updateIdentity
            // uinEntity.getUin() holds the encrypted UIN; we resolve the plain hash
            String uinHash = uinEntity.getUinHash();
            String plainUin = resolveDecryptedUin(uinEntity);

            idRepoServiceImpl.updateIdentity(updateRequest, plainUin);

            mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NAME,
                    "processUnsubscribe", "UIN deactivated successfully");
        }

        // 4. Persist the unsubscribe record (suppression list)
        saveUnsubscribeRecord(email, request.getReason(), request.getComments());
    }

    @Override
    public boolean isAlreadyUnsubscribed(String email) {
        return unsubscribeRecordRepo.existsByEmail(email.toLowerCase().trim());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private IdRequestDTO buildDeactivationRequest(Uin uinEntity, String status) {
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setStatus(status);
        // Preserve existing registration ID so history is traceable
        requestDTO.setRegistrationId(uinEntity.getRegId());
        // Identity object left null — we only want a status change, not data change
        requestDTO.setIdentity(null);

        IdRequestDTO idRequestDTO = new IdRequestDTO();
        idRequestDTO.setRequest(requestDTO);
        return idRequestDTO;
    }

    /**
     * Resolves the plain (decrypted) UIN string needed by updateIdentity.
     */
    private String resolveDecryptedUin(Uin uinEntity) throws IdRepoAppException {
        // LOCAL TESTING: For local development, if the UIN was stored as a hash/salt combination separated by '_',
        // we simply split it.
        // 
        // PRODUCTION TODO: In production, uinEntity.getUin() contains data encrypted by MOSIP's KeyManager.
        // You must use `idRepoSecurityManager.decrypt(...)` or the appropriate CryptoUtil to get the plaintext UIN.
        
        // uinEntity.getUin() is stored as:  saltId_SPLITTER_uin_SPLITTER_encryptSalt
        // Split and return the middle segment (plain UIN).
        try {
            String[] parts = uinEntity.getUin().split("_");
            if (parts.length >= 2) {
                return parts[1];
            }

/* issue 1.0 changes: u need to update as :
            // In production, the 'uin' field is stored as: saltId_SPLITTER_encryptedUin
            String[] parts = uinEntity.getUin().split(io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER);
            if (parts.length >= 2) {
                // Execute the proper decryption process using MOSIP's Security Manager
                byte[] decryptedBytes = securityManager.decrypt(parts[1].getBytes(), "uin");
                return new String(decryptedBytes);

*/



        } catch (Exception e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_NAME,
                    "resolveDecryptedUin", "Failed to extract UIN: " + e.getMessage());
        }
        throw new IdRepoAppException(NO_RECORD_FOUND.getErrorCode(),
                "Unable to resolve UIN for deactivation");
    }

    private void saveUnsubscribeRecord(String email, String reason, String comments) {
        UnsubscribeRecord record = new UnsubscribeRecord();
        record.setEmail(email);
        record.setReason(reason);
        record.setComments(comments);
        record.setUnsubscribedAt(DateUtils2.getUTCCurrentDateTime());
        record.setCreatedBy(IdRepoSecurityManager.getUser());
        unsubscribeRecordRepo.save(record);
        mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NAME,
                "saveUnsubscribeRecord", "Unsubscribe record saved");
    }
}