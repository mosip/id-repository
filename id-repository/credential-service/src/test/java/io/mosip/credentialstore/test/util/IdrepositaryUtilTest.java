package io.mosip.credentialstore.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.DataShareResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class IdrepositaryUtilTest {
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;
	
	@InjectMocks
	IdrepositaryUtil idrepositaryUtil;
	
	/** The id response. */
	private IdResponseDTO idRepoResponse = new IdResponseDTO();
	
	ResponseDTO response;

	String idRepo;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws ApiNotAccessibleException, JsonParseException, JsonMappingException, IOException {
		 response=new ResponseDTO();
		idRepoResponse.setResponse(response);
		idRepo="response";
		Mockito.when(objectMapper.readValue(idRepo, IdResponseDTO.class)).thenReturn(idRepoResponse);

		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(idRepo);
	}
	@Test
	public void idRepoSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		 IdResponseDTO idRepoResponseResult=idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);
		 assertEquals(idRepoResponseResult.getResponse(),idRepoResponse.getResponse());
		

	}
	@Test(expected = IdRepoException.class)
	public void idRepoResponseObjectNullTest() throws JsonParseException, JsonMappingException, ApiNotAccessibleException, IdRepoException, IOException  {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(objectMapper.readValue(idRepo, IdResponseDTO.class)).thenReturn(null);
         idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);
		

	}
	@Test(expected = IdRepoException.class)
	public void idRepoResponseWithErrorTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-SIG-001");
		error.setMessage("sign error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		idRepoResponse.setErrors(errors);
        idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);

	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpClientErrorException);
		idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws JsonParseException, JsonMappingException, IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(httpServerErrorException);
		idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);
	}
}
