package io.mosip.idrepository.identity.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.util.EnvUtil;

@RunWith(MockitoJUnitRunner.class)
public class IdentityUpdateTrackerPolicyProviderTest {

	private final IdentityUpdateTrackerPolicyProvider provider = new IdentityUpdateTrackerPolicyProvider();

	private final ObjectMapper mapper = new ObjectMapper();

	@Before
	public void init() throws Exception {
		URL mappingUrl = IdentityUpdateTrackerPolicyProviderTest.class
				.getResource("/identity-update-count-mapping.json");
		EnvUtil.setIdentityMappingJsonUrl(mappingUrl.toString());
		ReflectionTestUtils.setField(provider, "mapper", mapper);
		provider.loadUpdateCountPolicies();
	}

	@Test
	public void loadUpdateCountPoliciesPopulatesStaticMap() {
		Map<String, Integer> limits = IdentityUpdateTrackerPolicyProvider.getUpdateCountLimitMap();
		assertEquals(Integer.valueOf(3), limits.get("email"));
		assertEquals(Integer.valueOf(5), limits.get("phone"));
	}

	@Test
	public void getMaxUpdateCountLimitReturnsConfiguredValue() {
		assertEquals(Integer.valueOf(3), IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit("email"));
	}

	@Test
	public void getMaxUpdateCountLimitReturnsNullForUnknownAttribute() {
		assertNull(IdentityUpdateTrackerPolicyProvider.getMaxUpdateCountLimit("unknown"));
	}
}
