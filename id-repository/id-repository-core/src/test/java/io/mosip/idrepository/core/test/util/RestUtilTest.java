package io.mosip.idrepository.core.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;

public class RestUtilTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void getErrorReturnsPresentForNonEmptyErrorsList() {
		String body = "{\"errors\":[{\"errorCode\":\"ERR-001\",\"message\":\"failed\"}]}";
		Optional<Map.Entry<String, Object>> error = RestUtil.getError(body, mapper);
		assertTrue(error.isPresent());
		assertEquals("errors", error.get().getKey());
	}

	@Test
	public void getErrorReturnsEmptyForMissingErrors() {
		assertFalse(RestUtil.getError("{\"response\":{}}", mapper).isPresent());
	}

	@Test
	public void getErrorReturnsEmptyForEmptyErrorsList() {
		assertFalse(RestUtil.getError("{\"errors\":[]}", mapper).isPresent());
	}

	@Test
	public void getErrorReturnsEmptyForInvalidJson() {
		assertFalse(RestUtil.getError("not-json", mapper).isPresent());
	}

	@Test
	public void containsErrorReturnsTrueForNonEmptyErrorsList() {
		assertTrue(RestUtil.containsError("{\"errors\":[{\"errorCode\":\"ERR-001\"}]}", mapper));
	}

	@Test
	public void containsErrorReturnsFalseForMissingErrors() {
		assertFalse(RestUtil.containsError("{\"response\":{}}", mapper));
	}

	@Test
	public void containsErrorReturnsFalseForInvalidJson() {
		assertFalse(RestUtil.containsError("{bad", mapper));
	}

	@Test
	public void getErrorListReturnsMapShapedError() {
		String body = "{\"errors\":{\"errorCode\":\"ERR-001\",\"message\":\"failed\"}}";
		List<ServiceError> errors = RestUtil.getErrorList(body, mapper);
		assertEquals(1, errors.size());
		assertEquals("ERR-001", errors.get(0).getErrorCode());
		assertEquals("failed", errors.get(0).getMessage());
	}

	@Test
	public void getErrorListFallsBackToKernelParserForListShape() {
		String body = "{\"errors\":[{\"errorCode\":\"ERR-002\",\"message\":\"kernel\"}]}";
		List<ServiceError> errors = RestUtil.getErrorList(body, mapper);
		assertEquals(1, errors.size());
		assertEquals("ERR-002", errors.get(0).getErrorCode());
	}

	@Test
	public void getErrorReturnsEmptyForNullErrorsValue() {
		assertFalse(RestUtil.getError("{\"errors\":null}", mapper).isPresent());
	}

	@Test
	public void containsErrorReturnsFalseForNullErrorsValue() {
		assertFalse(RestUtil.containsError("{\"errors\":null}", mapper));
	}

	@Test
	public void getErrorReturnsEmptyForNonListErrorsValue() {
		assertFalse(RestUtil.getError("{\"errors\":{\"errorCode\":\"ERR\"}}", mapper).isPresent());
	}

	@Test
	public void containsErrorReturnsFalseForNonListErrorsValue() {
		assertFalse(RestUtil.containsError("{\"errors\":{\"errorCode\":\"ERR\"}}", mapper));
	}

	@Test
	public void getErrorAbbreviatesLongInvalidJsonBody() {
		String longBody = "x".repeat(600);
		assertFalse(RestUtil.getError(longBody, mapper).isPresent());
	}

	@Test
	public void abbreviateReturnsNullLiteralForNullInput() {
		String value = (String) ReflectionTestUtils.invokeMethod(RestUtil.class, "abbreviate", null, 500);
		assertEquals("null", value);
	}

	@Test
	public void getErrorListReturnsEmptyForInvalidJson() {
		assertTrue(RestUtil.getErrorList("not-json", mapper).isEmpty());
	}
}
