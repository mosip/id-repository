package io.mosip.credentialstore.test.provider.impl;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import foundation.identity.jsonld.ConfigurableDocumentLoader;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.provider.impl.VerCredProvider;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.util.EnvUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class VerCredProviderTest {

	@InjectMocks
	private VerCredProvider verCredProvider;

	@Mock
	EncryptionUtil encryptionUtil;

	@Mock
	private DigitalSignatureUtil digitalSignatureUtil;

	@Mock
	Utilities utilities;

	@Mock
	private ObjectMapper mapper;

	@Value("${config.server.file.storage.uri:}")
	private String configServerFileStorageURL;

	@Value("${mosip.credential.service.vercred.context.uri:}")
	private String vcContextUri;

	@Value("${mosip.credential.service.vercred.id.url:}")
	private String verCredIdUrl;

	@Value("${mosip.credential.service.vercred.issuer.url:}")
	private String verCredIssuer;

	@Value("#{'${mosip.credential.service.vercred.types:}'.split(',')}")
	private List<String> verCredTypes;

	@Value("${mosip.credential.service.vercred.proof.purpose:}")
	private String proofPurpose;

	@Value("${mosip.credential.service.vercred.proof.type:}")
	private String proofType;

	@Value("${mosip.credential.service.vercred.proof.verificationmethod:}")
	private String verificationMethod;

	private ConfigurableDocumentLoader confDocumentLoader = null;

	@Mock
	private JSONObject vcContextJsonld = null;

	@Mock
	private EnvUtil env;

	private static String dateTimePattern;

	@Before
	public void before() {
		ReflectionTestUtils.setField(verCredProvider, "configServerFileStorageURL", "https://test");
		ReflectionTestUtils.setField(verCredProvider, "verCredIdUrl", "https://test");
		ReflectionTestUtils.setField(verCredProvider, "verCredIssuer", "demo");
		ReflectionTestUtils.setField(verCredProvider, "proofPurpose", "test");
		ReflectionTestUtils.setField(verCredProvider, "verificationMethod", "test");
		ReflectionTestUtils.setField(env, "dateTimePattern", "yyyy-MM-ddHH:mm:ss");
	}

	@Test
	public void getFormattedCredentialDataTest() throws CredentialFormatterException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		additionalData.put("key1", "value1");
		credentialServiceRequestDto.setAdditionalData(additionalData);
		credentialServiceRequestDto.setEncryptionKey("Test");
		credentialServiceRequestDto.setId("02");
		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();
		AllowedKycDto dto = new AllowedKycDto();
		dto.setGroup("Test");
		dto.setEncrypted(true);
		dto.setAttributeName("Test");
		sharableAttributes.put(dto, "value");
		DataProviderResponse response = verCredProvider.getFormattedCredentialData(credentialServiceRequestDto,
				sharableAttributes);
		assertNotNull(response);
	}

	@Test
	public void getFormattedCredentialDataExceptionTest1() throws CredentialFormatterException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		additionalData.put("key1", "value1");
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();
		AllowedKycDto dto = new AllowedKycDto();
		dto.setGroup("Test");
		dto.setEncrypted(true);
		dto.setAttributeName("Test");
		sharableAttributes.put(dto, "value");
		DataProviderResponse response = verCredProvider.getFormattedCredentialData(credentialServiceRequestDto, sharableAttributes);
		assertNotNull(response);
	}

}
