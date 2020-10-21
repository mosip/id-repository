package io.mosip.credential.request.generator.test.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.credential.request.generator.controller.CredentialRequestGeneratorController;
import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.credential.request.generator.test.TestBootApplication;
import io.mosip.credential.request.generator.test.config.TestConfig;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.kernel.core.http.ResponseWrapper;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(locations = "classpath:application.properties")
@SpringBootTest(classes = TestBootApplication.class)
@AutoConfigureMockMvc
public class CredentialRequestGeneratorControllerTest {
	@Mock
	private CredentialRequestService credentialRequestService;

	@InjectMocks
	private CredentialRequestGeneratorController credentialRequestGeneratorController;

	private MockMvc mockMvc;

	Gson gson = new GsonBuilder().serializeNulls().create();
	
	String reqJson;

	ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper;
	
	ResponseWrapper<CredentialIssueStatusResponse> credentialIssueStatusResponseWrapper;

	String reqCredentialEventJson;


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

}
