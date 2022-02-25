package io.mosip.idrepository.core.test.manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@Ignore
public class CredentialServiceManagerTest {

	@InjectMocks
	private CredentialServiceManager credentialServiceManager;

	private static final String SEND_REQUEST_TO_CRED_SERVICE = "sendRequestToCredService";

	private static final String GET_PARTNER_IDS = "getPartnerIds";

	private static final String NOTIFY = "notify";

	private static final boolean DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = false;

	private static final String PROP_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = "skip-requesting-existing-credentials-for-partners";

	/** The Constant mosipLogger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialServiceManager.class);

	/** The Constant IDA. */
	private static final String IDA = "IDA";

	/** The Constant AUTH. */
	private static final String AUTH = "auth";

	/** The Constant ACTIVE. */
	private static final String ACTIVATED = "ACTIVATED";

	/** The Constant BLOCKED. */
	private static final String BLOCKED = "BLOCKED";

	/** The Constant REVOKED. */
	private static final String REVOKED = "REVOKED";

	/** The mapper. */
	@Mock
	private ObjectMapper mapper;

	/** The rest helper. */
	@Mock
	private RestHelper restHelper;

	/** The rest builder. */
	@Mock
	private RestRequestBuilder restBuilder;

	/** The security manager. */
	@Mock
	private IdRepoSecurityManager securityManager;

	/** The credential type. */
	@Value("${id-repo-ida-credential-type:}")
	private String credentialType;

	/** The credential recepiant. */
	@Value("${id-repo-ida-credential-recepiant:}")
	private String credentialRecepiant;

	@Value("$mosip.idrepo.vid.active-status}")
	private String vidActiveStatus;

	/** The token ID generator. */
	@Mock
	private TokenIDGenerator tokenIDGenerator;

	@Mock
	private IdRepoWebSubHelper websubHelper;

	@Mock
	private DummyPartnerCheckUtil dummyCheck;

	@Mock
	private ApplicationContext ctx;

	@Mock
	private PartnerServiceManager partnerServiceManager;

	private boolean skipExistingCredentialsForPartners;

	@Before
	public void before() {
	}

	@Test
	public void notifyUinCredentialTest() {
		String uin = "123";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		String status = "Test";
		boolean isUpdate = true;
		String txnId = "12";
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		Consumer<EventModel> idaEventModelConsumer = null;
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);
		credentialServiceManager.notifyUinCredential(uin, expiryTimestamp, status, isUpdate, txnId,
				saltRetreivalFunction, credentialRequestResponseConsumer, idaEventModelConsumer, partnerIds);
	}

}
