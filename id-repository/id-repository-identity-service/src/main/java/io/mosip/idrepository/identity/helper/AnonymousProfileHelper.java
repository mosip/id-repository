package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
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
import io.mosip.kernel.core.util.DateUtils2;

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

	private byte[] oldUinData;
	private byte[] newUinData;
	private String regId;
	private String oldCbeff;
	private String newCbeff;
	private String uinHash;
	private String oldCbeffRefId;
	private String newCbeffRefId;

	@PostConstruct
	public void init() throws IOException {
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
					IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder.setDateFormat(EnvUtil.getIovDateFormat());
	}

	@Async("anonymousProfileExecutor")
	public void buildAndsaveProfile(boolean isDraft) {
		if (isDraft) {
			return;
		}

		try {
			// prepare biometric data only if refIds are present and cbeff not already set
			if (oldCbeff == null && oldCbeffRefId != null) {
				this.oldCbeff = encodeCbeffIfAvailable(oldCbeffRefId);
			}
			if (newCbeff == null && newCbeffRefId != null) {
				this.newCbeff = encodeCbeffIfAvailable(newCbeffRefId);
			}

			List<DocumentsDTO> oldDocs = (oldCbeff != null)
					? List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
					.getIdentity().getIndividualBiometrics().getValue(), oldCbeff))
					: Collections.emptyList();

			List<DocumentsDTO> newDocs = (newCbeff != null)
					? List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
					.getIdentity().getIndividualBiometrics().getValue(), newCbeff))
					: Collections.emptyList();

			String id = UUID.randomUUID().toString();
			IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
					.setFilterLanguage(EnvUtil.getAnonymousProfileFilterLanguage())
					.setProcessName(oldUinData == null ? "New" : "Update")
					.setOldIdentity(oldUinData)
					.setOldDocuments(oldDocs)
					.setNewIdentity(newUinData)
					.setNewDocuments(newDocs)
					.build();

			// Upsert the profile in a single DB call
			anonymousProfileRepo.upsertAnonymousProfile(id,
					mapper.writeValueAsString(profile),
					IdRepoSecurityManager.getUser(),
					DateUtils2.getUTCCurrentDateTime()
			);

			// Update channel info (phone/email) - handled by ChannelInfoHelper (atomic ops)
			updateChannelInfo();

		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
					ExceptionUtils.getStackTrace(e));
		}
	}

	private String encodeCbeffIfAvailable(String refId) {
		try {
			byte[] bytes = objectStoreHelper.getBiometricObject(uinHash, refId);
			return CryptoUtil.encodeToURLSafeBase64(bytes);
		} catch (Exception e) {
			// preserve existing behavior of ignoring failures to read biometric data
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "encodeCbeffIfAvailable",
					e.getMessage());
			return null;
		}
	}

	@WithRetry
	public void updateChannelInfo() {
		// Two calls are preserved to keep original logic separation, but each call is optimized and atomic.
		channelInfoHelper.updatePhoneChannelInfo(oldUinData, newUinData);
		channelInfoHelper.updateEmailChannelInfo(oldUinData, newUinData);
	}

	// --- setters, getters and reset preserved exactly as original ---
	public AnonymousProfileHelper setOldUinData(byte[] oldUinData) {
		this.oldUinData = oldUinData;
		return this;
	}

	public AnonymousProfileHelper setNewUinData(byte[] newUinData) {
		this.newUinData = newUinData;
		return this;
	}

	public AnonymousProfileHelper setOldCbeff(String oldCbeff) {
		this.oldCbeff = oldCbeff;
		return this;
	}

	public boolean isOldCbeffPresent() {
		return oldCbeff != null;
	}

	public AnonymousProfileHelper setNewCbeff(String newCbeff) {
		this.newCbeff = newCbeff;
		return this;
	}

	public boolean isNewCbeffPresent() {
		return newCbeff != null;
	}

	public AnonymousProfileHelper setOldCbeff(String uinHash, String fileRefId) {
		if (oldCbeff == null) {
			String substringHash = org.apache.commons.lang3.StringUtils.substringAfter(uinHash, "_");
			this.uinHash = org.apache.commons.lang3.StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setNewCbeff(String uinHash, String fileRefId) {
		if (newCbeff == null) {
			String substringHash = org.apache.commons.lang3.StringUtils.substringAfter(uinHash, "_");
			this.uinHash = org.apache.commons.lang3.StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.newCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setRegId(String regId) {
		if (this.regId != null && !this.regId.contentEquals(regId)) {
			resetData();
		}
		this.regId = regId;
		return this;
	}

	private void resetData() {
		this.oldUinData = null;
		this.newUinData = null;
		this.oldCbeff = null;
		this.newCbeff = null;
		this.uinHash = null;
		this.newCbeffRefId = null;
		this.oldCbeffRefId = null;
		this.regId = null;
	}
}
