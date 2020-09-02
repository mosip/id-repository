package io.mosip.credentialstore.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.util.IOUtils;
import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.CredentialFormatter;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.CredentialType;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.DataShare;


import io.mosip.credentialstore.dto.JsonValue;
import io.mosip.credentialstore.dto.PolicyResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.PolicyUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.credentialstore.util.WebSubUtil;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.dto.Event;
import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.Type;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * The Class CredentialStoreServiceImpl.
 * 
 * @author Sowmya
 */
@Component
public class CredentialStoreServiceImpl implements CredentialStoreService {

	/** The policy util. */
	@Autowired
	private PolicyUtil policyUtil;

	/** The idrepositary util. */
	@Autowired
	private IdrepositaryUtil idrepositaryUtil;


	/** The Constant VALUE. */
	private static final String VALUE = "value";



	/** The id auth provider. */
	@Autowired(required = true)
	@Qualifier("idauth")
	CredentialProvider idAuthProvider;

	/** The default provider. */
	@Autowired(required = true)
	@Qualifier("default")
	CredentialProvider credentialDefaultProvider;
	
	
	/** The default provider. */
	@Autowired(required = true)
	@Qualifier("qrcode")
	CredentialProvider qrCodeProvider;


	/** The data share util. */
	@Autowired
	private DataShareUtil dataShareUtil;

	/** The web sub util. */
	@Autowired
	private WebSubUtil webSubUtil;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${credential.service.credentialtype.file}")
	private String credentialTypefile;

