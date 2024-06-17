package io.mosip.credentialstore.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.credentialstore.constants.ApiName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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

import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
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
	public void setUp() throws Exception {
		 response=new ResponseDTO();
		idRepoResponse.setResponse(response);
		idRepo="response";
		Mockito.when(objectMapper.readValue(idRepo, IdResponseDTO.class)).thenReturn(idRepoResponse);

		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(idRepo);
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(idRepo);
	}
	@Test
	public void idRepoSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		additionalData.put("idType", "UIN");
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "mock");
		bioAttributeFormatterMap.put("finger", "mock");
		bioAttributeFormatterMap.put("iris", "mock");
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
	public void testHttpClientException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpClientErrorException);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpServerErrorException);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("12345678");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		idrepositaryUtil.getData(credentialServiceRequestDto, bioAttributeFormatterMap);
	}
	// Get Data By Id
	@Test
	public void idRepoHandleSuccessTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		additionalData.put("idType", "handle");
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("111111/01/1@nrcid");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "mock");
		bioAttributeFormatterMap.put("finger", "mock");
		bioAttributeFormatterMap.put("iris", "mock");
		IdResponseDTO idRepoResponseResult=idrepositaryUtil.getDataById(credentialServiceRequestDto, bioAttributeFormatterMap);
		assertEquals(idRepoResponseResult.getResponse(),idRepoResponse.getResponse());


	}
	@Test(expected = IdRepoException.class)
	public void idRepoHandleResponseObjectNullTest() throws JsonParseException, JsonMappingException, ApiNotAccessibleException, IdRepoException, IOException  {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("111111/01/1@nrcid");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(objectMapper.readValue(idRepo, IdResponseDTO.class)).thenReturn(null);
		idrepositaryUtil.getDataById(credentialServiceRequestDto, bioAttributeFormatterMap);


	}
	@Test(expected = IdRepoException.class)
	public void idRepoHandleResponseWithErrorTest() throws IOException, ApiNotAccessibleException, SignatureException, DataShareException, IdRepoException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("111111/01/1@nrcid");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-SIG-001");
		error.setMessage("sign error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		idRepoResponse.setErrors(errors);
		idrepositaryUtil.getDataById(credentialServiceRequestDto, bioAttributeFormatterMap);

	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientExceptionPostAPI() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpClientErrorException);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("111111/01/1@nrcid");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		idrepositaryUtil.getDataById(credentialServiceRequestDto, bioAttributeFormatterMap);
	}
	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerExceptionPostAPI() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		Exception e=new Exception(httpServerErrorException);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setId("111111/01/1@nrcid");
		Map<String,String> bioAttributeFormatterMap=new HashMap<>();
		bioAttributeFormatterMap.put("face", "extractfmr");
		Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		idrepositaryUtil.getDataById(credentialServiceRequestDto, bioAttributeFormatterMap);
	}
}
