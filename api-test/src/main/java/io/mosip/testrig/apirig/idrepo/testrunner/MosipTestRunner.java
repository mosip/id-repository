package io.mosip.testrig.apirig.idrepo.testrunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
//import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import io.mosip.testrig.apirig.utils.DependencyResolver;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.testng.TestNG;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import io.mosip.testrig.apirig.dataprovider.BiometricDataProvider;
import io.mosip.testrig.apirig.dataprovider.util.DataProviderConstants;
import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.ExtractResource;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.testrunner.OTPListener;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthTestsUtil;
import io.mosip.testrig.apirig.utils.CertsUtil;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.JWKKeyUtil;
import io.mosip.testrig.apirig.utils.KernelAuthentication;
import io.mosip.testrig.apirig.utils.KeyCloakUserAndAPIKeyGeneration;
import io.mosip.testrig.apirig.utils.KeycloakUserManager;
import io.mosip.testrig.apirig.utils.MispPartnerAndLicenseKeyGeneration;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.PartnerRegistration;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;

/**
 * Class to initiate mosip api test execution
 *
 * @author Vignesh
 *
 */
public class MosipTestRunner {
	static {
		configureApiTestLogDir();
	}

	private static final Logger LOGGER = Logger.getLogger(MosipTestRunner.class);
	private static String cachedPath = null;

	public static String jarUrl = MosipTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	public static List<String> languageList = new ArrayList<>();

