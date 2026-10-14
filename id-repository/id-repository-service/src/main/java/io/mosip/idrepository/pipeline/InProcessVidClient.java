package io.mosip.idrepository.pipeline;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.VidResponseDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.vid.service.impl.VidServiceImpl;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;

/**
 * SDK adapter for VID lookups within the consolidated ID Repository JVM.
 */
@Component
public class InProcessVidClient {

	private static final String VID = "vid";

	private static final String UIN = "uin";

	@Autowired
	private VidServiceImpl vidService;

	@Autowired
	private VidValidator<String> vidValidator;

	@Autowired
	private UinValidator<String> uinValidator;

	/**
	 * Resolves plain UIN for a VID via direct service invocation (no HTTP).
	 * Applies the same Kernel VID format check as {@code GET /vid/{VID}}.
	 *
	 * @param vid virtual ID
	 * @return decrypted UIN linked to the VID
	 * @throws IdRepoAppException when lookup or validation fails
	 */
	public String getUinByVid(String vid) throws IdRepoAppException {
		try {
			vidValidator.validateId(vid);
			ResponseWrapper<VidResponseDTO> response = vidService.retrieveUinByVid(vid);
			return response.getResponse().getUin();
		} catch (InvalidIDException e) {
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), VID));
		}
	}

	/**
	 * Lists active VIDs for a UIN via direct service invocation (no HTTP).
	 * Applies Kernel UIN format check before the lookup.
	 *
	 * @param uin plain UIN
	 * @return VID metadata list wrapper
	 * @throws IdRepoAppException when lookup or validation fails
	 */
	public VidsInfosDTO retrieveVidsByUin(String uin) throws IdRepoAppException {
		try {
			uinValidator.validateId(uin);
			return vidService.retrieveVidsByUin(uin);
		} catch (InvalidIDException e) {
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), UIN));
		}
	}
}
