package io.mosip.idrepository.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderHolder;
import io.mosip.idrepository.test.support.ClassLoaderTestSupport;

@RunWith(MockitoJUnitRunner.class)
public class IdRepositoryLauncherTest {

	private static final String BOOT_APPLICATION = "io.mosip.idrepository.IdRepositoryBootApplication";

	private String originalClasspath;
	private String originalMarkerPath;
	private ClassLoader originalContextClassLoader;

	@Before
	public void setUp() {
		originalClasspath = System.getProperty("java.class.path");
		originalMarkerPath = System.getProperty("idrepo.launcher.test.marker");
		originalContextClassLoader = Thread.currentThread().getContextClassLoader();
		ContextClassLoaderHolder.set(null);
	}

	@After
	public void tearDown() {
		if (originalClasspath != null) {
			System.setProperty("java.class.path", originalClasspath);
		}
		if (originalMarkerPath == null) {
			System.clearProperty("idrepo.launcher.test.marker");
		} else {
			System.setProperty("idrepo.launcher.test.marker", originalMarkerPath);
		}
		Thread.currentThread().setContextClassLoader(originalContextClassLoader);
		ContextClassLoaderHolder.set(null);
	}

	@Test
	public void privateConstructorIsAccessibleForCoverage() throws Exception {
		Constructor<IdRepositoryLauncher> constructor = IdRepositoryLauncher.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void mainInstallsFilteringClassLoaderAndDelegatesToBootApplication() throws Exception {
		Path workDir = Files.createTempDirectory("idrepo-launcher-");
		Path marker = workDir.resolve("launcher-marker.txt");
		Path bootJar = workDir.resolve("boot-stub.jar");
		Path kernelAuthJar = workDir.resolve("kernel-auth-adapter-test.jar");

		ClassLoaderTestSupport.compileClassJar(bootJar, BOOT_APPLICATION, """
				package io.mosip.idrepository;
				import java.nio.file.Files;
				import java.nio.file.Path;
				public class IdRepositoryBootApplication {
					public static void main(String[] args) throws Exception {
						Path marker = Path.of(System.getProperty("idrepo.launcher.test.marker"));
						Files.writeString(marker, "launched");
					}
				}
				""");
		ClassLoaderTestSupport.compileClassJar(kernelAuthJar, "io.mosip.kernel.auth.LauncherAuthMarker", """
				package io.mosip.kernel.auth;
				public class LauncherAuthMarker {}
				""");

		System.setProperty("idrepo.launcher.test.marker", marker.toString());
		System.setProperty("java.class.path", ClassLoaderTestSupport.joinClasspath(bootJar, kernelAuthJar));

		IdRepositoryLauncher.main(new String[0]);

		assertEquals("launched", Files.readString(marker));
		assertTrue(ContextClassLoaderHolder.get() instanceof KernelAuthSpringFactoriesFilteringClassLoader);
	}
}
