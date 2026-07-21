package io.mosip.idrepository.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderHolder;
import io.mosip.idrepository.test.support.ClassLoaderTestSupport;

@RunWith(MockitoJUnitRunner.class)
public class KernelAuthSpringFactoriesFilteringClassLoaderTest {

	private static final String KERNEL_AUTH_CLASS = "io.mosip.kernel.auth.TestAuthMarker";
	private static final String KERNEL_AUTH_ONLY_CLASS = "io.mosip.kernel.auth.OnlyInKernelAuth";
	private static final String PRIMARY_ONLY_CLASS = "io.mosip.idrepository.bootstrap.PrimaryOnlyMarker";
	private static final String SHADED_SPRING_CLASS = "org.springframework.bootstrap.TestShadedMarker";
	private static final String JAXB_IMPL_CLASS = "org.glassfish.jaxb.runtime.TestJaxbMarker";
	private static final String JAXB_API_CLASS = "javax.xml.bind.JAXBContext";
	private static final String JACKSON_SHADED_CLASS = "com.fasterxml.jackson.databind.ObjectMapper";
	private static final String LOGBACK_SHADED_CLASS = "ch.qos.logback.classic.Logger";
	private static final String ASPECTJ_SHADED_CLASS = "org.aspectj.lang.JoinPoint";
	private static final String LOG4J_SHADED_CLASS = "org.apache.logging.log4j.Logger";
	private static final String JAXWS_CLASS = "javax.xml.ws.WebService";
	private static final String COM_SUN_ISTACK_CLASS = "com.sun.istack.TestIstackMarker";
	private static final String COM_SUN_XML_BIND_CLASS = "com.sun.xml.bind.TestBindMarker";

	private String originalClasspath;
	private ClassLoader originalContextClassLoader;

	@Before
	public void setUp() {
		originalClasspath = System.getProperty("java.class.path");
		originalContextClassLoader = Thread.currentThread().getContextClassLoader();
		ContextClassLoaderHolder.set(null);
	}

	@After
	public void tearDown() {
		if (originalClasspath != null) {
			System.setProperty("java.class.path", originalClasspath);
		}
		Thread.currentThread().setContextClassLoader(originalContextClassLoader);
		ContextClassLoaderHolder.set(null);
	}

	@Test
	public void installReturnsExistingFilteringClassLoader() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Thread.currentThread().setContextClassLoader(filtering);

		ClassLoader installed = KernelAuthSpringFactoriesFilteringClassLoader.install(filtering);

