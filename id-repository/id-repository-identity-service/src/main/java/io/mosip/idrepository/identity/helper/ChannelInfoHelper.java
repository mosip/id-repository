package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.time.LocalDateTime;
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
import io.mosip.idrepository.identity.repository.ChannelInfoRepo;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.StringUtils;

/**
 * ChannelInfoHelper optimized to use atomic DB upsert with delta increment/decrement.
 * Functional behavior (NO_EMAIL/NO_PHONE handling) preserved.
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ChannelInfoHelper {

	private static final String PHONE = "phone";
	private static final String EMAIL = "email";
	private static final String NO_EMAIL = "NO_EMAIL";
	private static final String NO_PHONE = "NO_PHONE";

	@Autowired
	private ChannelInfoRepo channelInfoRepo;

	@Autowired
	private UinHashSaltRepo saltRepo;

	@Autowired
	private IdRepoSecurityManager securityManager;

	@Autowired
	private ObjectMapper mapper;

	// -- Public entry points (called from AnonymousProfileHelper) --
	public void updateEmailChannelInfo(byte[] oldUinData, byte[] newUinData) {
		// addIdentity: old==null && new!=null
		if (oldUinData == null && newUinData != null) {
			Optional<String> newHashed = getHashedEmail(newUinData);
			if (newHashed.isPresent()) {
				// create or increment by +1
				upsertWithDelta(newHashed.get(), EMAIL, 1, 1);
			} else {
				// increment NO_EMAIL counter
				upsertWithDelta(NO_EMAIL, EMAIL, 1, 1);
			}
		}

		// updateIdentity: both present
		if (oldUinData != null && newUinData != null) {
			Optional<String> oldHashed = getHashedEmail(oldUinData);
			Optional<String> newHashed = getHashedEmail(newUinData);

			// Old had no email, new has email -> decrement NO_EMAIL, increment new email
			if (oldHashed.isEmpty() && newHashed.isPresent()) {
				upsertWithDelta(NO_EMAIL, EMAIL, 1, -1);
				upsertWithDelta(newHashed.get(), EMAIL, 1, 1);
			}

			// Old and new both present and different -> decrement old, increment new
			if (oldHashed.isPresent() && newHashed.isPresent()
					&& !StringUtils.equals(oldHashed.get(), newHashed.get())) {
				upsertWithDelta(oldHashed.get(), EMAIL, 0, -1);
				upsertWithDelta(newHashed.get(), EMAIL, 1, 1);
			}
		}
	}

	public void updatePhoneChannelInfo(byte[] oldUinData, byte[] newUinData) {
		// addIdentity: old==null && new!=null
		if (oldUinData == null && newUinData != null) {
			Optional<String> newHashed = getHashedPhoneNumber(newUinData);
			if (newHashed.isPresent()) {
				upsertWithDelta(newHashed.get(), PHONE, 1, 1);
			} else {
				upsertWithDelta(NO_PHONE, PHONE, 1, 1);
			}
		}

		// updateIdentity
		if (oldUinData != null && newUinData != null) {
			Optional<String> oldHashed = getHashedPhoneNumber(oldUinData);
			Optional<String> newHashed = getHashedPhoneNumber(newUinData);

			if (oldHashed.isEmpty() && newHashed.isPresent()) {
				upsertWithDelta(NO_PHONE, PHONE, 1, -1);
				upsertWithDelta(newHashed.get(), PHONE, 1, 1);
			}

			if (oldHashed.isPresent() && newHashed.isPresent()
					&& !StringUtils.equals(oldHashed.get(), newHashed.get())) {
				upsertWithDelta(oldHashed.get(), PHONE, 0, -1);
				upsertWithDelta(newHashed.get(), PHONE, 1, 1);
			}
		}
	}

	/**
	 * Single helper that calls repository upsert with delta.
	 *
	 * @param hashedChannel - hashed key (or NO_EMAIL / NO_PHONE)
	 * @param channelType - "email" or "phone"
	 * @param initial - value to use when inserting (usually 1 or 0)
	 * @param delta - change to apply (positive to increment, negative to decrement)
	 */
	private void upsertWithDelta(String hashedChannel, String channelType, int initial, int delta) {
		String user = IdRepoSecurityManager.getUser();
		LocalDateTime now = DateUtils2.getUTCCurrentDateTime();
		channelInfoRepo.upsertAndDelta(
				hashedChannel,
				channelType,
				initial,
				delta,
				user,
				now,
				user,
				now
		);
	}

	// ---------------- hashing helpers ----------------

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
		return emailAsNumber.substring(emailAsNumber.length() - 3);
	}
}
