package io.mosip.idrepository.identity.test.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UinDocument;
import io.mosip.idrepository.identity.entity.UinHistory;
import io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class IdRepoEntityInterceptorTest {

	@InjectMocks
	IdRepoEntityInterceptor interceptor;

	@InjectMocks
	IdRepoSecurityManager securityManager;

	@Mock
	RestHelper restHelper;

	@Mock
	RestRequestBuilder restBuilder;

	@InjectMocks
	ObjectMapper mapper;

	@Before
	public void setup() {
		ReflectionTestUtils.setField(securityManager, "mapper", mapper);
		ReflectionTestUtils.setField(securityManager, "restHelper", restHelper);
		ReflectionTestUtils.setField(securityManager, "restBuilder", restBuilder);
		ReflectionTestUtils.setField(interceptor, "securityManager", securityManager);
	}

	@Test
	public void testOnSaveUin() throws RestClientException, JsonParseException, JsonMappingException, IOException,
			RestServiceException, IdRepoDataValidationException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
		Uin uin = new Uin();
		uin.setUinData(new byte[] { 0 });
		uin.setUin("461_7329815461_7C9JlRD32RnFTzAmeTfIzg");
		Object[] state = new Object[] { new byte[] { 0 }, "461_7329815461_7C9JlRD32RnFTzAmeTfIzg" };
		String[] propertyNames = new String[] { "uinData", "uin" };
		assertFalse(interceptor.onSave(uin, null, state, propertyNames, null));
	}

	@Test
	public void testOnSaveUinHistory() throws RestClientException, JsonParseException, JsonMappingException,
			IOException, RestServiceException, IdRepoDataValidationException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
		UinHistory uin = new UinHistory();
		uin.setUinData(new byte[] { 0 });
		uin.setUin("461_7329815461_7C9JlRD32RnFTzAmeTfIzg");
		Object[] state = new Object[] { new byte[] { 0 }, "461_7329815461_7C9JlRD32RnFTzAmeTfIzg" };
		String[] propertyNames = new String[] { "uinData", "uin" };
		assertFalse(interceptor.onSave(uin, null, state, propertyNames, null));
	}

	@Test
	public void testOnSaveUinException() throws Throwable {
		try {
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new RestRequestDTO());
			when(restHelper.requestSync(Mockito.any()))
					.thenReturn(mapper.readValue("{\"value\":\"1234\"}".getBytes(), ObjectNode.class));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			Object[] state = new Object[] { new byte[] { 0 } };
			String[] propertyNames = new String[] { "uinData" };
			interceptor.onSave(uin, null, state, propertyNames, null);
		} catch (IdRepoAppUncheckedException e) {
			assertEquals(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testOnSaveUinUinDocEntity() throws RestClientException, JsonParseException, JsonMappingException,
			IOException, RestServiceException, IdRepoDataValidationException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"value\":\"1234\"}".getBytes(), ObjectNode.class));
		UinDocument uin = new UinDocument();
		assertFalse(interceptor.onSave(uin, null, null, new String[] {""}, null));
	}

	@Test
	public void testOnLoadUin() throws RestClientException, JsonParseException, JsonMappingException, IOException,
			IdRepoDataValidationException, RestServiceException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
		Uin uin = new Uin();
		uin.setUinData(new byte[] { 0 });
		Object[] state = new Object[] { new byte[] { 0 },
				"5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7" };
		String[] propertyNames = new String[] { "uinData", "uinDataHash" };
		assertFalse(interceptor.onLoad(uin, null, state, propertyNames, null));
	}

	@Test
	public void testOnLoadUinFailedEncrption() throws Throwable {
		try {
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new RestRequestDTO());
			when(restHelper.requestSync(Mockito.any()))
					.thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			Object[] state = new Object[] { new byte[] { 0 },
					"5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7" };
			String[] propertyNames = new String[] { "uinData", "uinDataHash" };
			interceptor.onLoad(uin, null, state, propertyNames, null);
		} catch (IdRepoAppUncheckedException e) {
			assertEquals(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testOnLoadUinException() throws Throwable {
		try {
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new RestRequestDTO());
			when(restHelper.requestSync(Mockito.any()))
					.thenReturn(mapper.readValue("{\"value\":\"1234\"}".getBytes(), ObjectNode.class));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			Object[] state = new Object[] { new byte[] { 0 }, "W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc" };
			String[] propertyNames = new String[] { "uinData", "uinDataHash" };
			interceptor.onLoad(uin, null, state, propertyNames, null);
		} catch (IdRepoAppUncheckedException e) {
			assertEquals(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testOnLoadUinHashMismatch() throws Throwable {
		try {
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new RestRequestDTO());
			when(restHelper.requestSync(Mockito.any()))
					.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			Object[] state = new Object[] { new byte[] { 0 }, "W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1L" };
			String[] propertyNames = new String[] { "uinData", "uinDataHash" };
			interceptor.onLoad(uin, null, state, propertyNames, null);
		} catch (IdRepoAppUncheckedException e) {
			assertEquals(IdRepoErrorConstants.IDENTITY_HASH_MISMATCH.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.IDENTITY_HASH_MISMATCH.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testOnFlushDirtyUin() throws RestClientException, JsonParseException, JsonMappingException, IOException,
			IdRepoDataValidationException, RestServiceException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
		Uin uin = new Uin();
		uin.setUinData(new byte[] { 0 });
		Object[] state = new Object[] { new byte[] { 0 } };
		String[] propertyNames = new String[] { "uinData" };
		assertFalse(interceptor.onFlushDirty(uin, null, state, state, propertyNames, null));
	}

	@Test
	public void testOnFlushDirtyUinHistory() throws RestClientException, JsonParseException, JsonMappingException,
			IOException, IdRepoDataValidationException, RestServiceException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
		UinHistory uinH = new UinHistory();
		uinH.setUinData(new byte[] { 0 });
		Object[] state = new Object[] { new byte[] { 0 } };
		String[] propertyNames = new String[] { "uinData" };
		assertFalse(interceptor.onFlushDirty(uinH, null, state, state, propertyNames, null));
	}

	@Test(expected = IdRepoAppUncheckedException.class)
	public void testOnFlushDirtyException() throws RestClientException, JsonParseException, JsonMappingException,
			IOException, IdRepoDataValidationException, RestServiceException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR));
		Uin uin = new Uin();
		uin.setUinData(new byte[] { 0 });
		Object[] state = new Object[] { new byte[] { 0 } };
		String[] propertyNames = new String[] { "uinData" };
		interceptor.onFlushDirty(uin, null, state, state, propertyNames, null);
	}
}
