package io.mosip.idrepository.identity.validator;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.apache.commons.lang.StringUtils;

/**
 * Validator class responsible for validating the individual ID and related data for the ID repository.
 *
 * This class implements the {@link Validator} interface and performs validation of the
 * {@link IdVidMetadataRequestDTO} and {@link IdVidMetadataRequestWrapper} objects. The validation includes
 * checking if the individual ID is present and valid, and ensures the individual ID corresponds to
 * the correct ID type (UIN, VID, or RID) as specified in the request.
 *
 * It uses a helper {@link IdRequestValidator} to validate the individual ID based on its type.
 *
 * The class logs validation events using the {@link Logger} from the MOSIP logging framework.
 */
@Component
public class IndividualIdValidator extends BaseIdRepoValidator implements Validator {

    /** The Constant ID_REQUEST_VALIDATOR. */
    private static final String INDIVIDUAL_ID_VALIDATOR = "IndividualIdValidator";

    /** The validator for individual ID. */
    @Autowired
    private IdRequestValidator validator;

    /**
     * Checks if this validator supports the given class type.
     *
     * This method checks if the provided class is either an {@link IdVidMetadataRequestDTO} or an
     * {@link IdVidMetadataRequestWrapper}. These are the expected types for validation in this class.
     *
     * @param clazz The class type to check.
     * @return True if the class is supported by this validator (i.e., either
     *         {@link IdVidMetadataRequestDTO} or {@link IdVidMetadataRequestWrapper}),
     *         false otherwise.
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return IdVidMetadataRequestDTO.class.isAssignableFrom(clazz)
                || IdVidMetadataRequestWrapper.class.isAssignableFrom(clazz);
    }

    /**
     * Validates the individual ID present in the {@link IdVidMetadataRequestWrapper} object.
     *
     * This method performs two types of validations:
     * 1. Checks if the individual ID is provided.
     * 2. Validates the individual ID against the expected ID type (UIN, VID, or RID). If the ID type is
     *    not provided, it tries to validate the ID as UIN, VID, or RID.
     *
     * If the individual ID is missing or invalid, it adds corresponding error messages to the
     * {@link Errors} object.
     *
     * @param target The target object to validate. It is expected to be of type
     *               {@link IdVidMetadataRequestWrapper}.
     * @param errors The {@link Errors} object used to collect validation errors.
     */
    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof IdVidMetadataRequestWrapper) {
            IdVidMetadataRequestDTO metadataRequest = ((IdVidMetadataRequestWrapper) target).getRequest();
            String individualId = metadataRequest.getIndividualId();
            String idType = metadataRequest.getIdType();

            // Validate if the individual ID is provided.
            validateIndividualId(individualId, errors);

            // If no errors, validate the individual ID type (UIN, VID, or RID).
            if (!errors.hasErrors()) {
                try {
                    validateUinOrVidOrRid(individualId, idType, errors);
                } catch (IdRepoAppException e) {
                    errors.rejectValue("request.individualId",
                            IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
                            String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "individualId"));
                }
            }
        }
    }

    /**
     * Validates the individual ID based on the specified ID type (UIN, VID, or RID).
     *
     * This method checks the individual ID against the specified ID type:
     * - If no ID type is provided, it tries to validate the ID as UIN, VID, or RID.
     * - If an ID type is specified, it validates the ID accordingly.
     *
     * If the individual ID is invalid, it adds a corresponding error message to the
     * {@link Errors} object.
     *
     * @param individualId The individual ID to validate.
     * @param idType The ID type to validate against (UIN, VID, or RID).
     * @param errors The {@link Errors} object used to collect validation errors.
     * @throws IdRepoAppException If an error occurs during validation.
     */
    private void validateUinOrVidOrRid(String individualId, String idType, Errors errors) throws IdRepoAppException {
        boolean valid = false;

        // If idType not provided → try all types (UIN, VID, or RID)
        if (StringUtils.isEmpty(idType)) {
            valid = validator.validateUin(individualId)
                    || validator.validateVid(individualId)
                    || validator.validateRid(individualId);
        } else {
            try {
                // Validate based on the specific ID type provided.
                IdType expectedIdType = IdType.valueOf(idType);
                switch (expectedIdType) {
                    case UIN:
                        valid = validator.validateUin(individualId);
                        break;
                    case VID:
                        valid = validator.validateVid(individualId);
                        break;
                    case ID:
                        valid = validator.validateRid(individualId);
                        break;
                }
            } catch (IllegalArgumentException | IdRepoAppException e) {
                valid = false;
            }
        }

        // If not valid, reject the individual ID.
        if (!valid) {
            errors.rejectValue("request.individualId",
                    IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
                    String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "individualId"));
        }
    }

    /**
     * Validates if the individual ID is provided.
     *
     * This method checks if the individual ID is not empty or null. If it is missing,
     * it adds an error message to the {@link Errors} object.
     *
     * @param individualId The individual ID to validate.
     * @param errors The {@link Errors} object used to collect validation errors.
     */
    private void validateIndividualId(String individualId, Errors errors) {
        if (StringUtils.isEmpty(individualId)) {
            errors.rejectValue("request.individualId",
                    IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(),
                    String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "individualId"));
        }
    }
}