package io.mosip.idrepository.identity.test.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;

import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.kernel.core.http.RequestWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.repository.UinHistoryRepo;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.idrepository.identity.service.impl.IdRepoProxyServiceImpl;
import io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;

/**
 * The Class IdRepoProxyServiceTest.
 *
 * @author Vishwanath V
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.identity")
public class IdRepoProxyServiceTest {

	private static final String ACTIVATED = "ACTIVATED";

	private static final String IDENTITY_CREATED = "IDENTITY_CREATED";

	private static final String IDENTITY_UPDATED = "IDENTITY_UPDATED";

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Environment env;

	@InjectMocks
	IdRepoProxyServiceImpl proxyService;

	@Mock
	CbeffImpl cbeffUtil;

	@Mock
	AuditHelper auditHelper;

	@Mock
	IdRepoServiceImpl service;

	@Mock
	IdRepoSecurityManager securityManager;

	@Mock
	private RestHelper restHelper;

	@Mock
	private UinRepo uinRepo;

	@Mock
	private UinDraftRepo uinDraftRepo;

	@Mock
	private UinHistoryRepo uinHistoryRepo;

	@Mock
	RestRequestBuilder restBuilder;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Mock
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Mock
	private PublisherClient<String, EventModel, HttpHeaders> publisherCient;

	@Mock
	private TokenIDGenerator tokenIDGenerator;

	RequestWrapper<IdRequestDTO<Object>> request = new RequestWrapper<>();

	private Map<String, String> id;

	public Map<String, String> getId() {
		return id;
	}

	public void setId(Map<String, String> id) {
		this.id = id;
	}

	/**
	 * Setup.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws IdRepoDataValidationException
	 * @throws RestServiceException
	 */
	@Before
	public void setup() throws FileNotFoundException, IOException, IdRepoDataValidationException, 
			RestServiceException {
		ReflectionTestUtils.setField(proxyService, "mapper", mapper);
		ReflectionTestUtils.setField(proxyService, "env", env);
		ReflectionTestUtils.setField(proxyService, "id", id);
		ReflectionTestUtils.setField(proxyService, "allowedBioAttributes",
				Collections.singletonList("individualBiometrics"));

		RestRequestDTO partnerServiceRequestObject = new RestRequestDTO();
		partnerServiceRequestObject.setResponseType(Map.class);
		when(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null,  Map.class))
			.thenReturn(partnerServiceRequestObject);
		when(restHelper.requestSync(partnerServiceRequestObject)).thenReturn(mapper.readValue(
			"{\"response\":{\"partners\":[{\"partnerID\":\"1234\", \"status\":\"Active\"}]}}".getBytes(), 
			Map.class));

		RestRequestDTO credServiceRequestObject = new RestRequestDTO();
		credServiceRequestObject.setResponseType(Map.class);
		when(restBuilder.buildRequest(RestServicesConstants.CREDENTIAL_REQUEST_SERVICE, null, 
			Map.class)).thenReturn(credServiceRequestObject);
		when(restHelper.requestSync(credServiceRequestObject))
				.thenReturn(mapper.readValue("{}".getBytes(), Map.class));
		when(tokenIDGenerator.generateTokenID(anyString(), anyString())).thenReturn("abcdef");

		RestRequestDTO retrieveVidsRequestObject = new RestRequestDTO();
		retrieveVidsRequestObject.setResponseType(VidsInfosDTO.class);
		retrieveVidsRequestObject.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null, 
			VidsInfosDTO.class)).thenReturn(retrieveVidsRequestObject);
		when(restHelper.requestSync(retrieveVidsRequestObject)).thenReturn(mapper
			.readValue("{}".getBytes(), VidsInfosDTO.class));

		when(securityManager.hashwithSalt(any(), any())).thenReturn("hashwithsalt");
	}

	@Test
	public void testAddIdentityForSendingGenericIdentityEvents() throws IdRepoAppException, 
			JsonParseException, JsonMappingException, IOException {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		uinObj.setUinData("".getBytes());
		when(service.addIdentity(any(), anyString())).thenReturn(uinObj);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg	");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			
		ObjectNode obj = mapper.readValue(
			"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
					.getBytes(), ObjectNode.class);
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		req.setIdentity(obj);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		IdResponseDTO<Object> addIdentity = proxyService.addIdentity(request.getRequest(), "1234");

		assertEquals(ACTIVATED, addIdentity.getResponse().getStatus());
		ArgumentCaptor<EventModel> argumentCaptor = ArgumentCaptor.forClass(EventModel.class);
		verify(publisherCient, times(1)).publishUpdate(anyString(), argumentCaptor.capture(), 
			anyString(), any(), any());
		EventModel eventModel = argumentCaptor.getValue();
		assertEquals(IDENTITY_CREATED, eventModel.getTopic());
		assertEquals("hashwithsalt", eventModel.getEvent().getData().get("id_hash"));
		assertEquals("27841457360002620190730095024", eventModel.getEvent().getData().get("registration_id"));
	}

	@Test
	public void testUpdateIdentityForSendingGenericIdentityEvents() throws IdRepoAppException, 
			JsonParseException, JsonMappingException, IOException {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		Object obj2 = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"}],\"lastName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"},{\"language\":\"FR\",\"value\":\"Mano\",\"label\":\"string\"}]}}"
						.getBytes(), Object.class);
		uinObj.setUinData(mapper.writeValueAsBytes(obj2));
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(service.updateIdentity(any(), anyString())).thenReturn(uinObj);
		when(service.retrieveIdentity(anyString(), any(), any(), any())).thenReturn(uinObj);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");

		Object obj = mapper.readValue(
			"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
					.getBytes(),
			Object.class);
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		req.setStatus(ACTIVATED);
		req.setRegistrationId("27841457360002620190730095024");
		req.setIdentity(obj);
		request.setRequest(req);
		proxyService.updateIdentity(request.getRequest(), "1234").getResponse().equals(obj2);

		ArgumentCaptor<EventModel> argumentCaptor = ArgumentCaptor.forClass(EventModel.class);
		verify(publisherCient, times(1)).publishUpdate(anyString(), argumentCaptor.capture(), anyString(), 
			any(), any());
		EventModel eventModel = argumentCaptor.getValue();
		assertEquals(IDENTITY_UPDATED, eventModel.getTopic());
		assertEquals("hashwithsalt", eventModel.getEvent().getData().get("id_hash"));
		assertEquals("27841457360002620190730095024", eventModel.getEvent().getData().get("registration_id"));
	}
}