		assertSame(filtering, installed);
		assertSame(filtering, ContextClassLoaderHolder.get());
		assertSame(filtering, Thread.currentThread().getContextClassLoader());
	}

	@Test
	public void installFailsWhenClasspathIsEmpty() {
		System.setProperty("java.class.path", "   ");
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> KernelAuthSpringFactoriesFilteringClassLoader.install(null));
		assertTrue(ex.getMessage().contains("Failed to install kernel-auth classpath filter"));
	}

	@Test
	public void installFailsWhenKernelAuthJarMissing() {
		Path primaryJar = tempPath("missing-kernel-auth-primary.jar");
		assertThrows(IllegalStateException.class, () -> withClasspath(primaryJar, () -> {
			KernelAuthSpringFactoriesFilteringClassLoader.install(null);
			return null;
		}));
	}

	@Test
	public void installLoadsKernelAuthAndPrimaryClasses() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();

		assertNotNull(filtering.loadClass(KERNEL_AUTH_CLASS));
		assertNotNull(filtering.loadClass(PRIMARY_ONLY_CLASS));
		assertNotNull(filtering.loadClass("java.lang.String"));
		assertNotNull(filtering.loadClass(SHADED_SPRING_CLASS));
	}

	@Test
	public void loadClassUsesPrimaryDefinitionBeforeKernelAuthJar() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Class<?> loaded = filtering.loadClass(KERNEL_AUTH_CLASS, true);
		assertEquals("primary", ReflectionTestUtils.invokeMethod(loaded.getDeclaredConstructor().newInstance(), "origin"));
	}

	@Test
	public void loadClassFallsBackToParentForUnknownClass() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass("java.util.ArrayList"));
	}

	@Test
	public void loadClassLoadsJaxbImplementationFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(JAXB_IMPL_CLASS));
	}

	@Test
	public void getResourceReturnsPrimaryResource() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		URL resource = filtering.getResource("primary-marker.txt");
		assertNotNull(resource);
		assertTrue(resource.toExternalForm().contains("primary-only.jar"));
	}

	@Test
	public void getResourceSkipsKernelAuthServicesMetadata() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNull(filtering.getResource("META-INF/services/java.sql.Driver"));
	}

	@Test
	public void getResourceFiltersSpringFactoriesFromKernelAuthJar() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Enumeration<URL> resources = filtering.getResources("META-INF/spring.factories");
		List<URL> urls = Collections.list(resources);
		assertEquals(1, urls.size());
		assertTrue(urls.get(0).toExternalForm().contains("primary-only.jar"));
	}

	@Test
	public void getResourcesFiltersSpringdocOpenApiFactories() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Enumeration<URL> resources = filtering.getResources("META-INF/spring.factories");
		List<URL> urls = Collections.list(resources);
		assertEquals(1, urls.size());
		assertTrue(urls.get(0).toExternalForm().contains("primary-only.jar"));
	}

	@Test
	public void loadPrimaryClassOnlyLoadsFromPrimaryUrls() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadPrimaryClassOnly(PRIMARY_ONLY_CLASS));
	}

	@Test
	public void installSkipsBlankClasspathEntries() throws Exception {
		Path workDir = Files.createTempDirectory("kernel-auth-filter-");
		Path primaryJar = workDir.resolve("primary-only.jar");
		Path kernelAuthJar = workDir.resolve("kernel-auth-adapter-test.jar");
		createMinimalClasspathJars(primaryJar, kernelAuthJar);
		String classpath = primaryJar.toAbsolutePath() + ";;" + kernelAuthJar.toAbsolutePath();
		System.setProperty("java.class.path", classpath);
		assertNotNull(KernelAuthSpringFactoriesFilteringClassLoader.install(null));
	}

	@Test
	public void installAcceptsKernelAuthPathWithBackslashes() throws Exception {
		Path workDir = Files.createTempDirectory("kernel-auth-filter-");
		Path primaryJar = workDir.resolve("primary-only.jar");
		Path kernelAuthJar = workDir.resolve("kernel-auth-adapter-test.jar");
		createMinimalClasspathJars(primaryJar, kernelAuthJar);
		String kernelAuthPath = kernelAuthJar.toAbsolutePath().toString().replace('/', '\\');
		System.setProperty("java.class.path", ClassLoaderTestSupport.joinClasspath(primaryJar)
				+ java.io.File.pathSeparator + kernelAuthPath);
		assertNotNull(KernelAuthSpringFactoriesFilteringClassLoader.install(null));
	}

	@Test
	public void loadClassReturnsCachedClassWithoutReloading() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Class<?> first = filtering.loadClass(PRIMARY_ONLY_CLASS);
		Class<?> second = filtering.loadClass(PRIMARY_ONLY_CLASS, false);
		assertSame(first, second);
	}

	@Test
	public void loadClassLoadsKernelAuthOnlyClassFromKernelAuthJar() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(KERNEL_AUTH_ONLY_CLASS));
	}

	@Test
	public void loadClassLoadsJaxbApiFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(JAXB_API_CLASS));
	}

	@Test
	public void loadClassLoadsJacksonShadedPackageFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(JACKSON_SHADED_CLASS));
	}

	@Test
	public void loadClassLoadsLogbackShadedPackageFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(LOGBACK_SHADED_CLASS));
	}

	@Test
	public void getResourceUsesFilteredMetadataLookup() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		URL resource = filtering.getResource("META-INF/spring.factories");
		assertNotNull(resource);
		assertTrue(resource.toExternalForm().contains("primary-only.jar"));
	}

	@Test
	public void getResourceUsesFilteredSpringMetadataPrefix() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		URL resource = filtering.getResource(
				"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
		assertNotNull(resource);
	}

	@Test
	public void getResourceFallsBackToParentWhenMissing() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.getResource("java/lang/Object.class"));
	}

	@Test
	public void getResourcesMergesKernelAuthResources() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Enumeration<URL> resources = filtering.getResources("kernel-auth-resource.txt");
		List<URL> urls = Collections.list(resources);
		assertEquals(1, urls.size());
		assertTrue(urls.get(0).toExternalForm().contains("kernel-auth-adapter-test.jar"));
	}

	@Test
	public void kernelAuthOnlyClassLoaderDelegatesShadedPackagesToParent() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Object kernelAuthLoader = ReflectionTestUtils.getField(filtering, "kernelAuthLoader");
		Method loadClass = kernelAuthLoader.getClass().getDeclaredMethod("loadClass", String.class, boolean.class);
		loadClass.setAccessible(true);
		assertNotNull(loadClass.invoke(kernelAuthLoader, SHADED_SPRING_CLASS, true));
	}

	@Test
	public void kernelAuthOnlyClassLoaderDelegatesUnknownPackagesToParent() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Object kernelAuthLoader = ReflectionTestUtils.getField(filtering, "kernelAuthLoader");
		Method loadClass = kernelAuthLoader.getClass().getDeclaredMethod("loadClass", String.class, boolean.class);
		loadClass.setAccessible(true);
		assertNotNull(loadClass.invoke(kernelAuthLoader, PRIMARY_ONLY_CLASS, true));
	}

	@Test
	public void kernelAuthOnlyClassLoaderLoadsKernelAuthClassFromJar() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Object kernelAuthLoader = ReflectionTestUtils.getField(filtering, "kernelAuthLoader");
		Method loadClass = kernelAuthLoader.getClass().getDeclaredMethod("loadClass", String.class, boolean.class);
		loadClass.setAccessible(true);
		assertNotNull(loadClass.invoke(kernelAuthLoader, KERNEL_AUTH_ONLY_CLASS, true));
	}

	@Test
	public void kernelAuthOnlyClassLoaderReturnsCachedKernelAuthClass() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Object kernelAuthLoader = ReflectionTestUtils.getField(filtering, "kernelAuthLoader");
		Method loadClass = kernelAuthLoader.getClass().getDeclaredMethod("loadClass", String.class, boolean.class);
		loadClass.setAccessible(true);
		Class<?> first = (Class<?>) loadClass.invoke(kernelAuthLoader, KERNEL_AUTH_ONLY_CLASS, true);
		Class<?> second = (Class<?>) loadClass.invoke(kernelAuthLoader, KERNEL_AUTH_ONLY_CLASS, false);
		assertSame(first, second);
	}

	@Test
	public void loadClassLoadsAdditionalShadedPackagesFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(ASPECTJ_SHADED_CLASS));
		assertNotNull(filtering.loadClass(LOG4J_SHADED_CLASS));
	}

	@Test
	public void loadClassLoadsJaxWsFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(JAXWS_CLASS));
	}

	@Test
	public void kernelAuthOnlyClassLoaderExposesResourceLookupMethods() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Object kernelAuthLoader = ReflectionTestUtils.getField(filtering, "kernelAuthLoader");
		assertNotNull(kernelAuthLoader.getClass().getMethod("getResource", String.class)
				.invoke(kernelAuthLoader, "kernel-auth-resource.txt"));
		@SuppressWarnings("unchecked")
		Enumeration<URL> resources = (Enumeration<URL>) kernelAuthLoader.getClass()
				.getMethod("getResources", String.class).invoke(kernelAuthLoader, "kernel-auth-resource.txt");
		assertTrue(resources.hasMoreElements());
	}

	@Test
	public void loadClassLoadsJaxbImplementationPackagesFromPrimaryClasspath() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass(COM_SUN_ISTACK_CLASS));
		assertNotNull(filtering.loadClass(COM_SUN_XML_BIND_CLASS));
	}

	@Test
	public void loadClassLoadsPlatformJavaxXmlParsersFromParent() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass("javax.xml.parsers.DocumentBuilderFactory"));
	}

	@Test
	public void getResourceReturnsNullForMissingFilteredMetadata() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNull(filtering.getResource("META-INF/spring/does-not-exist.properties"));
	}

	@Test
	public void loadPrimaryClassOnlyReturnsCachedClass() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		Class<?> first = filtering.loadPrimaryClassOnly(PRIMARY_ONLY_CLASS);
		Class<?> second = filtering.loadPrimaryClassOnly(PRIMARY_ONLY_CLASS);
		assertSame(first, second);
	}

	@Test
	public void privateConstructorIsAccessibleForCoverage() throws Exception {
		Constructor<KernelAuthSpringFactoriesFilteringClassLoader> constructor = KernelAuthSpringFactoriesFilteringClassLoader.class
				.getDeclaredConstructor(URL[].class, URL.class, ClassLoader.class);
		constructor.setAccessible(true);
		Path workDir = Files.createTempDirectory("kernel-auth-private-");
		Path primaryJar = workDir.resolve("primary-only.jar");
		Path kernelAuthJar = workDir.resolve("kernel-auth-adapter-test.jar");
		createMinimalClasspathJars(primaryJar, kernelAuthJar);
		constructor.newInstance(new URL[] { primaryJar.toUri().toURL() }, kernelAuthJar.toUri().toURL(),
				ClassLoader.getPlatformClassLoader());
	}

	@Test
	public void loadClassLoadsCommonPlatformPackagesFromParent() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		assertNotNull(filtering.loadClass("javax.crypto.Cipher"));
		assertNotNull(filtering.loadClass("javax.net.ssl.SSLContext"));
		assertNotNull(filtering.loadClass("javax.security.auth.Subject"));
		assertNotNull(filtering.loadClass("org.w3c.dom.Node"));
	}

	@Test
	public void kernelAuthOnlyClassLoaderLoadsDirectlyFromJarWhenParentIsNotFilteringLoader() throws Exception {
		Path workDir = Files.createTempDirectory("kernel-auth-only-");
		Path kernelAuthJar = workDir.resolve("kernel-auth-adapter-test.jar");
		ClassLoaderTestSupport.compileClassJar(kernelAuthJar, KERNEL_AUTH_ONLY_CLASS, """
				package io.mosip.kernel.auth;
				public class OnlyInKernelAuth {}
				""");

		Class<?> innerClass = Class.forName(
				"io.mosip.idrepository.bootstrap.KernelAuthSpringFactoriesFilteringClassLoader$KernelAuthOnlyClassLoader");
		Constructor<?> constructor = innerClass.getDeclaredConstructor(URL.class, ClassLoader.class);
		constructor.setAccessible(true);
		Object standaloneLoader = constructor.newInstance(kernelAuthJar.toUri().toURL(),
				new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader()));
		Method loadFromKernelAuthJar = innerClass.getDeclaredMethod("loadFromKernelAuthJar", String.class);
		loadFromKernelAuthJar.setAccessible(true);
		assertNotNull(loadFromKernelAuthJar.invoke(standaloneLoader, KERNEL_AUTH_ONLY_CLASS));
	}

	@Test
	public void loadKernelAuthClassThrowsWhenDelegateIsNotKernelAuthOnlyClassLoader() throws Exception {
		KernelAuthSpringFactoriesFilteringClassLoader filtering = createFilteringClassLoader();
		URLClassLoader foreignLoader = new URLClassLoader(new URL[0], filtering);
		ReflectionTestUtils.setField(filtering, "kernelAuthLoader", foreignLoader);
		assertThrows(ClassNotFoundException.class, () -> filtering.loadClass(KERNEL_AUTH_ONLY_CLASS));
	}

	private void createMinimalClasspathJars(Path primaryJar, Path kernelAuthJar) throws Exception {
		ClassLoaderTestSupport.compileClassJar(primaryJar, KERNEL_AUTH_CLASS, """
				package io.mosip.kernel.auth;
				public class TestAuthMarker {}
				""");
		ClassLoaderTestSupport.compileClassJar(kernelAuthJar, KERNEL_AUTH_CLASS, """
				package io.mosip.kernel.auth;
				public class TestAuthMarker {}
				""");
	}

	private KernelAuthSpringFactoriesFilteringClassLoader createFilteringClassLoader() throws Exception {
		Path workDir = Files.createTempDirectory("kernel-auth-filter-");
		Path primaryJar = workDir.resolve("primary-only.jar");
		Path kernelAuthJar = workDir.resolve("kernel-auth-adapter-test.jar");
		Path springdocJar = workDir.resolve("springdoc-openapi-stub.jar");

		ClassLoaderTestSupport.compileClassJar(primaryJar, primarySources(), Map.of(
				"primary-marker.txt", "primary".getBytes(StandardCharsets.UTF_8),
				"META-INF/spring.factories", "primary.Factory=io.mosip.idrepository.bootstrap.PrimaryOnlyMarker"
						.getBytes(StandardCharsets.UTF_8),
				"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
				"primary.Config".getBytes(StandardCharsets.UTF_8)));
		ClassLoaderTestSupport.compileClassJar(kernelAuthJar, Map.of(
				KERNEL_AUTH_CLASS, """
						package io.mosip.kernel.auth;
						public class TestAuthMarker {
							public String origin() { return "kernel-auth"; }
						}
						""",
				KERNEL_AUTH_ONLY_CLASS, """
						package io.mosip.kernel.auth;
						public class OnlyInKernelAuth {}
						"""), Map.of(
				"META-INF/services/java.sql.Driver", "ignored".getBytes(StandardCharsets.UTF_8),
				"META-INF/spring.factories", "kernel.auth.Factory=io.mosip.kernel.auth.TestAuthMarker"
						.getBytes(StandardCharsets.UTF_8),
				"kernel-auth-resource.txt", "kernel".getBytes(StandardCharsets.UTF_8)));
		ClassLoaderTestSupport.createJar(springdocJar, Map.of(
				"META-INF/spring.factories", "springdoc.Factory=org.springdoc.OpenApi".getBytes(StandardCharsets.UTF_8)));

		return withClasspath(primaryJar, kernelAuthJar, springdocJar,
				() -> (KernelAuthSpringFactoriesFilteringClassLoader) KernelAuthSpringFactoriesFilteringClassLoader
						.install(null));
	}

	private <T> T withClasspath(Path primaryJar, Path kernelAuthJar, Path extraJar, ClasspathAction<T> action)
			throws Exception {
		System.setProperty("java.class.path", ClassLoaderTestSupport.joinClasspath(primaryJar, kernelAuthJar, extraJar));
		return action.run();
	}

	private <T> T withClasspath(Path primaryJar, ClasspathAction<T> action) throws Exception {
		System.setProperty("java.class.path", ClassLoaderTestSupport.joinClasspath(primaryJar));
		return action.run();
	}

	private static Path tempPath(String fileName) {
		try {
			return Files.createTempDirectory("kernel-auth-filter-").resolve(fileName);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static Map<String, String> primarySources() {
		Map<String, String> sources = new java.util.LinkedHashMap<>();
		sources.put(KERNEL_AUTH_CLASS, """
				package io.mosip.kernel.auth;
				public class TestAuthMarker {
					public String origin() { return "primary"; }
				}
				""");
		sources.put(PRIMARY_ONLY_CLASS, """
				package io.mosip.idrepository.bootstrap;
				public class PrimaryOnlyMarker {}
				""");
		sources.put(SHADED_SPRING_CLASS, """
				package org.springframework.bootstrap;
				public class TestShadedMarker {}
				""");
		sources.put(JAXB_IMPL_CLASS, """
				package org.glassfish.jaxb.runtime;
				public class TestJaxbMarker {}
				""");
		sources.put(JAXB_API_CLASS, """
				package javax.xml.bind;
				public class JAXBContext {}
				""");
		sources.put(JACKSON_SHADED_CLASS, """
				package com.fasterxml.jackson.databind;
				public class ObjectMapper {}
				""");
		sources.put(LOGBACK_SHADED_CLASS, """
				package ch.qos.logback.classic;
				public class Logger {}
				""");
		sources.put(ASPECTJ_SHADED_CLASS, """
				package org.aspectj.lang;
				public class JoinPoint {}
				""");
		sources.put(LOG4J_SHADED_CLASS, """
				package org.apache.logging.log4j;
				public class Logger {}
				""");
		sources.put(JAXWS_CLASS, """
				package javax.xml.ws;
				public class WebService {}
				""");
		sources.put(COM_SUN_ISTACK_CLASS, """
				package com.sun.istack;
				public class TestIstackMarker {}
				""");
		sources.put(COM_SUN_XML_BIND_CLASS, """
				package com.sun.xml.bind;
				public class TestBindMarker {}
				""");
		return sources;
	}

	@FunctionalInterface
	private interface ClasspathAction<T> {
		T run() throws Exception;
	}
}
