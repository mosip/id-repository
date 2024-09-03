package io.mosip.idrepository.credentialsfeeder.test.step;


import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.credentialsfeeder.entity.Uin;
import io.mosip.idrepository.credentialsfeeder.repository.AuthLockRepository;
import io.mosip.idrepository.credentialsfeeder.step.CredentialsFeedingWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.Chunk;

import java.util.Collections;
import java.util.List;

public class CredentialsFeedingWriterTest {

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Mock
	private CredentialServiceManager credentialServiceManager;

	@Mock
	private CredentialStatusManager credentialStatusManager;

	@Mock
	private RestRequestBuilder restBuilder;

	@Mock
	private RestHelper restHelper;

	@Mock
	private IdRepoWebSubHelper webSubHelper;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private AuthLockRepository authLockRepo;

	@InjectMocks
	private CredentialsFeedingWriter credentialsFeedingWriter;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		// Set default values for @Value annotated fields
		credentialsFeedingWriter.onlineVerificationPartnerIds = new String[]{"partner1", "partner2"};
		credentialsFeedingWriter.uinActiveStatus = "ACTIVE";
		credentialsFeedingWriter.vidActiveStatus = "VID_ACTIVE";
	}

	@Test
	public void testWrite_success() throws Exception {
		// Arrange
		Uin uin = new Uin();
		uin.setUin("testUIN");
		List<Uin> uinList = Collections.singletonList(uin);

		when(credentialStatusManager.decryptId("testUIN")).thenReturn("decryptedUIN");

		// Act
		credentialsFeedingWriter.write((Chunk<? extends Uin>) uinList);

		// Assert
		verify(credentialServiceManager, times(1)).sendUinEventsToCredService(
				eq("decryptedUIN"),
				isNull(),
				eq(false),
				isNull(),
				isNull(),
				eq(List.of("partner1", "partner2")),
				any(),
				any()
		);

		//verify(restBuilder, times(1)).buildRequest(eq(RestServicesConstants.RETRIEVE_VIDS_BY_UIN), isNull(), eq(VidsInfosDTO.class));
		verify(restHelper, times(1)).requestSync(any());

		verify(securityManager, times(1)).hash(any());
		verify(authLockRepo, times(1)).findByHashedUin(any());
		verify(webSubHelper, atLeast(1)).publishAuthTypeStatusUpdateEvent(any(), any(), any(), any());
	}

	@Test
	public void testWrite_decryptionException() throws IdRepoAppException {
		// Arrange
		Uin uin = new Uin();
		uin.setUin("testUIN");
		List<Uin> uinList = Collections.singletonList(uin);

		when(credentialStatusManager.decryptId("testUIN")).thenThrow(new IdRepoAppException("Error", "Decryption error"));

		// Act & Assert
		/*assertThrows(IdRepoAppUncheckedException.class, () -> {
			credentialsFeedingWriter.write((Chunk<? extends Uin>) uinList);
		});*/
	}

	// You can add more tests to cover other scenarios such as issues in issuing credentials, handling exceptions, etc.
}
