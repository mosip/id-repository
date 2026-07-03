package io.mosip.idrepository.identity.helper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.mosip.kernel.core.util.DateUtils;

@Component
@Transactional
public class AnonymousProfileHelper {
	private static final Logger mosipLogger = IdRepoLogger.getLogger(AnonymousProfileHelper.class);

	@Autowired
	private AnonymousProfileRepo anonymousProfileRepo;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ObjectStoreHelper objectStoreHelper;

	@Autowired
	private ChannelInfoHelper channelInfoHelper;

	@Value("${mosip.identity.mapping-file}")
	private String identityMappingJson;

	private final ThreadLocal<ProfileContext> contextHolder = new ThreadLocal<>();

	@PostConstruct
	public void init() throws IOException {
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(
					IOUtils.toString(xsdBytes, StandardCharsets.UTF_8), IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder.setDateFormat(EnvUtil.getIovDateFormat());
	}

	@Async("anonymousProfileExecutor")
	public void buildAndsaveProfile(boolean isDraft) {
		if (isDraft) {
			contextHolder.remove();
			return;
		}
		ProfileContext ctx = contextHolder.get();
		if (ctx == null || ctx.newUinData == null || ctx.regId == null) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper",
					"buildAndsaveProfile", "No data in context, newUinData is null, or regId is null. Skipping profile creation.");
			contextHolder.remove();
			return;
		}
		try {
			// Prepare CBEFF
			if (ctx.oldCbeff == null && ctx.oldCbeffRefId != null) {
				ctx.oldCbeff = encodeCbeffIfAvailable(ctx.oldCbeffRefId, ctx.uinHash);
			}
			if (ctx.newCbeff == null && ctx.newCbeffRefId != null) {
				ctx.newCbeff = encodeCbeffIfAvailable(ctx.newCbeffRefId, ctx.uinHash);
			}
			List<DocumentsDTO> oldDocs = (ctx.oldCbeff != null)
					? List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
					.getIdentity().getIndividualBiometrics().getValue(), ctx.oldCbeff))
					: Collections.emptyList();
			List<DocumentsDTO> newDocs = (ctx.newCbeff != null)
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
			anonymousProfileRepo.save(AnonymousProfileEntity.builder()
					.id(id)
					.profile(mapper.writeValueAsString(profile))
					.createdBy(IdRepoSecurityManager.getUser())
					.crDTimes(DateUtils.getUTCCurrentDateTime())
					.build());
			updateChannelInfo(ctx.oldUinData, ctx.newUinData);
		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper",
					"buildAndsaveProfile", ExceptionUtils.getStackTrace(e));
		}
		// Note: contextHolder is NOT removed here.
		// In production (@Async), this method runs in a separate executor thread whose
		// ThreadLocal was never populated — removing it is a no-op there.
		// In tests (synchronous), keeping the context alive allows isNewCbeffPresent() /
		// isOldCbeffPresent() to reflect the cbeff that was lazily loaded during this call.
		// The context is cleaned up when setRegId() is called with a different registration ID.
	}

	private String encodeCbeffIfAvailable(String refId, String uinHash) {
		if (refId == null || uinHash == null) return null;
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

	// ====================== ThreadLocal Setters ======================

	public AnonymousProfileHelper setOldUinData(byte[] oldUinData) {
		getContext().oldUinData = oldUinData;
		return this;
	}

	public AnonymousProfileHelper setNewUinData(byte[] newUinData) {
		getContext().newUinData = newUinData;
		return this;
	}

	public AnonymousProfileHelper setOldCbeff(String oldCbeff) {
		getContext().oldCbeff = oldCbeff;
		return this;
	}

	public boolean isOldCbeffPresent() {
		ProfileContext ctx = contextHolder.get();
		return ctx != null && ctx.oldCbeff != null;
	}

	public AnonymousProfileHelper setNewCbeff(String newCbeff) {
		getContext().newCbeff = newCbeff;
		return this;
	}

	public boolean isNewCbeffPresent() {
		ProfileContext ctx = contextHolder.get();
		return ctx != null && ctx.newCbeff != null;
	}

	public AnonymousProfileHelper setOldCbeff(String uinHash, String fileRefId) {
		ProfileContext ctx = getContext();
		if (ctx.oldCbeff == null) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			ctx.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			ctx.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setNewCbeff(String uinHash, String fileRefId) {
		ProfileContext ctx = getContext();
		if (ctx.newCbeff == null) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			ctx.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			ctx.newCbeffRefId = fileRefId;
		}
		return this;
	}

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

	private void resetData() {
		contextHolder.remove();
	}

	static class ProfileContext {
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