package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
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
import io.mosip.kernel.core.util.DateUtils;
@Ignore
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
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());
		channelInfoHelper.updateEmailChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("", actualValue.getHashedChannel());
		assertEquals(1, actualValue.getNoOfRecords().intValue());
		assertEquals("email", actualValue.getChannelType());
	}
	
	@Test
	public void testUpdatePhoneChannelInfoNewRecord() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());
		channelInfoHelper.updatePhoneChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("", actualValue.getHashedChannel());
		assertEquals(1, actualValue.getNoOfRecords().intValue());
		assertEquals("phone", actualValue.getChannelType());
	}
	
	@Test
	public void testUpdateNoEmailChannelInfoNewRecord() {
		IdentityIssuanceProfileBuilder.setIdentityMapping(null);
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());
		channelInfoHelper.updateEmailChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("NO_EMAIL", actualValue.getHashedChannel());
		assertEquals(1, actualValue.getNoOfRecords().intValue());
		assertEquals("email", actualValue.getChannelType());
	}
	
	@Test
	public void testUpdateNoPhoneChannelInfoNewRecord() {
		IdentityIssuanceProfileBuilder.setIdentityMapping(null);
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.empty());
		channelInfoHelper.updatePhoneChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("NO_PHONE", actualValue.getHashedChannel());
		assertEquals(1, actualValue.getNoOfRecords().intValue());
		assertEquals("phone", actualValue.getChannelType());
	}
	
	@Test
	public void testUpdateEmailChannelInfoNewRecordEmailAlreadyPresent() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
									.hashedChannel("")
									.noOfRecords(1)
									.channelType("email")
									.createdBy(IdRepoSecurityManager.getUser())
									.crDTimes(DateUtils.getUTCCurrentDateTime())
									.build()));
		channelInfoHelper.updateEmailChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("", actualValue.getHashedChannel());
		assertEquals(2, actualValue.getNoOfRecords().intValue());
		assertEquals("email", actualValue.getChannelType());
		assertNotNull(actualValue.getUpdatedBy());
		assertNotNull(actualValue.getUpdDTimes());
	}
	
	@Test
	public void testUpdatePhoneChannelInfoNewRecordPhoneAlreadyPresent() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("")
				.noOfRecords(1)
				.channelType("phone")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()));
		channelInfoHelper.updatePhoneChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("", actualValue.getHashedChannel());
		assertEquals(2, actualValue.getNoOfRecords().intValue());
		assertEquals("phone", actualValue.getChannelType());
		assertNotNull(actualValue.getUpdatedBy());
		assertNotNull(actualValue.getUpdDTimes());
	}
	
	@Test
	public void testUpdateNoEmailChannelInfoNewRecordNoEmailRecordAlreadyExists() {
		IdentityIssuanceProfileBuilder.setIdentityMapping(null);
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("NO_EMAIL")
				.noOfRecords(1)
				.channelType("email")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()));
		channelInfoHelper.updateEmailChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("NO_EMAIL", actualValue.getHashedChannel());
		assertEquals(2, actualValue.getNoOfRecords().intValue());
		assertEquals("email", actualValue.getChannelType());
		assertNotNull(actualValue.getUpdatedBy());
		assertNotNull(actualValue.getUpdDTimes());
	}
	
	@Test
	public void testUpdateNoPhoneChannelInfoNewRecordNoPhoneRecordAlreadyExists() {
		IdentityIssuanceProfileBuilder.setIdentityMapping(null);
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("NO_PHONE")
				.noOfRecords(1)
				.channelType("phone")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()));
		channelInfoHelper.updatePhoneChannelInfo(null, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo).save(captureData.capture());
		ChannelInfo actualValue = captureData.getValue();
		assertEquals("NO_PHONE", actualValue.getHashedChannel());
		assertEquals(2, actualValue.getNoOfRecords().intValue());
		assertEquals("phone", actualValue.getChannelType());
		assertNotNull(actualValue.getUpdatedBy());
		assertNotNull(actualValue.getUpdDTimes());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateEmailChannelInfoUpdateRecordEmailUpdate() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("", "a");
		when(channelInfoRepo.save(any())).thenReturn(ChannelInfo.builder()
								.hashedChannel("a")
								.noOfRecords(0)
								.channelType("email")
								.createdBy(IdRepoSecurityManager.getUser())
								.crDTimes(DateUtils.getUTCCurrentDateTime())
								.build());
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("")
				.noOfRecords(1)
				.channelType("email")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()),
				Optional.empty());
		channelInfoHelper.updateEmailChannelInfo(identityData, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo, times(3)).save(captureData.capture());
		ChannelInfo newChannelData = captureData.getAllValues().get(0);
		assertEquals("a", newChannelData.getHashedChannel());
		assertEquals(0, newChannelData.getNoOfRecords().intValue());
		assertEquals("email", newChannelData.getChannelType());
		ChannelInfo reducedChannelData = captureData.getAllValues().get(1);
		assertEquals("", reducedChannelData.getHashedChannel());
		assertEquals(0, reducedChannelData.getNoOfRecords().intValue());
		assertEquals("email", reducedChannelData.getChannelType());
		ChannelInfo updatedChannelData = captureData.getAllValues().get(2);
		assertEquals("a", updatedChannelData.getHashedChannel());
		assertEquals(1, updatedChannelData.getNoOfRecords().intValue());
		assertEquals("email", updatedChannelData.getChannelType());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdatePhoneChannelInfoUpdateRecordPhoneUpdate() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("", "a");
		when(channelInfoRepo.save(any())).thenReturn(ChannelInfo.builder()
				.hashedChannel("a")
				.noOfRecords(0)
				.channelType("phone")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build());
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("")
				.noOfRecords(1)
				.channelType("phone")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()), 
				Optional.empty());
		channelInfoHelper.updatePhoneChannelInfo(identityData, identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo, times(3)).save(captureData.capture());
		ChannelInfo newChannelData = captureData.getAllValues().get(0);
		assertEquals("a", newChannelData.getHashedChannel());
		assertEquals(0, newChannelData.getNoOfRecords().intValue());
		assertEquals("phone", newChannelData.getChannelType());
		ChannelInfo reducedChannelData = captureData.getAllValues().get(1);
		assertEquals("", reducedChannelData.getHashedChannel());
		assertEquals(0, reducedChannelData.getNoOfRecords().intValue());
		assertEquals("phone", reducedChannelData.getChannelType());
		ChannelInfo updatedChannelData = captureData.getAllValues().get(2);
		assertEquals("a", updatedChannelData.getHashedChannel());
		assertEquals(1, updatedChannelData.getNoOfRecords().intValue());
		assertEquals("phone", updatedChannelData.getChannelType());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateEmailChannelInfoOldHasNoEmailNewHasEmailNewEmailHasNoRecord() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("NO_EMAIL")
				.noOfRecords(1)
				.channelType("email")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()),
				Optional.empty());
		channelInfoHelper.updateEmailChannelInfo("{}".getBytes(), identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo, times(2)).save(captureData.capture());
		ChannelInfo reducedNoEmailValue = captureData.getAllValues().get(0);
		assertEquals("NO_EMAIL", reducedNoEmailValue.getHashedChannel());
		assertEquals(0, reducedNoEmailValue.getNoOfRecords().intValue());
		assertEquals("email", reducedNoEmailValue.getChannelType());
		ChannelInfo newEmailNewRecord = captureData.getAllValues().get(1);
		assertEquals("", newEmailNewRecord.getHashedChannel());
		assertEquals(1, newEmailNewRecord.getNoOfRecords().intValue());
		assertEquals("email", newEmailNewRecord.getChannelType());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdatePhoneChannelInfoOldHasNoPhoneNewHasPhoneNewPhoneHasNoRecord() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("NO_PHONE")
				.noOfRecords(1)
				.channelType("phone")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()),
				Optional.empty());
		channelInfoHelper.updatePhoneChannelInfo("{}".getBytes(), identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo, times(2)).save(captureData.capture());
		ChannelInfo reducedNoPhoneRecord = captureData.getAllValues().get(0);
		assertEquals("NO_PHONE", reducedNoPhoneRecord.getHashedChannel());
		assertEquals(0, reducedNoPhoneRecord.getNoOfRecords().intValue());
		assertEquals("phone", reducedNoPhoneRecord.getChannelType());
		ChannelInfo newPhoneNewRecord = captureData.getAllValues().get(1);
		assertEquals("", newPhoneNewRecord.getHashedChannel());
		assertEquals(1, newPhoneNewRecord.getNoOfRecords().intValue());
		assertEquals("phone", newPhoneNewRecord.getChannelType());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateEmailChannelInfoOldHasNoEmailNewHasEmailNewEmailHasRecord() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("NO_EMAIL")
				.noOfRecords(1)
				.channelType("email")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()),
				Optional.of(ChannelInfo.builder()
						.hashedChannel("")
						.noOfRecords(1)
						.channelType("email")
						.createdBy(IdRepoSecurityManager.getUser())
						.crDTimes(DateUtils.getUTCCurrentDateTime())
						.build()));
		channelInfoHelper.updateEmailChannelInfo("{}".getBytes(), identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo, times(2)).save(captureData.capture());
		ChannelInfo reducedNoEmailValue = captureData.getAllValues().get(0);
		assertEquals("NO_EMAIL", reducedNoEmailValue.getHashedChannel());
		assertEquals(0, reducedNoEmailValue.getNoOfRecords().intValue());
		assertEquals("email", reducedNoEmailValue.getChannelType());
		ChannelInfo newEmailNewRecord = captureData.getAllValues().get(1);
		assertEquals("", newEmailNewRecord.getHashedChannel());
		assertEquals(2, newEmailNewRecord.getNoOfRecords().intValue());
		assertEquals("email", newEmailNewRecord.getChannelType());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdatePhoneChannelInfoOldHasNoPhoneNewHasPhoneNewPhoneHasRecord() {
		when(saltRepo.retrieveSaltById(anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("");
		when(channelInfoRepo.findById(any())).thenReturn(Optional.of(ChannelInfo.builder()
				.hashedChannel("NO_PHONE")
				.noOfRecords(1)
				.channelType("phone")
				.createdBy(IdRepoSecurityManager.getUser())
				.crDTimes(DateUtils.getUTCCurrentDateTime())
				.build()),
				Optional.of(ChannelInfo.builder()
						.hashedChannel("")
						.noOfRecords(1)
						.channelType("phone")
						.createdBy(IdRepoSecurityManager.getUser())
						.crDTimes(DateUtils.getUTCCurrentDateTime())
						.build()));
		channelInfoHelper.updatePhoneChannelInfo("{}".getBytes(), identityData);
		ArgumentCaptor<ChannelInfo> captureData = ArgumentCaptor.forClass(ChannelInfo.class);
		verify(channelInfoRepo, times(2)).save(captureData.capture());
		ChannelInfo reducedNoPhoneRecord = captureData.getAllValues().get(0);
		assertEquals("NO_PHONE", reducedNoPhoneRecord.getHashedChannel());
		assertEquals(0, reducedNoPhoneRecord.getNoOfRecords().intValue());
		assertEquals("phone", reducedNoPhoneRecord.getChannelType());
		ChannelInfo newPhoneNewRecord = captureData.getAllValues().get(1);
		assertEquals("", newPhoneNewRecord.getHashedChannel());
		assertEquals(2, newPhoneNewRecord.getNoOfRecords().intValue());
		assertEquals("phone", newPhoneNewRecord.getChannelType());
	}
}
