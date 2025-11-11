package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
        // Run the method under test
        anonymousProfileHelper
                .setRegId("1")
                .setNewUinData(identityData.getBytes())
                .setNewCbeff(cbeff)
                .setOldCbeff(cbeff)
                .setOldUinData(identityData.getBytes())
                .buildAndsaveProfile(false);

        // Prepare expected AnonymousProfileEntity
        IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName("Update")
                .setOldIdentity(identityData.getBytes())
                .setOldDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .setNewIdentity(identityData.getBytes())
                .setNewDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .build();

        AnonymousProfileEntity expectedData = new AnonymousProfileEntity();
        expectedData.setProfile(mapper.writeValueAsString(profile));
        expectedData.setCreatedBy(IdRepoSecurityManager.getUser());

        // Capture the arguments passed to upsertAnonymousProfile
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> profileCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> somethingCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> timestampCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(anonymousProfileRepo, times(1))
                .upsertAnonymousProfile(
                        idCaptor.capture(),
                        profileCaptor.capture(),
                        somethingCaptor.capture(),
                        timestampCaptor.capture()
                );

        // Reconstruct the actual AnonymousProfileEntity from captured arguments
        AnonymousProfileEntity actualData = new AnonymousProfileEntity();
        actualData.setId(idCaptor.getValue());
        actualData.setProfile(profileCaptor.getValue());
        actualData.setCreatedBy(IdRepoSecurityManager.getUser());
        actualData.setCrDTimes(null); // reset timestamp if needed for comparison

        // Set expected ID (if it's generated dynamically)
        expectedData.setId(actualData.getId());

        // Assert equality
        assertEquals(expectedData, actualData);
    }


    @Test
    public void testBuildAndsaveProfileWithFileRefId() throws JsonProcessingException, IdRepoAppException {
        // Mock the ObjectStoreHelper to return decoded CBEFF bytes
        when(objectStoreHelper.getBiometricObject(any(), any()))
                .thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));

        // Call the method under test
        anonymousProfileHelper
                .setRegId("1")
                .setNewUinData(identityData.getBytes())
                .setNewCbeff("12_12", "1234")
                .setOldCbeff("12_12", "1234")
                .setOldUinData(identityData.getBytes())
                .buildAndsaveProfile(false);

        // Prepare expected profile JSON
        IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName("Update")
                .setOldIdentity(identityData.getBytes())
                .setOldDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .setNewIdentity(identityData.getBytes())
                .setNewDocuments(List.of(new DocumentsDTO(
                        IdentityIssuanceProfileBuilder.getIdentityMapping()
                                .getIdentity()
                                .getIndividualBiometrics()
                                .getValue(),
                        cbeff)))
                .build();

        String expectedProfileJson = mapper.writeValueAsString(profile);

        // Capture arguments passed to upsertAnonymousProfile
        ArgumentCaptor<String> regIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> profileCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> someStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> timestampCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(anonymousProfileRepo, times(1))
                .upsertAnonymousProfile(
                        regIdCaptor.capture(),
                        profileCaptor.capture(),
                        someStringCaptor.capture(),
                        timestampCaptor.capture()
                );

        // Verify the captured values
        assertEquals(expectedProfileJson, profileCaptor.getValue());
        assertTrue(anonymousProfileHelper.isNewCbeffPresent());
        assertTrue(anonymousProfileHelper.isOldCbeffPresent());
    }


    @Test
    public void testBuildAndsaveProfileWithInvalidCbeff() throws JsonProcessingException, IdRepoAppException {
        // Mock ObjectStoreHelper to return invalid CBEFF bytes
        when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn("abcd".getBytes());

        // Call the method under test
        anonymousProfileHelper
                .setRegId("1")
                .setNewUinData(identityData.getBytes())
                .setNewCbeff("12_12", "1234")
                .setOldCbeff("12_12", "1234")
                .setOldUinData(identityData.getBytes())
                .buildAndsaveProfile(false);

        // Prepare expected profile JSON
        IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
                .setFilterLanguage("eng")
                .setProcessName("Update")
                .setOldIdentity(identityData.getBytes())
                .setOldDocuments(List.of()) // invalid CBEFF results in empty list
                .setNewIdentity(identityData.getBytes())
                .setNewDocuments(List.of())
                .build();

        String expectedProfileJson = mapper.writeValueAsString(profile);

        // Capture arguments for upsertAnonymousProfile
        ArgumentCaptor<String> regIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> profileCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> someStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> timestampCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(anonymousProfileRepo, times(1))
                .upsertAnonymousProfile(
                        regIdCaptor.capture(),
                        profileCaptor.capture(),
                        someStringCaptor.capture(),
                        timestampCaptor.capture()
                );

        // Build actual AnonymousProfileEntity from captured values
        AnonymousProfileEntity actualData = new AnonymousProfileEntity();
        actualData.setId(regIdCaptor.getValue()); // set captured regId as id
        actualData.setProfile(profileCaptor.getValue());
        actualData.setCreatedBy(IdRepoSecurityManager.getUser());
        actualData.setCrDTimes(null); // reset timestamp to match expected

        // Prepare expected data entity
        AnonymousProfileEntity expectedData = new AnonymousProfileEntity();
        expectedData.setId(actualData.getId());
        expectedData.setProfile(expectedProfileJson);
        expectedData.setCreatedBy(IdRepoSecurityManager.getUser());

        // Assertions
        assertTrue(anonymousProfileHelper.isNewCbeffPresent());
        assertTrue(anonymousProfileHelper.isOldCbeffPresent());
        assertEquals(expectedData, actualData);
    }

    @Test
	public void testBuildAndsaveProfileWithNullRegId() throws JsonProcessingException, IdRepoAppException {
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn("abcd".getBytes());
		anonymousProfileHelper
				.setRegId(null)
				.setNewUinData(identityData.getBytes())
				.setNewCbeff("12_12", "1234")
				.setOldCbeff("12_12", "1234")
				.setOldUinData(identityData.getBytes())
				.buildAndsaveProfile(false);
	}
}