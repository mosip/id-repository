package io.mosip.idrepository.core.spi;

import java.util.List;
import java.util.Map;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.HandleInfoDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;

/**
 * The Interface IdRepoService - service to provide functionality to create, 
 * retrieve and update Uin data in Id repository.
 *
 * @author Manoj SP
 * @param <REQUEST> the Request Object
 * @param <RESPONSE> the Response Object
 */
public interface IdRepoService<REQUEST, RESPONSE> {

	/**
	 * This service will create a new ID record in ID repository and store 
	 * corresponding demographic and bio-metric documents.
	 *
	 * @param request the request
	 * @param uin     uin
	 * @return the response
	 * @throws IdRepoAppException the id repo app exception
	 */
	RESPONSE addIdentity(REQUEST request, String uin) throws IdRepoAppException;

	/**
	 * This service will retrieve an ID record from ID repository for a given UIN
	 * (Unique Identification Number) and identity type as bio/demo/all.
	 * 
	 * 1. When type=bio is selected, individualBiometrics along with Identity
	 * details of the Individual are returned 
	 * 2. When type=demo is selected,
	 * Demographic documents along with Identity details of the Individual are
	 * returned 
	 * 3. When type=all is selected, both individualBiometrics and
	 * demographic documents are returned along with Identity details of the
	 * Individual 
	 * 4. If no identity type is provided, stored Identity details of the
	 * Individual will be returned as a default response.
	 *
	 * @param id uin/vid/rid
	 * @param idType 
	 * @param type the type
	 * @param extractionFormat 
	 * @return the response
	 * @throws IdRepoAppException the id repo app exception
	 */
	RESPONSE retrieveIdentity(String id, IdType idType, String type, Map<String, String> extractionFormats) throws IdRepoAppException;

	/**
	 * This operation will update an existing ID record in the ID repository for a 
	 * given UIN (Unique Identification Number).
	 *
	 * @param request the request
	 * @param uin     uin
	 * @return the response
	 * @throws IdRepoAppException the id repo app exception
	 */
	RESPONSE updateIdentity(REQUEST request, String uin) throws IdRepoAppException;

	/**
	 * This function takes an individualId and an IdType as input and returns the
	 * RID in the
	 * form of a ResponseWrapper object
	 * 
	 * @param individualId The ID of the individual whose RID is to be retrieved.
	 * @param idType       The type of ID that you're passing in.
	 * @return ResponseWrapper<String>
	 */
	String getRidByIndividualId(String individualId, IdType idType) throws IdRepoAppException;
	
	/**
	 * This function is used to get the maximum allowed update count of an attribute
	 * for the given individual id
	 * 
	 * @param individualId  The UIN of the individual
	 * @param idType        The type of the ID. For example, UIN, RID, VID, etc.
	 * @param attributeList List of attributes for which the update count is to be
	 *                      retrieved.
	 * @return A map of attribute name and the maximum allowed update count for that
	 *         attribute.
	 */
	Map<String, Integer> getRemainingUpdateCountByIndividualId(String individualId, IdType idType,
			List<String> attributeList) throws IdRepoAppException;
}
