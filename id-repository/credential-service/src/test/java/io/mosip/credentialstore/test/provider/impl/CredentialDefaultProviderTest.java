package io.mosip.credentialstore.test.provider.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class CredentialDefaultProviderTest {
	/** The environment. */
	@Mock
	private Environment environment;	
	

	
	/** The utilities. */
	@Mock
    Utilities utilities;
	
	/** The encryption util. */
	@Mock
	EncryptionUtil encryptionUtil;
	
	// @InjectMocks
	// private CredentialDefaultProvider credentialDefaultProvider;
	
	
	@Before
	public void setUp() throws DataEncryptionFailureException, ApiNotAccessibleException, SignatureException {
		Mockito.when(environment.getProperty("mosip.credential.service.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any())).thenReturn("testdata");
		

		Mockito.when(utilities.generateId()).thenReturn("test123");
	}
	
	@Test
	public void testGetFormattedCredentialDataSuccess() throws CredentialFormatterException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
        Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<String,Boolean> encryptMap=new HashMap<>();
		encryptMap.put("name",true);
        Map<String, Object> sharableAttributesMap=new  HashMap<>();
		sharableAttributesMap.put("name", "test");
		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		// DataProviderResponse
		// dataProviderResponse=credentialDefaultProvider.getFormattedCredentialData(encryptMap,
		// credentialServiceRequestDto, sharableAttributesMap);
		// assertNotNull(dataProviderResponse);
	}

	@Ignore
	@Test(expected = CredentialFormatterException.class)
	public void testEncryptionFailure()
			throws CredentialFormatterException, DataEncryptionFailureException, ApiNotAccessibleException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<String, Boolean> encryptMap = new HashMap<>();
		encryptMap.put("name", true);
		Map<String, Object> sharableAttributesMap = new HashMap<>();
		sharableAttributesMap.put("name", "test");
		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any()))
				.thenThrow(new DataEncryptionFailureException());
		// credentialDefaultProvider.getFormattedCredentialData(encryptMap,
		// credentialServiceRequestDto, sharableAttributesMap);

	}

	@Ignore
	@Test(expected = CredentialFormatterException.class)
	public void testApiNotAccessible()
			throws CredentialFormatterException, DataEncryptionFailureException, ApiNotAccessibleException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<String, Boolean> encryptMap = new HashMap<>();
		encryptMap.put("name", true);
		Map<String, Object> sharableAttributesMap = new HashMap<>();
		sharableAttributesMap.put("name", "test");
		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any()))
				.thenThrow(new ApiNotAccessibleException());
		// credentialDefaultProvider.getFormattedCredentialData(encryptMap,
		// credentialServiceRequestDto,
		// sharableAttributesMap);

	}
}
