package io.mosip.testrig.apirig.idrepo.testrunner;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.mosip.testrig.apirig.dataprovider.BiometricDataProvider;
import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoConfigManager;
import io.mosip.testrig.apirig.idrepo.utils.IdRepoUtil;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.ExtractResource;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.testrunner.OTPListener;
import io.mosip.testrig.apirig.utils.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.testng.TestNG;

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
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

//import java.util.Map;

/**
 * Class to initiate mosip api test execution
 * 
 * @author Vignesh
 *
 */
public class MosipTestRunner {
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

			boolean skipPartnerSetup = shouldSkipPartnerSetup();
			if (skipPartnerSetup) {
				LOGGER.warn("Skipping Keycloak user setup (local endpoint has no Keycloak Admin API).");
			} else {
				KeycloakUserManager.removeUser();
				KeycloakUserManager.createUsers();
				KeycloakUserManager.closeKeycloakInstance();
			}
			AdminTestUtil.getRequiredField();

			BaseTestCase.getLanguageList();
			AdminTestUtil.getLocationData();

			if (skipPartnerSetup) {
				LOGGER.warn("Skipping PartnerRegistration.deviceGeneration() on local endpoint.");
			} else {
				PartnerRegistration.deleteCertificates();
				PartnerRegistration.deviceGeneration();
			}

			try {
				if (skipPartnerSetup) {
					// Local: bundled bioValue.properties only (do not require Mock SBI Face).
					LOGGER.warn("Local mode: loading bundled bioValue.properties when present.");
					loadBundledBioValueProperties();
					if (!hasUsableBioValue()) {
						LOGGER.warn("No usable bundled BioValue; attempting Mock SBI under mds/.");
						generateBioValueViaMockSbi();
					}
				} else {
					// Env/server: Mock SBI only — never fall back to bioValue.properties.
					LOGGER.info("Env mode: generating BioValue via Mock SBI");
					Boolean mdsOk = generateBioValueViaMockSbi();
					if (!Boolean.TRUE.equals(mdsOk) || !hasUsableBioValue()) {
						throw new IllegalStateException(
								"Mock SBI did not produce usable BioValue/FaceBioValue for env run "
										+ "(mdsOk=" + mdsOk + ", bioLen="
										+ bioValueLength() + "). Ensure src/main/resources/mds has "
										+ "Biometric Devices + resource/Profile Face.iso, and Face BIR "
										+ "keeps empty <Subtype></Subtype>.");
					}
				}
				if (!hasUsableBioValue()) {
					throw new IllegalStateException(
							"No usable BioValue/FaceBioValue before test start (bioLen="
									+ bioValueLength() + ").");
				}
				LOGGER.info("BioValue ready for AddIdentity (len=" + bioValueLength() + ")");
			} catch (Exception bioEx) {
				if (skipPartnerSetup) {
					LOGGER.warn("Biometric test data generation skipped/failed in local mode: "
							+ bioEx.getMessage());
					loadBundledBioValueProperties();
				} else {
					throw bioEx;
				}
			}
			
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

