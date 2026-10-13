package io.mosip.idrepository.identity.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.dto.HandleDto;
import io.mosip.kernel.core.util.CryptoUtil;

@RunWith(MockitoJUnitRunner.class)
public class IdRepoServiceHelperHandleTest {

	@InjectMocks
	private IdRepoServiceHelper helper;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(helper, "mapper", new ObjectMapper());
		ReflectionTestUtils.setField(helper, "fieldIdHandlePostfixMapping", Map.of());

		IdentityMapping mapping = new IdentityMapping();
		IdentityMapping.Identity identity = new IdentityMapping.Identity();
		IdentityMapping.IDSchemaVersion version = new IdentityMapping.IDSchemaVersion();
		version.setValue("IDSchemaVersion");
		identity.setIDSchemaVersion(version);
		IdentityMapping.SelectedHandles selectedHandles = new IdentityMapping.SelectedHandles();
		selectedHandles.setValue("selectedHandles");
		identity.setSelectedHandles(selectedHandles);
		mapping.setIdentity(identity);
		ReflectionTestUtils.setField(helper, "identityMapping", mapping);

		Map<String, List<String>> schemaHandles = new HashMap<>();
		schemaHandles.put("0.1", List.of("phone", "email"));
		ReflectionTestUtils.setField(IdRepoServiceHelper.class, "supportedHandlesInSchema", schemaHandles);

		when(securityManager.getSaltKeyForHashOfId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn(CryptoUtil.encodeToPlainBase64("salt".getBytes()));
		when(securityManager.hashwithSalt(any(), any())).thenReturn("hash");
	}

	@Test
	public void testStringAndListHandleValues() throws Exception {
		Map<String, Object> identity = new HashMap<>();
		identity.put("IDSchemaVersion", 0.1);
		identity.put("selectedHandles", List.of("phone", "email"));
		identity.put("phone", List.of("  911111111111", "  922222222222"));
		identity.put("email", "  User@Example.COM  ");
		Map<String, Object> request = Map.of("identity", identity);

		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(request, null);

		assertEquals(2, handles.get("phone").size());
		assertEquals("911111111111@phone", handles.get("phone").get(0).getHandle());
		assertEquals("922222222222@phone", handles.get("phone").get(1).getHandle());
		assertEquals(1, handles.get("email").size());
		assertEquals("user@example.com@email", handles.get("email").get(0).getHandle());
	}

	@Test
	public void testTaggedHandleValues() throws Exception {
		Map<String, Object> phoneItem = new HashMap<>();
		phoneItem.put("value", "91111");
		phoneItem.put("tags", List.of("handle"));
		Map<String, Object> skipped = new HashMap<>();
		skipped.put("value", "90000");
		skipped.put("tags", List.of("other"));
		Map<String, Object> identity = new HashMap<>();
		identity.put("IDSchemaVersion", 0.1);
		identity.put("selectedHandles", List.of("phone"));
		identity.put("phone", List.of(phoneItem, skipped));
		Map<String, Object> request = Map.of("identity", identity);

		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(request, null);

		assertEquals(1, handles.get("phone").size());
		assertEquals("91111@phone", handles.get("phone").get(0).getHandle());
	}

	@Test
	public void testKeepExistingOnPartialUpdate() throws Exception {
		HandleDto existing = new HandleDto("keep@phone", "1_hash");
		Map<String, List<HandleDto>> existingMap = Map.of("phone", List.of(existing));
		Map<String, Object> identity = new HashMap<>();
		identity.put("IDSchemaVersion", 0.1);
		identity.put("fullName", "Ada");
		Map<String, Object> request = Map.of("identity", identity);

		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(request, existingMap);

		assertEquals(existing, handles.get("phone").get(0));
	}

	@Test
	public void testUpdateSelectedHandleFields() {
		Map<String, Object> identity = new HashMap<>();
		identity.put("selectedHandles", List.of("phone", "email"));
		Map<String, List<HandleDto>> accepted = new HashMap<>();
		accepted.put("phone", List.of(new HandleDto("a@phone", "h")));

		helper.updateSelectedHandleFields(identity, accepted);

		assertEquals(List.of("phone"), identity.get("selectedHandles"));
		assertTrue(((List<?>) identity.get("selectedHandles")).contains("phone"));
	}
}
