package io.mosip.idrepository.vid.test.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.dto.VidPolicy;
import io.mosip.idrepository.vid.provider.VidPolicyProvider;

@RunWith(MockitoJUnitRunner.class)
public class VidPolicyProviderTest {

	private final VidPolicyProvider policyProvider = new VidPolicyProvider();

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void loadPolicies() throws IOException, ProcessingException {
		ObjectNode policy = objectMapper.readValue(
				getClass().getClassLoader().getResourceAsStream("vid_policy.json"), ObjectNode.class);
		List<String> vidTypes = JsonPath.compile(IdRepoConstants.VID_TYPE_PATH).read(policy.toString(),
				Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST));
		List<Object> vidPolicies = JsonPath.compile(IdRepoConstants.VID_POLICY_PATH).read(policy.toString(),
				Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST));
		Map<String, VidPolicy> policies = IntStream.range(0, vidTypes.size()).boxed()
				.collect(Collectors.toMap(i -> vidTypes.get(i).toUpperCase(),
						i -> objectMapper.convertValue(vidPolicies.get(i), VidPolicy.class)));
		ReflectionTestUtils.setField(policyProvider, "vidPolicies", policies);
	}

	@Test
	public void testPolicyDetails() {
		assertTrue(policyProvider.getPolicy("Perpetual".toUpperCase()).getAutoRestoreAllowed());
		assertFalse(policyProvider.getPolicy("Temporary".toUpperCase()).getAutoRestoreAllowed());
		assertTrue(policyProvider.getAllVidTypes()
				.containsAll(Lists.newArrayList("Perpetual".toUpperCase(), "Temporary".toUpperCase())));
	}
}
