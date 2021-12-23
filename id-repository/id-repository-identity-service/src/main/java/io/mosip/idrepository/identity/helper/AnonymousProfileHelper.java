package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfile;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.entity.AnonymousProfileEntity;
import io.mosip.idrepository.identity.repository.AnonymousProfileRepo;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.UUIDUtils;

@Component
@Transactional
@ConditionalOnProperty("mosip.idrepo.anonymous-profiling-enabled")
public class AnonymousProfileHelper {
	
	Logger mosipLogger = IdRepoLogger.getLogger(AnonymousProfileHelper.class);

	@Autowired
	private AnonymousProfileRepo anonymousProfileRepo;

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ObjectStoreHelper objectStoreHelper;
	
	@Autowired
	private ChannelInfoHelper channelInfoHelper;

	@Autowired
	private Environment env;

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
		try (InputStream xsdBytes = new URL(env.getProperty("mosip.identity.mapping-file")).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, StandardCharsets.UTF_8),
					IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder.setDateFormat(env.getProperty("mosip.kernel.idobjectvalidator.date-format"));
	}

	@Async
	public void buildAndsaveProfile(boolean profilingEnabled) {
		if (profilingEnabled)
			try {
				channelInfoHelper.updatePhoneChannelInfo(oldUinData, newUinData);
				channelInfoHelper.updateEmailChannelInfo(oldUinData, newUinData);
				List<DocumentsDTO> oldDocList = List.of(new DocumentsDTO());
				List<DocumentsDTO> newDocList = List.of(new DocumentsDTO());
				if (Objects.isNull(oldCbeff) && Objects.nonNull(oldCbeffRefId))
					this.oldCbeff = CryptoUtil
							.encodeToURLSafeBase64(objectStoreHelper.getBiometricObject(uinHash, oldCbeffRefId));
				if (Objects.isNull(newCbeff) && Objects.nonNull(newCbeffRefId))
					this.newCbeff = CryptoUtil
							.encodeToURLSafeBase64(objectStoreHelper.getBiometricObject(uinHash, newCbeffRefId));
				if (Objects.nonNull(oldCbeff))
					oldDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), oldCbeff));
				if (Objects.nonNull(newCbeff))
					newDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), newCbeff));
				String id = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, regId).toString();
				IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
						.setFilterLanguage(env.getProperty("mosip.mandatory-languages", "").split(",")[0])
						.setProcessName(Objects.isNull(oldUinData) ? "New" : "Update").setOldIdentity(oldUinData)
						.setOldDocuments(oldDocList).setNewIdentity(newUinData).setNewDocuments(newDocList).build();
				AnonymousProfileEntity anonymousProfile = AnonymousProfileEntity.builder().id(id)
						.profile(mapper.writeValueAsString(profile)).createdBy(IdRepoSecurityManager.getUser())
						.crDTimes(DateUtils.getUTCCurrentDateTime()).build();
				anonymousProfileRepo.save(anonymousProfile);
			} catch (Exception e) {
				mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						ExceptionUtils.getStackTrace(e));
			}
	}

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
		return Objects.nonNull(this.oldCbeff);
	} 

	public AnonymousProfileHelper setNewCbeff(String newCbeff) {
		this.newCbeff = newCbeff;
		return this;
	}

	public boolean isNewCbeffPresent() {
		return Objects.nonNull(this.newCbeff);
	}

	public AnonymousProfileHelper setOldCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(oldCbeff)) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			this.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setNewCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(newCbeff)) {
			String substringHash = StringUtils.substringAfter(uinHash, "_");
			this.uinHash = StringUtils.isBlank(substringHash) ? uinHash : substringHash;
			this.newCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setRegId(String regId) {
		if (Objects.nonNull(this.regId) && !this.regId.contentEquals(regId))
			resetData();
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