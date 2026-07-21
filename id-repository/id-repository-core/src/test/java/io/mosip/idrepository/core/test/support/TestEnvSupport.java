package io.mosip.idrepository.core.test.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.util.EnvUtil;

/**
 * Loads {@code application-test.properties} and initializes {@link EnvUtil} static fields
 * without starting a Spring Boot test context.
 */
public final class TestEnvSupport {

	private TestEnvSupport() {
	}

	public static MockEnvironment loadTestEnvironment() {
		MockEnvironment env = new MockEnvironment();
		Properties props = new Properties();
		try (InputStream in = openTestPropertiesStream()) {
			if (in == null) {
				throw new IllegalStateException("application-test.properties not found on classpath");
			}
			props.load(in);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load application-test.properties", e);
		}
		Map<String, Object> properties = new HashMap<>();
		props.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
		env.getPropertySources().addFirst(new MapPropertySource("application-test", properties));
		return env;
	}

	/** Spring 7 MockEnvironment uses {@code setProperty(String, Object)}; tests override via mutable property source. */
	public static void setProperty(MockEnvironment env, String key, String value) {
		PropertySource<?> source = env.getPropertySources().get("application-test");
		if (source instanceof MapPropertySource mapPropertySource) {
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) mapPropertySource.getSource();
			properties.put(key, value);
			return;
		}
		env.getPropertySources().addFirst(new MapPropertySource("test-override",
				new HashMap<>(Map.of(key, value))));
	}

	public static void initEnvUtil(MockEnvironment env) {
		EnvUtil envUtil = new EnvUtil();
		ReflectionTestUtils.setField(envUtil, "env", env);
		envUtil.init();
	}

	private static InputStream openTestPropertiesStream() {
		ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
		if (contextLoader != null) {
			InputStream fromContext = contextLoader.getResourceAsStream("application-test.properties");
			if (fromContext != null) {
				return fromContext;
			}
		}
		InputStream fromTestClass = TestEnvSupport.class.getClassLoader()
				.getResourceAsStream("application-test.properties");
		if (fromTestClass != null) {
			return fromTestClass;
		}
		return TestEnvSupport.class.getResourceAsStream("/application-test.properties");
	}
}
