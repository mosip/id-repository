package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.VID_GENERATION_FAILED;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;

/**
 * @author Manoj SP
 *
 */
@Component
public class VidDraftHelper {
	
	private static final Logger mosipLogger = IdRepoLogger.getLogger(VidDraftHelper.class);

	@Autowired
	protected EnvUtil env;

	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private RestHelper restHelper;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public String generateDraftVid(String uin) throws IdRepoAppException {
		try {
			if (EnvUtil.getIsDraftVidTypePresent()) {
				VidRequestDTO vidCreationRequest = new VidRequestDTO();
				vidCreationRequest.setUin(uin);
				vidCreationRequest.setVidType(EnvUtil.getDraftVidType());
				RequestWrapper<VidRequestDTO> request = new RequestWrapper<>();
				request.setId(EnvUtil.getCreateVidId());
				request.setVersion(EnvUtil.getVidAppVersion());
				request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
				request.setRequest(vidCreationRequest);
				ResponseWrapper<Map<String, String>> vidResponse = this.restHelper
						.requestSync(this.restBuilder.buildRequest(RestServicesConstants.VID_DRAFT_GENERATOR_SERVICE,
								request, ResponseWrapper.class));
				return vidResponse.getResponse().get("VID");
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "VidDraftHelper", "generateDraftVid", ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(VID_GENERATION_FAILED);
		}
		return null;
	}

	public void activateDraftVid(String draftVid) throws IdRepoAppException {
		try {
			if (Objects.nonNull(draftVid)) {
				VidRequestDTO vidUpdationRequest = new VidRequestDTO();
				vidUpdationRequest.setVidStatus(EnvUtil.getVidActiveStatus());
				RequestWrapper<VidRequestDTO> request = new RequestWrapper<>();
				request.setId(EnvUtil.getUpdatedVidId());
				request.setVersion(EnvUtil.getVidAppVersion());
				request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
				request.setRequest(vidUpdationRequest);
				RestRequestDTO restRequest = this.restBuilder.buildRequest(RestServicesConstants.VID_UPDATE_SERVICE,
						request, ResponseWrapper.class);
				restRequest.setUri(restRequest.getUri().replace("{vid}", draftVid));
				this.restHelper.requestSync(restRequest);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "VidDraftHelper", "activateDraftVid", ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppException(VID_GENERATION_FAILED);
		}
	}

	/**
	 * Fire-and-forget variant of {@link #activateDraftVid(String)} used on the
	 * {@code publishDraft} hot path.
	 *
	 * <p>VID activation is a remote REST call to the VID service whose result is
	 * not consumed by the caller — neither the response body nor any thrown
	 * exception influences the response sent back to the user. Running it
	 * synchronously on the request thread therefore only adds latency.
	 *
	 * <p>This method is annotated {@code @Async} so the call is dispatched on
	 * the default async executor and the caller returns immediately. Any failure
	 * is logged in place of being re-thrown, because the worker has no caller to
	 * propagate to. The synchronous {@link #activateDraftVid(String)} method is
	 * preserved for callers that still want the strict-throwing semantics or
	 * that need to observe the call completing before continuing.
	 *
	 * <p><b>Operational note:</b> a failed asynchronous activation leaves the VID
	 * in {@code draft} state. The id-repository already had this exposure for
	 * stranded VIDs (the VID is generated in a separate, non-participating
	 * transaction), so this change preserves rather than introduces that risk.
	 * Reconciliation should be handled out-of-band.
	 */
	@Async
	public void activateDraftVidAsync(String draftVid) {
		try {
			activateDraftVid(draftVid);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "VidDraftHelper", "activateDraftVidAsync",
					"Async VID activation failed | draftVid=" + draftVid + " | error="
							+ ExceptionUtils.getStackTrace(e));
		}
	}
}
