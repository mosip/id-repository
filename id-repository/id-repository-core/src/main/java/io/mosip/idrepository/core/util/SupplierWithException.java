package io.mosip.idrepository.core.util;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;

/**
 * @author Manoj SP
 *
 */
@FunctionalInterface
public interface SupplierWithException<R, E extends Exception> {
	R get() throws E;
	
	public static <T> T execute(SupplierWithException<T, Exception> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}
}