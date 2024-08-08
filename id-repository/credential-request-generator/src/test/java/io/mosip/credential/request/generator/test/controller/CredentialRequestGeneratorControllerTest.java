package io.mosip.credential.request.generator.test.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mosip.credential.request.generator.controller.CredentialRequestGeneratorController;
import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.init.CredentialInstializer;
import io.mosip.credential.request.generator.init.SubscribeEvent;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.credential.request.generator.test.TestBootApplication;
import io.mosip.credential.request.generator.test.config.TestConfig;
import io.mosip.credential.request.generator.validator.RequestValidator;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.dto.CredentialRequestIdsDto;
import io.mosip.idrepository.core.dto.PageDto;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(locations = "classpath:application.properties")
@SpringBootTest(classes = TestBootApplication.class)
@AutoConfigureMockMvc
public class CredentialRequestGeneratorControllerTest {
	@Mock
	private CredentialRequestService credentialRequestService;
	
	@Mock
	private CredentialInstializer credentialInstializer;
	
	@Mock
	private SubscribeEvent subscribeEvent;
	
	@Mock
	JobLauncher jobLauncher;

	@Mock
	private RequestValidator requestValidator;

	@InjectMocks
	private CredentialRequestGeneratorController credentialRequestGeneratorController;

	private MockMvc mockMvc;

	Gson gson = new GsonBuilder().serializeNulls().create();
	
	String reqJson;

	ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper;
	
	ResponseWrapper<CredentialIssueStatusResponse> credentialIssueStatusResponseWrapper;

	String reqCredentialEventJson;

	ResponseWrapper<PageDto<CredentialRequestIdsDto>> responseWrapper;


	@Before
	public void setup() throws Exception {
		credentialIssueStatusResponseWrapper=new ResponseWrapper<CredentialIssueStatusResponse>();
		credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(credentialRequestGeneratorController).build();
		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setId("12345");
		reqJson = gson.toJson(credentialIssueRequestDto);
		CredentialStatusEvent credentialStatusEvent = new CredentialStatusEvent();
		reqCredentialEventJson = gson.toJson(credentialStatusEvent);
		PageDto<CredentialRequestIdsDto> pageDto = new PageDto<CredentialRequestIdsDto>();
		responseWrapper = new ResponseWrapper<PageDto<CredentialRequestIdsDto>>();
		responseWrapper.setResponse(pageDto);
	}


	@Test
	@WithUserDetails("test")
	public void testCreateRequestGenerationSuccess() throws Exception {


		Mockito.when(credentialRequestService.createCredentialIssuance(Mockito.any()))
				.thenReturn(credentialIssueResponseWrapper);

		mockMvc.perform(
				MockMvcRequestBuilders.post("/requestgenerator")
						.contentType(MediaType.APPLICATION_JSON_VALUE).content(reqJson.getBytes()))
				.andExpect(status().isOk());

	}


	@Test
	@WithUserDetails("test")
	public void testCancelRequestSuccess() throws Exception {

		Mockito.when(credentialRequestService.cancelCredentialRequest(Mockito.any()))
				.thenReturn(credentialIssueResponseWrapper);

		mockMvc.perform(MockMvcRequestBuilders.get("/cancel/requestId").contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());

	}

	@Test
	@WithUserDetails("test")
	public void testgetCredentialRequestStatusSuccess() throws Exception {

		Mockito.when(credentialRequestService.getCredentialRequestStatus(Mockito.any()))
				.thenReturn(credentialIssueStatusResponseWrapper);

		mockMvc.perform(MockMvcRequestBuilders.get("/get/requestId").contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());

	}

	@Test
	@WithUserDetails("test")
	public void testHandleSubscribeEventSuccess() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/callback/notifyStatus")
				.contentType(MediaType.APPLICATION_JSON_VALUE).content(reqCredentialEventJson.getBytes()))
				.andExpect(status().isOk());


	}

	@Test
	@WithUserDetails("test")
	public void testGetRequestIdsSuccess() throws Exception {

		Mockito.when(credentialRequestService.getRequestIds(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(),Mockito.anyInt(),
				Mockito.anyString(), Mockito.anyString()))
				.thenReturn(responseWrapper);

		mockMvc.perform(MockMvcRequestBuilders.get(
				"/getRequestIds")
				.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());

	}

	@Test
	@WithUserDetails("test")
	public void testReprocessCredentialRequestSuccess() throws Exception {

		Mockito.when(credentialRequestService.retriggerCredentialRequest(Mockito.any()))
				.thenReturn(credentialIssueResponseWrapper);

		mockMvc.perform(
				MockMvcRequestBuilders.put("/retrigger/requestId").contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());

	}
	@Test
	@WithUserDetails("test")
	public void testHandleReSubscribeEventSuccess() throws Exception {


		mockMvc.perform(
				MockMvcRequestBuilders.get("/scheduleRetrySubscription"))
				.andExpect(status().isOk());

	}
	@Test
	@WithUserDetails("test")
	public void testScheduleWebsubSubscriptionSuccess() throws Exception {


		mockMvc.perform(
				MockMvcRequestBuilders.get("/scheduleWebsubSubscription"))
				.andExpect(status().isOk());

	}

}
