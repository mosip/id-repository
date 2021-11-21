package io.mosip.idrepository.identity.test.controller;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.util.HMACUtils2;

public class ConversionTest {

	@Test
	public void test() throws JsonProcessingException, JSONException, NoSuchAlgorithmException {
		System.out.println(HMACUtils2.digestAsPlainText("1000210040000520200925005778".getBytes()));
//		System.out.println(HMACUtils2.digestAsPlainTextWithSalt("3529406472".getBytes(), "z3+ys+fXo7Fk6rhjtKEfWA==".getBytes()));
	}
}
