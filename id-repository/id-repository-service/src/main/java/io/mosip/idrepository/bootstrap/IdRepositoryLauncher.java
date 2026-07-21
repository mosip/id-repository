package io.mosip.idrepository.bootstrap;

import java.lang.reflect.Method;

/**
 * Production JVM entry point for the ID-Repository Spring Boot fat JAR.
 * <p>
 * Must run <em>before</em> any {@code org.springframework.*} or
 * {@link io.mosip.idrepository.IdRepositoryBootApplication} class is initialized on the
 * application class loader. The Maven {@code spring-boot-maven-plugin} declares this class as
 * {@code mainClass} so {@code java -jar} and container {@code CMD} invocations always pass through
 * the kernel-auth classpath filter.
 * </p>
 *
 * <h2>Why reflection?</h2>
 * <p>
 * {@link #main(String[])} cannot call {@code IdRepositoryBootApplication.main(args)} directly:
 * compiling that reference would load Boot classes through the launcher’s loader (system/app loader)
 * before {@link KernelAuthSpringFactoriesFilteringClassLoader} is installed. Reflective loading
 * ensures every subsequent class — including Spring Boot — resolves through the filtered loader.
 * </p>
 *
 * <h2>Steps performed</h2>
 * <ol>
 *   <li>{@link KernelAuthSpringFactoriesFilteringClassLoader#install(ClassLoader)} — split classpath, set context loader</li>
 *   <li>{@code Class.forName(..., filteringLoader)} — load boot application class</li>
 *   <li>Invoke {@code main} — Spring Boot starts; {@code DefaultResourceLoader} in boot app uses the same context loader</li>
 * </ol>
 *
 * @see KernelAuthSpringFactoriesFilteringClassLoader
 * @see io.mosip.idrepository.IdRepositoryBootApplication
 * @see io.mosip.idrepository.core.bootstrap.ContextClassLoaderHolder
 */
public final class IdRepositoryLauncher {

	private IdRepositoryLauncher() {
	}

	/**
	 * Installs the kernel-auth classpath filter and starts Spring Boot.
	 *
	 * @param args command-line arguments forwarded unchanged to
	 *             {@link io.mosip.idrepository.IdRepositoryBootApplication#main(String[])}
	 * @throws Exception if class-loader installation fails or reflective {@code main} invocation fails
	 */
	public static void main(String[] args) throws Exception {
		ClassLoader classLoader = KernelAuthSpringFactoriesFilteringClassLoader.install(null);
		Class<?> applicationClass = Class.forName("io.mosip.idrepository.IdRepositoryBootApplication", true,
				classLoader);
		Method main = applicationClass.getMethod("main", String[].class);
		main.invoke(null, (Object) args);
	}
}