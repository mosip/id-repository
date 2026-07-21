package io.mosip.idrepository.credential.store.dto;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract JSON-LD verifiable document base for W3C Verifiable Credentials.
 * <p>
 * Manages {@code @context}, {@code type}, {@code id}, and {@code proof} fields shared by
 * {@link VerifiableCredential}. Used by {@link io.mosip.idrepository.credential.store.provider.impl.VerCredProvider}.
 * </p>
 */
public abstract class Verifiable {

	/** JSON-LD {@code @context} key. */
	public static final String JSONLD_CONTEXT = "@context";

	/** JSON-LD credential id key. */
	public static final String JSONLD_KEY_ID = "id";

	/** JSON-LD type key. */
	public static final String JSONLD_KEY_TYPE = "type";

	/** JSON-LD proof key. */
	public static final String JSONLD_KEY_PROOF = "proof";

	/** Default W3C credentials vocabulary context URL. */
	public static final String JSONLD_CONTEXT_CREDENTIALS = "https://w3id.org/credentials/v1";

	/** Backing ordered map serialized to the final VC JSON. */
	protected final LinkedHashMap<String, Object> jsonObject = new LinkedHashMap<>();

	/**
	 * Creates a verifiable document with default W3C context and concrete {@link #getType()}.
	 */
	public Verifiable() {
		ArrayList<String> context = new ArrayList<>();
		context.add(JSONLD_CONTEXT_CREDENTIALS);
		jsonObject.put(JSONLD_CONTEXT, context);

		ArrayList<String> type = new ArrayList<>();
		type.add(getType());
		jsonObject.put(JSONLD_KEY_TYPE, type);
	}

	/**
	 * Wraps an existing JSON-LD map (e.g. when rehydrating a signed credential).
	 *
	 * @param jsonObject source JSON-LD object
	 */
	public Verifiable(Map<String, Object> jsonObject) {
		this.jsonObject.putAll(jsonObject);
	}

	/**
	 * Returns the primary JSON-LD type string for this verifiable document.
	 *
	 * @return type name (e.g. {@link VerifiableCredential#JSONLD_TYPE_CREDENTIAL})
	 */
	public abstract String getType();

	/**
	 * Appends additional JSON-LD context URLs.
	 *
	 * @param contexts extra context strings
	 */
	@SuppressWarnings("unchecked")
	public void addContexts(Collection<String> contexts) {
		((List<String>) jsonObject.get(JSONLD_CONTEXT)).addAll(contexts);
	}

	/**
	 * Returns an unmodifiable view of JSON-LD contexts.
	 *
	 * @return context URL collection
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getContexts() {
		return Collections.unmodifiableCollection(((Collection<String>) jsonObject.get(JSONLD_CONTEXT)));
	}

	/**
	 * Sets the verifiable document id (typically a URI).
	 *
	 * @param id document identifier
	 */
	public void setId(String id) {
		jsonObject.put(JSONLD_KEY_ID, id.toString());
	}

	/**
	 * Returns the document id as a URI.
	 *
	 * @return document URI or {@code null}
	 */
	public URI getId() {
		String id = (String) jsonObject.get(JSONLD_KEY_ID);
		return id == null ? null : URI.create(id);
	}

	/**
	 * Appends additional JSON-LD type strings.
	 *
	 * @param types extra type values
	 */
	@SuppressWarnings("unchecked")
	public void addTypes(Collection<String> types) {
		((List<String>) jsonObject.get(JSONLD_KEY_TYPE)).addAll(types);
	}

	/**
	 * Returns an unmodifiable view of JSON-LD types.
	 *
	 * @return type collection
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getTypes() {
		return Collections.unmodifiableCollection((Collection<String>) jsonObject.get(JSONLD_KEY_TYPE));
	}

	/**
	 * Returns the mutable JSON-LD object backing this verifiable document.
	 *
	 * @return ordered JSON-LD map
	 */
	public LinkedHashMap<String, Object> getJsonObject() {
		return jsonObject;
	}

	/**
	 * Attaches a signed proof object to the document.
	 *
	 * @param proof proof map or {@link Proof} serialized form
	 */
	public void setProof(Object proof) {
		jsonObject.put(JSONLD_KEY_PROOF, proof);
	}

	/**
	 * Returns the attached proof object.
	 *
	 * @return proof JSON object
	 */
	public Object getProof() {
		return jsonObject.get(JSONLD_KEY_PROOF);
	}
}
