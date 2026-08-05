package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.dto.HandleDto;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;

@RunWith(MockitoJUnitRunner.class)
public class IdRepoServiceHelperTest {

	private final IdRepoServiceHelper helper = new IdRepoServiceHelper();

	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Before
	public void setUp() {
		IdentityMapping identityMapping = new IdentityMapping();
		identityMapping.setIdentity(new IdentityMapping.Identity());
		IdentityMapping.IDSchemaVersion schemaVersion = new IdentityMapping.IDSchemaVersion();
		schemaVersion.setValue("IDSchemaVersion");
		IdentityMapping.SelectedHandles selectedHandles = new IdentityMapping.SelectedHandles();
		selectedHandles.setValue("selectedHandles");
		identityMapping.getIdentity().setIDSchemaVersion(schemaVersion);
		identityMapping.getIdentity().setSelectedHandles(selectedHandles);

		ReflectionTestUtils.setField(helper, "mapper", new ObjectMapper());
		ReflectionTestUtils.setField(helper, "identityMapping", identityMapping);
		ReflectionTestUtils.setField(helper, "fieldIdHandlePostfixMapping", Collections.emptyMap());
		ReflectionTestUtils.setField(helper, "securityManager", securityManager);
		ReflectionTestUtils.setField(helper, "uinHashSaltRepo", uinHashSaltRepo);
		ReflectionTestUtils.setField(IdRepoServiceHelper.class, "supportedHandlesInSchema",
				new HashMap<>(Map.of("1.0", List.of("email"))));
		when(securityManager.getSaltKeyForHashOfId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(1)).thenReturn("dGVzdA==");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("hash");
	}

	@Test
	public void getSelectedHandles_selectsOnlyValuesTaggedHandle() throws IdRepoAppException {
		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(identityWithEmail(Arrays.asList(
				handleValue("first@example.com", List.of("handle")),
				handleValue("second@example.com", List.of("notification")),
				handleValue("third@example.com", null))), null);

		assertEquals(1, handles.get("email").size());
		assertEquals("first@example.com@email", handles.get("email").get(0).getHandle());
	}

	@Test
	public void getSelectedHandles_defaultsToAllValuesWhenNoTagsExist() throws IdRepoAppException {
		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(identityWithEmail(Arrays.asList(
				handleValue("first@example.com", List.of("notification")),
				handleValue("second@example.com", Collections.emptyList()),
				handleValue("third@example.com", null))), null);

		assertEquals(3, handles.get("email").size());
	}

	@Test
	public void getSelectedHandles_defaultsToAllValuesWhenTagsAreInvalid() throws IdRepoAppException {
		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(identityWithEmail(Arrays.asList(
				handleValue("first@example.com", List.of("notification")),
				handleValue("second@example.com", List.of("unknown")))), null);

		assertEquals(2, handles.get("email").size());
	}

	@Test
	public void getSelectedHandles_resolvesMultipleExplicitHandleValues() throws IdRepoAppException {
		Map<String, List<HandleDto>> handles = helper.getSelectedHandles(identityWithEmail(Arrays.asList(
				handleValue("first@example.com", List.of("handle")),
				handleValue("second@example.com", List.of("handle")))), null);

		assertEquals(2, handles.get("email").size());
	}

	@Test
	public void getSelectedHandles_supportsLegacyScalarHandleValues() throws IdRepoAppException {
		Map<String, List<HandleDto>> scalarHandles = helper.getSelectedHandles(identityWithEmail("legacy@example.com"), null);

		assertEquals(1, scalarHandles.get("email").size());
		assertEquals("legacy@example.com@email", scalarHandles.get("email").get(0).getHandle());
	}

	@Test
	public void getSelectedHandles_returnsNoHandlesWhenSelectedHandlesIsAbsent() throws IdRepoAppException {
		Map<String, Object> identity = new HashMap<>();
		identity.put("IDSchemaVersion", "1.0");
		identity.put("email", "resident@example.com");

		assertNull(helper.getSelectedHandles(Map.of("identity", identity), null));
	}

	private Map<String, Object> identityWithEmail(Object email) {
		Map<String, Object> identity = new HashMap<>();
		identity.put("IDSchemaVersion", "1.0");
		identity.put("selectedHandles", List.of("email"));
		identity.put("email", email);
		return Map.of("identity", identity);
	}

	private Map<String, Object> handleValue(String value, List<String> tags) {
		Map<String, Object> result = new HashMap<>();
		result.put("value", value);
		if (tags != null) {
			result.put("tags", tags);
		}
		return result;
	}
}
