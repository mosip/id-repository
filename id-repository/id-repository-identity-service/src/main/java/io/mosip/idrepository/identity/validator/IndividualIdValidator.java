package io.mosip.idrepository.identity.validator;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.apache.commons.lang.StringUtils;
import java.util.Objects;

@Component
public class IndividualIdValidator extends BaseIdRepoValidator implements Validator {

    /** The Constant ID_REQUEST_VALIDATOR. */
    private static final String INDIVIDUAL_ID_VALIDATOR = "IndividualIdValidator";

    /** The validator. */
    @Autowired
    private IdRequestValidator validator;

    /** The mosip logger. */
    Logger mosipLogger = IdRepoLogger.getLogger(IndividualIdValidator.class);

    /**
     * @param clazz
     * @return
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return IdVidMetadataRequestDTO.class.isAssignableFrom(clazz)
        || RequestWrapper.class.isAssignableFrom(clazz) // ← if you validate the whole wrapper
        || IdVidMetadataRequestWrapper.class.isAssignableFrom(clazz);
    }

    /**
     * @param target
     * @param errors
     */
    @Override
    public void validate(Object target, Errors errors) {
        mosipLogger.error(IdRepoSecurityManager.getUser(), INDIVIDUAL_ID_VALIDATOR, "validateIndividualID", "NULL RID");
        if (target instanceof IdVidMetadataRequestWrapper) {

            IdVidMetadataRequestDTO metadataRequest = ((IdVidMetadataRequestWrapper) target).getRequest();
            String individualId = metadataRequest.getIndividualId();
            String idType = metadataRequest.getIdType();

            validateIndividualId(individualId, errors);
            if (!errors.hasErrors()) {
                try {
                    validateUinOrVidOrRid(individualId, idType, errors);
                } catch (IdRepoAppException e) {
                    errors.rejectValue(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
                            String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), individualId));
                }
            }
        }
    }

    private void validateUinOrVidOrRid(String individualId, String idType, Errors errors) throws IdRepoAppException {
        boolean valid = false;

        // If idType not provided → try all
        if (StringUtils.isEmpty(idType)) {
            valid = validator.validateUin(individualId)
                    || validator.validateVid(individualId)
                    || validator.validateRid(individualId);

            if(!valid)
                errors.rejectValue(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(),
                        String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), individualId));

        }
        try {
            IdType expectedIdType = IdType.valueOf(idType);
            switch (expectedIdType) {
                case UIN:
                    valid = validator.validateUin(individualId);
                case VID:
                    valid = validator.validateVid(individualId);
                case ID:
                    valid =  validator.validateRid(individualId);
            }
        } catch (IllegalArgumentException | IdRepoAppException e) {
            // Handle invalid idType (e.g., "demo")
            valid = false;
        }

        if (!valid) {
            errors.rejectValue(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(),
                String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), individualId));
        }
    }

    private void validateIndividualId(String individualId, Errors errors) {
        if (Objects.isNull(individualId)) {
            errors.rejectValue(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(),
                    String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), individualId));
        }
    }
}
