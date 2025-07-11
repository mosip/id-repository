package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.ChannelInfoHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.UUIDUtils;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class AnonymousProfileHelperTest {

	@InjectMocks
	private AnonymousProfileHelper anonymousProfileHelper;

	@Mock
	private AnonymousProfileRepo anonymousProfileRepo;

	@Autowired
	private ObjectMapper mapper;

	@Mock
	private ObjectStoreHelper objectStoreHelper;

	@Mock
	private ChannelInfoHelper channelInfoHelper;

	IdentityMapping identityMapping;
	
	private String cbeff;
	
	private String identityData;

	@Before
	public void init() throws Exception {
		ReflectionTestUtils.setField(anonymousProfileHelper, "mapper", mapper);
		ReflectionTestUtils.setField(anonymousProfileHelper, "identityMappingJson", "");
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		identityMapping = mapper.readValue(
				IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-mapping.json"),
						StandardCharsets.UTF_8),
				IdentityMapping.class);
		IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");
	}

	@Test
	public void testBuildAndsaveProfile() throws JsonProcessingException {
		anonymousProfileHelper
		.setRegId("1")
		.setNewUinData(identityData.getBytes())
		.setNewCbeff(cbeff)
		.setOldCbeff(cbeff)
		.setOldUinData(identityData.getBytes())
		.buildAndsaveProfile(false);
		AnonymousProfileEntity expectedData = new AnonymousProfileEntity();
		IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
				.setFilterLanguage("eng")
				.setProcessName("Update")
				.setOldIdentity(identityData.getBytes())
				.setOldDocuments(List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
						.getIdentity().getIndividualBiometrics().getValue(), cbeff)))
				.setNewIdentity(identityData.getBytes())
				.setNewDocuments(List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
								.getIdentity().getIndividualBiometrics().getValue(), cbeff)))
				.build();
		expectedData.setProfile(mapper.writeValueAsString(profile));
		expectedData.setCreatedBy(IdRepoSecurityManager.getUser());
		ArgumentCaptor<AnonymousProfileEntity> capturedData = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo).save(capturedData.capture());
		AnonymousProfileEntity actualData = capturedData.getValue();
		expectedData.setId(actualData.getId());
		actualData.setCrDTimes(null);
		assertEquals(expectedData, actualData);
	}

	@Test
	public void testBuildAndsaveProfileWithFileRefId() throws JsonProcessingException, IdRepoAppException {
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		anonymousProfileHelper
		.setRegId("1")
		.setNewUinData(identityData.getBytes())
		.setNewCbeff("12_12", "1234")
		.setOldCbeff("12_12", "1234")
		.setOldUinData(identityData.getBytes())
		.buildAndsaveProfile(false);
		AnonymousProfileEntity expectedData = new AnonymousProfileEntity();
		IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
				.setFilterLanguage("eng")
				.setProcessName("Update")
				.setOldIdentity(identityData.getBytes())
				.setOldDocuments(List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
						.getIdentity().getIndividualBiometrics().getValue(), cbeff)))
				.setNewIdentity(identityData.getBytes())
				.setNewDocuments(List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
								.getIdentity().getIndividualBiometrics().getValue(), cbeff)))
				.build();
		expectedData.setProfile(mapper.writeValueAsString(profile));
		expectedData.setCreatedBy(IdRepoSecurityManager.getUser());
		ArgumentCaptor<AnonymousProfileEntity> capturedData = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo).save(capturedData.capture());
		AnonymousProfileEntity actualData = capturedData.getValue();
		actualData.setCrDTimes(null);
		expectedData.setId(actualData.getId());
		assertTrue(anonymousProfileHelper.isNewCbeffPresent());
		assertTrue(anonymousProfileHelper.isOldCbeffPresent());
		assertEquals(expectedData, actualData);
		anonymousProfileHelper
		.setRegId("2");
		assertEquals(expectedData, actualData);
	}

	@Test
	public void testBuildAndsaveProfileWithInvalidCbeff() throws JsonProcessingException, IdRepoAppException {
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn("abcd".getBytes());
		anonymousProfileHelper
		.setRegId("1")
		.setNewUinData(identityData.getBytes())
		.setNewCbeff("12_12", "1234")
		.setOldCbeff("12_12", "1234")
		.setOldUinData(identityData.getBytes())
		.buildAndsaveProfile(false);
		AnonymousProfileEntity expectedData = new AnonymousProfileEntity();
		IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
				.setFilterLanguage("eng")
				.setProcessName("Update")
				.setOldIdentity(identityData.getBytes())
				.setOldDocuments(List.of())
				.setNewIdentity(identityData.getBytes())
				.setNewDocuments(List.of())
				.build();
		expectedData.setProfile(mapper.writeValueAsString(profile));
		expectedData.setCreatedBy(IdRepoSecurityManager.getUser());
		ArgumentCaptor<AnonymousProfileEntity> capturedData = ArgumentCaptor.forClass(AnonymousProfileEntity.class);
		verify(anonymousProfileRepo).save(capturedData.capture());
		AnonymousProfileEntity actualData = capturedData.getValue();
		actualData.setCrDTimes(null);
		expectedData.setId(actualData.getId());
		assertTrue(anonymousProfileHelper.isNewCbeffPresent());
		assertTrue(anonymousProfileHelper.isOldCbeffPresent());
		assertEquals(expectedData, actualData);
		anonymousProfileHelper
		.setRegId("2");
		assertEquals(expectedData, actualData);
	}

	@Test
	public void testBuildAndsaveProfileWithNullRegId() throws JsonProcessingException, IdRepoAppException {
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn("abcd".getBytes());
		anonymousProfileHelper
		.setRegId(null)
		.setNewUinData(identityData.getBytes())
		.setNewCbeff("12_12", "1234")
		.setOldCbeff("12_12", "1234")
		.setOldUinData(identityData.getBytes())
		.buildAndsaveProfile(false);
	}
}