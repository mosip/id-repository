package io.mosip.credentialstore.test.controller;

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

import io.mosip.credentialstore.controller.CredentialStoreController;

import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.credentialstore.test.TestBootApplication;
import io.mosip.credentialstore.test.config.TestConfig;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(locations = "classpath:application.properties")
@SpringBootTest(classes = TestBootApplication.class)
@AutoConfigureMockMvc
public class CredentialStoreControllerTest {
	@Mock
	private CredentialStoreService credentialStoreService;

	@InjectMocks
	private CredentialStoreController credentialStoreController;

	private MockMvc mockMvc;

	Gson gson = new GsonBuilder().serializeNulls().create();
	
	String reqJson;

	CredentialServiceResponseDto credentialServiceResponseDto;

	CredentialTypeResponse credentialTypeResponse;


	@Before
	public void setup() throws Exception {
		credentialTypeResponse = new CredentialTypeResponse();
		credentialServiceResponseDto = new CredentialServiceResponseDto();
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(credentialStoreController).build();
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		credentialServiceRequestDto.setId("12345");
		reqJson = gson.toJson(credentialServiceRequestDto);
	}

	@Test
	@WithUserDetails("test")
	public void testCreateCredentialIssuanceSuccess() throws Exception {


		Mockito.when(credentialStoreService.createCredentialIssuance(Mockito.any()))
				.thenReturn(credentialServiceResponseDto);

		mockMvc.perform(
				MockMvcRequestBuilders.post("/issue")
						.contentType(MediaType.APPLICATION_JSON_VALUE).content(reqJson.getBytes()))
				.andExpect(status().isOk());

	}

	@Test
	@WithUserDetails("test")
	public void testGetCredentialTypesSuccess() throws Exception {

		Mockito.when(credentialStoreService.getCredentialTypes())
				.thenReturn(credentialTypeResponse);

		mockMvc.perform(MockMvcRequestBuilders.get("/types").contentType(MediaType.APPLICATION_JSON_VALUE)
		).andExpect(status().isOk());

	}
}