	/**
	 * C Main method to start mosip test execution
	 *
	 * @param arg
	 */
	public static void main(String[] arg) {

		try {
			LOGGER.info("** ------------- API Test Rig Run Started --------------------------------------------- **");

			BaseTestCase.setRunContext(getRunType(), jarUrl);
			ExtractResource.removeOldMosipTestTestResource();
			if (getRunType().equalsIgnoreCase("JAR")) {
				ExtractResource.extractCommonResourceFromJar();
			} else {
				ExtractResource.copyCommonResources();
			}
			AdminTestUtil.init();
			IdRepoConfigManager.init();
			suiteSetup(getRunType());
			SkipTestCaseHandler.loadTestcaseToBeSkippedList("testCaseSkippedList.txt");
			GlobalMethods.setModuleNameAndReCompilePattern(IdRepoConfigManager.getproperty("moduleNamePattern"));
			setLogLevels();

			HealthChecker healthcheck = new HealthChecker();
			healthcheck.setCurrentRunningModule(GlobalConstants.IDREPO);
			Thread trigger = new Thread(healthcheck);
			trigger.start();

			// Skip Keycloak Admin + PMS partner registration for local WireMock (no Admin API).
			// MDS / Mock SBI still runs — local device keystore + Profile ISOs are seeded below.
			boolean skipPartnerSetup = shouldSkipPartnerSetup();
			if (skipPartnerSetup) {
				LOGGER.warn("Skipping KeycloakUserManager user create/remove "
						+ "(local IAM on WireMock has no Keycloak Admin API).");
			} else {
				KeycloakUserManager.removeUser();
				KeycloakUserManager.createUsers();
				KeycloakUserManager.closeKeycloakInstance();
			}
			try {
				AdminTestUtil.getRequiredField();
			} catch (Exception requiredFieldEx) {
				if (isLocalEndpoint()) {
					throw new RuntimeException(
							"Local admin auth / schema bootstrap failed. "
									+ "Check WireMock POST /v1/authmanager/authenticate/internal/useridPwd "
									+ "returns JSON with response.token (not HTTP 500/HTML). "
									+ "Recreate mock-service after mapping edits: "
									+ "docker compose up -d --force-recreate --no-deps mock-service",
							requiredFieldEx);
				}
				throw requiredFieldEx;
			}

			BaseTestCase.getLanguageList();
			AdminTestUtil.getLocationData();

			// Mock SBI always loads ./application.properties from cwd.
			// Keep that file under src/main/resources/mds and execute biometric
			// generation with user.dir temporarily pointed at mds (no root-level copy).
			Path mdsRoot = ensureMockSbiResourcesFromClasspath();

			if (skipPartnerSetup) {
				LOGGER.warn("Skipping PartnerRegistration.deviceGeneration() "
						+ "(idrepo.skipPartnerSetup or local endpoint); "
						+ "seeding Mock SBI keystore from classpath mds/ instead.");
				seedMockSbiSigningKeys();
			} else {
				try {
					PartnerRegistration.deleteCertificates();
					PartnerRegistration.deviceGeneration();
				} catch (Exception partnerEx) {
					LOGGER.error("PartnerRegistration failed", partnerEx);
					if (isLocalEndpoint()) {
						LOGGER.warn("PartnerRegistration failed locally; seeding Mock SBI signing keys instead.");
						seedMockSbiSigningKeys();
					} else {
						throw partnerEx;
					}
				}
			}

			runWithUserDir(mdsRoot, () -> BiometricDataProvider.generateBiometricTestData("Registration"));
			if (!isGeneratedCbeffAcceptable()) {
				LOGGER.warn("Mock SBI CBEFF is not identity-schema safe (missing BDB or "
						+ "XMLBuilder layout fails idrepo CBEFF XSD). Loading bundled "
						+ "config/bioValue.properties instead.");
				loadBundledBioValueProperties();
			}
			assertNonEmptyBioValue();

			String testCasesToExecuteString = IdRepoConfigManager.getproperty("testCasesToExecute");

			DependencyResolver.loadDependencies(
					getGlobalResourcePath() + "/" + "config/testCaseInterDependency.json");
			if (!testCasesToExecuteString.isBlank()) {
				IdRepoUtil.testCasesInRunScope = DependencyResolver.getDependencies(testCasesToExecuteString);
			}

			startTestRunner();
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		} catch (Error e) {
			LOGGER.fatal("Fatal error during test run", e);
			throw e;
		} finally {
			OTPListener.bTerminate = true;
			HealthChecker.bTerminate = true;

			try {
				IdRepoUtil.dbCleanUp();
			} catch (Exception cleanupEx) {
				LOGGER.error("DB cleanup failed", cleanupEx);
			}
			if (!shouldSkipPartnerSetup()) {
				try {
					KeycloakUserManager.removeUser();
				} catch (Exception cleanupEx) {
					LOGGER.error("Keycloak user removal failed", cleanupEx);
				} finally {
					KeycloakUserManager.closeKeycloakInstance();
				}
			}
		}

		// Used for generating the test case interdependency JSON file
		// AdminTestUtil.generateTestCaseInterDependencies(getGlobalResourcePath() + "/config/testCaseInterDependency.json");
		System.exit(0);

	}

	public static void suiteSetup(String runType) {
		if (IdRepoConfigManager.IsDebugEnabled())
			LOGGER.setLevel(Level.ALL);
		else
			LOGGER.info("Test Framework for Mosip api Initialized");
		BaseTestCase.initialize();
		// apitest-commons 1.7.0 KeyMgrUtility replaces ':' on Windows; BiometricDataProvider
		// still uses BaseTestCase.domain as a folder name (localhost:8082 is illegal on Windows).
		sanitizeCertDomainForWindows();
		LOGGER.info("Done with BeforeSuite and test case setup! su TEST EXECUTION!\n\n");

		if (!runType.equalsIgnoreCase("JAR")) {
			AuthTestsUtil.removeOldMosipTempTestResource();
		}
		BaseTestCase.currentModule = BaseTestCase.runContext + GlobalConstants.IDREPO;
		BaseTestCase.certsForModule = BaseTestCase.runContext + GlobalConstants.IDREPO;
		IdRepoUtil.dbCleanUp();

		AdminTestUtil.copyIdrepoTestResource();
		BaseTestCase.otpListener = new OTPListener();
		BaseTestCase.otpListener.run();
	}

