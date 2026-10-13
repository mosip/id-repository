package io.mosip.idrepository.core.util;

import java.util.Locale;

/**
 * String helpers shared by identity handle hashing and comparison.
 */
public final class IdRepositoryUtil {

	private IdRepositoryUtil() {
	}

	/**
	 * Trims and lowercases a handle value using {@link Locale#ROOT}.
	 *
	 * @param handle raw handle or field value; may be {@code null}
	 * @return normalized value, or {@code null} when {@code handle} is {@code null}
	 */
	public static String getTrimmedLowercaseValue(String handle) {
		return handle == null ? null : handle.trim().toLowerCase(Locale.ROOT);
	}
}
