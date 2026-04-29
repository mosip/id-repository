package io.mosip.idrepository.identity.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.provider.IdentityUpdateTrackerPolicyProvider;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class IdentityUpdateTrackerPolicyProviderTest {

	@Mock
	private ObjectMapper mapper;
	
	@InjectMocks
	private IdentityUpdateTrackerPolicyProvider provider;
	
	private JsonNode policy;
	private String policyJson = "{\"attributeUpdateCountLimit\":{\"fullName\":2}}";
	
	@PostConstruct
	public void init() throws JsonMappingException, JsonProcessingException {
		EnvUtil.setIdentityMappingJsonUrl("https://localhost:8090");
		policy = new ObjectMapper().readValue(policyJson, JsonNode.class);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadUpdateCountPolicies() throws IOException {
		when(mapper.readValue(any(URL.class), any(Class.class))).thenReturn(policy);
		when(mapper.readValue(any(String.class), any(TypeReference.class))).thenReturn(Map.of("fullName", 2));
		provider.loadUpdateCountPolicies();
		assertEquals(Map.of("fullName", 2), IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap());
		assertTrue(2 == IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit("fullName"));
	}
}