	/**
	 * Same Windows path rule as apitest-commons 1.7.0 {@code KeyMgrUtility.getKeysDirPath}:
	 * {@code localhost:8082} cannot be a folder name.
	 */
	static void sanitizeCertDomainForWindows() {
		if (BaseTestCase.domain != null
				&& System.getProperty("os.name").toLowerCase().contains("windows")
				&& BaseTestCase.domain.contains(":")) {
			String sanitized = BaseTestCase.domain.replace(":", "_");
			LOGGER.info("Windows certs folder: BaseTestCase.domain " + BaseTestCase.domain + " -> " + sanitized);
			BaseTestCase.domain = sanitized;
		}
	}

	private static final String MDS_CLASSPATH_ROOT = "mds";
	private static final String MDS_PROPS = "mds/application.properties";
	private static final String MDS_SRC_RELATIVE = "src/main/resources/mds";

	/**
	 * Mock SBI always loads {@code ./application.properties} from cwd and joins paths as
	 * {@code cwd + /Biometric Devices/...} and {@code cwd + /resource/Profile}. It cannot
	 * take absolute paths (it prefixes cwd). Canonical files live under
	 * {@code src/main/resources/mds} — write only a rewritten properties file to cwd so
	 * those joins resolve into mds. Do not copy Biometric Devices / Profile / resource
	 * into the module root.
	 */
	static Path ensureMockSbiResourcesFromClasspath() throws IOException {
		Path mdsRoot = resolveMdsSourceDir();
		if (!isCompleteMds(mdsRoot)) {
			throw new IllegalStateException(
					"Incomplete mds/ at " + mdsRoot
							+ " (need application.properties, Biometric Devices/.../mosipface.p12, "
							+ "resource/Profile/Default/Registration/Face.iso).");
		}

		DataProviderConstants.RESOURCE = mdsRoot.resolve("resource").toString().replace('\\', '/') + "/";
		LOGGER.info("Mock SBI devices/profiles from " + mdsRoot
				+ " (using mds/application.properties directly)");
		return mdsRoot;
	}

	static boolean isCompleteMds(Path mdsRoot) {
		return Files.isRegularFile(mdsRoot.resolve("application.properties"))
				&& Files.isRegularFile(mdsRoot.resolve("Biometric Devices").resolve("Face")
				.resolve("Keys").resolve("mosipface.p12"))
				&& Files.isRegularFile(mdsRoot.resolve("resource").resolve("Profile")
				.resolve("Default").resolve("Registration").resolve("Face.iso"));
	}

	@FunctionalInterface
	interface ThrowingRunnable {
		void run() throws Exception;
	}

	static void runWithUserDir(Path dir, ThrowingRunnable action) throws Exception {
		String originalUserDir = System.getProperty("user.dir");
		System.setProperty("user.dir", dir.toAbsolutePath().normalize().toString());
		try {
			action.run();
		} finally {
			if (originalUserDir != null) {
				System.setProperty("user.dir", originalUserDir);
			}
		}
	}

	/**
	 * Local runs skip PMS {@code PartnerRegistration.deviceGeneration()}. Copy bundled
	 * modality keystores from mds under {@code keysDir} (TEMP {@code AUTHCERTS} /
	 * {@code authCertsPath}).
	 */
	static void seedMockSbiSigningKeys() throws IOException {
		Path mdsRoot = resolveMdsSourceDir();
		Path biometricDevices = mdsRoot.resolve("Biometric Devices");
		Path faceKey = biometricDevices.resolve("Face").resolve("Keys").resolve("mosipface.p12");
		if (!Files.isRegularFile(faceKey)) {
			throw new IllegalStateException("Missing Mock SBI device keystore in mds: " + faceKey);
		}

		String keysDir = BiometricDataProvider.getKeysDirPath("", BaseTestCase.certsForModule);
		Path keysBiometricDevices = Path.of(keysDir).resolve("Biometric Devices");
		Path keysFace = keysBiometricDevices.resolve("Face").resolve("Keys").resolve("mosipface.p12");
		if (!Files.isRegularFile(keysFace)) {
			LOGGER.info("Seeding Mock SBI signing keystore: " + biometricDevices + " -> " + keysBiometricDevices);
			Files.createDirectories(Path.of(keysDir));
			copyDirectory(biometricDevices, keysBiometricDevices);
		} else {
			LOGGER.info("Mock SBI signing keystore already present: " + keysFace);
		}
		if (!Files.isRegularFile(keysFace)) {
			throw new IllegalStateException("Failed to seed Mock SBI face keystore at " + keysFace);
		}
	}

