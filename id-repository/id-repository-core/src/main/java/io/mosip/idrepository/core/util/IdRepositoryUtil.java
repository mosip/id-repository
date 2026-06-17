package io.mosip.idrepository.core.util;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IdRepositoryUtil {

    public static String getTrimmedLowercaseValue(String handle) {
        return handle.trim().toLowerCase(Locale.ROOT);
    }
}