	/** Load BioValue keys from config/bioValue.properties (local runs only). */
	private static void loadBundledBioValueProperties() throws IOException {
		String path = getGlobalResourcePath() + "/config/bioValue.properties";
		File file = new File(path);
		if (!file.isFile()) {
			LOGGER.warn("Bundled bioValue.properties not found at " + path);
			return;
		}
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(file)) {
			props.load(in);
		}
		int loaded = 0;
		for (String key : props.stringPropertyNames()) {
			String value = props.getProperty(key);
			if (value != null && !value.isBlank()) {
				BiometricDataProvider.addToBiometricMap(key, value);
				loaded++;
			}
		}
		LOGGER.info("Loaded " + loaded + " biometric value(s) from " + path);
	}

	/** Shell CBEFF from empty Face capture is ~200 chars; require Face + real size. */
	private static boolean hasUsableBioValue() {
		String bio = BiometricDataProvider.getFromBiometricMap("BioValue");
		String face = BiometricDataProvider.getFromBiometricMap("FaceBioValue");
		return bio != null && bio.length() > 500 && face != null && !face.isBlank();
	}

	private static int bioValueLength() {
		String bio = BiometricDataProvider.getFromBiometricMap("BioValue");
		return bio == null ? 0 : bio.length();
	}

	private static final String MDS_CLASSPATH_ROOT = "mds";
	private static final String MDS_PROPS = "mds/application.properties";
	private static final String MDS_SRC_RELATIVE = "src/main/resources/mds";

	/**
	 * Mock SBI loads {@code ./application.properties} from cwd and joins
	 * {@code Biometric Devices} / {@code resource/Profile} relative to that cwd.
	 * Canonical assets live under {@code src/main/resources/mds}.
	 */
	static Path ensureMockSbiResourcesFromClasspath() throws IOException {
		Path mdsRoot = resolveMdsSourceDir();
		if (!isCompleteMds(mdsRoot)) {
			throw new IllegalStateException(
					"Incomplete mds/ at " + mdsRoot
							+ " (need application.properties, Biometric Devices/.../mosipface.p12, "
							+ "resource/Profile/Default/Registration/Face.iso).");
		}
		LOGGER.info("Mock SBI devices/profiles from " + mdsRoot);
		return mdsRoot;
	}

	/**
	 * Resolve mds/, seed device keystores, and expose that tree on the process cwd
	 * before Mock SBI starts. Skipped when localhost already has a usable bundled BioValue.
	 */
	private static Boolean generateBioValueViaMockSbi() throws Exception {
		Path mdsRoot = ensureMockSbiResourcesFromClasspath();
		seedMockSbiSigningKeys();
		LOGGER.info("Generating BioValue via Mock SBI (mds=" + mdsRoot + ")");
		return runWithMdsOnCwd(mdsRoot,
				() -> BiometricDataProvider.generateBiometricTestData("Registration"));
	}

	static boolean isCompleteMds(Path mdsRoot) {
		return Files.isRegularFile(mdsRoot.resolve("application.properties"))
				&& Files.isRegularFile(mdsRoot.resolve("Biometric Devices").resolve("Face")
						.resolve("Keys").resolve("mosipface.p12"))
				&& Files.isRegularFile(mdsRoot.resolve("resource").resolve("Profile")
						.resolve("Default").resolve("Registration").resolve("Face.iso"));
	}

	@FunctionalInterface
	interface ThrowingSupplier<T> {
		T get() throws Exception;
	}

	/**
	 * mock-mds resolves {@code ./application.properties} via {@code new File(".")}
	 * (process cwd). Bridge the canonical mds tree onto that cwd before starting Mock SBI.
	 */
	static <T> T runWithMdsOnCwd(Path dir, ThrowingSupplier<T> action) throws Exception {
		Path mdsRoot = dir.toAbsolutePath().normalize();
		exposeMdsAtProcessCwd(mdsRoot, resolveProcessCwd());
		resetMockSbiPropertyCache();
		return action.get();
	}

	private static Path resolveProcessCwd() throws IOException {
		return Path.of(new File(".").getCanonicalPath());
	}

	private static void exposeMdsAtProcessCwd(Path mdsRoot, Path processCwd) throws IOException {
		Path normalizedMds = mdsRoot.toAbsolutePath().normalize();
		Path normalizedCwd = processCwd.toAbsolutePath().normalize();
		if (normalizedCwd.equals(normalizedMds)) {
			return;
		}
		ensureMdsEntry(normalizedMds.resolve("application.properties"),
				normalizedCwd.resolve("application.properties"), false);
		ensureMdsEntry(normalizedMds.resolve("Biometric Devices"),
				normalizedCwd.resolve("Biometric Devices"), true);
		ensureMdsEntry(normalizedMds.resolve("resource"), normalizedCwd.resolve("resource"), true);
		LOGGER.info("Mock SBI cwd bridge: exposed " + normalizedMds + " at " + normalizedCwd);
	}

	private static void ensureMdsEntry(Path source, Path target, boolean directory) throws IOException {
		Path normalizedSource = source.toAbsolutePath().normalize();
		if (Files.exists(target)) {
			if (directory) {
				if (pathPointsTo(target, normalizedSource)) {
					return;
				}
				throw new IllegalStateException("Mock SBI needs " + target
						+ " but an unrelated path already exists. Remove or relocate it.");
			}
			if (Files.isRegularFile(target)) {
				Files.copy(normalizedSource, target, StandardCopyOption.REPLACE_EXISTING);
				return;
			}
			throw new IllegalStateException("Mock SBI needs " + target
					+ " but an unrelated path already exists. Remove or relocate it.");
		}
		Path parent = target.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		if (directory) {
			createDirectoryLink(normalizedSource, target);
		} else {
			Files.copy(normalizedSource, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static boolean pathPointsTo(Path link, Path target) throws IOException {
		if (!Files.exists(link)) {
			return false;
		}
		Path normalizedTarget = target.toAbsolutePath().normalize();
		if (Files.isSymbolicLink(link)) {
			return Files.readSymbolicLink(link).toAbsolutePath().normalize().equals(normalizedTarget);
		}
		return link.toRealPath().normalize().equals(normalizedTarget.toRealPath().normalize());
	}

	private static void createDirectoryLink(Path source, Path target) throws IOException {
		try {
			Files.createSymbolicLink(target, source);
		} catch (IOException | UnsupportedOperationException first) {
			if (!isWindows()) {
				throw new IOException("Failed to link Mock SBI directory " + source + " -> " + target, first);
			}
			createWindowsJunction(source, target);
		}
	}

	private static void createWindowsJunction(Path source, Path target) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J",
				target.toAbsolutePath().normalize().toString(),
				source.toAbsolutePath().normalize().toString());
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();
		String output;
		try (InputStream in = process.getInputStream()) {
			output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		try {
			if (process.waitFor() != 0) {
				throw new IOException("mklink /J failed for " + target + ": " + output.trim());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while creating junction for " + target, e);
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	private static void resetMockSbiPropertyCache() {
		try {
			Class<?> helperClass = Class.forName("io.mosip.mock.sbi.util.ApplicationPropertyHelper");
			Field propertiesField = helperClass.getDeclaredField("properties");
			propertiesField.setAccessible(true);
			propertiesField.set(null, null);
		} catch (ReflectiveOperationException e) {
			LOGGER.warn("Could not reset Mock SBI ApplicationPropertyHelper cache: " + e.getMessage());
		}
	}

	/** Seed bundled mds modality keystores into the certs module dir for Mock SBI signing. */
	static void seedMockSbiSigningKeys() throws IOException {
		Path mdsRoot = resolveMdsSourceDir();
		Path biometricDevices = mdsRoot.resolve("Biometric Devices");
		Path faceKey = biometricDevices.resolve("Face").resolve("Keys").resolve("mosipface.p12");
		if (!Files.isRegularFile(faceKey)) {
			throw new IllegalStateException("Missing Mock SBI device keystore in mds: " + faceKey);
		}
		String keysDir = BiometricDataProvider.getKeysDirPath("", BaseTestCase.certsForModule);
		Path keysBiometricDevices = Path.of(keysDir).resolve("Biometric Devices");
		LOGGER.info("Seeding Mock SBI signing keystore: " + biometricDevices + " -> " + keysBiometricDevices);
		Files.createDirectories(Path.of(keysDir));
		copyDirectory(biometricDevices, keysBiometricDevices);
	}

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

	private static File resolveApiTestModuleDir() {
		try {
			URL location = MosipTestRunner.class.getProtectionDomain().getCodeSource().getLocation();
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
		if (new File(cwd, "pom.xml").isFile() && new File(cwd, MDS_SRC_RELATIVE).isDirectory()) {
			return cwd;
		}
		File nested = new File(cwd, "api-test");
		if (new File(nested, "pom.xml").isFile()) {
			return nested;
		}
		return cwd;
	}

	public static void suiteSetup(String runType) {
		if (IdRepoConfigManager.IsDebugEnabled())
			LOGGER.setLevel(Level.ALL);
		else
			LOGGER.info("Test Framework for Mosip api Initialized");
		BaseTestCase.initialize();
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
	 * Windows cannot use {@code localhost:8082} as a folder name (illegal ':').
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

	static boolean shouldSkipPartnerSetup() {
		String flag = System.getProperty("idrepo.skipPartnerSetup");
		if (flag != null && !flag.isBlank()) {
			return Boolean.parseBoolean(flag);
		}
		return isLocalEndpoint();
	}

	static boolean isLocalEndpoint() {
		String endpoint = System.getProperty("env.endpoint", "");
		if (endpoint == null || endpoint.isBlank()) {
			return false;
		}
		try {
			String host = URI.create(endpoint).getHost();
			return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
		} catch (IllegalArgumentException ex) {
			return false;
		}
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
