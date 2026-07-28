package io.mosip.idrepository.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderHolder;

/**
 * Classpath filter that isolates {@code kernel-auth-adapter} from the Spring Boot 4 application.
 * <p>
 * The MOSIP kernel-auth fat JAR shades Spring 5, Jackson, AspectJ, Logback, and Log4j. If those
 * types load from kernel-auth while the service uses Spring Framework 7, runtime failures such as
 * {@code NoSuchMethodError: HttpHeaders.get(Object)} occur in {@code SelfTokenRestInterceptor} and
 * {@code AuthFilter}. This loader keeps kernel-auth available only for {@code io.mosip.kernel.auth.*}
 * and forces all shaded packages to resolve from primary application JARs.
 * </p>
 *
 * <h2>Loader topology</h2>
 * <pre>
 * PlatformClassLoader (JDK)
 *   └── KernelAuthSpringFactoriesFilteringClassLoader  ← primary app jars (no kernel-auth)
 *         └── KernelAuthOnlyClassLoader                ← kernel-auth-adapter jar only
 * </pre>
 *
 * <h2>Class resolution rules</h2>
 * <table>
 *   <caption>{@link #loadClass(String, boolean)} delegation</caption>
 *   <tr><th>Package / type</th><th>Source</th></tr>
 *   <tr><td>{@code java.*}, JDK {@code javax.xml.*} (API), {@code sun.*}, …</td><td>Platform parent</td></tr>
 *   <tr><td>{@code org.springframework.*}, shaded Jackson/Logback/Log4j/AspectJ</td><td>Primary URLs only (never kernel-auth)</td></tr>
 *   <tr><td>{@code io.mosip.kernel.auth.*}</td><td>Primary shadow class if present, else kernel-auth child loader</td></tr>
 *   <tr><td>All other application types</td><td>Primary URLs, then platform parent</td></tr>
 *   <tr><td>JAXB runtime ({@code org.glassfish.jaxb.*}, {@code com.sun.xml.bind.*})</td><td>Primary (not JDK platform)</td></tr>
 * </table>
 *
 * <h2>Resource filtering</h2>
 * <ul>
 *   <li>{@code META-INF/spring.factories} and {@code META-INF/spring/*} from kernel-auth are dropped</li>
 *   <li>{@code META-INF/spring.factories} from {@code springdoc-openapi} is dropped to avoid duplicate OpenAPI auto-config</li>
 *   <li>{@code META-INF/services/*} from kernel-auth is excluded (SPI conflicts)</li>
 * </ul>
 *
 * <h2>Installation</h2>
 * <p>
 * {@link #install(ClassLoader)} parses {@code java.class.path}, locates the entry whose path contains
 * {@code kernel-auth-adapter}, builds primary URL array from all other entries, registers as parallel-capable,
 * and stores the instance in {@link ContextClassLoaderHolder} and the current thread’s context class loader.
 * </p>
 *
 * @see IdRepositoryLauncher
 * @see io.mosip.idrepository.core.bootstrap.ContextClassLoaderRunnable
 * @see io.mosip.idrepository.config.IdRepoKernelAuthHelperConfig
 */
public final class KernelAuthSpringFactoriesFilteringClassLoader extends URLClassLoader {

	private static final String SPRING_METADATA_PREFIX = "META-INF/spring/";

	private static final String SPRING_FACTORIES = "META-INF/spring.factories";

	private static final String KERNEL_AUTH_ADAPTER = "kernel-auth-adapter";

	private static final Pattern CLASSPATH_SEPARATOR = Pattern.compile(Pattern.quote(File.pathSeparator));

	/** Child loader that reads only the kernel-auth JAR; parent is this filtering loader. */
	private final ClassLoader kernelAuthLoader;

	static {
		ClassLoader.registerAsParallelCapable();
	}

	/**
	 * @param primaryUrls     classpath entries excluding kernel-auth-adapter
	 * @param kernelAuthJarUrl sole URL for the kernel-auth fat JAR
	 * @param parent          typically {@link ClassLoader#getPlatformClassLoader()}
	 */
	private KernelAuthSpringFactoriesFilteringClassLoader(URL[] primaryUrls, URL kernelAuthJarUrl, ClassLoader parent) {
		super(primaryUrls, parent);
		this.kernelAuthLoader = new KernelAuthOnlyClassLoader(kernelAuthJarUrl, this);
	}

