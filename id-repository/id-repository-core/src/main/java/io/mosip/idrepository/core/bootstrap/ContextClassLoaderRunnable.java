package io.mosip.idrepository.core.bootstrap;

import java.util.concurrent.ThreadFactory;

/**
 * Ensures runnables and pool threads execute with the application {@link ClassLoader}
 * from {@link ContextClassLoaderHolder} as their thread context class loader (TCCL).
 * <p>
 * Spring {@code ThreadPoolTaskExecutor} / {@code ThreadPoolTaskScheduler} thread factories
 * should use {@link #delegatingThreadFactory(ThreadFactory)} so WebSub, {@code @Async}, and
 * deferred credential subscription work resolve the same types as the main HTTP threads
 * (required when the kernel-auth filtering class loader is installed).
 * </p>
 *
 * <h2>API</h2>
 * <ul>
 *   <li>{@link #wrap(Runnable)} — one-shot wrapper that sets TCCL for the duration of {@code run()}</li>
 *   <li>{@link #threadFactory()} — default factory naming threads {@code idrepo-async-*}</li>
 *   <li>{@link #delegatingThreadFactory(ThreadFactory)} — wraps an existing factory (preferred for Spring pools)</li>
 * </ul>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code IdRepoConfig} — identity async / WebSub executors</li>
 *   <li>{@code IdRepoTaskSchedulerConfig} — credential WebSub {@code ThreadPoolTaskScheduler}</li>
 * </ul>
 *
 * @see ContextClassLoaderHolder
 */
public final class ContextClassLoaderRunnable {

	private ContextClassLoaderRunnable() {
	}

	/**
	 * Returns a runnable that sets the thread context class loader to
	 * {@link ContextClassLoaderHolder#get()} for the duration of {@code task.run()},
	 * then restores the previous TCCL.
	 *
	 * @param task original work to execute
	 * @return wrapped runnable; never {@code null}
	 */
	public static Runnable wrap(Runnable task) {
		ClassLoader applicationClassLoader = ContextClassLoaderHolder.get();
		return () -> runWithClassLoader(task, applicationClassLoader);
	}

	/**
	 * Thread factory that creates threads named {@code idrepo-async-{threadId}} and applies
	 * {@link #wrap(Runnable)} plus TCCL assignment via {@link #delegatingThreadFactory(ThreadFactory)}.
	 *
	 * @return thread factory suitable for ad-hoc async pools
	 */
	public static ThreadFactory threadFactory() {
		return delegatingThreadFactory(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName("idrepo-async-" + thread.threadId());
			return thread;
		});
	}

	/**
	 * Decorates {@code delegate} so every new thread:
	 * <ol>
	 *   <li>runs a {@link #wrap(Runnable)} of the submitted task</li>
	 *   <li>has {@link Thread#setContextClassLoader(ClassLoader)} set to
	 *       {@link ContextClassLoaderHolder#get()}</li>
	 * </ol>
	 * <p>
	 * The class loader is captured when this factory is created (typically at Spring bean
	 * init), so it remains stable for the life of the pool.
	 * </p>
	 *
	 * @param delegate underlying factory that allocates and names threads
	 * @return decorating thread factory
	 */
	public static ThreadFactory delegatingThreadFactory(ThreadFactory delegate) {
		ClassLoader applicationClassLoader = ContextClassLoaderHolder.get();
		return runnable -> {
			Thread thread = delegate.newThread(wrap(runnable));
			thread.setContextClassLoader(applicationClassLoader);
			return thread;
		};
	}

	/**
	 * Sets TCCL to {@code applicationClassLoader}, runs {@code task}, then restores the prior TCCL.
	 *
	 * @param task                    work to run
	 * @param applicationClassLoader  loader from {@link ContextClassLoaderHolder}
	 */
	private static void runWithClassLoader(Runnable task, ClassLoader applicationClassLoader) {
		Thread current = Thread.currentThread();
		ClassLoader previous = current.getContextClassLoader();
		current.setContextClassLoader(applicationClassLoader);
		try {
			task.run();
		}
		finally {
			current.setContextClassLoader(previous);
		}
	}

}
