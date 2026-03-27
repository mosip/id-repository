package io.mosip.credentialstore.test.provider.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.credentialstore.dto.BestFingerDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.SignatureException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
		ReflectionTestUtils.setField(verCredProvider, "vcContextJsonld", vcContextJsonld);
		ReflectionTestUtils.setField(verCredProvider, "confDocumentLoader", new ConfigurableDocumentLoader());
		ReflectionTestUtils.setField(verCredProvider, "verCredTypes", List.of("TestType"));
		ReflectionTestUtils.setField(verCredProvider, "proofType", "Ed25519Signature2018");
		ReflectionTestUtils.setField(verCredProvider, "proofPurpose", "assertionMethod");
		ReflectionTestUtils.setField(verCredProvider, "verificationMethod", "did:example:123#key-1");
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

	@Test
	public void testBestTwoFingerBranchExceptionThrown() throws ApiNotAccessibleException, SignatureException {

		Mockito.when(vcContextJsonld.get("context")).thenReturn("dummy");
		Mockito.when(utilities.generateId()).thenReturn("CRED-001");
		Mockito.when(digitalSignatureUtil.signVerCred(Mockito.any(), Mockito.any()))
				.thenReturn("signed-jws-value");
		AllowedKycDto dto = new AllowedKycDto();
		dto.setAttributeName("bestFinger");
		dto.setFormat("bestTwoFingers");

		BestFingerDto bf1 = Mockito.mock(BestFingerDto.class);
		bf1.setSubType("L1");
		bf1.setRank(1);

		List<BestFingerDto> list = List.of(bf1);

		Map<AllowedKycDto, Object> sharable = new HashMap<>();
		sharable.put(dto, list);

		CredentialServiceRequestDto req = new CredentialServiceRequestDto();
		req.setId("123");
		req.setRequestId("REQ1");
		req.setEncryptionKey("pin");
		CredentialFormatterException ex = assertThrows(
				CredentialFormatterException.class,
				() -> verCredProvider.getFormattedCredentialData(req, sharable)
		);
	}

	@Test
	public void testNonStringValueBranchThrowsFormatterException() throws Exception {

		Mockito.when(vcContextJsonld.get("context")).thenReturn("dummy");
		Mockito.when(utilities.generateId()).thenReturn("CRED-001");
		Mockito.when(digitalSignatureUtil.signVerCred(Mockito.any(), Mockito.any()))
				.thenReturn("signed-jws-value");
		AllowedKycDto dto = new AllowedKycDto();
		dto.setAttributeName("age");

		Map<AllowedKycDto, Object> sharable = new HashMap<>();
		sharable.put(dto, 25);

		CredentialServiceRequestDto req = new CredentialServiceRequestDto();
		req.setId("001");
		req.setRequestId("REQ2");
		req.setEncryptionKey("pin");
		CredentialFormatterException ex = assertThrows(
				CredentialFormatterException.class,
				() -> verCredProvider.getFormattedCredentialData(req, sharable)
		);
		assertTrue(ex.getMessage().contains("IDR-CRS-006"));
	}
}
