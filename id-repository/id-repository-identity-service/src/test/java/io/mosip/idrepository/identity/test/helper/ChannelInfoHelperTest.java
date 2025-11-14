package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.ChannelInfo;
import io.mosip.idrepository.identity.helper.ChannelInfoHelper;
import io.mosip.idrepository.identity.repository.ChannelInfoRepo;
import io.mosip.kernel.core.util.DateUtils2;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class ChannelInfoHelperTest {

	@InjectMocks
	private ChannelInfoHelper channelInfoHelper;

	@Mock
	private ChannelInfoRepo channelInfoRepo;

	@Mock
	private UinHashSaltRepo saltRepo;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Autowired
	private ObjectMapper mapper;

	private byte[] identityData;

	@Before
	public void init() throws IOException {
		ReflectionTestUtils.setField(channelInfoHelper, "mapper", mapper);
		IdentityIssuanceProfileBuilder.setIdentityMapping(mapper.readValue(
				IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-mapping.json"),
						StandardCharsets.UTF_8),
				IdentityMapping.class));
		identityData = IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"));
	}

    @Test
    public void testUpdateEmailChannelInfoNewRecord() {
        // Arrange
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");
        when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());

        // Act
        channelInfoHelper.updateEmailChannelInfo(null, identityData);

        // Capture the arguments passed to upsertAndDelta
        ArgumentCaptor<String> regIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> channelTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> noOfRecordsCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> deltaCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> hashedChannelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> createdAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> createdByCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> updatedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // Verify and capture
        verify(channelInfoRepo, times(1)).upsertAndDelta(
                regIdCaptor.capture(),
                channelTypeCaptor.capture(),
                noOfRecordsCaptor.capture(),
                deltaCaptor.capture(),
                hashedChannelCaptor.capture(),
                createdAtCaptor.capture(),
                createdByCaptor.capture(),
                updatedAtCaptor.capture()
        );

        // Assert the captured values
        assertEquals("", regIdCaptor.getValue());
        assertEquals("email", channelTypeCaptor.getValue());
        assertEquals(1, noOfRecordsCaptor.getValue().intValue());
        assertEquals(1, deltaCaptor.getValue().intValue());
        assertEquals("", hashedChannelCaptor.getValue());
        assertNotNull(createdAtCaptor.getValue());
        assertNotNull(updatedAtCaptor.getValue());
    }
    
    @Test
    public void testUpdateNoEmailChannelInfoNewRecord() {
        IdentityIssuanceProfileBuilder.setIdentityMapping(null);
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");
        when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());

        channelInfoHelper.updateEmailChannelInfo(null, identityData);

        // verify that upsertAndDelta is called with NO_EMAIL
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_EMAIL"),  // hashedChannel
                eq("email"),     // channelType
                eq(1),           // initial
                eq(1),           // delta
                anyString(),     // createdBy
                any(LocalDateTime.class), // crDTimes
                anyString(),     // updatedBy
                any(LocalDateTime.class)  // updDTimes
        );
    }


    @Test
    public void testUpdateNoPhoneChannelInfoNewRecord() {
        IdentityIssuanceProfileBuilder.setIdentityMapping(null);
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");
        when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());

        channelInfoHelper.updatePhoneChannelInfo(null, identityData);

        // Verify upsertAndDelta is called correctly for NO_PHONE
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_PHONE"),          // hashedChannel
                eq("phone"),             // channelType
                eq(1),                   // initial
                eq(1),                   // delta
                anyString(),             // createdBy
                any(LocalDateTime.class),// crDTimes
                anyString(),             // updatedBy
                any(LocalDateTime.class) // updDTimes
        );
    }
    
    @Test
    public void testUpdateEmailChannelInfoNewRecordEmailAlreadyPresent() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate existing email record
        when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
                .hashedChannel("")
                .noOfRecords(1)
                .channelType("email")
                .createdBy(IdRepoSecurityManager.getUser())
                .crDTimes(DateUtils2.getUTCCurrentDateTime())
                .build()));

        channelInfoHelper.updateEmailChannelInfo(null, identityData);

        // Verify upsertAndDelta is called for existing email increment
        verify(channelInfoRepo).upsertAndDelta(
                eq(""),                     // hashedChannel
                eq("email"),                // channelType
                eq(1),                      // initial
                eq(1),                      // delta (+1)
                anyString(),                // createdBy
                any(LocalDateTime.class),   // crDTimes
                anyString(),                // updatedBy
                any(LocalDateTime.class)    // updDTimes
        );
    }

    @Test
    public void testUpdatePhoneChannelInfoNewRecordPhoneAlreadyPresent() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate existing phone record
        when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
                .hashedChannel("")
                .noOfRecords(1)
                .channelType("phone")
                .createdBy(IdRepoSecurityManager.getUser())
                .crDTimes(DateUtils2.getUTCCurrentDateTime())
                .build()));

        channelInfoHelper.updatePhoneChannelInfo(null, identityData);

        // Verify upsertAndDelta is called correctly for incrementing existing record
        verify(channelInfoRepo).upsertAndDelta(
                eq(""),                    // hashedChannel
                eq("phone"),               // channelType
                eq(1),                     // initial (existing record)
                eq(1),                     // delta (+1)
                anyString(),               // createdBy
                any(LocalDateTime.class),  // crDTimes
                anyString(),               // updatedBy
                any(LocalDateTime.class)   // updDTimes
        );
    }
    
    @Test
    public void testUpdateNoEmailChannelInfoNewRecordNoEmailRecordAlreadyExists() {
        IdentityIssuanceProfileBuilder.setIdentityMapping(null);
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate existing NO_EMAIL record
        when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
                .hashedChannel("NO_EMAIL")
                .noOfRecords(1)
                .channelType("email")
                .createdBy(IdRepoSecurityManager.getUser())
                .crDTimes(DateUtils2.getUTCCurrentDateTime())
                .build()));

        channelInfoHelper.updateEmailChannelInfo(null, identityData);

        // Verify upsertAndDelta is called for NO_EMAIL increment
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_EMAIL"),             // hashedChannel
                eq("email"),                // channelType
                eq(1),                      // initial
                eq(1),                      // delta (+1)
                anyString(),                // createdBy
                any(LocalDateTime.class),   // crDTimes
                anyString(),                // updatedBy
                any(LocalDateTime.class)    // updDTimes
        );
    }

    @Test
    public void testUpdateNoPhoneChannelInfoNewRecordNoPhoneRecordAlreadyExists() {
        IdentityIssuanceProfileBuilder.setIdentityMapping(null);
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate existing NO_PHONE record
        when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
                .hashedChannel("NO_PHONE")
                .noOfRecords(1)
                .channelType("phone")
                .createdBy(IdRepoSecurityManager.getUser())
                .crDTimes(DateUtils2.getUTCCurrentDateTime())
                .build()));

        channelInfoHelper.updatePhoneChannelInfo(null, identityData);

        // Verify upsertAndDelta is called for NO_PHONE increment
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_PHONE"),            // hashedChannel
                eq("phone"),               // channelType
                eq(1),                     // initial
                eq(1),                     // delta (+1)
                anyString(),               // createdBy
                any(LocalDateTime.class),  // crDTimes
                anyString(),               // updatedBy
                any(LocalDateTime.class)   // updDTimes
        );
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEmailChannelInfoUpdateRecordEmailUpdate() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("", "a");

        // Simulate existing old email record
        when(channelInfoRepo.findById(any())).thenReturn(
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("")
                        .noOfRecords(1)
                        .channelType("email")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build()),
                Optional.empty() // simulate second call returns empty
        );

        channelInfoHelper.updateEmailChannelInfo(identityData, identityData);

        // Verify upsertAndDelta calls: 1) decrement old, 2) increment new
        verify(channelInfoRepo).upsertAndDelta(
                eq(""),                     // old hashed channel
                eq("email"),
                eq(0),                      // initial
                eq(-1),                     // delta
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );

        verify(channelInfoRepo).upsertAndDelta(
                eq("a"),                    // new hashed channel
                eq("email"),
                eq(1),                      // initial
                eq(1),                      // delta
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePhoneChannelInfoUpdateRecordPhoneUpdate() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("", "a");

        // Simulate existing old phone record
        when(channelInfoRepo.findById(any())).thenReturn(
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("")
                        .noOfRecords(1)
                        .channelType("phone")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build()),
                Optional.empty() // second call returns empty
        );

        channelInfoHelper.updatePhoneChannelInfo(identityData, identityData);

        // Verify upsertAndDelta calls: 1) decrement old, 2) increment new
        verify(channelInfoRepo).upsertAndDelta(
                eq(""),                     // old hashed channel
                eq("phone"),
                eq(0),                      // initial
                eq(-1),                     // delta
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );

        verify(channelInfoRepo).upsertAndDelta(
                eq("a"),                    // new hashed channel
                eq("phone"),
                eq(1),                      // initial
                eq(1),                      // delta
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEmailChannelInfoOldHasNoEmailNewHasEmailNewEmailHasNoRecord() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate old record with NO_EMAIL and no record for the new email yet
        when(channelInfoRepo.findById(any())).thenReturn(
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("NO_EMAIL")
                        .noOfRecords(1)
                        .channelType("email")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build()),
                Optional.empty() // second call returns empty
        );

        channelInfoHelper.updateEmailChannelInfo("{}".getBytes(), identityData);

        // Verify decrement of NO_EMAIL
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_EMAIL"),
                eq("email"),
                eq(1),
                eq(-1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );

        // Verify increment of new email
        verify(channelInfoRepo).upsertAndDelta(
                eq(""), // hashed value for new email
                eq("email"),
                eq(1),
                eq(1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePhoneChannelInfoOldHasNoPhoneNewHasPhoneNewPhoneHasNoRecord() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate old record with NO_PHONE and no record for the new phone yet
        when(channelInfoRepo.findById(any())).thenReturn(
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("NO_PHONE")
                        .noOfRecords(1)
                        .channelType("phone")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build()),
                Optional.empty() // second call returns empty
        );

        channelInfoHelper.updatePhoneChannelInfo("{}".getBytes(), identityData);

        // Verify decrement of NO_PHONE
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_PHONE"),
                eq("phone"),
                eq(1),
                eq(-1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );

        // Verify increment of new phone
        verify(channelInfoRepo).upsertAndDelta(
                eq(""), // hashed value for new phone
                eq("phone"),
                eq(1),
                eq(1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEmailChannelInfoOldHasNoEmailNewHasEmailNewEmailHasRecord() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate old record with NO_EMAIL and new email already exists
        when(channelInfoRepo.findById(any())).thenReturn(
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("NO_EMAIL")
                        .noOfRecords(1)
                        .channelType("email")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build()),
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("")
                        .noOfRecords(1)
                        .channelType("email")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build())
        );

        channelInfoHelper.updateEmailChannelInfo("{}".getBytes(), identityData);

        // Verify decrement of NO_EMAIL
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_EMAIL"),
                eq("email"),
                eq(1),
                eq(-1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );

        // Verify increment of existing email
        verify(channelInfoRepo).upsertAndDelta(
                eq(""),
                eq("email"),
                eq(1),
                eq(1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePhoneChannelInfoOldHasNoPhoneNewHasPhoneNewPhoneHasRecord() {
        when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
        when(securityManager.hashwithSalt(any(), any())).thenReturn("");

        // Simulate old record with NO_PHONE and new phone already exists
        when(channelInfoRepo.findById(any())).thenReturn(
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("NO_PHONE")
                        .noOfRecords(1)
                        .channelType("phone")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build()),
                Optional.of(ChannelInfo.builder()
                        .hashedChannel("")
                        .noOfRecords(1)
                        .channelType("phone")
                        .createdBy(IdRepoSecurityManager.getUser())
                        .crDTimes(DateUtils2.getUTCCurrentDateTime())
                        .build())
        );

        channelInfoHelper.updatePhoneChannelInfo("{}".getBytes(), identityData);

        // Verify decrement of NO_PHONE
        verify(channelInfoRepo).upsertAndDelta(
                eq("NO_PHONE"),
                eq("phone"),
                eq(1),
                eq(-1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );

        // Verify increment of existing phone
        verify(channelInfoRepo).upsertAndDelta(
                eq(""),
                eq("phone"),
                eq(1),
                eq(1),
                anyString(),
                any(LocalDateTime.class),
                anyString(),
                any(LocalDateTime.class)
        );
    }

}