	/**
	 * Prefer {@code api-test/src/main/resources/mds} so source edits apply without rebuild.
	 * Fall back to exploded classpath {@code target/classes/mds}, then extract from the fat
	 * jar into {@code target/mds-runtime}.
	 */
	static Path resolveMdsSourceDir() throws IOException {
		File moduleDir = resolveApiTestModuleDir();
		Path srcMds = moduleDir.toPath().resolve(MDS_SRC_RELATIVE).toAbsolutePath().normalize();
		if (isCompleteMds(srcMds)) {
			LOGGER.info("Using Mock SBI mds from source: " + srcMds);
			return srcMds;
		}

		URL marker = MosipTestRunner.class.getClassLoader().getResource(MDS_PROPS);
		if (marker == null) {
			throw new IllegalStateException(
					"mds not found at " + srcMds + " and classpath resource " + MDS_PROPS
							+ " is missing. Expected src/main/resources/mds.");
		}
		try {
			if ("file".equalsIgnoreCase(marker.getProtocol())) {
				Path classpathMds = Path.of(marker.toURI()).getParent();
				LOGGER.info("Using Mock SBI mds from classpath: " + classpathMds);
				return classpathMds;
			}
			if ("jar".equalsIgnoreCase(marker.getProtocol())) {
				Path extractTo = Path.of("target", "mds-runtime").toAbsolutePath().normalize();
				extractMdsFromJar(marker.toURI(), extractTo);
				return extractTo;
			}
			throw new IllegalStateException("Unsupported mds resource URL: " + marker);
		} catch (URISyntaxException e) {
			throw new IOException("Failed to resolve mds classpath location: " + marker, e);
		}
	}

