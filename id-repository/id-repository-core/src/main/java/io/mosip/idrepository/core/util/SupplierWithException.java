package io.mosip.idrepository.core.util;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;

/**
 * Functional interface for lazy suppliers that may throw checked exceptions.
 *
 * <p>
 * Java’s standard {@link java.util.function.Supplier} cannot declare checked exceptions on
 * {@code get()}. This interface allows lambdas wrapping IO- or service-layer calls to
 * propagate checked exceptions to a central handler. The static
 * {@link #execute(SupplierWithException)} helper wraps any thrown {@link Exception} in
 * {@link IdRepoAppUncheckedException} with {@link IdRepoErrorConstants#UNKNOWN_ERROR}.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * String result = SupplierWithException.execute(() -&gt; {
 *     return restHelper.requestSync(request); // may throw checked Exception
 * });
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential / identity managers that need checked-exception lambdas inside
 *       streams or retry helpers</li>
 * </ul>
 *
 * @param <R> the type of result supplied
 * @param <E> the type of checked exception that {@link #get()} may throw
 * @author Manoj SP
 * @see IdRepoAppUncheckedException
 * @see IdRepoErrorConstants#UNKNOWN_ERROR
 */
@FunctionalInterface
public interface SupplierWithException<R, E extends Exception> {

	/**
	 * Computes or retrieves a value, possibly throwing a checked exception.
	 *
	 * @return the supplied result
	 * @throws E when the underlying operation fails with a checked exception of type
	 *           {@code E}
	 */
	R get() throws E;

	/**
	 * Invokes {@code supplier.get()} and returns the result, converting any thrown
	 * {@link Exception} into {@link IdRepoAppUncheckedException}.
	 * <p>
	 * The unchecked exception uses error code {@link IdRepoErrorConstants#UNKNOWN_ERROR}
	 * and retains the original cause for logging and diagnostics.
	 * </p>
	 *
	 * @param <T>      type of value produced by the supplier
	 * @param supplier operation to execute; must not be {@code null}
	 * @return result of {@code supplier.get()} when no exception occurs
	 * @throws IdRepoAppUncheckedException wrapping the original cause when
	 *                                     {@code supplier.get()} throws any
	 *                                     {@link Exception}
	 * @see IdRepoErrorConstants#UNKNOWN_ERROR
	 */
	public static <T> T execute(SupplierWithException<T, Exception> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}
}
