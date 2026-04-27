package io.mosip.idrepository.core.test.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdentityIssuanceProfile;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.kernel.core.util.CryptoUtil;

public class IdentityIssuanceProfileBuilderTest {

	String identityData;

	String cbeff;

	ObjectMapper mapper = new ObjectMapper();

	IdentityMapping identityMapping;

	JsonNode expectedNode;

	@Before
	public void init() throws IOException {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		identityMapping = mapper.readValue(
				IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-mapping.json"),
						StandardCharsets.UTF_8),
				IdentityMapping.class);
		IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");
		cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		expectedNode = mapper.readTree("[\"x\",\"y\"]");
	}

	@Test
	public void testNewProfileSuccessBuild() {
		IdentityIssuanceProfileBuilder builder = IdentityIssuanceProfile.builder().setProcessName("New");
		assertEquals("New", builder.getProcessName());
		assertEquals(null, builder.getOldIdentity());
		assertEquals(null, builder.getNewIdentity());
		assertEquals(null, builder.getOldDocuments());
		assertEquals(null, builder.getNewDocuments());
		assertEquals(null, builder.getOldProfile());
		assertEquals(null, builder.getNewProfile());
		assertEquals(null, builder.getFilterLanguage());
		builder.setNewIdentity(identityData.getBytes()).setOldDocuments(List.of(new DocumentsDTO()))
		.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff)));
		IdentityIssuanceProfile newProfile = builder.build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testUpdateProfileSuccessBuild() {
		IdentityIssuanceProfile updateProfile = IdentityIssuanceProfile.builder().setProcessName("Update")
				.setNewIdentity(identityData.getBytes()).setOldIdentity(identityData.getBytes())
				.setOldDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff)))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), updateProfile.getDate());
		assertEquals("Update", updateProfile.getProcessName());
		assertNotNull(updateProfile.getOldProfile());
		assertEquals("1972", updateProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", updateProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), updateProfile.getNewProfile().getLocation());
		assertNull(updateProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), updateProfile.getNewProfile().getChannel());
		assertEquals(5, updateProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, updateProfile.getNewProfile().getVerified());
		assertEquals(13, updateProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), updateProfile.getNewProfile().getDocuments());
		assertEquals("1972", updateProfile.getOldProfile().getYearOfBirth());
		assertEquals("Male", updateProfile.getOldProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), updateProfile.getOldProfile().getLocation());
		assertNull(updateProfile.getOldProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), updateProfile.getOldProfile().getChannel());
		assertEquals(5, updateProfile.getOldProfile().getExceptions().size());
		assertEquals(expectedNode, updateProfile.getOldProfile().getVerified());
		assertEquals(13, updateProfile.getOldProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), updateProfile.getOldProfile().getDocuments());
	}

	@Test
	public void testBuildProfileWithoutData() {
		IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder().build();
		IdentityIssuanceProfile expected = new IdentityIssuanceProfile();
		expected.setDate(LocalDate.now());
		assertEquals(expected, profile);
	}

	@Test
	public void testBuildProfileWithInvalidData() {
		IdentityIssuanceProfile profile = IdentityIssuanceProfile.builder().setProcessName("Update")
				.setNewIdentity("a".getBytes()).setOldIdentity("a".getBytes())
				.setOldDocuments(List.of(new DocumentsDTO("individualBiometrics", "")))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", ""))).build();
		assertEquals(IdentityIssuanceProfile.builder().setProcessName("Update").build(), profile);
	}

	@Test
	public void testNewProfileSuccessWithPrefLanguage() throws IOException {
		ObjectNode identityDataAsObjectNode = mapper.readValue(identityData, ObjectNode.class);
		identityDataAsObjectNode.put("preferredLang", "eng");
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setFilterLanguage("eng")
				.setProcessName("New").setNewIdentity(identityDataAsObjectNode.toString().getBytes())
				.setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertEquals("eng", newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessWithoutPrefLanguageAndMandatoryLang() throws IOException {
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setFilterLanguage("eng")
				.setProcessName("New").setNewIdentity(identityData.getBytes())
				.setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessWithoutDOB() throws IOException {
		ObjectNode identityDataAsObjectNode = mapper.readValue(identityData, ObjectNode.class);
		identityDataAsObjectNode.remove(identityMapping.getIdentity().getDob().getValue());
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setFilterLanguage("eng")
				.setProcessName("New").setNewIdentity(identityDataAsObjectNode.toString().getBytes())
				.setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertNull(newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessWithoutGender() throws IOException {
		identityMapping.getIdentity().getGender().setValue(null);
		ObjectNode identityDataAsObjectNode = mapper.readValue(identityData, ObjectNode.class);
		identityDataAsObjectNode.remove(identityMapping.getIdentity().getGender().getValue());
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setFilterLanguage("eng")
				.setProcessName("New").setNewIdentity(identityDataAsObjectNode.toString().getBytes())
				.setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertNull(newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessWithoutLocation() throws IOException {
		IdentityIssuanceProfileBuilder.getIdentityMapping().getIdentity().getLocationHierarchyForProfiling()
				.setValue(null);
		ObjectNode identityDataAsObjectNode = mapper.readValue(identityData, ObjectNode.class);
		IdentityIssuanceProfileBuilder.getIdentityMapping().getIdentity().getLocationHierarchyForProfiling()
				.getValueList().forEach(identityDataAsObjectNode::remove);
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setProcessName("New")
				.setFilterLanguage("eng").setNewIdentity(identityDataAsObjectNode.toString().getBytes())
				.setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertTrue(newProfile.getNewProfile().getLocation().isEmpty());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessWithoutChannel() throws IOException {
		ObjectNode identityDataAsObjectNode = mapper.readValue(identityData, ObjectNode.class);
		identityDataAsObjectNode.remove(identityMapping.getIdentity().getPhone().getValue());
		identityDataAsObjectNode.remove(identityMapping.getIdentity().getEmail().getValue());
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setProcessName("New")
				.setFilterLanguage("eng").setNewIdentity(identityDataAsObjectNode.toString().getBytes())
				.setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertTrue(newProfile.getNewProfile().getChannel().isEmpty());
		assertEquals(5, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(13, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessWithoutExceptions() throws IOException {
		ObjectNode identityDataAsObjectNode = mapper.readValue(identityData, ObjectNode.class);
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setFilterLanguage("eng")
				.setProcessName("New").setNewIdentity(identityDataAsObjectNode.toString().getBytes())
				.setOldDocuments(List.of(new DocumentsDTO())).setNewDocuments(List.of(new DocumentsDTO())).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertTrue(newProfile.getNewProfile().getExceptions().isEmpty());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertTrue(newProfile.getNewProfile().getBiometricInfo().isEmpty());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessBuildException() {
		IdentityIssuanceProfileBuilder.setIdentityMapping(null);
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setProcessName("New")
				.setNewIdentity(identityData.getBytes()).setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List.of(new DocumentsDTO("individualBiometrics", cbeff))).build();
		assertEquals(new IdentityIssuanceProfile(), newProfile);
	}

	@Test
	public void testNewProfileSuccessBuildWithoutBioDocuments() {
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setProcessName("New")
				.setNewIdentity(identityData.getBytes()).build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(0, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(0, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

	@Test
	public void testNewProfileSuccessBuildInvalidBioDocuments() {
		IdentityIssuanceProfile newProfile = IdentityIssuanceProfile.builder().setProcessName("New")
				.setNewIdentity(identityData.getBytes()).setOldDocuments(List.of(new DocumentsDTO()))
				.setNewDocuments(List
						.of(new DocumentsDTO("individualBiometrics", CryptoUtil.encodeToPlainBase64("".getBytes()))))
				.build();
		assertEquals(LocalDate.now(), newProfile.getDate());
		assertEquals("New", newProfile.getProcessName());
		assertNull(newProfile.getOldProfile());
		assertEquals("1972", newProfile.getNewProfile().getYearOfBirth());
		assertEquals("Male", newProfile.getNewProfile().getGender());
		assertEquals(List.of("BNMR", "14022", "RSK", "KTA"), newProfile.getNewProfile().getLocation());
		assertNull(newProfile.getNewProfile().getPreferredLanguage());
		assertEquals(List.of("PHONE", "EMAIL"), newProfile.getNewProfile().getChannel());
		assertEquals(0, newProfile.getNewProfile().getExceptions().size());
		assertEquals(expectedNode, newProfile.getNewProfile().getVerified());
		assertEquals(0, newProfile.getNewProfile().getBiometricInfo().size());
		assertEquals(List.of("DOC015", "DOC006", "DOC025", "COE"), newProfile.getNewProfile().getDocuments());
	}

}