	/**
	 * Creates or reuses the filtering class loader and publishes it as the application loader.
	 * <p>
	 * Idempotent: if {@code parent} is already a {@code KernelAuthSpringFactoriesFilteringClassLoader},
	 * updates {@link ContextClassLoaderHolder} and the thread context loader and returns the same instance.
	 * </p>
	 *
	 * @param parent unused when creating anew (platform loader is used); may be an existing filter instance
	 * @return installed filtering class loader
	 * @throws IllegalStateException if {@code java.class.path} is empty, kernel-auth JAR is missing, or I/O fails
	 */
	public static ClassLoader install(ClassLoader parent) {
		if (parent instanceof KernelAuthSpringFactoriesFilteringClassLoader filtering) {
			ContextClassLoaderHolder.set(filtering);
			Thread.currentThread().setContextClassLoader(filtering);
			return filtering;
		}
		try {
			KernelAuthSpringFactoriesFilteringClassLoader filtering = create(parent);
			ContextClassLoaderHolder.set(filtering);
			Thread.currentThread().setContextClassLoader(filtering);
			return filtering;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to install kernel-auth classpath filter", ex);
		}
	}

	/**
	 * Splits {@code java.class.path} into primary application URLs and the kernel-auth JAR URL.
	 *
	 * @param parent ignored; platform class loader is always used as parent
	 * @return new filtering loader instance
	 * @throws IOException if classpath is empty or kernel-auth entry is not found
	 */
	private static KernelAuthSpringFactoriesFilteringClassLoader create(ClassLoader parent) throws IOException {
		List<URL> primaryUrls = new ArrayList<>();
		URL kernelAuthJar = null;
		if (parent instanceof URLClassLoader urlClassLoader) {
			for (URL url : urlClassLoader.getURLs()) {
				String entry = url.toExternalForm();
				if (isKernelAuthClasspathEntry(entry)) {
					kernelAuthJar = url;
					continue;
				}
				primaryUrls.add(url);
			}
		}
		if (kernelAuthJar == null) {
			primaryUrls.clear();
			String classpath = System.getProperty("java.class.path");
			if (classpath == null || classpath.isBlank()) {
				throw new IOException("java.class.path is empty");
			}
			for (String entry : CLASSPATH_SEPARATOR.split(classpath)) {
				if (entry.isBlank()) {
					continue;
				}
				if (isKernelAuthClasspathEntry(entry)) {
					kernelAuthJar = Path.of(entry).toUri().toURL();
					continue;
				}
				primaryUrls.add(Path.of(entry).toUri().toURL());
			}
		}
		if (kernelAuthJar == null) {
			throw new IOException("kernel-auth-adapter jar not found on java.class.path or parent URLClassLoader");
		}
		return new KernelAuthSpringFactoriesFilteringClassLoader(primaryUrls.toArray(URL[]::new), kernelAuthJar,
				ClassLoader.getPlatformClassLoader());
	}

	/**
	 * Loads a class with kernel-auth / shaded-package rules (parallel-capable, synchronized per name).
	 *
	 * @param name    binary class name
	 * @param resolve whether to resolve the class
	 * @return loaded class
	 * @throws ClassNotFoundException if the class cannot be resolved on any delegate
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded != null) {
				if (resolve) {
					resolveClass(loaded);
				}
				return loaded;
			}
			Class<?> clazz = loadClassDelegate(name);
			if (resolve) {
				resolveClass(clazz);
			}
			return clazz;
		}
	}

	/**
	 * Core delegation: platform → shaded-on-primary → primary → kernel-auth → parent.
	 *
	 * @param name binary class name
	 * @return resolved class
	 * @throws ClassNotFoundException when no delegate can load the name
	 */
	private Class<?> loadClassDelegate(String name) throws ClassNotFoundException {
		if (isPlatformClass(name)) {
			return getParent().loadClass(name);
		}
		if (isKernelAuthShadedPackage(name)) {
			return findClass(name);
		}
		try {
			return findClass(name);
		}
		catch (ClassNotFoundException ex) {
			if (isKernelAuthPackage(name)) {
				return loadKernelAuthClass(name);
			}
			return getParent().loadClass(name);
		}
	}

	/**
	 * Delegates {@code io.mosip.kernel.auth.*} to the child loader.
	 *
	 * @param name auth package class name
	 * @return class from primary shadow or kernel-auth JAR
	 * @throws ClassNotFoundException if the child loader cannot resolve the name
	 */
	private Class<?> loadKernelAuthClass(String name) throws ClassNotFoundException {
		if (kernelAuthLoader instanceof KernelAuthOnlyClassLoader authLoader) {
			return authLoader.loadFromKernelAuthJar(name);
		}
		throw new ClassNotFoundException(name);
	}