	static void extractMdsFromJar(URI jarEntryUri, Path extractTo) throws IOException {
		String raw = jarEntryUri.toString();
		int sep = raw.indexOf("!/");
		if (sep < 0) {
			throw new IOException("Not a jar resource URI: " + jarEntryUri);
		}
		URI jarFileUri = URI.create(raw.substring(0, sep));
		Path markerOut = extractTo.resolve("application.properties");
		if (Files.isRegularFile(markerOut)
				&& Files.isRegularFile(extractTo.resolve("Biometric Devices").resolve("Face")
				.resolve("Keys").resolve("mosipface.p12"))) {
			LOGGER.info("Reusing extracted Mock SBI mds at " + extractTo);
			return;
		}
		Files.createDirectories(extractTo);
		try (FileSystem jarFs = FileSystems.newFileSystem(jarFileUri, Collections.emptyMap())) {
			Path mdsInJar = jarFs.getPath("/" + MDS_CLASSPATH_ROOT);
			if (!Files.isDirectory(mdsInJar)) {
				mdsInJar = jarFs.getPath(MDS_CLASSPATH_ROOT);
			}
			if (!Files.isDirectory(mdsInJar)) {
				throw new IOException("mds/ not found inside jar: " + jarFileUri);
			}
			Path root = mdsInJar;
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path rel = root.relativize(dir);
					Files.createDirectories(extractTo.resolve(rel.toString()));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path rel = root.relativize(file);
					Path out = extractTo.resolve(rel.toString());
					Files.createDirectories(out.getParent());
					try (InputStream in = Files.newInputStream(file)) {
						Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		LOGGER.info("Extracted Mock SBI mds from jar -> " + extractTo);
	}

	/**
	 * Empty shell CBEFF (no BIRs) is accepted by AddIdentity but breaks credential extract (IDR-IDS-009).
	 * Identity service also XSD-validates CBEFF ({@code IDR-IDC-002} on {@code documents/0/value}).
	 */
	static void assertNonEmptyBioValue() {
		String bioValue = BiometricDataProvider.getFromBiometricMap("BioValue");
		if (bioValue == null || bioValue.isBlank() || bioValue.length() < 500) {
			throw new IllegalStateException(
					"Mock SBI produced empty/shell BioValue (len="
							+ (bioValue == null ? 0 : bioValue.length())
							+ "). Check src/main/resources/mds (Biometric Devices + resource/Profile) "
							+ "and keys under getKeysDirPath. "
							+ "Do not continue — AddIdentity would store a shell CBEFF and credential "
							+ "issue fails with IDR-IDS-009.");
		}
		if (!isGeneratedCbeffAcceptable()) {
			throw new IllegalStateException(
					"BioValue is present but not a usable CBEFF for AddIdentity "
							+ "(missing BDB or not JAXB/MOSIP CBEFF layout). "
							+ "idrepo rejects that as IDR-IDC-002 documents/0/value. "
							+ "Expected config/bioValue.properties on the classpath.");
		}
		LOGGER.info("BioValue ready for AddIdentity (len=" + bioValue.length() + ")");
	}

	/**
	 * Identity {@code cbeffUtil.validateXML} runs MOSIP CBEFF XSD. Mock SBI's
	 * XMLBuilder CBEFF often fails that XSD even when BDB is present; the bundled
	 * {@code bioValue.properties} blob is JAXB-produced ({@code standalone="yes"})
	 * and is what AddIdentity historically sent.
	 */
	static boolean isGeneratedCbeffAcceptable() {
		String bioValue = BiometricDataProvider.getFromBiometricMap("BioValue");
		if (bioValue == null || bioValue.isBlank()) {
			return false;
		}
		try {
			String padded = bioValue + "=".repeat((4 - (bioValue.length() % 4)) % 4);
			byte[] xmlBytes = Base64.getUrlDecoder().decode(padded);
			String xml = new String(xmlBytes, StandardCharsets.UTF_8);
			if (!xml.contains("<BIR")) {
				return false;
			}
			int bdb = xml.indexOf("<BDB>");
			int bdbEnd = xml.indexOf("</BDB>");
			if (bdb < 0 || bdbEnd <= bdb + 20) {
				return false;
			}
			return xml.contains("standalone=\"yes\"");
		} catch (Exception e) {
			LOGGER.warn("Generated BioValue is not decodable CBEFF: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Known-good CBEFF moved to {@code src/main/resources/config/bioValue.properties}
	 * when MDS folders were relocated. Overlay onto the biometric map so AddIdentity
	 * does not send an XSD-invalid Mock SBI blob.
	 */
	static void loadBundledBioValueProperties() throws IOException {
		URL resource = MosipTestRunner.class.getClassLoader().getResource("config/bioValue.properties");
		if (resource == null) {
			throw new IllegalStateException(
					"config/bioValue.properties not on classpath. It must live under "
							+ "api-test/src/main/resources/config/ after the MDS/config folder move.");
		}
		Properties props = new Properties();
		try (InputStream in = resource.openStream()) {
			props.load(in);
		}
		String bio = props.getProperty("BioValue");
		if (bio == null || bio.isBlank()) {
			throw new IllegalStateException("config/bioValue.properties has no BioValue key");
		}
		for (String key : props.stringPropertyNames()) {
			String val = props.getProperty(key);
			if (val != null && !val.isBlank()) {
				BiometricDataProvider.addToBiometricMap(key, val.trim());
			}
		}
		LOGGER.info("Loaded bundled BioValue from config/bioValue.properties (len="
				+ bio.trim().length() + ")");
	}

	static void copyDirectory(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(target.resolve(source.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	static boolean shouldSkipPartnerSetup() {
		String flag = System.getProperty("idrepo.skipPartnerSetup");
		if (flag != null && !flag.isBlank()) {
			return Boolean.parseBoolean(flag);
		}
		return isLocalEndpoint();
	}

	static boolean isLocalEndpoint() {
		String endpoint = System.getProperty("env.endpoint", "");
		return endpoint.contains("localhost") || endpoint.contains("127.0.0.1");
	}

	private static void setLogLevels() {
		AdminTestUtil.setLogLevel();
		OutputValidationUtil.setLogLevel();
		PartnerRegistration.setLogLevel();
		KeyCloakUserAndAPIKeyGeneration.setLogLevel();
		MispPartnerAndLicenseKeyGeneration.setLogLevel();
		JWKKeyUtil.setLogLevel();
		CertsUtil.setLogLevel();
		KernelAuthentication.setLogLevel();
		BaseTestCase.setLogLevel();
		IdRepoUtil.setLogLevel();
		KeycloakUserManager.setLogLevel();
		DBManager.setLogLevel();
		BiometricDataProvider.setLogLevel();
	}

	/**
	 * The method to start mosip testng execution
	 *
	 * @throws IOException
	 */
	public static void startTestRunner() {
		File homeDir = null;
		String os = System.getProperty("os.name");
		LOGGER.info(os);
		if (getRunType().contains("IDE") || os.toLowerCase().contains("windows")) {
			homeDir = new File(System.getProperty("user.dir") + "/testNgXmlFiles");
			LOGGER.info("IDE :" + homeDir);
		} else {
			File dir = new File(System.getProperty("user.dir"));
			homeDir = new File(dir.getParent() + "/mosip/testNgXmlFiles");
			LOGGER.info("ELSE :" + homeDir);
		}
		File[] files = homeDir.listFiles();
		if (files != null) {
			for (File file : files) {
				TestNG runner = new TestNG();
				List<String> suitefiles = new ArrayList<>();
				if (file.getName().toLowerCase().contains("mastertestsuite")) {
					BaseTestCase.setReportName(GlobalConstants.IDREPO);
					suitefiles.add(file.getAbsolutePath());
					runner.setTestSuites(suitefiles);
					System.getProperties().setProperty("testng.outpur.dir", "testng-report");
					runner.setOutputDirectory("testng-report");
					runner.run();
				}
			}
		} else {
			LOGGER.error("No files found in directory: " + homeDir);
		}
	}

	public static String getGlobalResourcePath() {
		if (cachedPath != null) {
			return cachedPath;
		}

		String path = null;
		if (getRunType().equalsIgnoreCase("JAR")) {
			path = new File(jarUrl).getParentFile().getAbsolutePath() + "/MosipTestResource/MosipTemporaryTestResource";
		} else if (getRunType().equalsIgnoreCase("IDE")) {
			path = new File(MosipTestRunner.class.getClassLoader().getResource("").getPath()).getAbsolutePath()
					+ "/MosipTestResource/MosipTemporaryTestResource";
			if (path.contains(GlobalConstants.TESTCLASSES))
				path = path.replace(GlobalConstants.TESTCLASSES, "classes");
		}

		if (path != null) {
			cachedPath = path;
			return path;
		} else {
			return "Global Resource File Path Not Found";
		}
	}

	public static String getResourcePath() {
		return getGlobalResourcePath();
	}

	public static String generatePulicKey() {
		String publicKey = null;
		try {
			KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
			keyGenerator.initialize(2048, BaseTestCase.secureRandom);
			final KeyPair keypair = keyGenerator.generateKeyPair();
			publicKey = java.util.Base64.getEncoder().encodeToString(keypair.getPublic().getEncoded());
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage());
		}
		return publicKey;
	}

	public static KeyPairGenerator keyPairGen = null;

	public static KeyPairGenerator getKeyPairGeneratorInstance() {
		if (keyPairGen != null)
			return keyPairGen;
		try {
			keyPairGen = KeyPairGenerator.getInstance("RSA");
			keyPairGen.initialize(2048);

		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage());
		}

		return keyPairGen;
	}

	public static String generatePublicKeyForMimoto() {

		String vcString = "";
		try {
			KeyPairGenerator keyPairGenerator = getKeyPairGeneratorInstance();
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			StringWriter stringWriter = new StringWriter();
			try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
				pemWriter.writeObject(publicKey);
				pemWriter.flush();
				vcString = stringWriter.toString();
				if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					vcString = vcString.replaceAll("\r\n", "\\\\n");
				} else {
					vcString = vcString.replaceAll("\n", "\\\\n");
				}
			} catch (Exception e) {
				throw e;
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return vcString;
	}

	public static String generateJWKPublicKey() {
		try {
			KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
			keyGenerator.initialize(2048, BaseTestCase.secureRandom);
			final KeyPair keypair = keyGenerator.generateKeyPair();
			RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keypair.getPublic()).keyID("RSAKeyID")
					.keyUse(KeyUse.SIGNATURE).privateKey(keypair.getPrivate()).build();

			return jwk.toJSONString();
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage());
			return null;
		}
	}

	public static Properties getproperty(String path) {
		Properties prop = new Properties();
		FileInputStream inputStream = null;
		try {
			File file = new File(path);
			inputStream = new FileInputStream(file);
			prop.load(inputStream);
		} catch (Exception e) {
			LOGGER.error(GlobalConstants.EXCEPTION_STRING_2 + e.getMessage());
		} finally {
			AdminTestUtil.closeInputStream(inputStream);
		}
		return prop;
	}

	/**
	 * Point Log4j at {@code api-test/logs} before the first logger is created.
	 * Relative {@code logs/} or {@code src/logs/} follows {@code user.dir}, which is
	 * the repo root when launching from the IDE.
	 */
	private static void configureApiTestLogDir() {
		File moduleDir = resolveApiTestModuleDir();
		File logDir;
		String override = System.getProperty("api.test.log.dir");
		if (override != null && !override.isBlank()) {
			logDir = new File(override);
		} else {
			logDir = new File(moduleDir, "logs");
		}
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		System.setProperty("api.test.log.dir", logDir.getAbsolutePath().replace('\\', '/'));
		File log4j = new File(moduleDir, "src/main/resources/log4j.properties");
		if (log4j.isFile()) {
			PropertyConfigurator.configure(log4j.getAbsolutePath());
		}
	}

	private static File resolveApiTestModuleDir() {
		try {
			java.net.URL location = MosipTestRunner.class.getProtectionDomain().getCodeSource().getLocation();
			if (location != null) {
				File locFile;
				try {
					locFile = new File(location.toURI());
				} catch (Exception uriEx) {
					String decoded = URLDecoder.decode(location.getPath(), StandardCharsets.UTF_8);
					locFile = new File(decoded);
				}
				File dir = locFile.isFile() ? locFile.getParentFile() : locFile;
				if (dir != null && "classes".equalsIgnoreCase(dir.getName())) {
					dir = dir.getParentFile();
				}
				if (dir != null && "target".equalsIgnoreCase(dir.getName()) && dir.getParentFile() != null) {
					return dir.getParentFile();
				}
			}
		} catch (Exception ignored) {
		}
		File cwd = new File(System.getProperty("user.dir", "."));
		if (new File(cwd, "src/main/resources/log4j.properties").isFile()) {
			return cwd;
		}
		File nested = new File(cwd, "api-test");
		if (new File(nested, "pom.xml").isFile()) {
			return nested;
		}
		return cwd;
	}

	/**
	 * The method will return mode of application started either from jar or eclipse
	 * ide
	 *
	 * @return
	 */
	public static String getRunType() {
		if (MosipTestRunner.class.getResource("MosipTestRunner.class").getPath().contains(".jar"))
			return "JAR";
		else
			return "IDE";
	}

}