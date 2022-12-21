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
import io.mosip.idrepository.identity.entity.AnonymousProfileDto;
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
	public void buildAndsaveProfile(AnonymousProfileDto anonymousProfileDto) {
			try {
				channelInfoHelper.updatePhoneChannelInfo(anonymousProfileDto.getOldUinData(), anonymousProfileDto.getNewUinData());
				channelInfoHelper.updateEmailChannelInfo(anonymousProfileDto.getOldUinData(), anonymousProfileDto.getNewUinData());
				List<DocumentsDTO> oldDocList = List.of(new DocumentsDTO());
				List<DocumentsDTO> newDocList = List.of(new DocumentsDTO());
				String oldCbeff = null;
				String newCbeff = null;
				if (Objects.isNull(anonymousProfileDto.getOldCbeff())
						&& Objects.nonNull(anonymousProfileDto.getOldCbeffRefId())) {
					oldCbeff = CryptoUtil.encodeToURLSafeBase64(objectStoreHelper.getBiometricObject(
							anonymousProfileDto.getUinHash(), anonymousProfileDto.getOldCbeffRefId()));
					anonymousProfileDto.setOldCbeff(oldCbeff);
				}
				if (Objects.isNull(anonymousProfileDto.getNewCbeff())
						&& Objects.nonNull(anonymousProfileDto.getNewCbeffRefId())) {
					newCbeff = CryptoUtil.encodeToURLSafeBase64(objectStoreHelper.getBiometricObject(
							anonymousProfileDto.getUinHash(), anonymousProfileDto.getNewCbeffRefId()));
					anonymousProfileDto.setNewCbeff(newCbeff);
				}
				if (Objects.nonNull(oldCbeff)) {
					oldDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), oldCbeff));
				}
				if (Objects.nonNull(newCbeff)) {
					newDocList = List.of(new DocumentsDTO(IdentityIssuanceProfileBuilder.getIdentityMapping()
							.getIdentity().getIndividualBiometrics().getValue(), newCbeff));
				}
				String id = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, anonymousProfileDto.getRegId()).toString();
				IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder()
						.setFilterLanguage(env.getProperty("mosip.mandatory-languages", "").split(",")[0])
						.setProcessName(Objects.isNull(anonymousProfileDto.getOldUinData()) ? "New" : "Update").setOldIdentity(anonymousProfileDto.getOldUinData())
						.setOldDocuments(oldDocList).setNewIdentity(anonymousProfileDto.getNewUinData()).setNewDocuments(newDocList).build();
				AnonymousProfileEntity anonymousProfile = AnonymousProfileEntity.builder().id(id)
						.profile(mapper.writeValueAsString(profile)).createdBy(IdRepoSecurityManager.getUser())
						.crDTimes(DateUtils.getUTCCurrentDateTime()).build();
				anonymousProfileRepo.save(anonymousProfile);
			} catch (Exception e) {
				mosipLogger.warn(IdRepoSecurityManager.getUser(), "AnonymousProfileHelper", "buildAndsaveProfile",
						ExceptionUtils.getStackTrace(e));
			}
	}

}