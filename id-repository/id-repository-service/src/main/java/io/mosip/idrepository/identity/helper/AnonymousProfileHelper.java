package io.mosip.idrepository.identity.helper;

import io.mosip.kernel.core.util.DateUtils2;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.retry.WithRetry;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * Builds and persists anonymous identity issuance profiles (IOV) asynchronously.
 * <p>
 * Request-scoped data is accumulated via fluent setters in a {@link ThreadLocal}
 * so concurrent HTTP threads do not overwrite each other. {@link #buildAndsaveProfile(boolean)}
 * captures that context on the caller thread and passes a snapshot to the async worker,
 * because {@code @Async} runs on a different thread where {@link ThreadLocal} is empty.
 * </p>
 */
@Component
@Transactional
public class AnonymousProfileHelper {

	/** Mosip logger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(AnonymousProfileHelper.class);

	@Autowired
	/** Anonymous profile repo. */
	private AnonymousProfileRepo anonymousProfileRepo;

	@Autowired
	/** Mapper. */
	private ObjectMapper mapper;

	@Autowired
	/** Object store helper. */
	private ObjectStoreHelper objectStoreHelper;

	@Autowired
	/** Channel info helper. */
	private ChannelInfoHelper channelInfoHelper;

	@Autowired
	@Lazy
	/** Self. */
	private AnonymousProfileHelper self;

	@Value("${" + IdRepoConstants.IDENTITY_MAPPING_JSON + "}")
	/** Identity mapping json. */
	private String identityMappingJson;

	/** Context holder. */
	private final ThreadLocal<ProfileContext> contextHolder = new ThreadLocal<>();

	@PostConstruct
	/**
	 * Init.
	 */
	public void init() throws IOException {
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(
					IOUtils.toString(xsdBytes, StandardCharsets.UTF_8), IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder.setDateFormat(EnvUtil.getIovDateFormat());
	}

	/**
	 * Captures request-thread context and hands off profile construction to the async executor.
	 *
	 * @param isDraft when {@code true}, clears context and skips profile creation (draft flow)
	 */
	public void buildAndsaveProfile(boolean isDraft) {
		ProfileContext ctx = detachContext();
		if (isDraft) {
			return;
		}
		if (ctx == null || ctx.newUinData == null || ctx.regId == null) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper",
					"buildAndsaveProfile",
					"Missing regId, context, or newUinData. Skipping profile creation.");
			return;
		}
		self.executeBuildAndSaveProfile(copyContext(ctx));
	}

	@Async("anonymousProfileExecutor")
	/**
	 * Execute build and save profile.
	 * @param ctx ctx
	 */
	public void executeBuildAndSaveProfile(ProfileContext ctx) {
		try {
			if (ctx.oldCbeff == null && ctx.oldCbeffRefId != null) {
				ctx.oldCbeff = encodeCbeffIfAvailable(ctx.oldCbeffRefId, ctx.uinHash);
			}
			if (ctx.newCbeff == null && ctx.newCbeffRefId != null) {
				ctx.newCbeff = encodeCbeffIfAvailable(ctx.newCbeffRefId, ctx.uinHash);
			}
			List<DocumentsDTO> oldDocs = ctx.oldCbeff != null
					? List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), ctx.oldCbeff))
					: Collections.emptyList();
			List<DocumentsDTO> newDocs = ctx.newCbeff != null
					? List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), ctx.newCbeff))
					: Collections.emptyList();
			String id = UUID.randomUUID().toString();
			IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
					.setFilterLanguage(EnvUtil.getAnonymousProfileFilterLanguage())
					.setProcessName(ctx.oldUinData == null ? "New" : "Update")
					.setOldIdentity(ctx.oldUinData)
					.setOldDocuments(oldDocs)
					.setNewIdentity(ctx.newUinData)
					.setNewDocuments(newDocs)
					.build();
			AnonymousProfileEntity anonymousProfile = AnonymousProfileEntity.builder().id(id)
					.profile(mapper.writeValueAsString(profile))
					.createdBy(IdRepoSecurityManager.getUser())
					.crDTimes(DateUtils2.getUTCCurrentDateTime()).build();
			anonymousProfileRepo.save(anonymousProfile);
			updateChannelInfo(ctx.oldUinData, ctx.newUinData);
		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper",
					"executeBuildAndSaveProfile", ExceptionUtils.getStackTrace(e));
		}
	}

	private String encodeCbeffIfAvailable(String refId, String uinHash) {
		if (refId == null || uinHash == null) {
			return null;
		}
		try {
			byte[] bytes = objectStoreHelper.getBiometricObject(uinHash, refId);
			return CryptoUtil.encodeToURLSafeBase64(bytes);
		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper",
					"encodeCbeffIfAvailable", e.getMessage());
			return null;
		}
	}

	@WithRetry
	private void updateChannelInfo(byte[] oldUinData, byte[] newUinData) {
		channelInfoHelper.updatePhoneChannelInfo(oldUinData, newUinData);
		channelInfoHelper.updateEmailChannelInfo(oldUinData, newUinData);
	}

	/**
	 * @param oldUinData old uin data
	 */
	public AnonymousProfileHelper setOldUinData(byte[] oldUinData) {
		getContext().oldUinData = oldUinData;
		return this;
	}

	/**
	 * @param newUinData new uin data
	 */
	public AnonymousProfileHelper setNewUinData(byte[] newUinData) {
		getContext().newUinData = newUinData;
		return this;
	}

	/**
	 * @param oldCbeff old cbeff
	 */
	public AnonymousProfileHelper setOldCbeff(String oldCbeff) {
		getContext().oldCbeff = oldCbeff;
		return this;
	}

	/**
	 * @return whether old cbeff present
	 */
	public boolean isOldCbeffPresent() {
		ProfileContext ctx = contextHolder.get();
		return ctx != null && ctx.oldCbeff != null;
	}

	/**
	 * @param newCbeff new cbeff
	 */
	public AnonymousProfileHelper setNewCbeff(String newCbeff) {
		getContext().newCbeff = newCbeff;
		return this;
	}

	/**
	 * @return whether new cbeff present
	 */
	public boolean isNewCbeffPresent() {
		ProfileContext ctx = contextHolder.get();
		return ctx != null && ctx.newCbeff != null;
	}

	/**
	 * Set old cbeff.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @return anonymous profile helper
	 */
	public AnonymousProfileHelper setOldCbeff(String uinHash, String fileRefId) {
		ProfileContext ctx = getContext();
		if (ctx.oldCbeff == null) {
			ctx.uinHash = normalizeUinHash(uinHash);
			ctx.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	/**
	 * Set new cbeff.
	 * @param uinHash uin hash
	 * @param fileRefId file ref id
	 * @return anonymous profile helper
	 */
	public AnonymousProfileHelper setNewCbeff(String uinHash, String fileRefId) {
		ProfileContext ctx = getContext();
		if (ctx.newCbeff == null) {
			ctx.uinHash = normalizeUinHash(uinHash);
			ctx.newCbeffRefId = fileRefId;
		}
		return this;
	}

	/**
	 * @param regId reg id
	 */
	public AnonymousProfileHelper setRegId(String regId) {
		ProfileContext ctx = getContext();
		if (ctx.regId != null && regId != null && !ctx.regId.contentEquals(regId)) {
			resetData();
			ctx = getContext();
		}
		ctx.regId = regId;
		return this;
	}

	private ProfileContext getContext() {
		ProfileContext ctx = contextHolder.get();
		if (ctx == null) {
			ctx = new ProfileContext();
			contextHolder.set(ctx);
		}
		return ctx;
	}

	private ProfileContext detachContext() {
		ProfileContext ctx = contextHolder.get();
		contextHolder.remove();
		return ctx;
	}

	private void resetData() {
		contextHolder.remove();
	}

	private static String normalizeUinHash(String uinHash) {
		String substringHash = StringUtils.substringAfter(uinHash, "_");
		return StringUtils.isBlank(substringHash) ? uinHash : substringHash;
	}

	private static ProfileContext copyContext(ProfileContext src) {
		ProfileContext copy = new ProfileContext();
		copy.oldUinData = src.oldUinData;
		copy.newUinData = src.newUinData;
		copy.oldCbeff = src.oldCbeff;
		copy.newCbeff = src.newCbeff;
		copy.oldCbeffRefId = src.oldCbeffRefId;
		copy.newCbeffRefId = src.newCbeffRefId;
		copy.uinHash = src.uinHash;
		copy.regId = src.regId;
		return copy;
	}

	private static class ProfileContext {
		byte[] oldUinData;
		byte[] newUinData;
		String oldCbeff;
		String newCbeff;
		String oldCbeffRefId;
		String newCbeffRefId;
		String uinHash;
		String regId;
	}
}
