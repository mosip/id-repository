package io.mosip.idrepository.pipeline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.VidResponseDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.vid.service.impl.VidServiceImpl;
import io.mosip.kernel.core.http.ResponseWrapper;

/**
 * SDK adapter for VID lookups within the consolidated ID Repository JVM.
 */
@Component
public class InProcessVidClient {

	@Autowired
	private VidServiceImpl vidService;

	/**
	 * Resolves plain UIN for a VID via direct service invocation (no HTTP).
	 *
	 * @param vid virtual ID
	 * @return decrypted UIN linked to the VID
	 * @throws IdRepoAppException when lookup or validation fails
	 */
	public String getUinByVid(String vid) throws IdRepoAppException {
		ResponseWrapper<VidResponseDTO> response = vidService.retrieveUinByVid(vid);
		return response.getResponse().getUin();
	}

	/**
	 * Lists active VIDs for a UIN via direct service invocation (no HTTP).
	 *
	 * @param uin plain UIN
	 * @return VID metadata list wrapper
	 * @throws IdRepoAppException when lookup fails
	 */
	public VidsInfosDTO retrieveVidsByUin(String uin) throws IdRepoAppException {
		return vidService.retrieveVidsByUin(uin);
	}
}
