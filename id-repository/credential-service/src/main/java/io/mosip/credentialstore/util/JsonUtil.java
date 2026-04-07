package io.mosip.credentialstore.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import io.mosip.credentialstore.exception.FieldNotFoundException;
import io.mosip.credentialstore.exception.InstantanceCreationException;

/**
 * Utility class for JSON operations used across the MOSIP Credential Store service.
 *
 * <p>This class provides high-performance, thread-safe utilities for:
 * <ul>
 *   <li>Converting between Java objects and JSON strings using a pre-configured singleton {@link ObjectMapper}</li>
 *   <li>Extracting values from {@code org.json.simple} JSONObject and JSONArray structures commonly used in MOSIP identity data</li>
 *   <li>Mapping demographic multi-language fields (name, address, etc.) from JSON to Java objects</li>
 * </ul>
 *
 * <p><strong>Performance Note:</strong> A single, optimized {@link ObjectMapper} instance is reused for all operations
 * to avoid the high cost of creating new mappers per request. This significantly improves credential issuance performance.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class JsonUtil {

	/** The Constant LANGUAGE. */
	private static final String LANGUAGE = "language";

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/**
	 * Singleton, thread-safe, and highly optimized {@link ObjectMapper} instance.
	 *
	 * <p>Configured with:
	 * <ul>
	 *   <li>Ignore unknown properties during deserialization</li>
	 *   <li>Do not fail on empty beans during serialization</li>
	 *   <li>Write dates as ISO strings (not timestamps)</li>
	 *   <li>Support for Java 8+ date/time types ({@code LocalDateTime}, etc.)</li>
	 * </ul>
	 *
	 * <p>AfterburnerModule can be enabled later for further performance gains if needed.
	 */
	private static final ObjectMapper OBJECT_MAPPER = createOptimizedObjectMapper();

	/**
	 * Creates and configures the shared ObjectMapper instance.
	 *
	 * @return a pre-configured ObjectMapper optimized for performance and MOSIP data structures
	 */
	private static ObjectMapper createOptimizedObjectMapper() {
		return new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.registerModule(new JavaTimeModule());
	}

	/**
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private JsonUtil() {

	}


	/**
	 * Retrieves a nested JSONObject from a parent JSONObject using the given key.
	 *
	 * <p>This is commonly used to extract the "identity" object from MOSIP identity JSON responses.
	 *
	 * <p><strong>Example:</strong>
	 * <pre>{@code
	 * JSONObject demographicIdentity = ...;
	 * JSONObject identity = JsonUtil.getJSONObject(demographicIdentity, "identity");
	 * }</pre>
	 *
	 * @param jsonObject the parent JSON object (can be null)
	 * @param key        the key whose value should be converted to JSONObject
	 * @return the JSONObject if the value exists and is a LinkedHashMap, otherwise null
	 */
	public static JSONObject getJSONObject(JSONObject jsonObject, Object key) {
		if (jsonObject == null) return null;
		Object value = jsonObject.get(key);
		return value instanceof LinkedHashMap
				? new JSONObject((LinkedHashMap<?, ?>) value)
				: null;
	}

	/**
	 * Retrieves a JSONArray from a JSONObject for the given key.
	 *
	 * <p>Used to extract array fields like "fullName", "address", "phone", etc. from MOSIP demographic data.
	 *
	 * <p><strong>Example:</strong>
	 * <pre>{@code
	 * JSONArray fullNameArray = JsonUtil.getJSONArray(identity, "fullName");
	 * }</pre>
	 *
	 * @param jsonObject the parent JSON object (can be null)
	 * @param key        the key whose value should be converted to JSONArray
	 * @return the JSONArray if the value exists and is an ArrayList, otherwise null
	 */
	public static JSONArray getJSONArray(JSONObject jsonObject, Object key) {
		if (jsonObject == null) return null;
		Object value = jsonObject.get(key);
		if (!(value instanceof ArrayList)) return null;

		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll((ArrayList<?>) value);
		return jsonArray;

	}

	/**
	 * Safely retrieves a value from a JSONObject by key with generic type casting.
	 *
	 * @param <T>        the type of the value to return
	 * @param jsonObject the JSON object (can be null)
	 * @param key        the key whose value to retrieve
	 * @return the value associated with the key, or null if jsonObject is null or key doesn't exist
	 */
	public static <T> T getJSONValue(JSONObject jsonObject, String key) {
		if (jsonObject == null) return null;
		return (T) jsonObject.get(key);
	}

	/**
	 * Returns the JSONObject at the specified index in a JSONArray.
	 *
	 * <p>Handles both {@code LinkedHashMap} (from json-simple parsing) and direct {@code JSONObject} cases.
	 *
	 * @param jsonArray the JSONArray to read from (can be null)
	 * @param index     the index of the element to retrieve
	 * @return the JSONObject at the given index, or null if index is invalid or type doesn't match
	 */
	public static JSONObject getJSONObjectFromArray(JSONArray jsonArray, int index) {
		if (jsonArray == null || index < 0 || index >= jsonArray.size()) {
			return null;
		}
		Object object = jsonArray.get(index);
		if (object instanceof LinkedHashMap) {
			return new JSONObject((LinkedHashMap<?, ?>) object);
		} else if (object instanceof JSONObject) {
			return (JSONObject) object;
		}
		return null;
	}

	/**
	 * Converts a JSON string to a Java object of the specified class using the shared ObjectMapper.
	 *
	 * <p>This is the recommended high-performance method for deserializing responses from ID Repository,
	 * policy service, or Key Manager.
	 *
	 * @param <T>        the type of the object to return
	 * @param jsonString the JSON string to deserialize
	 * @param clazz      the target class
	 * @return the deserialized object, or null if jsonString is null or blank
	 * @throws IOException if an I/O error occurs during deserialization
	 */
	@SuppressWarnings("unchecked")
	public static <T> T objectMapperReadValue(String jsonString, Class<?> clazz) throws IOException {
		if (jsonString == null || jsonString.isBlank()) return null;
		return (T) OBJECT_MAPPER.readValue(jsonString, clazz);
	}



	/**
	 * Map json node to java object.
	 *
	 * <p><strong>Usage Example:</strong>
	 * <pre>{@code
	 * FullName[] names = JsonUtil.mapJsonNodeToJavaObject(FullName.class, fullNameArray);
	 * }</pre>
	 *
	 * @param <T>                 the type of objects in the resulting array
	 * @param genericType         the class of the target object (must have "language" and "value" fields)
	 * @param demographicJsonNode the JSONArray containing language-value pairs
	 * @return an array of mapped objects, or empty array if input is null or empty
	 * @throws InstantanceCreationException if object instantiation fails
	 * @throws FieldNotFoundException       if "language" or "value" fields are not found in the class
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] mapJsonNodeToJavaObject(Class<? extends Object> genericType, JSONArray demographicJsonNode) {
		if (demographicJsonNode == null || demographicJsonNode.isEmpty()) {
			return (T[]) Array.newInstance(genericType, 0);
		}

		T[] javaObject = (T[]) Array.newInstance(genericType, demographicJsonNode.size());

		try {
			for (int i = 0; i < demographicJsonNode.size(); i++) {
				JSONObject objects = getJSONObjectFromArray(demographicJsonNode, i);
				if (objects == null) continue;

				T jsonNodeElement = (T) genericType.newInstance();

				String language = (String) objects.get(LANGUAGE);
				String value = (String) objects.get(VALUE);

				Field languageField = genericType.getDeclaredField(LANGUAGE);
				languageField.setAccessible(true);
				languageField.set(jsonNodeElement, language);

				Field valueField = genericType.getDeclaredField(VALUE);
				valueField.setAccessible(true);
				valueField.set(jsonNodeElement, value);

				javaObject[i] = jsonNodeElement;
			}
		} catch (InstantiationException | IllegalAccessException e) {
			throw new InstantanceCreationException(e);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new FieldNotFoundException(e);
		}

		return javaObject;

	}

	/**
	 * Converts a Java object to its JSON string representation using the shared ObjectMapper.
	 *
	 * <p>This is the recommended high-performance method used when preparing credential data
	 * before encryption or data share.
	 *
	 * @param obj the Java object to serialize
	 * @return JSON string representation of the object, or null if obj is null
	 * @throws IOException if serialization fails
	 */
	public static String objectMapperObjectToJson(Object obj) throws IOException {
		if (obj == null) return null;
		try {
			return OBJECT_MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new IOException("Failed to convert object to JSON", e);
		}
	}
}
