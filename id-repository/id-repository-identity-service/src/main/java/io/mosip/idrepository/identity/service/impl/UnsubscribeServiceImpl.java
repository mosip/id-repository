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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED;

@Service
@Transactional(rollbackFor = { IdRepoAppException.class })
public class UnsubscribeServiceImpl implements UnsubscribeService {

    private static final String CLASS_NAME = "UnsubscribeServiceImpl";

    Logger mosipLogger = IdRepoLogger.getLogger(UnsubscribeServiceImpl.class);
// these are writing in the field injection 
    @Autowired
    private UinRepo uinRepo;

    @Autowired
    private UnsubscribeRecordRepo unsubscribeRecordRepo;

    @Autowired
    private IdRepoServiceImpl idRepoServiceImpl;

    @Autowired
    private IdRepoSecurityManager securityManager;

    @Value("${mosip.idrepo.identity.uin-status.deactivated:DEACTIVATED}")
    private String deactivatedStatus;


    /* below is the alter method of the field injection which is constructor injection:
    this will make the class immutable explicitly define its depedencies and make it much easier 
    to write isolated unit test for it 
      
    private final UinRepo uinRepo;
    private final UnsubscribeRecordRepo unsubscribeRecordRepo;
    private final IdRepoServiceImpl idRepoServiceImpl;
    private final IdRepoSecurityManager securityManager;

    // Spring automatically autowires a class with a single constructor
    public UnsubscribeServiceImpl(UinRepo uinRepo,
                                  UnsubscribeRecordRepo unsubscribeRecordRepo,
                                  IdRepoServiceImpl idRepoServiceImpl,
                                  IdRepoSecurityManager securityManager) {
        this.uinRepo = uinRepo;
        this.unsubscribeRecordRepo = unsubscribeRecordRepo;
        this.idRepoServiceImpl = idRepoServiceImpl;
        this.securityManager = securityManager;
    }

    @Value("${mosip.idrepo.identity.uin-status.deactivated:DEACTIVATED}")
    private String deactivatedStatus;
    
    */
    @Override
    public void processUnsubscribe(UnsubscribeRequestDto request) throws IdRepoAppException {
        // Security Note: request.getEmail() is now securely populated by the Controller after JWT verification.
        String email = request.getEmail().toLowerCase(Locale.ENGLISH).trim();

        // 1. Hash the email to securely search the database
        // Because uin_data is highly encrypted, we cannot search plain-text emails using SQL.
        // We must search against an indexed hashed value of the email.
        String emailHash = securityManager.hash(email.getBytes());
        List<Uin> uinList = uinRepo.findByEmailHash(emailHash);

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
        return unsubscribeRecordRepo.existsByEmail(email.toLowerCase(Locale.ENGLISH).trim());
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
        try {
            // In production, the 'uin' field is stored as: saltId_SPLITTER_encryptedUin.
            // We split with a limit of 2 to prevent the encrypted data from being truncated if it contains the splitter.
            String[] parts = uinEntity.getUin().split(Pattern.quote(io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER), 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                // Execute the proper decryption process using MOSIP's Security Manager
                byte[] decryptedBytes = securityManager.decrypt(parts[1].getBytes(), "uin");
                return new String(decryptedBytes);
            }
        } catch (Exception e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_NAME,
                    "resolveDecryptedUin", "Failed to decrypt UIN: " + e.getMessage());
        }
        throw new IdRepoAppException(ENCRYPTION_DECRYPTION_FAILED.getErrorCode(),
                "Unable to decrypt UIN for deactivation");
    }

    private void saveUnsubscribeRecord(String email, String reason, String comments) {
        UnsubscribeRecord record = new UnsubscribeRecord();
        record.setEmail(email);
        record.setReason(reason);
        record.setComments(comments);
        record.setUnsubscribedAt(DateUtils2.getUTCCurrentDateTime());
        record.setCreatedBy(IdRepoSecurityManager.getUser());
        
        try {
            unsubscribeRecordRepo.save(record);
            mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NAME,
                    "saveUnsubscribeRecord", "Unsubscribe record saved");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NAME,
                    "saveUnsubscribeRecord", "Unsubscribe record already exists. Treating as idempotent.");
        }
    }
}