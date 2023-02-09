package io.mosip.idrepository.identity.util;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.jose4j.json.internal.json_simple.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;



/**
 * This class provides JSON utilites.
 *
 * @author Neha Farheen
 *
 * */
public class JsonUtil {

	private static ObjectMapper objectMapper;

	/**
	 * This method returns the Json Object as value from identity.json
	 * object(JSONObject). jsonObject - then identity demographic json object key
	 * - demographic json label name EX:- demographicIdentity : { "identity" : {
	 * "fullName" : [ { "language" : "eng", "value" : "Taleev Aalam" }, {
	 * "language": "ara", "value" : "Taleev Aalam" } ] }
	 *
	 * method call :- getJSONObject(demographicIdentity,identity)
	 *
	 * @param jsonObject the json object
	 * @param key        the key
	 * @return the JSON object
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject getJSONObject(JSONObject jsonObject, Object key) {
		LinkedHashMap<Object, Object> identity = null;
		if (jsonObject.get(key) instanceof LinkedHashMap) {
			identity = (LinkedHashMap<Object, Object>) jsonObject.get(key);
		}
		return identity != null ? new JSONObject(identity) : null;
	}


	/**
	 * Object mapper read value. This method maps the jsonString to particular type
	 * 
	 * @param <T>        the generic type
	 * @param jsonString the json string
	 * @param clazz      the clazz
	 * @return the t
	 * @throws JsonParseException   the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException          Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T readValue(String jsonString, Class<?> clazz) throws IOException {
		return (T) objectMapper.readValue(jsonString, clazz);
	}


}
