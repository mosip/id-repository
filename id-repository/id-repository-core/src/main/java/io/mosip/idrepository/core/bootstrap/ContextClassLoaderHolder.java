package io.mosip.idrepository.core.bootstrap;

/**
 * Process-wide holder for the application {@link ClassLoader} installed before Spring Boot.
 * <p>
 * Set once by
 * {@code io.mosip.idrepository.bootstrap.KernelAuthSpringFactoriesFilteringClassLoader#install}
 * (id-repository-service) so async and scheduled workers can restore the same loader that
 * HTTP request threads use. Without this, pool threads often keep the system class loader and
 * may resolve shaded {@code org.springframework.*} types from {@code kernel-auth-adapter}.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * The stored reference is {@code volatile}. {@link #set(ClassLoader)} is expected at startup
 * (and in tests for cleanup); {@link #get()} is read frequently from worker threads.
 * </p>
 *
 * <h2>Fallback</h2>
 * <p>
 * If {@link #set(ClassLoader)} was never called (IDE direct Boot run, unit tests),
 * {@link #get()} returns {@link Thread#getContextClassLoader()} of the calling thread.
 * </p>
 *
 * @see ContextClassLoaderRunnable
 * @see java.lang.Thread#getContextClassLoader()
 */
public final class ContextClassLoaderHolder {

	/** Application filtering class loader; {@code null} until {@link #set(ClassLoader)}. */
	private static volatile ClassLoader applicationClassLoader;

	private ContextClassLoaderHolder() {
	}

	/**
	 * Publishes the application class loader for later retrieval by worker threads.
	 * <p>
	 * Pass {@code null} only in tests to clear state between cases.
	 * </p>
	 *
	 * @param classLoader filtering loader from kernel-auth classpath install, or {@code null} to clear
	 */
	public static void set(ClassLoader classLoader) {
		applicationClassLoader = classLoader;
	}

	/**
	 * Returns the installed application class loader, or the current thread's context loader.
	 *
	 * @return non-null class loader when a thread context loader exists; may be {@code null}
	 *         only if neither holder nor TCCL is set
	 */
	public static ClassLoader get() {
		ClassLoader holder = applicationClassLoader;
		if (holder != null) {
			return holder;
		}
		return Thread.currentThread().getContextClassLoader();
	}

}
