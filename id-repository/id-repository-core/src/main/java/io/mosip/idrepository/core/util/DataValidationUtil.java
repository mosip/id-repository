package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.getAllErrorCodes;

import org.springframework.validation.Errors;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.idrepository.core.validator.IdRepoValidationMessageHelper;

/**
 * Converts Spring {@link Errors} binding results into a single
 * {@link IdRepoDataValidationException}.
 *
 * <p>
 * Controllers and services invoke {@link #validate(Errors)} after a
 * {@link org.springframework.validation.Validator} (often extending
 * {@link BaseIdRepoValidator}) has populated an {@code Errors} object. Only error codes
 * registered in {@link IdRepoErrorConstants#getAllErrorCodes()} are included in the thrown
 * exception; unknown / framework-level codes are silently ignored so they do not leak into
 * the MOSIP error contract.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * Errors errors = new BeanPropertyBindingResult(request, "request");
 * validator.validate(request, errors);
 * DataValidationUtil.validate(errors); // throws IdRepoDataValidationException if needed
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity / VID controllers and services after Spring validation</li>
 *   <li>Any path that accumulates {@link Errors} via
 *       {@link org.springframework.validation.Errors#rejectValue(String, String, String)}</li>
 * </ul>
 *
 * <h2>Detail messages</h2>
 * <p>
 * Default messages on {@link Errors} are typically built with
 * {@link IdRepoValidationMessageHelper} and become the exception’s error texts via
 * {@link IdRepoDataValidationException#addInfo(String, String)}.
 * </p>
 *
 * @author Manoj SP
 * @see IdRepoDataValidationException
 * @see IdRepoErrorConstants
 * @see BaseIdRepoValidator
 * @see IdRepoValidationMessageHelper
 */
public final class DataValidationUtil {

	/**
	 * Prevents instantiation; use static helpers only.
	 */
	private DataValidationUtil() {
	}

	/**
	 * Inspects {@code errors} and throws when at least one recognized ID Repository error
	 * is present.
	 * <p>
	 * Behaviour:
	 * </p>
	 * <ul>
	 *   <li>If {@code !errors.hasErrors()} — returns immediately</li>
	 *   <li>Otherwise copies each error whose {@code code} is in
	 *       {@link IdRepoErrorConstants#getAllErrorCodes()} into a new
	 *       {@link IdRepoDataValidationException}</li>
	 *   <li>Throws that exception when it contains at least one code or text; otherwise
	 *       returns (all errors were filtered out as unknown codes)</li>
	 * </ul>
	 *
	 * @param errors Spring binding/validation errors produced by a {@code Validator};
	 *               must not be {@code null}
	 * @throws IdRepoDataValidationException when {@code errors.hasErrors()} and at least
	 *                                       one error code is a known ID Repository error
	 *                                       code
	 * @see IdRepoDataValidationException#addInfo(String, String)
	 */
	public static void validate(Errors errors) throws IdRepoDataValidationException {
		if (!errors.hasErrors()) {
			return;
		}
		IdRepoDataValidationException exception = new IdRepoDataValidationException();
		errors.getAllErrors().stream()
				.filter(error -> getAllErrorCodes().contains(error.getCode()))
				.forEach(error -> exception.addInfo(error.getCode(), error.getDefaultMessage()));
		if (exception.getErrorCode() != null || !exception.getErrorTexts().isEmpty()) {
			throw exception;
		}
	}
}
