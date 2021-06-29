package io.mosip.idrepository.identity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.spi.IdRepoDraftService;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Manoj SP
 *
 */
@RestController
public class IdRepoDraftController {

	@Autowired
	private IdRequestValidator validator;

	@Autowired
	private IdRepoDraftService<IdRequestDTO, IdResponseDTO> draftService;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(validator);
	}

	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PostMapping(path = "/createDraft", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> createDraft(@Validated @RequestBody IdRequestDTO request, @ApiIgnore Errors errors)
			throws IdRepoAppException {
		DataValidationUtil.validate(errors);
		return new ResponseEntity<>(draftService.createDraft(request), HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PatchMapping(path = "/updateDraft", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> updateDraft(@Validated @RequestBody IdRequestDTO request, @ApiIgnore Errors errors)
			throws IdRepoAppException {
		DataValidationUtil.validate(errors);
		return new ResponseEntity<>(draftService.updateDraft(request), HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@GetMapping(path = "/publishDraft/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> publishDraft(@PathVariable String registrationId) throws IdRepoAppException {
		validateRegistrationId(registrationId);
		return new ResponseEntity<>(draftService.publishDraft(registrationId), HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@DeleteMapping(path = "/discardDraft/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> discardDraft(@PathVariable String registrationId) throws IdRepoAppException {
		validateRegistrationId(registrationId);
		return new ResponseEntity<>(draftService.discardDraft(registrationId), HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@GetMapping(path = "/hasDraft/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> hasDraft(@PathVariable String registrationId) throws IdRepoAppException {
		validateRegistrationId(registrationId);
		return new ResponseEntity<>(draftService.hasDraft(registrationId), HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@GetMapping(path = "/getDraft/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> getDraft(@PathVariable String registrationId) throws IdRepoAppException {
		validateRegistrationId(registrationId);
		return new ResponseEntity<>(draftService.getDraft(registrationId), HttpStatus.OK);
	}

	private void validateRegistrationId(String registrationId) throws IdRepoAppException {
		if (!validator.validateRid(registrationId)) {
			throw new IdRepoAppException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "registrationId"));
		}
	}
}
