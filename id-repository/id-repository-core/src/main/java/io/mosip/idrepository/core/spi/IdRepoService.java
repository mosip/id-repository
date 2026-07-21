package io.mosip.idrepository.core.spi;

import java.util.List;
import java.util.Map;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.IdVidMetadataResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * Core SPI for UIN identity lifecycle operations in ID Repository.
 * <p>
 * Defines create, retrieve, update, and metadata APIs shared across identity
 * controllers and in-process callers. Generic type parameters allow the REST
 * layer ({@code IdRequestDTO}/{@code IdResponseDTO}) and the internal entity
 * layer ({@code Uin}) to share one contract.
 * </p>
 * <p>
 * <b>Implementors:</b> service-layer classes in {@code id-repository-service}
 * (for example {@code IdRepoServiceImpl}, {@code IdRepoProxyServiceImpl}).
 * </p>
 * <p>
 * <b>Callers:</b> identity REST controllers, in-process clients such as
 * {@code InProcessIdentityClient}, and auth-type status services.
 * </p>
 *
 * @author Manoj SP
 * @param <REQUEST>  inbound request DTO type
 * @param <RESPONSE> outbound response DTO or entity type
 */
public interface IdRepoService<REQUEST, RESPONSE> {

	/**
	 * Creates a new identity record with demographic and biometric documents.
	 *
	 * @param request identity registration/update payload
	 * @param uin     assigned UIN for the new record
	 * @return created identity response
	 * @throws IdRepoAppException on validation or persistence failure
	 */
	RESPONSE addIdentity(REQUEST request, String uin) throws IdRepoAppException;

	/**
	 * Retrieves an identity record for the given identifier and type.
	 * <p>
	 * When {@code type} is {@code bio}, individual biometrics plus identity
	 * metadata are returned; {@code demo} returns demographic documents;
	 * {@code all} returns both. When {@code type} is omitted, stored identity
	 * JSON is returned. {@code extractionFormats} selects biometric template
	 * formats per modality.
	 * </p>
	 *
	 * @param id                 UIN, VID, RID, or handle value
	 * @param idType             identifier type ({@link IdType})
	 * @param type               retrieval scope ({@code bio}, {@code demo}, {@code all}, or null)
	 * @param extractionFormats  per-modality biometric extraction format map
	 * @return identity response with requested data subsets
	 * @throws IdRepoAppException if the identifier is not found or access is denied
	 */
	RESPONSE retrieveIdentity(String id, IdType idType, String type, Map<String, String> extractionFormats) throws IdRepoAppException;

	/**
	 * Updates an existing identity record for the given UIN.
	 *
	 * @param request update payload
	 * @param uin     UIN of the record to update
	 * @return updated identity response
	 * @throws IdRepoAppException on validation or persistence failure
	 */
	RESPONSE updateIdentity(REQUEST request, String uin) throws IdRepoAppException;

	/**
	 * Resolves the Registration ID (RID) for a given individual identifier.
	 *
	 * @param individualId individual identifier (UIN, VID, etc.)
	 * @param idType       identifier type
	 * @return RID string for the individual
	 * @throws IdRepoAppException if the identifier cannot be resolved
	 */
	String getRidByIndividualId(String individualId, IdType idType) throws IdRepoAppException;

	/**
	 * Returns metadata (RID, created-on, updated-on) for the given identifier.
	 *
	 * @param individualId individual identifier
	 * @param idType       identifier type
	 * @return metadata DTO
	 * @throws IdRepoAppException if the identifier is not found
	 */
	IdVidMetadataResponseDTO getIdVidMetadata(String individualId, IdType idType) throws IdRepoAppException;

	/**
	 * Returns the remaining allowed update count per attribute for an individual.
	 *
	 * @param individualId  UIN or other individual identifier
	 * @param idType        identifier type
	 * @param attributeList demographic attributes to check
	 * @return map of attribute name to remaining update count
	 * @throws IdRepoAppException if the individual is not found
	 */
	Map<String, Integer> getRemainingUpdateCountByIndividualId(String individualId, IdType idType,
			List<String> attributeList) throws IdRepoAppException;
}