	/**
	 * Loads a class from primary classpath URLs only (never from the kernel-auth JAR).
	 * <p>
	 * Used by {@link KernelAuthOnlyClassLoader} so service-module shadow classes
	 * ({@code ValidateTokenHelper}, {@code SelfTokenRestInterceptor}, …) share one {@code Class}
	 * definition with Spring {@code @Bean} registration and avoid duplicate-type ambiguity.
	 * </p>
	 *
	 * @param name binary class name
	 * @return class from primary URLs
	 * @throws ClassNotFoundException if not present on the primary classpath
	 */
	Class<?> loadPrimaryClassOnly(String name) throws ClassNotFoundException {
		Class<?> loaded = findLoadedClass(name);
		if (loaded != null) {
			return loaded;
		}
		return findClass(name);
	}

	/** @return {@code true} for {@code io.mosip.kernel.auth.*} */
	private static boolean isKernelAuthPackage(String name) {
		return name.startsWith("io.mosip.kernel.auth.");
	}

	/**
	 * Packages that kernel-auth shades and must never load from its JAR.
	 *
	 * @return {@code true} for Spring, Jackson, AspectJ, Logback, Log4j prefixes
	 */
	private static boolean isKernelAuthShadedPackage(String name) {
		return name.startsWith("org.springframework.") || name.startsWith("com.fasterxml.jackson.")
				|| name.startsWith("org.aspectj.") || name.startsWith("ch.qos.logback.")
				|| name.startsWith("org.apache.logging.log4j.");
	}

	/**
	 * JDK/platform types delegated to the parent loader.
	 * <p>
	 * JAXB/JAX-WS implementation classes are excluded — they are not in the JDK since Java 11.
	 * </p>
	 *
	 * @param name binary class name
	 * @return {@code true} if the name should load from the platform parent
	 */
	private static boolean isPlatformClass(String name) {
		if (isJaxbImplementationClass(name)) {
			return false;
		}
		return name.startsWith("java.") || name.startsWith("javax.sql.") || isJdkJavaxXmlClass(name)
				|| name.startsWith("javax.crypto.") || name.startsWith("javax.net.")
				|| name.startsWith("javax.security.") || name.startsWith("javax.naming.")
				|| name.startsWith("javax.management.")
				|| name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith("jdk.")
				|| name.startsWith("org.xml.") || name.startsWith("org.w3c.");
	}

	/** JAXB 2.x runtime (CBEFF/BIR) — not part of the JDK since Java 11; must load from app classpath. */
	private static boolean isJaxbImplementationClass(String name) {
		return name.startsWith("com.sun.xml.bind.") || name.startsWith("com.sun.istack.")
				|| name.startsWith("org.glassfish.jaxb.");
	}

	/**
	 * JDK-bundled {@code javax.xml.*} API types (excludes JAXB/JAX-WS implementation packages).
	 *
	 * @param name binary class name
	 * @return {@code true} for JDK XML API classes
	 */
	private static boolean isJdkJavaxXmlClass(String name) {
		if (!name.startsWith("javax.xml.")) {
			return false;
		}
		return !name.startsWith("javax.xml.bind.") && !name.startsWith("javax.xml.ws.");
	}

