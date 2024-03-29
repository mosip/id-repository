package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.idrepository.identity.entity.ChannelInfo;
import io.mosip.idrepository.identity.repository.ChannelInfoRepo;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;

@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ChannelInfoHelper {

	private static final String PHONE = "phone";

	private static final String EMAIL = "email";

	@Autowired
	private ChannelInfoRepo channelInfoRepo;

	@Autowired
	private UinHashSaltRepo saltRepo;

	@Autowired
	private IdRepoSecurityManager securityManager;

	@Autowired
	private ObjectMapper mapper;
	
	public void updateEmailChannelInfo(byte[] oldUinData, byte[] newUinData) {

		// addIdentity
		if (Objects.isNull(oldUinData) && Objects.nonNull(newUinData)) {
			Optional<String> hashedOldEmailOpt = getHashedEmail(newUinData);
			if (hashedOldEmailOpt.isPresent()) {
				String hashedOldEmail = hashedOldEmailOpt.get();
				Optional<ChannelInfo> oldChannelInfoOpt = channelInfoRepo.findById(hashedOldEmail);
				if (oldChannelInfoOpt.isPresent()) {
					// Update count if phone number present
					updateNoOfRecords(oldChannelInfoOpt.get(), 1);
				} else {
					// create record if record not present
					channelInfoRepo
							.save(ChannelInfo.builder()
									.hashedChannel(hashedOldEmail)
									.noOfRecords(1)
									.channelType(EMAIL)
									.createdBy(IdRepoSecurityManager.getUser())
									.crDTimes(DateUtils.getUTCCurrentDateTime())
									.build());
				}
			} else {
				// Update NO_PHONE is email not present
				updateNoChannel("NO_EMAIL", EMAIL, 1);
			}
		}

		// UpdateIdentity
		if (Objects.nonNull(oldUinData) && Objects.nonNull(newUinData)) {
			Optional<String> hashedOldEmailOpt = getHashedEmail(oldUinData);
			Optional<String> hashedNewEmailOpt = getHashedEmail(newUinData);
			
			//Old has no email. new has email.
			if (!hashedOldEmailOpt.isPresent() && hashedNewEmailOpt.isPresent()) {
				
				// update NO_EMAIL if email is updated
				updateNoChannel("NO_EMAIL", EMAIL, -1);
				String hashedNewEmail = hashedNewEmailOpt.get();
				Optional<ChannelInfo> newChannelInfoOpt = channelInfoRepo.findById(hashedNewEmail);
				if (newChannelInfoOpt.isPresent()) {
					updateNoOfRecords(newChannelInfoOpt.get(), 1);
				} else {
					channelInfoRepo
							.save(ChannelInfo.builder()
									.hashedChannel(hashedNewEmail)
									.noOfRecords(1)
									.channelType(EMAIL)
									.createdBy(IdRepoSecurityManager.getUser())
									.crDTimes(DateUtils.getUTCCurrentDateTime())
									.build());
				}
			}
			if (hashedOldEmailOpt.isPresent() && hashedNewEmailOpt.isPresent()) {
				String hashedOldEmail = hashedOldEmailOpt.get();
				String hashedNewEmail = hashedNewEmailOpt.get();
				
				if (!StringUtils.equals(hashedOldEmail, hashedNewEmail)) {
				//old channel info should exist at this stage as it will be created as part of addIdentity flow
				Optional<ChannelInfo> oldChannelInfoOpt = channelInfoRepo.findById(hashedOldEmail);
				Optional<ChannelInfo> newChannelInfoOpt = channelInfoRepo.findById(hashedNewEmail);
				
				// if email update and new email don't have a record, new record is created
				newChannelInfoOpt = Optional.of(newChannelInfoOpt.orElseGet(() -> channelInfoRepo
						.save(ChannelInfo.builder()
								.hashedChannel(hashedNewEmail)
								.noOfRecords(0)
								.channelType(EMAIL)
								.createdBy(IdRepoSecurityManager.getUser())
								.crDTimes(DateUtils.getUTCCurrentDateTime())
								.build())));
				if (oldChannelInfoOpt.isPresent())
					updateNoOfRecords(oldChannelInfoOpt.get(), -1);
				updateNoOfRecords(newChannelInfoOpt.get(), 1);
				}
			}
		}
	}

	public void updatePhoneChannelInfo(byte[] oldUinData, byte[] newUinData) {

		// addIdentity
		if (Objects.isNull(oldUinData) && Objects.nonNull(newUinData)) {
			Optional<String> hashedOldPhoneNumberOpt = getHashedPhoneNumber(newUinData);
			if (hashedOldPhoneNumberOpt.isPresent()) {
				String hashedOldPhoneNumber = hashedOldPhoneNumberOpt.get();
				Optional<ChannelInfo> oldChannelInfoOpt = channelInfoRepo.findById(hashedOldPhoneNumber);
				if (oldChannelInfoOpt.isPresent()) {
					// Update count if phone number present
					updateNoOfRecords(oldChannelInfoOpt.get(), 1);
				} else {
					// create record if record not present
					channelInfoRepo.save(ChannelInfo.builder()
							.hashedChannel(hashedOldPhoneNumber)
							.noOfRecords(1)
							.channelType(PHONE)
							.createdBy(IdRepoSecurityManager.getUser())
							.crDTimes(DateUtils.getUTCCurrentDateTime())
							.build());
				}
			} else {
				// Update NO_PHONE is phone number not present
				updateNoChannel("NO_PHONE", PHONE, 1);
			}
		}

		// UpdateIdentity
		if (Objects.nonNull(oldUinData) && Objects.nonNull(newUinData)) {
			Optional<String> hashedOldPhoneNumberOpt = getHashedPhoneNumber(oldUinData);
			Optional<String> hashedNewPhoneNumberOpt = getHashedPhoneNumber(newUinData);
			
			//Old has no phone. new has phone.
			if (!hashedOldPhoneNumberOpt.isPresent() && hashedNewPhoneNumberOpt.isPresent()) {
				updateNoChannel("NO_PHONE", PHONE, -1);
				String hashedNewPhoneNumber = hashedNewPhoneNumberOpt.get();
				Optional<ChannelInfo> newChannelInfoOpt = channelInfoRepo.findById(hashedNewPhoneNumber);
				if (newChannelInfoOpt.isPresent()) {
					updateNoOfRecords(newChannelInfoOpt.get(), 1);
				} else {
					channelInfoRepo
							.save(ChannelInfo.builder()
									.hashedChannel(hashedNewPhoneNumber)
									.channelType(PHONE)
									.noOfRecords(1)
									.createdBy(IdRepoSecurityManager.getUser())
									.crDTimes(DateUtils.getUTCCurrentDateTime())
									.build());
				}
			}
			if (hashedOldPhoneNumberOpt.isPresent() && hashedNewPhoneNumberOpt.isPresent()) {
				String hashedOldPhoneNumber = hashedOldPhoneNumberOpt.get();
				String hashedNewPhoneNumber = hashedNewPhoneNumberOpt.get();
				
				if (!StringUtils.equals(hashedOldPhoneNumber, hashedNewPhoneNumber)) {
				//old channel info should exist at this stage as it will be created as part of addIdentity flow
				Optional<ChannelInfo> oldChannelInfoOpt = channelInfoRepo.findById(hashedOldPhoneNumber);
				Optional<ChannelInfo> newChannelInfoOpt = channelInfoRepo.findById(hashedNewPhoneNumber);
				
				// if phone number update and new phone dont have a record, new record is created
				newChannelInfoOpt = Optional.of(newChannelInfoOpt.orElseGet(() -> channelInfoRepo
						.save(ChannelInfo.builder()
								.hashedChannel(hashedNewPhoneNumber)
								.channelType(PHONE)
								.noOfRecords(0)
								.createdBy(IdRepoSecurityManager.getUser())
								.crDTimes(DateUtils.getUTCCurrentDateTime())
								.build())));
				if (oldChannelInfoOpt.isPresent())
					updateNoOfRecords(oldChannelInfoOpt.get(), -1);
				updateNoOfRecords(newChannelInfoOpt.get(), 1);
				}
			}
		}
	}

	private void updateNoChannel(String channel, String channelType, Integer value) {
		Optional<ChannelInfo> noChannelOpt = channelInfoRepo.findById(channel);
		if (noChannelOpt.isPresent())
			updateNoOfRecords(noChannelOpt.get(), value);
		else
			channelInfoRepo
			.save(ChannelInfo.builder()
					.hashedChannel(channel)
					.channelType(channelType)
					.noOfRecords(1)
					.createdBy(IdRepoSecurityManager.getUser())
					.crDTimes(DateUtils.getUTCCurrentDateTime())
					.build());
	}

	private void updateNoOfRecords(ChannelInfo channelInfo, Integer value) {
		channelInfo.setNoOfRecords(channelInfo.getNoOfRecords() + value);
		channelInfo.setUpdatedBy(IdRepoSecurityManager.getUser());
		channelInfo.setUpdDTimes(DateUtils.getUTCCurrentDateTime());
		channelInfoRepo.save(channelInfo);
	}

	private Optional<String> getHashedPhoneNumber(byte[] uinData) {
		try {
			String phoneNumber = getPhoneNumber(uinData);
			String salt = saltRepo.retrieveSaltById(securityManager.getSaltKeyForId(phoneNumber));
			return Optional.of(securityManager.hashwithSalt(phoneNumber.getBytes(), CryptoUtil.decodePlainBase64(salt)));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private String getPhoneNumber(byte[] uinData) throws IOException {
		JsonNode identityData = mapper.readTree(uinData);
		return identityData
				.get(IdentityIssuanceProfileBuilder.getIdentityMapping().getIdentity().getPhone().getValue())
				.asText();
	}
	
	private Optional<String> getHashedEmail(byte[] uinData) {
		try {
			String email = getEmail(uinData);
			String emailAsNumber = emailAsNumber(email);
			String salt = saltRepo.retrieveSaltById(securityManager.getSaltKeyForId(emailAsNumber));
			return Optional.of(securityManager.hashwithSalt(email.getBytes(), CryptoUtil.decodePlainBase64(salt)));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private String getEmail(byte[] uinData) throws IOException {
		JsonNode identityData = mapper.readTree(uinData);
		return identityData
				.get(IdentityIssuanceProfileBuilder.getIdentityMapping().getIdentity().getEmail().getValue())
				.asText();
	}
	
	private String emailAsNumber(String email) {
		String emailAsNumber = email.chars().boxed().map(String::valueOf).collect(Collectors.joining());
		return emailAsNumber.substring(emailAsNumber.length() - 3, emailAsNumber.length());
	}

}