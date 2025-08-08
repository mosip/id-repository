package io.mosip.idrepository.vid.provider;

import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_POLICY_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_TYPE_PATH;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.vid.service.impl.VidServiceImpl;
import io.mosip.kernel.core.logger.spi.Logger;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.mosip.idrepository.core.dto.VidPolicy;
import io.mosip.idrepository.core.util.EnvUtil;

/**
 * The Class VidPolicyProvider - Provider class to load policy from policy json
 * and provide the vid policy details based on vid type.
 *
 * @author Manoj SP
 */
@Component
@RefreshScope
public class VidPolicyProvider {

	private static final Configuration READ_LIST_OPTIONS = Configuration.defaultConfiguration()
			.addOptions(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST);

	private static final JsonPath VID_TYPE_PATH_EXPR = JsonPath.compile(VID_TYPE_PATH);
	private static final JsonPath VID_POLICY_PATH_EXPR = JsonPath.compile(VID_POLICY_PATH);

	@Autowired
	private ObjectMapper mapper;

	private Logger mosipLogger = IdRepoLogger.getLogger(VidPolicyProvider.class);


	// Thread-safe reference for policies
	private final AtomicReference<Map<String, VidPolicy>> vidPoliciesRef = new AtomicReference<>(Collections.emptyMap());

	@PostConstruct
	public void loadPolicyDetails() throws IOException, ProcessingException {
		long start = System.currentTimeMillis();

		JsonNode policyJson = mapper.readValue(new URL(EnvUtil.getVidPolicyFileUrl()), JsonNode.class);
		JsonNode schema = mapper.readValue(new URL(EnvUtil.getVidPolicySchemaUrl()), JsonNode.class);

		// Validate JSON against schema
		JsonSchema jsonSchema = JsonSchemaFactory.byDefault().getJsonSchema(schema);
		jsonSchema.validate(policyJson);

		// Extract types and policies directly from JsonNode
		List<String> vidTypes = VID_TYPE_PATH_EXPR.read(policyJson, READ_LIST_OPTIONS);
		List<Object> vidPolicyObjs = VID_POLICY_PATH_EXPR.read(policyJson, READ_LIST_OPTIONS);

		Map<String, VidPolicy> newMap = IntStream.range(0, vidTypes.size())
				.boxed()
				.collect(Collectors.toMap(
						i -> vidTypes.get(i).toUpperCase(Locale.ROOT),
						i -> mapper.convertValue(vidPolicyObjs.get(i), VidPolicy.class)
				));

		vidPoliciesRef.set(Collections.unmodifiableMap(newMap));

		mosipLogger.info("VID policies loaded: {} entries in {} ms",
						newMap.size(), (System.currentTimeMillis() - start));
	}

	public VidPolicy getPolicy(String vidType) {
		if (vidType == null) return null;
		return vidPoliciesRef.get().get(vidType.toUpperCase(Locale.ROOT));
	}

	public Set<String> getAllVidTypes() {
		return vidPoliciesRef.get().keySet();
	}
}