	/**
	 * Resolves a single resource from primary URLs, then kernel-auth (unless excluded), then parent.
	 *
	 * @param name resource path (for example {@code META-INF/spring.factories})
	 * @return resource URL or {@code null}
	 */
	@Override
	public URL getResource(String name) {
		if (!isFilteredMetadataName(name)) {
			URL resource = findResourceOnPrimaryOrKernelAuth(name);
			return resource != null ? resource : getParent().getResource(name);
		}
		try {
			Enumeration<URL> resources = getResources(name);
			return resources.hasMoreElements() ? resources.nextElement() : null;
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Looks up a resource on primary classpath first, then kernel-auth child loader.
	 *
	 * @param name resource path
	 * @return URL or {@code null}
	 */
	private URL findResourceOnPrimaryOrKernelAuth(String name) {
		URL resource = findResource(name);
		if (resource != null) {
			return resource;
		}
		if (isKernelAuthExcludedResourceName(name)) {
			return null;
		}
		return kernelAuthLoader.getResource(name);
	}

	/**
	 * Aggregates resources from primary and kernel-auth loaders; applies metadata filtering for Spring factories.
	 *
	 * @param name resource path
	 * @return enumeration of matching URLs (may be empty)
	 * @throws IOException if enumeration fails
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		if (!isFilteredMetadataName(name)) {
			List<URL> urls = new ArrayList<>();
			Enumeration<URL> primary = findResources(name);
			while (primary.hasMoreElements()) {
				urls.add(primary.nextElement());
			}
			if (!isKernelAuthExcludedResourceName(name)) {
				Enumeration<URL> kernelAuth = kernelAuthLoader.getResources(name);
				while (kernelAuth.hasMoreElements()) {
					URL url = kernelAuth.nextElement();
					if (!urls.contains(url)) {
						urls.add(url);
					}
				}
			}
			return Collections.enumeration(urls);
		}
		List<URL> filtered = new ArrayList<>();
		Enumeration<URL> resources = findResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			if (!shouldFilterMetadataResource(name, url)) {
				filtered.add(url);
			}
		}
		return Collections.enumeration(filtered);
	}

	/** @return {@code true} for Spring Boot auto-configuration metadata paths */
	private static boolean isFilteredMetadataName(String name) {
		return SPRING_FACTORIES.equals(name) || name.startsWith(SPRING_METADATA_PREFIX);
	}

	/** @return {@code true} for {@code META-INF/services/*} (kernel-auth SPI must not register) */
	private static boolean isKernelAuthExcludedResourceName(String name) {
		return name.startsWith("META-INF/services/");
	}

	/**
	 * Decides whether a metadata URL should be omitted from {@link #getResources(String)}.
	 * <p>
	 * Drops kernel-auth Spring metadata and duplicate springdoc {@code spring.factories}.
	 * </p>
	 *
	 * @param name resource path
	 * @param url  candidate URL
	 * @return {@code true} to exclude this URL from the result
	 */
	private static boolean shouldFilterMetadataResource(String name, URL url) {
		String location = url.toExternalForm().toLowerCase();
		if (location.contains(KERNEL_AUTH_ADAPTER)) {
			return isFilteredMetadataName(name);
		}
		return SPRING_FACTORIES.equals(name) && location.contains("springdoc-openapi");
	}

	/**
	 * Detects the kernel-auth fat JAR on the process classpath string.
	 *
	 * @param entry single {@code java.class.path} entry (file path)
	 * @return {@code true} if the path contains {@code kernel-auth-adapter}
	 */
	private static boolean isKernelAuthClasspathEntry(String entry) {
		return entry.replace('\\', '/').toLowerCase().contains(KERNEL_AUTH_ADAPTER);
	}

	/**
	 * Child {@link URLClassLoader} scoped to the kernel-auth JAR only.
	 * <p>
	 * Shaded Spring/Jackson/Logback requests delegate to the parent filtering loader so
	 * {@code AuthFilter} bytecode links against application Spring 7 types. Auth-package classes
	 * prefer primary shadows via {@link #loadFromKernelAuthJar(String)} before reading the JAR.
	 * </p>
	 */
	private static final class KernelAuthOnlyClassLoader extends URLClassLoader {

		static {
			ClassLoader.registerAsParallelCapable();
		}

		/**
		 * @param kernelAuthJarUrl URL of the kernel-auth-adapter JAR
		 * @param parent           parent filtering class loader
		 */
		private KernelAuthOnlyClassLoader(URL kernelAuthJarUrl, ClassLoader parent) {
			super(new URL[] { kernelAuthJarUrl }, parent);
		}

		/** Restricts resource lookup to this JAR (no parent delegation for resources). */
		@Override
		public URL getResource(String name) {
			return findResource(name);
		}

		/** Restricts resource enumeration to this JAR. */
		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return findResources(name);
		}

		/**
		 * Loads shaded packages from parent; auth packages from primary shadow or this JAR.
		 *
		 * @param name    binary class name
		 * @param resolve whether to resolve
		 * @return loaded class
		 * @throws ClassNotFoundException if resolution fails
		 */
		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (isKernelAuthShadedPackage(name)) {
				return getParent().loadClass(name);
			}
			if (isKernelAuthPackage(name)) {
				Class<?> clazz = loadFromKernelAuthJar(name);
				if (resolve) {
					resolveClass(clazz);
				}
				return clazz;
			}
			return getParent().loadClass(name);
		}

		/**
		 * Resolves {@code io.mosip.kernel.auth.*}: primary classpath shadow first, then kernel-auth JAR.
		 *
		 * @param name binary class name in the auth package
		 * @return loaded class
		 * @throws ClassNotFoundException if neither primary nor JAR contains the class
		 */
		private Class<?> loadFromKernelAuthJar(String name) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				Class<?> loaded = findLoadedClass(name);
				if (loaded != null) {
					return loaded;
				}
				ClassLoader parent = getParent();
				if (parent instanceof KernelAuthSpringFactoriesFilteringClassLoader filtering) {
					try {
						return filtering.loadPrimaryClassOnly(name);
					}
					catch (ClassNotFoundException ignored) {
						// fall through to kernel-auth jar
					}
				}
				Class<?> clazz = findClass(name);
				resolveClass(clazz);
				return clazz;
			}
		}

		/** @return {@code true} for {@code io.mosip.kernel.auth.*} */
		private static boolean isKernelAuthPackage(String name) {
			return name.startsWith("io.mosip.kernel.auth.");
		}

	}
}