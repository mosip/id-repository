package io.mosip.credentialstore.dto;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Verifiable {
	public static final String JSONLD_CONTEXT = "@context";
	public static final String JSONLD_KEY_ID = "id";
	public static final String JSONLD_KEY_TYPE = "type";
	public static final String JSONLD_KEY_PROOF = "proof";

	// context
	public static final String JSONLD_CONTEXT_CREDENTIALS = "https://w3id.org/credentials/v1";

	// json object
	protected final LinkedHashMap<String, Object> jsonObject = new LinkedHashMap<>();



	/**
	 * Create with base context
	 */
	public Verifiable() {
		// context
		ArrayList<String> context = new ArrayList<>();
		context.add(JSONLD_CONTEXT_CREDENTIALS);
		jsonObject.put(JSONLD_CONTEXT, context);

		ArrayList<String> type = new ArrayList<>();
		type.add(getType());

		// type
		jsonObject.put(JSONLD_KEY_TYPE, type);
	}

	/**
	 * Create with json object
	 * 
	 * @param jsonObject
	 */
	public Verifiable(Map<String, Object> jsonObject) {
		this.jsonObject.putAll(jsonObject);
	}

	public abstract String getType();

	/**
	 * Add contexts
	 * 
	 * @param contexts list of context
	 */
	@SuppressWarnings("unchecked")
	public void addContexts(Collection<String> contexts) {
		((List<String>) jsonObject.get(JSONLD_CONTEXT)).addAll(contexts);
	}

	/**
	 * Get contexts
	 * 
	 * @return list of context
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getContexts() {
		return Collections.unmodifiableCollection(((Collection<String>) jsonObject.get(JSONLD_CONTEXT)));
	}

	/**
	 * Set id
	 * 
	 * @param id of verifiable credential
	 */
	public void setId(String id) {
		jsonObject.put(JSONLD_KEY_ID, id.toString());
	}

	/**
	 * Get id
	 * 
	 * @return id of verifiable credential
	 */
	public URI getId() {
		String id = (String) jsonObject.get(JSONLD_KEY_ID);
		return id == null ? null : URI.create(id);
	}

	/**
	 * Add type list of credential<br>
	 * 
	 * @param types to add
	 */
	@SuppressWarnings("unchecked")
	public void addTypes(Collection<String> types) {
		((List<String>) jsonObject.get(JSONLD_KEY_TYPE)).addAll(types);
	}

	/**
	 * Get type list
	 * 
	 * @return type list
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getTypes() {
		return Collections.unmodifiableCollection((Collection<String>) jsonObject.get(JSONLD_KEY_TYPE));
	}



	/**
	 * Get json object
	 * 
	 * @return json object
	 */
	public LinkedHashMap<String, Object> getJsonObject() {
		return jsonObject;
	}

	/**
	 * Set proof
	 * 
	 * @param proof
	 */
	public void setProof(Object proof) {
		jsonObject.put(JSONLD_KEY_PROOF, proof);
	}

	/**
	 * Get proof
	 * 
	 * @return proof object
	 */
	public Object getProof() {
		return jsonObject.get(JSONLD_KEY_PROOF);
	}
}