	@Autowired
	Utilities utilities;

	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_ID. */
	private static final String CREDENTIAL_SERVICE_SERVICE_ID = "mosip.credential.service.service.id";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_VERSION. */
	private static final String CREDENTIAL_SERVICE_SERVICE_VERSION = "mosip.credential.service.service.version";
	
	
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialStoreServiceImpl.class);

	private static final String CREATE_CRDENTIAL = "createCredentialIssuance";
	
	private static final String CREDENTIAL_STORE = "CredentialStoreServiceImpl";
	
	private static final String CREDENTIAL_SERVICE_TYPE_NAME = "mosip.credential.service.type.name";
	
	private static final String CREDENTIAL_SERVICE_TYPE_NAMESPACE = "mosip.credential.service.type.namespace";
	
	@Autowired
	private CbeffUtil cbeffutil;

	
	
	@Autowired
	private AuditHelper auditHelper;
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.service.CredentialStoreService#
	 * createCredentialIssuance(io.mosip.credentialstore.dto.
	 * CredentialServiceRequestDto)
	 */
	public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
				"started creating credential");
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialServiceResponseDto credentialIssueResponseDto = new CredentialServiceResponseDto();
		CredentialServiceResponse credentialServiceResponse=null;
		CredentialProvider credentialProvider;

		try{
			String policyId=getPolicyId(credentialServiceRequestDto.getCredentialType());
			PolicyResponseDto policyDetailResponseDto = policyUtil.getPolicyDetail(policyId, credentialServiceRequestDto.getIssuer());

			     if(credentialServiceRequestDto.getAdditionalData()==null) {
			    	 Map<String,Object> additionalData=new HashMap<>();
			    	 credentialServiceRequestDto.setAdditionalData(additionalData);
			     }
                Map<String,String> bioAttributeFormatterMap= getFormatters(policyDetailResponseDto);    
				IdResponseDTO idResponseDto = idrepositaryUtil.getData(credentialServiceRequestDto,bioAttributeFormatterMap);
				Map<String,Boolean> encryptMap=new HashMap<>();
				Map<String, Object> sharableAttributeMap = setSharableAttributeValues(idResponseDto,
					credentialServiceRequestDto, policyDetailResponseDto, encryptMap);

				credentialProvider = getProvider(credentialServiceRequestDto.getCredentialType());

				DataProviderResponse dataProviderResponse = credentialProvider.getFormattedCredentialData(encryptMap,
						credentialServiceRequestDto,
						sharableAttributeMap);

				DataShare dataShare = dataShareUtil.getDataShare(dataProviderResponse.getFormattedData(), policyId,
						credentialServiceRequestDto.getIssuer());
			    EventModel eventModel=getEventModel(dataShare.getUrl(),credentialServiceRequestDto);
				webSubUtil.publishSuccess(credentialServiceRequestDto.getIssuer(),eventModel);
				credentialServiceResponse=new CredentialServiceResponse();
				credentialServiceResponse.setStatus("DONE");
				credentialServiceResponse.setCredentialId(dataProviderResponse.getCredentialId());
				credentialServiceResponse.setDataShareUrl(dataShare.getUrl());
				credentialServiceResponse.setSignature(dataShare.getSignature());
				
				credentialServiceResponse.setIssuanceDate(dataProviderResponse.getIssuanceDate());
				LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
						"ended creating credential");

		
		} catch (ApiNotAccessibleException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (IdRepoException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (CredentialFormatterException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch(WebSubClientException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorMessage());
			errorList.add(error);
			
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		}
		catch(DataShareException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
			errorList.add(error);
			
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		}catch(PolicyException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage());
			errorList.add(error);
			
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		}
		catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));

		}finally {

			credentialIssueResponseDto.setId(CREDENTIAL_SERVICE_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_SERVICE_SERVICE_VERSION));
			
			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			}else {
				credentialIssueResponseDto.setResponse(credentialServiceResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL, credentialServiceRequestDto.getId(), IdType.ID,"create credential requested");
		}
		return credentialIssueResponseDto;
	}

	private EventModel getEventModel(String url, CredentialServiceRequestDto credentialServiceRequestDto) {
		Map<String,Object> map=credentialServiceRequestDto.getAdditionalData();
		map.put("recepiant", credentialServiceRequestDto.getRecepiant());
		credentialServiceRequestDto.setAdditionalData(map);
		EventModel eventModel=new EventModel();
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		eventModel.setPublishedOn(DateUtils.toISOString(localdatetime));
		eventModel.setPublisher("CREDENTIAL_SERVICE");
		eventModel.setTopic(credentialServiceRequestDto.getIssuer()+"/"+CredentialConstants.CREDENTIAL_ISSUED);
		Event event=new Event();
		event.setData(credentialServiceRequestDto.getAdditionalData());
		event.setTimestamp(DateUtils.toISOString(localdatetime));
		event.setDataShareUri(url);
		String eventId=utilities.generateId();
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
				"event id"+eventId);
		event.setId(eventId);
		event.setTransactionId(credentialServiceRequestDto.getRequestId());
		Type type=new Type();
		type.setName(env.getProperty(CREDENTIAL_SERVICE_TYPE_NAME));
		type.setNamespace(env.getProperty(CREDENTIAL_SERVICE_TYPE_NAMESPACE));
		event.setType(type);
		eventModel.setEvent(event);
		return eventModel;
	}

	private Map<String, String> getFormatters(PolicyResponseDto policyResponseDto) {
		Map<String, String> formatterMap = new HashMap<>();
		List<AllowedKycDto> sharableAttributeList=policyResponseDto.getPolicies().getShareableAttributes();
		sharableAttributeList.forEach(dto -> {
			if(dto.getAttributeName().equalsIgnoreCase(CredentialConstants.FACE) ||dto.getAttributeName().equalsIgnoreCase(CredentialConstants.IRIS) ||dto.getAttributeName().equalsIgnoreCase(CredentialConstants.FINGER)) {
				formatterMap.put(dto.getAttributeName(), dto.getFormat());
			}
		});
		return formatterMap;
	}

	/**
	 * Gets the provider.
	 *
	 * @param credentialType the credential type
	 * @return the provider
	 */
	private CredentialProvider getProvider(String credentialType)
	                                                            {

    String provider=env.getProperty("credentialType.formatter."+credentialType.toUpperCase());
   
    if (provider.equalsIgnoreCase(CredentialFormatter.IdAuthProvider.name())) {
    	return idAuthProvider;
    }else if(provider.equalsIgnoreCase(CredentialFormatter.QrCodeProvider.name())) {
    	return qrCodeProvider;
     }else {
    	 return credentialDefaultProvider;
     }

	}
	private String getPolicyId(String credentialType)
	{
		  String policId=env.getProperty("credentialType.policyid."+credentialType.toUpperCase());
          return policId;
	}
	/**
	 * Sets the sharable attribute values.
	 *
	 * @param idResponseDto               the id response dto
	 * @param credentialServiceRequestDto the credential service request dto
	 * @param policyDetailResponseDto     the policy detail response dto
	 * @return the map
	 * @throws Exception 
	 */
	private Map<String, Object> setSharableAttributeValues(IdResponseDTO idResponseDto,
			CredentialServiceRequestDto credentialServiceRequestDto, PolicyResponseDto policyResponseDto,Map<String,Boolean> encryptionMap) throws Exception {

		Map<String, Object> attributesMap = new HashMap<>(); 
		JSONObject identity = new JSONObject((Map) idResponseDto.getResponse().getIdentity());

		List<AllowedKycDto> sharableAttributeList=policyResponseDto.getPolicies().getShareableAttributes();
		Set<String> sharableAttributeDemographicKeySet=new HashSet<>();
		Set<String> sharableAttributeBiometricKeySet=new HashSet<>();
		

		if(credentialServiceRequestDto.getSharableAttributes()!=null && !credentialServiceRequestDto.getSharableAttributes().isEmpty()) {
			credentialServiceRequestDto.getSharableAttributes().forEach(attribute -> {
				if(attribute.equalsIgnoreCase(SingleType.FACE.value()) ||attribute.equalsIgnoreCase(SingleType.IRIS.value()) ||attribute.equalsIgnoreCase(SingleType.FINGER.value())) {
					sharableAttributeBiometricKeySet.add(attribute);
					
				}else {
					sharableAttributeDemographicKeySet.add(attribute);
					
				}
				encryptionMap.put(attribute, credentialServiceRequestDto.isEncrypt());
			});
		}
			

		sharableAttributeList.forEach(dto -> {
			if(dto.getAttributeName().equalsIgnoreCase(SingleType.FACE.value()) ||dto.getAttributeName().equalsIgnoreCase(SingleType.IRIS.value()) ||dto.getAttributeName().equalsIgnoreCase(SingleType.FINGER.value())) {
				
				sharableAttributeBiometricKeySet.add(dto.getAttributeName());
				
			}else {
				sharableAttributeDemographicKeySet.add(dto.getAttributeName());
				
			}
			encryptionMap.put(dto.getAttributeName(), dto.isEncrypted());
		});
		
		for (String key : sharableAttributeDemographicKeySet) {
			Object object = identity.get(key);
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(identity, key);
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				attributesMap.put(key,  String.valueOf(jsonValues));
			} else if (object instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(identity, key);
				attributesMap.put(key, (String) json.get(VALUE));
			} else {
				attributesMap.put(key, String.valueOf(object));
			}
		}
		String value=null;
		List<DocumentsDTO> documents = idResponseDto.getResponse().getDocuments();
		for (DocumentsDTO doc : documents) {
			if (doc.getCategory().equals(CredentialConstants.INDIVIDUAL_BIOMETRICS)) {
				value = doc.getValue();
				break;
			}
		}
		
		List<BIRType>  typeList= cbeffutil.getBIRDataFromXML(value.getBytes());
		List<BIR> birList=cbeffutil.convertBIRTypeToBIR(typeList);
		Map<String, Map<String, byte[]>> biometrics = birList.stream().collect(Collectors.groupingBy(bir -> bir.getBdbInfo().getType().get(0).value(), 
				Collectors.toMap(bir -> {
					List<String> subtype = bir.getBdbInfo().getSubtype();
					String type = bir.getBdbInfo().getType().get(0).value();
					return subtype.isEmpty() ? type : (type + "_" + subtype.get(0));
				},
		  				bir -> (byte[])bir.getBdb())
				));
		 for (Entry<String, Map<String, byte[]>> entry : biometrics.entrySet()) {
			  String key=entry.getKey().toLowerCase();
			  if(sharableAttributeBiometricKeySet.contains(key)) {
				  attributesMap.put(key, entry.getValue());
			  }
		 }
		
		return attributesMap;
	}

	@Override
	public CredentialTypeResponse getCredentialTypes() {
		List<io.mosip.credentialstore.dto.Type> credentialTypes = utilities.getTypes(configServerFileStorageURL, credentialTypefile);
		CredentialTypeResponse CredentialTypeResponse = new CredentialTypeResponse();
		CredentialTypeResponse.setCredentialTypes(credentialTypes);
		return CredentialTypeResponse;
	}



}
