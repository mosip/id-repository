package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import io.mosip.idrepository.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.UUIDUtils;

@Component
@Transactional
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

	@Value("${mosip.identity.mapping-file}")
	private String identityMappingJson;
	
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
	
	private boolean isDraft;

	@PostConstruct
	public void init() throws MalformedURLException, IOException {
		try (InputStream xsdBytes = new URL(identityMappingJson).openStream()) {
			IdentityMapping identityMapping = mapper.readValue(IOUtils.toString(xsdBytes, Charset.forName("UTF-8")),
					IdentityMapping.class);
			IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		}
		IdentityIssuanceProfileBuilder
				.setFilterLanguage(env.getProperty("mosip.mandatory-languages", "").split(",")[0]);
		IdentityIssuanceProfileBuilder.setDateFormat(env.getProperty("mosip.kernel.idobjectvalidator.date-format"));
	}

	@Async
	public void buildAndsaveProfile() {
		if (!isDraft)
			try {
				channelInfoHelper.updatePhoneChannelInfo(oldUinData, newUinData);
				channelInfoHelper.updateEmailChannelInfo(oldUinData, newUinData);
				List<DocumentsDTO> oldDocList = List.of(new DocumentsDTO());
				List<DocumentsDTO> newDocList = List.of(new DocumentsDTO());
				try {
					if (Objects.isNull(oldCbeff) && Objects.nonNull(oldCbeffRefId))
						this.oldCbeff = CryptoUtil.encodeToPlainBase64(objectStoreHelper.getBiometricObject(uinHash, oldCbeffRefId));
					if (Objects.isNull(newCbeff) && Objects.nonNull(newCbeffRefId))
						this.newCbeff = CryptoUtil.encodeToPlainBase64(objectStoreHelper.getBiometricObject(uinHash, newCbeffRefId));
				} catch (Exception e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
							e.getMessage());
				}
				if (Objects.nonNull(oldCbeff))
					oldDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), oldCbeff));
				if (Objects.nonNull(newCbeff))
					newDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), newCbeff));
				String id = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, new String(regId)).toString();
				IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
						.setProcessName(Objects.isNull(oldUinData) ? "New" : "Update").setOldIdentity(oldUinData)
						.setOldDocuments(oldDocList).setNewIdentity(newUinData).setNewDocuments(newDocList).build();
				AnonymousProfileEntity anonymousProfile = AnonymousProfileEntity.builder().id(id)
						.profile(mapper.writeValueAsString(profile)).createdBy(IdRepoSecurityManager.getUser())
						.crDTimes(DateUtils.getUTCCurrentDateTime()).build();
				anonymousProfileRepo.save(anonymousProfile);
			} catch (Exception e) {
				e.printStackTrace();
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
			this.uinHash = StringUtils.substringAfter(uinHash, "_");
			this.oldCbeffRefId = fileRefId;
		}
		return this;
	}

	public AnonymousProfileHelper setNewCbeff(String uinHash, String fileRefId) {
		if (Objects.isNull(newCbeff)) {
			this.uinHash = StringUtils.substringAfter(uinHash, "_");
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
		this.regId = null;
	}

	public AnonymousProfileHelper setIsDraft(boolean isDraft) {
		this.isDraft = isDraft;
		return this;
	}
}
