package io.mosip.idrepository.credential.store.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.credential.store.exception.FieldNotFoundException;
import io.mosip.idrepository.credential.store.exception.InstantanceCreationException;

/**
 * Static JSON helpers for identity demographic documents and credential config parsing.
 * <p>
 * Operates on {@link org.json.simple.JSONObject} structures returned from identity-service
 * and config-server files. Supports multilingual {@code language}/{@code value} arrays
 * via reflection-based mapping to Java DTOs.
 * </p>
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class JsonUtil {

	private static final String LANGUAGE = "language";

	private static final String VALUE = "value";

	/**
	 * Private constructor — utility class.
	 */
	private JsonUtil() {

	}

	/**
	 * Returns a nested {@link JSONObject} for the given key from a parent object.
	 * <p>
	 * Example: {@code getJSONObject(demographicIdentity, "identity")} returns the
	 * inner identity block from a demographic envelope.
	 * </p>
	 *
	 * @param jsonObject parent JSON object; may be {@code null}
	 * @param key        label of the nested object
	 * @return nested object, or {@code null} when parent or key is absent
	 */
	public static JSONObject getJSONObject(JSONObject jsonObject, Object key) {
		if(jsonObject == null)
			return null;
		LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
		return identity != null ? new JSONObject(identity) : null;
	}

	/**
	 * Returns a {@link JSONArray} for the given key from a JSON object.
	 * <p>
	 * Example: {@code getJSONArray(identity, "fullName")} returns the multilingual
	 * name array {@code [{language, value}, ...]}.
	 * </p>
	 *
	 * @param jsonObject parent JSON object
	 * @param key        label of the array property
	 * @return JSON array copy, or {@code null} when the key is missing
	 */
	public static JSONArray getJSONArray(JSONObject jsonObject, Object key) {
		ArrayList value = (ArrayList) jsonObject.get(key);
		if (value == null)
			return null;
		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll(value);

		return jsonArray;

	}

	/**
	 * Returns a typed scalar value for the given key.
	 *
	 * @param <T>        expected value type
	 * @param jsonObject parent JSON object; may be {@code null}
	 * @param key        property name
	 * @return cast value, or {@code null} when parent or key is absent
	 */
	public static <T> T getJSONValue(JSONObject jsonObject, String key) {
		if(jsonObject == null)
			return null;
		T value = (T) jsonObject.get(key);
		return value;
	}

	/**
	 * Returns the JSONObject at the given index within a JSONArray.
	 * <p>
	 * Handles elements stored as {@link LinkedHashMap} (from simple-json parse) or {@link JSONObject}.
	 * </p>
	 *
	 * @param jsonObject source array
	 * @param key        zero-based index
	 * @return object at index, or {@code null} when the slot is empty
	 */
	public static JSONObject getJSONObjectFromArray(JSONArray jsonObject, int key) {
		Object object = jsonObject.get(key);
		if(object instanceof LinkedHashMap) {
			LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
			return identity != null ? new JSONObject(identity) : null;
		}else {
			return (JSONObject)object;
		}
	}

	/**
	 * Deserializes a JSON string into an instance of {@code clazz} using Jackson.
	 *
	 * @param <T>         target type
	 * @param jsonString  raw JSON text
	 * @param clazz       class to instantiate
	 * @return deserialized object
	 * @throws JsonParseException    when JSON syntax is invalid
	 * @throws JsonMappingException  when binding fails
	 * @throws IOException           on I/O errors
	 */
	@SuppressWarnings("unchecked")
	public static <T> T objectMapperReadValue(String jsonString, Class<?> clazz) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return (T) objectMapper.readValue(jsonString, clazz);
	}

	/**
	 * Maps a multilingual JSONArray ({@code language}/{@code value} entries) to a Java object array.
	 * <p>
	 * Each array element is instantiated via {@code genericType.newInstance()} and populated
	 * by reflection on {@code language} and {@code value} fields.
	 * </p>
	 *
	 * @param <T>                 element type with {@code language} and {@code value} fields
	 * @param genericType         class of each array element
	 * @param demographicJsonNode JSONArray of language/value objects
	 * @return populated array of length {@code demographicJsonNode.size()}
	 * @throws InstantanceCreationException when element instantiation fails
	 * @throws FieldNotFoundException       when {@code language} or {@code value} fields are missing
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] mapJsonNodeToJavaObject(Class<? extends Object> genericType, JSONArray demographicJsonNode) {
		String language;
		String value;
		T[] javaObject = (T[]) Array.newInstance(genericType, demographicJsonNode.size());
		try {
			for (int i = 0; i < demographicJsonNode.size(); i++) {

				T jsonNodeElement = (T) genericType.newInstance();

				JSONObject objects = JsonUtil.getJSONObjectFromArray(demographicJsonNode, i);
				if (objects != null) {
					language = (String) objects.get(LANGUAGE);
					value = (String) objects.get(VALUE);

					Field languageField = jsonNodeElement.getClass().getDeclaredField(LANGUAGE);
					languageField.setAccessible(true);
					languageField.set(jsonNodeElement, language);

					Field valueField = jsonNodeElement.getClass().getDeclaredField(VALUE);
					valueField.setAccessible(true);
					valueField.set(jsonNodeElement, value);

					javaObject[i] = jsonNodeElement;
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {

			throw new InstantanceCreationException(
					e);

		} catch (NoSuchFieldException | SecurityException e) {
			throw new FieldNotFoundException(e);


		}

		return javaObject;

	}

	/**
	 * Serializes a Java object to a JSON string using Jackson.
	 *
	 * @param obj object to serialize
	 * @return JSON string
	 * @throws IOException when serialization fails
	 */
	public static String objectMapperObjectToJson(Object obj) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(obj);
	}

}
