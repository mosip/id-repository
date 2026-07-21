package io.mosip.idrepository.core.dto;

import java.util.List;
import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP {@link ResponseWrapper} listing all VIDs associated with a UIN.
 *
 * <p>
 * Preferred response type for retrieve-VIDs-by-UIN. Declares an explicit
 * {@link #response} list of {@link VidInfoDTO} entries consumed by the
 * credential issuance pipeline and VID clients. Prefer this over the legacy
 * {@link VidInfoResponsDTO}.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Returned by {@code /idrepository/v1/vid} retrieve-by-UIN
 * ({@code retrieveVidsByUin}). Kernel envelope fields are inherited from
 * {@link ResponseWrapper}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code VidServiceImpl}</li>
 *   <li>{@code CredentialServiceManager} — VID list for credential issuance</li>
 *   <li>Partner / registration clients</li>
 * </ul>
 *
 * @see VidInfoDTO
 * @see VidInfoResponsDTO
 * @see io.mosip.idrepository.vid.service.impl.VidServiceImpl
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class VidsInfosDTO extends ResponseWrapper<List<VidInfoDTO>>{

	/** List of VID metadata entries for the requested UIN. */
	private List<VidInfoDTO> response;
}
