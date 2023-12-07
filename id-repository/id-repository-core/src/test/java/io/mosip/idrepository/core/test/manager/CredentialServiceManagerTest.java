package io.mosip.idrepository.core.test.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.websub.model.EventModel;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
public class CredentialServiceManagerTest {

	@InjectMocks
	private CredentialServiceManager credentialServiceManager;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private RestRequestBuilder restBuilder;

	/** The security manager. */
	@Mock
	private IdRepoSecurityManager securityManager;

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

	@Mock
	private RestHelper restHelper;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(credentialServiceManager, "restHelper", restHelper);
	}

	@Test
	public void notifyUinCredentialForStatusUpdateTest() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String uin = "123";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		String status = "ACTIVATED";
		boolean isUpdate = true;
		String txnId = "12";
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		Consumer<EventModel> idaEventModelConsumer = null;
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);
		String requestId = "123465";
		credentialServiceManager.notifyUinCredential(uin, expiryTimestamp, status, isUpdate, txnId,
				saltRetreivalFunction, credentialRequestResponseConsumer, idaEventModelConsumer, partnerIds, requestId);
	}

}
