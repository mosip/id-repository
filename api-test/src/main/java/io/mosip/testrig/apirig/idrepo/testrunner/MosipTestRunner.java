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
 * Entry point of the ID Repository API test rig ({@code apitest-idrepo}).
 * <p>
 * {@link #main(String[])} prepares the environment and then runs every TestNG suite file whose
 * name contains {@code mastertestsuite}. Steps, in order:
 * <ol>
 * <li>Extracts the common and idrepo test resources (from the jar or the IDE classpath) into
 * {@code MosipTestResource/MosipTemporaryTestResource}.</li>
 * <li>Initialises configuration ({@code AdminTestUtil}, {@link IdRepoConfigManager}), cleans the
 * databases, loads the known-issue skip list and starts the health checker and OTP listener.</li>
 * <li>Against a real environment: creates the Keycloak users and registers partner devices.
 * Locally (endpoint on {@code localhost}/{@code 127.0.0.1}, or {@code -Didrepo.skipPartnerSetup=true})
 * both steps are skipped, because WireMock has no Keycloak Admin or PMS API, and the Mock SBI
 * signing keys are seeded from {@code src/main/resources/mds} instead.</li>
 * <li>Loads the IdSchema required fields, languages and location data, then generates biometric
 * test data through Mock SBI. It falls back to the bundled {@code config/bioValue.properties} when
 * the generated CBEFF would fail idrepo's CBEFF XSD.</li>
 * <li>Restricts the run to {@code testCasesToExecute} plus their dependencies, when that property
 * is set.</li>
 * <li>Always: cleans the databases and removes Keycloak users (when they were created), then exits.</li>
 * </ol>
 * Logs are written to {@code api-test/.local/logs}, or to {@code -Dapi.test.log.dir}.
 *
 * @author Vignesh
 *
 */
public class MosipTestRunner {
	static {
		configureApiTestLogDir();
	}

	private static final Logger LOGGER = Logger.getLogger(MosipTestRunner.class);
	/** Memoised result of {@link #getGlobalResourcePath()}. */
	private static String cachedPath = null;

	/** Location of this class's code source (the fat jar, or {@code target/classes} in the IDE). */
	public static String jarUrl = MosipTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	/** Not populated by this class; kept for callers that expect it. Use {@code BaseTestCase.languageList}. */
	public static List<String> languageList = new ArrayList<>();

	/**
	 * Runs the whole API test rig: setup (including DB cleanup), every master TestNG suite, then
	 * {@code System.exit(0)}. Test data is left in the DB after the run for inspection; the next
	 * run's setup deletes it.
	 * <p>
	 * Any setup or run failure is logged and rethrown (checked exceptions wrapped in
	 * {@link RuntimeException}); Keycloak users are still removed. Locally, a schema bootstrap failure is
	 * reported with a hint that the WireMock admin-auth stub is broken.
	 *
	 * @param arg not used; configuration comes from system properties and the idrepo properties
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

	/**
	 * One-time suite setup that runs before the TestNG suites.
	 * <p>
	 * Calls {@code BaseTestCase.initialize()}, then redirects Log4j back to the api-test log folder
	 * (commons resets it to {@code src/logs}) and removes the stray commons log. On Windows it
	 * makes the certificate-folder domain path-safe. It also sets the module and certificate
	 * prefixes for this run, cleans the databases, copies the idrepo test resources and starts the
	 * OTP listener.
	 *
	 * @param runType {@code JAR} or {@code IDE}; outside a jar the old temporary resources are deleted first
	 */
	public static void suiteSetup(String runType) {
		if (IdRepoConfigManager.IsDebugEnabled())
			LOGGER.setLevel(Level.ALL);
		else
			LOGGER.info("Test Framework for Mosip api Initialized");
		BaseTestCase.initialize();
		// apitest-commons initialize() reconfigures Log4j to a hard-coded src/logs/mosip-api-test.log.
		configureApiTestLogDir();
		removeCommonsSrcLogs();
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

	/** Classpath folder holding the Mock SBI resources. */
	private static final String MDS_CLASSPATH_ROOT = "mds";
	/** Marker resource used to locate {@link #MDS_CLASSPATH_ROOT} on the classpath. */
	private static final String MDS_PROPS = "mds/application.properties";
	/** Source location of the Mock SBI resources, relative to the api-test module. */
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

	/**
	 * @return true when {@code mdsRoot} has {@code application.properties}, the face device keystore
	 *         and the default Registration face profile
	 */
	static boolean isCompleteMds(Path mdsRoot) {
		return Files.isRegularFile(mdsRoot.resolve("application.properties"))
				&& Files.isRegularFile(mdsRoot.resolve("Biometric Devices").resolve("Face")
				.resolve("Keys").resolve("mosipface.p12"))
				&& Files.isRegularFile(mdsRoot.resolve("resource").resolve("Profile")
				.resolve("Default").resolve("Registration").resolve("Face.iso"));
	}

	/** A {@link Runnable} that may throw a checked exception. */
	@FunctionalInterface
	interface ThrowingRunnable {
		void run() throws Exception;
	}

	/**
	 * Runs {@code action} with the system property {@code user.dir} temporarily set to {@code dir},
	 * then restores it, even on failure. Mock SBI resolves its files from {@code user.dir}.
	 */
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

	/**
	 * Copies the {@code mds/} tree out of the fat jar into {@code extractTo}. If an earlier
	 * extraction (properties file plus face keystore) is already there, it is reused.
	 *
	 * @param jarEntryUri {@code jar:file:...!/mds/application.properties} URI
	 * @param extractTo   target folder
	 * @throws IOException when the URI is not a jar entry or the jar has no {@code mds/} folder
	 */
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

	/** Recursively copies {@code source} into {@code target}, overwriting existing files. */
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

	/**
	 * Whether to skip Keycloak user management and PMS partner/device registration.
	 *
	 * @return the value of {@code -Didrepo.skipPartnerSetup} when set, otherwise
	 *         {@link #isLocalEndpoint()}
	 */
	static boolean shouldSkipPartnerSetup() {
		String flag = System.getProperty("idrepo.skipPartnerSetup");
		if (flag != null && !flag.isBlank()) {
			return Boolean.parseBoolean(flag);
		}
		return isLocalEndpoint();
	}

	/** @return true when {@code -Denv.endpoint} points to {@code localhost} or {@code 127.0.0.1} */
	static boolean isLocalEndpoint() {
		String endpoint = System.getProperty("env.endpoint", "");
		return endpoint.contains("localhost") || endpoint.contains("127.0.0.1");
	}

	/** Applies the debug / non-debug log level to every commons and idrepo helper the run uses. */
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
	 * Runs, one after another, every TestNG suite file whose name contains {@code mastertestsuite}.
	 * <p>
	 * The suite folder is {@code <user.dir>/testNgXmlFiles} when run from the IDE or on Windows, and
	 * {@code <parent of user.dir>/mosip/testNgXmlFiles} otherwise (container layout). Reports are
	 * written to {@code testng-report}. A missing folder is logged and nothing runs.
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

	/**
	 * Folder that holds the extracted test resources ({@code config/}, {@code idRepository/} YAML and
	 * HBS, ...): {@code <jar dir>/MosipTestResource/MosipTemporaryTestResource} in JAR mode, or
	 * {@code <classes dir>/MosipTestResource/MosipTemporaryTestResource} in IDE mode. The result is
	 * cached after the first successful call.
	 *
	 * @return absolute path, or the literal {@code "Global Resource File Path Not Found"}
	 */
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

	/** Alias of {@link #getGlobalResourcePath()}. */
	public static String getResourcePath() {
		return getGlobalResourcePath();
	}

	/**
	 * Generates a fresh RSA-2048 key pair and returns its public key.
	 *
	 * @return the X.509 public key in standard Base64, or null if RSA is unavailable
	 */
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

	/** Lazily created RSA-2048 generator shared by {@link #generatePublicKeyForMimoto()}. */
	public static KeyPairGenerator keyPairGen = null;

	/** @return the shared RSA-2048 {@link KeyPairGenerator}, created on first use (not thread-safe) */
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

	/**
	 * Generates an RSA public key as PEM, with line breaks escaped as a literal {@code \n} so the key
	 * can be embedded in a JSON string (Mimoto wallet-binding requests).
	 *
	 * @return escaped PEM, or an empty string on failure
	 */
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

	/**
	 * Generates an RSA-2048 key pair as a JWK with {@code kid=RSAKeyID} and {@code use=sig}.
	 * Despite the name, the JSON also contains the private parts.
	 *
	 * @return JWK JSON, or null if RSA is unavailable
	 */
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

	/**
	 * Loads a properties file from the file system.
	 *
	 * @param path file path
	 * @return the loaded properties; empty (with an error logged) when the file cannot be read
	 */
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
	 * Point Log4j at {@code api-test/.local/logs} before the first logger is created.
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
			logDir = new File(moduleDir, ".local/logs");
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

	/**
	 * Deletes the {@code src/logs} file and folder that {@code BaseTestCase.initialize()} creates
	 * under {@code user.dir}. Call only after {@link #configureApiTestLogDir()} has closed that appender.
	 */
	private static void removeCommonsSrcLogs() {
		File srcLogDir = new File(System.getProperty("user.dir"), "src/logs");
		File srcLog = new File(srcLogDir, "mosip-api-test.log");
		if (srcLog.isFile() && !srcLog.delete()) {
			LOGGER.warn("Could not delete " + srcLog.getAbsolutePath());
		}
		String[] remaining = srcLogDir.list();
		if (remaining != null && remaining.length == 0) {
			srcLogDir.delete();
		}
	}

	/**
	 * Locates the {@code api-test} module folder. It is derived from the code source
	 * ({@code target/classes} or {@code target/*.jar}, then two levels up). Otherwise
	 * {@code user.dir} is used when it is the module, or {@code user.dir/api-test} when launched from
	 * the repository root, with plain {@code user.dir} as the last resort.
	 */
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
	 * Detects how the rig was launched.
	 *
	 * @return {@code JAR} when this class was loaded from a jar, otherwise {@code IDE}
	 */
	public static String getRunType() {
		if (MosipTestRunner.class.getResource("MosipTestRunner.class").getPath().contains(".jar"))
			return "JAR";
		else
			return "IDE";
	}

}