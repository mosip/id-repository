package io.mosip.testrig.apirig.idrepo.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.AdminTestUtil;

/**
 * Request-body mutator for the identity <em>handle</em> scenarios ({@code selectedHandles} plus
 * handle fields of type string or array of {@code {value, tags}}).
 * <p>
 * The AddIdentity / UpdateIdentity scripts render a schema-driven HBS template that produces a
 * valid identity. For handle test cases the YAML only names the scenario; this class reads the
 * scenario from a lower/camel-case token inside the test case name (for example
 * {@code _withouttags}, {@code _withusedhandlevalue}) and rewrites the rendered JSON so the
 * request exercises that rule on the server.
 * <p>
 * Rules shared by every scenario:
 * <ul>
 * <li>Field names are resolved from the live IdSchema and the auth actuator (email / phone
 * property names), never hard-coded, so the same YAML works across country schemas.</li>
 * <li>Name tokens are matched with {@link String#contains(CharSequence)}; a longer token that
 * contains a shorter one is always checked first.</li>
 * <li>Global scenarios (that rewrite {@code selectedHandles} or the whole identity) return
 * immediately; per-handle scenarios run once for every array-typed handle in
 * {@code selectedHandles}.</li>
 * <li>{@link #savedHandleValues} lets a "_save_" AddIdentity case record its handle values so a
 * later case can replay them and expect the duplicate-handle error {@code IDR-IDC-014}.</li>
 * </ul>
 * All state is static because TestNG drives the scripts sequentially within one JVM run.
 */
public class IdRepoArrayHandle {
	/** Handle values captured from a "_save_" test's identity, replayed to trigger IDR-IDC-014. */
	private static final Map<String, String> savedHandleValues = new LinkedHashMap<>();
	/**
	 * Suffix fixed once per run and appended to values and tags in update scenarios, so a value
	 * written by one step can be predicted by a later step of the same run.
	 */
	public static String RANDOM_ID = nextRandomId();

	/** Fresh id per call so array-handle values/tags do not collide across tests in the same run. */
	public static String nextRandomId() {
		return "mosip" + BaseTestCase.generateRandomNumberString(2)
				+ (System.nanoTime() % 1_000_000_000L);
	}

	/** Value that matches the live IdSchema validator for this handle field. */
	private static String schemaHandleValue(String handle) {
		String generated = IdRepoUtil.generateSchemaFieldValue(handle);
		if (generated == null || generated.isBlank()) {
			return nextRandomId();
		}
		return generated;
	}

	/**
	 * Replaces array-handle {@code value} fields with a unique schema-legal id.
	 * String handles (email/phone) are left to {@code $EMAILVALUE$} / {@code $PHONENUMBERFORIDENTITY$}.
	 */
	private static void uniquifyArrayHandleValues(JSONObject identity) {
		if (identity == null || !identity.has("selectedHandles")
				|| !(identity.get("selectedHandles") instanceof JSONArray)) {
			return;
		}
		JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
		for (int i = 0; i < selectedHandles.length(); i++) {
			String handle = selectedHandles.optString(i, null);
			if (handle == null || !identity.has(handle) || !(identity.get(handle) instanceof JSONArray)) {
				continue;
			}
			JSONArray handleArray = identity.getJSONArray(handle);
			for (int j = 0; j < handleArray.length(); j++) {
				if (!(handleArray.get(j) instanceof JSONObject)) {
					continue;
				}
				JSONObject entry = handleArray.getJSONObject(j);
				if (entry.has("value") && entry.get("value") instanceof String) {
					entry.put("value", schemaHandleValue(handle));
				}
			}
		}
	}

	// ===== Public Entry Points =====

	/**
	 * Applies the AddIdentity handle scenario named in {@code testCaseName} to a rendered request.
	 * <p>
	 * Array-handle values are first made unique for this run. Then the first matching global
	 * scenario is applied and the method returns, for example: no / empty / null / wrongly typed
	 * {@code selectedHandles}; a non-handle or unknown field listed as a handle; case or
	 * whitespace mismatch; duplicate entries; a schema-derived missing required handle or an
	 * invalid handle value; the save/replay duplicate-value cases. If no global scenario matches,
	 * {@link #applyAddIdentityHandleMutation} runs for each array-typed handle in
	 * {@code selectedHandles}.
	 *
	 * @param inputJson    rendered AddIdentity request ({@code request.identity} is required)
	 * @param testCaseName YAML test case name that carries the scenario token
	 * @return the mutated request JSON
	 * @throws org.json.JSONException if the body has no {@code request.identity} object
	 */
	public static String replaceArrayHandleValues(String inputJson, String testCaseName) {
		JSONObject jsonObj = new JSONObject(inputJson);
		JSONObject request = jsonObj.getJSONObject("request");
		JSONObject identity = request.getJSONObject("identity");
		uniquifyArrayHandleValues(identity);
		String emailResult = resolveEmailFieldName();

		// Handle cases that modify selectedHandles globally — resolved before per-handle loop.
		// More-specific patterns checked before substrings they contain.
		if (testCaseName.contains("_withoutselectedhandlesinidentity")) {
			identity.remove("selectedHandles");
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withoutselectedhandles")) {
			identity.remove("selectedHandles");
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withemptyselecthandles")) {
			identity.put("selectedHandles", new JSONArray());
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withinvalidselectedhandletype")) {
			identity.put("selectedHandles", "email");
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withnullselectedhandles")) {
			identity.put("selectedHandles", JSONObject.NULL);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withNotApplicableSelectedHandles")) {
			JSONArray selectedHandles = new JSONArray();
			selectedHandles.put(resolveNonHandleSchemaField());
			identity.put("selectedHandles", selectedHandles);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withUnknownSelectedHandles")) {
			JSONArray selectedHandles = new JSONArray();
			selectedHandles.put(resolveFieldNotInSchema());
			identity.put("selectedHandles", selectedHandles);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withTaggedListTypeNotInSchema")) {
			String nonHandleField = "fullName";
			if (identity.has(nonHandleField)) {
				JSONObject entry = new JSONObject();
				entry.put("value", "InvalidTaggedValue_" + RANDOM_ID);
				entry.put("tags", new JSONArray().put("handle"));
				identity.put(nonHandleField, new JSONArray().put(entry));
			}
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withArrayOfStringNotInSchema")) {
			String nonSimpleTypeField = "postalCode";
			if (identity.has(nonSimpleTypeField)) {
				JSONArray stringArray = new JSONArray();
				stringArray.put("140221");
				stringArray.put("140222");
				identity.put(nonSimpleTypeField, stringArray);
			}
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withSelectedHandlesCaseMismatch")) {
			JSONArray selectedHandles = new JSONArray();
			selectedHandles.put(resolveEmailFieldName().toUpperCase());
			identity.put("selectedHandles", selectedHandles);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withSelectedHandlesSpace")) {
			JSONArray selectedHandles = new JSONArray();
			selectedHandles.put(" " + resolveEmailFieldName() + " ");
			identity.put("selectedHandles", selectedHandles);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withDuplicateSelectedHandles")) {
			JSONArray selectedHandles = new JSONArray();
			selectedHandles.put(resolveEmailFieldName());
			selectedHandles.put(resolveEmailFieldName());
			identity.put("selectedHandles", selectedHandles);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_removealltagshandles")) {
			removeTagsHandles(jsonObj);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_replaceselectedhandles")) {
			identity.put("selectedHandles", new JSONArray().put(emailResult));
			return jsonObj.toString();
		}
		if (testCaseName.contains("_onlywithemail")) {
			identity.put("selectedHandles", new JSONArray().put(emailResult));
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withselectedhandlephone")) {
			applyWithSelectedHandlePhone(identity);
			return jsonObj.toString();
		}
		// Field-agnostic negative scenarios; the target handle is resolved from the schema and is
		// guaranteed present here (the matching capability skip in IdRepoUtil ran first).
		if (testCaseName.contains("_missingRequiredHandle")) {
			removeHandleFromIdentity(identity, IdRepoUtil.resolveRequiredHandle());
			return jsonObj.toString();
		}
		if (testCaseName.contains("_invalidStringHandleValue")) {
			setInvalidHandleValue(identity, IdRepoUtil.resolveHandleOfType("string"));
			return jsonObj.toString();
		}
		if (testCaseName.contains("_invalidArrayHandleValue")) {
			setInvalidHandleValue(identity, IdRepoUtil.resolveHandleOfType("array"));
			return jsonObj.toString();
		}
		// Save/replay run outside the per-handle loop below, which only visits array-typed handles —
		// these two must also cover string-typed handles (e.g. licenseNo).
		// _save_withdublicatevalue contains _withdublicatevalue, so it must be checked first.
		if (testCaseName.contains("_save_withdublicatevalue")) {
			saveHandleValues(identity);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withdublicatevalue")) {
			applySavedHandleValues(identity);
			return jsonObj.toString();
		}
		// Replays only the saved email value, avoiding a false IDR-IDC-014 collision from other active handles.
		if (testCaseName.contains("_withReusableEmailAfterRemoval")) {
			applySavedEmailValueOnly(identity);
			return jsonObj.toString();
		}

		JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
		for (int i = 0; i < selectedHandles.length(); i++) {
			String handle = selectedHandles.getString(i);
			if (!identity.has(handle)) continue;
			Object handleObj = identity.get(handle);
			if (!(handleObj instanceof JSONArray)) continue;
			JSONArray handleArray = (JSONArray) handleObj;

			applyAddIdentityHandleMutation(testCaseName, identity, selectedHandles, handle, handleArray);
			identity.put(handle, handleArray);
		}
		return jsonObj.toString();
	}

	/** Adds a field the live schema does not define, for the _extraNonSchemaField test. */
	public static String injectExtraNonSchemaField(String inputJson) {
		JSONObject jsonObj = new JSONObject(inputJson);
		JSONObject identity = jsonObj.getJSONObject("request").getJSONObject("identity");
		identity.put(resolveFieldNotInSchema(), "extraFieldValue");
		return jsonObj.toString();
	}

	/**
	 * Applies the UpdateIdentity handle scenario named in {@code testCaseName} to a rendered request.
	 * <p>
	 * The update template contains every schema field, including freshly generated array handles
	 * that do not match the stored record. These are removed first by
	 * {@link #omitUnmutatedArrayHandleFields} so a plain email/phone update stays a partial update.
	 * After that the first matching global scenario is applied (remove handles from the record,
	 * reshape or invalidate {@code selectedHandles}, replay saved values for IDR-IDC-014, and so
	 * on); otherwise {@link #applyUpdateIdentityHandleMutation} runs for each array-typed handle.
	 *
	 * @param inputJson    rendered UpdateIdentity request ({@code request.identity} is required)
	 * @param testCaseName YAML test case name that carries the scenario token
	 * @return the mutated request JSON
	 * @throws org.json.JSONException if the body has no {@code request.identity} object
	 */
	public static String replaceArrayHandleValuesForUpdateIdentity(String inputJson, String testCaseName) {
		JSONObject jsonObj = new JSONObject(inputJson);
		JSONObject request = jsonObj.getJSONObject("request");
		JSONObject identity = request.getJSONObject("identity");
		String phoneFieldName = resolvePhoneFieldName();
		String emailFieldName = resolveEmailFieldName();
		// Schema HBS dumps every field, including a fresh functionalId [{value, tags}] that is not
		// the stored one. updateJsonObject only merges language lists, so that leftover recurses
		// to IDR-IDC-004. Omit unmutated array handles so email/phone-only updates stay partial.
		omitUnmutatedArrayHandleFields(identity, testCaseName, emailFieldName);

		// Handle cases that exit early or modify identity structure globally.
		// More-specific patterns checked before substrings they contain.
		if (testCaseName.contains("_withdeletehandlefromrecord")) {
			applyDeleteHandleFromRecord(identity);
			return jsonObj.toString();
		}
		// Removes only the email handle, leaving other handles untouched, so its value can be reused later.
		if (testCaseName.contains("_removeSavedEmailHandle")) {
			removeHandleFromIdentity(identity, emailFieldName);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withemptyhandles")) {
			identity.remove("selectedHandles");
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withouthandlesattr")) {
			applyWithoutHandlesAttr(identity);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withoutselectedhandlesandattri")) {
			applyWithoutSelectedHandlesAndAttri(identity);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withnullselectedhandles")) {
		    identity.put("selectedHandles", JSONObject.NULL);
		    return jsonObj.toString();
		}
		if (testCaseName.contains("_withinvalidselectedhandletype")) {
		    identity.put("selectedHandles", "email");
		    return jsonObj.toString();
		}
		if (testCaseName.contains("_withSelectedHandlesSpace")) {
		    JSONArray selectedHandles = new JSONArray();
		    selectedHandles.put(" " + resolveEmailFieldName() + " ");
		    identity.put("selectedHandles", selectedHandles);
		    return jsonObj.toString();
		}
		if (testCaseName.contains("_withemptyselectedhandle")) {
			identity.put("selectedHandles", new JSONArray());
			return jsonObj.toString();
		}
		if (testCaseName.contains("_updateselectedhandleswithinvalid")) {
			identity.put("selectedHandles", new JSONArray().put("invalidscehema123"));
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withupdatedhandlewhichisnotinschema")) {
			identity.put("selectedHandles", new JSONArray().put("invalid12@@"));
			return jsonObj.toString();
		}
		if (testCaseName.contains("_replaceselectedhandles")) {
			identity.put("selectedHandles", new JSONArray().put(phoneFieldName));
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withonehandle")) {
		    JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
		    JSONArray updatedHandles = new JSONArray();
		    for (int i = 0; i < selectedHandles.length(); i++) {
		        String handle = selectedHandles.getString(i);
		        if (handle.equalsIgnoreCase("email")) {
		            updatedHandles.put(handle);
		            break; // keep only email
		        }
		    }
		    identity.put("selectedHandles", updatedHandles);
		    return jsonObj.toString();
		}
		if (testCaseName.contains("_updatewithphoneemail")) {
			JSONArray updatedHandles = new JSONArray();
			updatedHandles.put(emailFieldName);
			updatedHandles.put(phoneFieldName);
			identity.put("selectedHandles", updatedHandles);
			return jsonObj.toString();
		}
		if (testCaseName.contains("_removeselectedhandlesandupdateemail")) {
			identity.remove("selectedHandles");
			// Email stays as already set by updateIdentityHbs + $EMAILVALUE$ replacement.
			// $ID: tokens put here would not be resolved by the framework at this stage.
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withTaggedListTypeNotInSchema")) {
		    String nonHandleField = "fullName";
		    if (identity.has(nonHandleField)) {
		        JSONObject entry = new JSONObject();
		        entry.put("value", "InvalidTaggedValue_" + RANDOM_ID);
		        entry.put("tags", new JSONArray().put("handle"));
		        identity.put(nonHandleField, new JSONArray().put(entry));
		    }
		    return jsonObj.toString();
		}
		if (testCaseName.contains("_withArrayOfStringNotInSchema")) {
		    String taggedListTypeField = emailFieldName;
		    if (identity.has(taggedListTypeField)) {
		        JSONArray stringArray = new JSONArray();
		        stringArray.put("invalidStringValue1_" + RANDOM_ID);
		        stringArray.put("invalidStringValue2_" + RANDOM_ID);
		        identity.put(taggedListTypeField, stringArray);
		    }
		    return jsonObj.toString();
		}
		// Replays the handle values of the identity created by the "_save_" AddIdentity test onto
		// this (different) identity, so every schema-declared handle collides with an existing one
		// and the server must reject the update with IDR-IDC-014. Runs outside the per-handle loop
		// below so string-typed handles are covered too.
		// _withusedhandlevaluewithoutselectedhandles contains _withusedhandlevalue — check it first.
		if (testCaseName.contains("_withusedhandlevaluewithoutselectedhandles")) {
			applySavedHandleValues(identity);
			identity.remove("selectedHandles");
			return jsonObj.toString();
		}
		if (testCaseName.contains("_withusedhandlevalue")) {
			applySavedHandleValues(identity);
			return jsonObj.toString();
		}

		if (!identity.has("selectedHandles") || !(identity.get("selectedHandles") instanceof JSONArray)) {
			return jsonObj.toString();
		}
		JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
		for (int i = 0; i < selectedHandles.length(); i++) {
			String handle = selectedHandles.getString(i);
			if (!identity.has(handle) || !(identity.get(handle) instanceof JSONArray)) continue;
			JSONArray handleArray = identity.getJSONArray(handle);

			applyUpdateIdentityHandleMutation(testCaseName, identity, selectedHandles, handle, handleArray,
					phoneFieldName, i);
			identity.put(handle, handleArray);
		}
		return jsonObj.toString();
	}

	// ===== AddIdentity per-handle dispatch =====

	/**
	 * Runs the first AddIdentity per-handle scenario whose token is in {@code testCaseName}
	 * (tags removed or invalid, extra values, email-only variants, functional-id variants,
	 * case-sensitive tags, duplicate values replayed from a "_save_" case). With no match the
	 * handle array is left as rendered.
	 *
	 * @param testCaseName    YAML test case name
	 * @param identity        {@code request.identity} being mutated
	 * @param selectedHandles the identity's {@code selectedHandles} array
	 * @param handle          name of the handle field currently visited
	 * @param handleArray     that field's {@code [{value, tags}]} array, mutated in place
	 */
	private static void applyAddIdentityHandleMutation(String testCaseName, JSONObject identity,
			JSONArray selectedHandles, String handle, JSONArray handleArray) {
		// More-specific patterns must appear before shorter substrings they contain:
		//   _withmultiplevaluesandwithouttags before _withouttags and _withmultiplevalues
		//   _withfunctionalIdsUsedFirstTwoValueOutOfFive before _withfunctionalIdsUsedFirstTwoValue before _withfunctionalIds
		if (testCaseName.contains("_withmultiplevaluesandwithouttags")) {
			applyMultipleValuesAndWithoutTags(handleArray);
		} else if (testCaseName.contains("_onlywithtags")) {
			applyOnlyWithTags(handleArray);
		} else if (testCaseName.contains("_withouttags")) {
			applyWithoutTags(handleArray);
		} else if (testCaseName.contains("_withtagwithoutselectedhandles")) {
			applyWithTagWithoutSelectedHandles(handleArray);
		} else if (testCaseName.contains("_withinvalidtag")) {
			applyWithInvalidTag(handleArray);
		} else if (testCaseName.contains("_withmultiplevalues")) {
			putMultipleValues(handleArray);
		} else if (testCaseName.contains("_WithEmailHandleAndNotification")
		        && handle.equals(resolveEmailFieldName())) {
		    applyEmailHandleAndNotification(handleArray);
		} else if (testCaseName.contains("_withmultiplehandleswithoutvalue")) {
			applyMultipleHandlesWithoutValue(handleArray);
		} else if (testCaseName.contains("_WithMultipleEmail_And_OneEmail_As_Handle")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleEmailOneHandle(handleArray);
		} else if (testCaseName.contains("_WithMultipleEmailWithoutHandle")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleEmailWithoutHandle(handleArray);
		} else if (testCaseName.contains("_SelectedHandlesOrderVariation")
		        && handle.equals(resolveEmailFieldName())) {
		    applySelectedHandlesOrderVariation(handleArray);
		} else if (testCaseName.contains("_OnlyNotificationTag")
		        && handle.equals(resolveEmailFieldName())) {
		    applyOnlyNotificationTag(handleArray);
		} else if (testCaseName.contains("_WithLengthyEmailArray")
		        && handle.equals(resolveEmailFieldName())) {
		    applyLengthyEmailArray(handleArray);
		} else if (testCaseName.contains("_WithMultipleHandlesDifferentDomains")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleHandlesDifferentDomains(handleArray);
		} else if (testCaseName.contains("_WithMultipleEmailHandlesTagged")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleEmailHandlesTagged(handleArray);
		} else if (testCaseName.contains("_withfunctionalIdsUsedFirstTwoValueOutOfFive") && isArrayHandle(handle)) {
			applyFunctionalIdsUsedFirstTwoValueOutOfFive(handleArray, handle);
		} else if (testCaseName.contains("_withfunctionalIdsUsedFirstTwoValue") && isArrayHandle(handle)) {
			applyFunctionalIdsUsedFirstTwoValue(handleArray, handle);
		} else if (testCaseName.contains("_withfunctionalIdsandPhoneWithoutTags")) {
			applyFunctionalIdsAndPhoneWithoutTags(handleArray);
		} else if (testCaseName.contains("_withfunctionalIds") && isArrayHandle(handle)) {
			applyFunctionalIds(handleArray);
		} else if (testCaseName.contains("_removeexceptfirsthandle")) {
			applyRemoveExceptFirstHandle(identity, selectedHandles, handle);
		} else if (testCaseName.contains("_withinvaliddemofield_inupdate")) {
			applyRemoveExceptFirstHandle(identity, selectedHandles, handle);
		} else if (testCaseName.contains("_withonedemofield")) {
			applyWithOneDemoField(identity, selectedHandles, handle);
		} else if (testCaseName.contains("_withcasesensitivehandles")) {
			applyWithCaseSensitiveHandles(handleArray);
		} else if (testCaseName.contains("_withmultipledublicatevalue")) {
			applyWithMultipleDuplicateValue(handleArray, handle);
		} else if (testCaseName.contains("_removevalueaddexistingvalue")) {
			applyRemoveValueAddExistingValue(handleArray, handle);
		} else {
			for (int j = 0; j < handleArray.length(); j++) {
				JSONObject obj = handleArray.getJSONObject(j);
				obj.put("value", obj.getString("value"));
			}
		}
	}

	// ===== UpdateIdentity per-handle dispatch =====

	/**
	 * Runs the first UpdateIdentity per-handle scenario whose token is in {@code testCaseName}
	 * (new values or tags, appended untagged values, renamed or invalid {@code selectedHandles},
	 * removed tags or demographic fields, email-array variants, a non-handle attribute listed as a
	 * handle). With no match the handle array is left as rendered.
	 *
	 * @param testCaseName    YAML test case name
	 * @param identity        {@code request.identity} being mutated
	 * @param selectedHandles the identity's {@code selectedHandles} array
	 * @param handle          name of the handle field currently visited
	 * @param handleArray     that field's {@code [{value, tags}]} array, mutated in place
	 * @param phoneFieldName  phone property name from the auth actuator
	 * @param outerIndex      position of {@code handle} within {@code selectedHandles}
	 */
	private static void applyUpdateIdentityHandleMutation(String testCaseName, JSONObject identity,
			JSONArray selectedHandles, String handle, JSONArray handleArray,
			String phoneFieldName, int outerIndex) {
		// More-specific patterns must appear before shorter substrings they contain:
		//   _withupdatedselectedhandleanddemo and _withupdatedselectedhandleandfirstattribute before _withupdatedselectedhandle
		//   _withupdatetagsandhandles before _withupdatetags
		if (testCaseName.contains("_withupdatevalues")) {
			applyWithUpdateValues(handleArray, handle, resolveEmailFieldName());
		} else if (testCaseName.contains("_withmultiplevalues")) {
			putMultipleValues(handleArray);
		} else if (testCaseName.contains("_appendUntaggedValuesToArrayHandle")
				&& handle.equals(IdRepoUtil.resolveHandleOfType("array"))) {
			applyAppendUntaggedValues(handleArray, handle);
		} else if (testCaseName.contains("_withupdatetagsandhandles")) {
			applyWithUpdateTagsAndHandles(handleArray);
		} else if (testCaseName.contains("_withupdatetags")) {
			applyWithUpdateTags(handleArray);
		} else if (testCaseName.contains("_withupdatedselectedhandleandfirstattribute")) {
			applyWithUpdatedSelectedHandleAndFirstAttribute(identity, selectedHandles);
		} else if (testCaseName.contains("_withupdatedselectedhandleanddemo")) {
			applyWithUpdatedSelectedHandleAndDemo(identity, selectedHandles, outerIndex);
		} else if (testCaseName.contains("_withupdatedselectedhandle")) {
			applyWithUpdatedSelectedHandle(selectedHandles);
		} else if (testCaseName.contains("_withremovedtaggedattribute")) {
			applyWithRemovedTaggedAttribute(identity, selectedHandles, handle, handleArray);
		} else if (testCaseName.contains("_withinvaliddemofield")) {
			applyWithInvalidDemoField(identity, selectedHandles, handle, outerIndex);
		} else if (testCaseName.contains("_withalldemofieldsremoved")) {
			applyWithAllDemoFieldsRemoved(identity, selectedHandles, handle);
		} else if (testCaseName.contains("_witharandomnonhandleattr")) {
			applyWithARandomNonHandleAttr(identity, selectedHandles);
		} else if (testCaseName.contains("_WithMultipleEmail_And_OneEmail_As_Handle")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleEmailOneHandle(handleArray);
		} else if (testCaseName.contains("_WithMultipleEmailWithoutHandle")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleEmailWithoutHandle(handleArray);
		} else if (testCaseName.contains("_WithMultipleEmailHandlesTagged")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleEmailHandlesTagged(handleArray);
		} else if (testCaseName.contains("_RetainTaggedEmail")
		        && handle.equals(resolveEmailFieldName())) {
		    applyRetainTaggedEmail(handleArray);
		} else if (testCaseName.contains("_WithLengthyEmailArray")
		        && handle.equals(resolveEmailFieldName())) {
		    applyLengthyEmailArray(handleArray);
		} else if (testCaseName.contains("_WithMultipleHandlesDifferentDomains")
		        && handle.equals(resolveEmailFieldName())) {
		    applyMultipleHandlesDifferentDomains(handleArray);
		} else if (testCaseName.contains("_updateselectedhandleswithscehmaattrwhichisnothandle")) {
			applyUpdateSelectedHandlesWithSchemaAttrWhichIsNotHandle(identity, selectedHandles);
		} else if (testCaseName.contains("_removeselectedhandle_updatephone")) {
			applyRemoveSelectedHandleUpdatePhone(identity, phoneFieldName);
		} else if (testCaseName.contains("_withinvaliddhandle")) {
			selectedHandles.put("newFieldHandle");
		}
	}

	// ===== Shared Helpers =====

	/** Phone property name from the auth actuator ({@code json-property.phone_number}), e.g. {@code phone}. */
	private static String resolvePhoneFieldName() {
		String phone = AdminTestUtil.getValueFromAuthActuator("json-property", "phone_number");
		return phone.replaceAll("\\[\"|\"]", "");
	}

	/** Handle fields declared by the live IdSchema ("handle":true), e.g. [licenseNo, functionalId]. */
	private static List<String> resolveSchemaHandleFields() {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		List<String> handleFields = new ArrayList<>();
		for (String fieldName : props.keySet()) {
			if (props.getJSONObject(fieldName).optBoolean("handle", false)) {
				handleFields.add(fieldName);
			}
		}
		return handleFields;
	}

	/** True when the given field is an array-typed handle in the live schema (e.g. functionalId). */
	private static boolean isArrayHandle(String handle) {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		if (!props.has(handle)) {
			return false;
		}
		JSONObject fieldDef = props.getJSONObject(handle);
		return fieldDef.optBoolean("handle", false) && "array".equals(fieldDef.optString("type", "string"));
	}

	/**
	 * Drops array-handle fields the HBS filled with new values, unless this test is
	 * rewriting those arrays. Also drops {@code selectedHandles} on email/phone-only
	 * updates so stored handles are not deleted as leftovers.
	 */
	private static void omitUnmutatedArrayHandleFields(JSONObject identity, String testCaseName,
			String emailFieldName) {
		if (keepsArrayHandlePayload(testCaseName)) {
			return;
		}
		boolean keepEmailArray = mutatesEmailArray(testCaseName) && isArrayHandle(emailFieldName);
		List<String> toRemove = new ArrayList<>();
		for (String field : identity.keySet()) {
			if (!isArrayHandle(field)) {
				continue;
			}
			if (keepEmailArray && field.equals(emailFieldName)) {
				continue;
			}
			if (testCaseName.toLowerCase().contains(field.toLowerCase())) {
				continue;
			}
			toRemove.add(field);
		}
		for (String field : toRemove) {
			identity.remove(field);
		}
		if (!mutatesSelectedHandlesStructure(testCaseName)) {
			identity.remove("selectedHandles");
		}
	}

	/** True for update scenarios that rewrite array-handle payloads, so the template's arrays must be kept. */
	private static boolean keepsArrayHandlePayload(String testCaseName) {
		return testCaseName.contains("_withupdatevalues")
				|| testCaseName.contains("_withmultiplevalues")
				|| testCaseName.contains("_appendUntaggedValuesToArrayHandle")
				|| testCaseName.contains("_withupdatetagsandhandles")
				|| testCaseName.contains("_withupdatetags")
				|| testCaseName.contains("_withremovedtaggedattribute")
				|| testCaseName.contains("_withinvaliddemofield")
				|| testCaseName.contains("_withalldemofieldsremoved")
				|| testCaseName.contains("_withfunctionalIds")
				|| testCaseName.contains("_withusedhandlevalue")
				|| testCaseName.contains("_withdeletehandlefromrecord")
				|| testCaseName.contains("_withcasesensitivehandles")
				|| testCaseName.contains("_withmultiplehandleswithoutvalue");
	}

	/** True for scenarios that rewrite the email field as an {@code [{value, tags}]} array. */
	private static boolean mutatesEmailArray(String testCaseName) {
		return testCaseName.contains("_WithMultipleEmail")
				|| testCaseName.contains("_RetainTaggedEmail")
				|| testCaseName.contains("_WithLengthyEmailArray")
				|| testCaseName.contains("_WithMultipleHandlesDifferentDomains")
				|| testCaseName.contains("_WithEmailHandleAndNotification")
				|| testCaseName.contains("_OnlyNotificationTag")
				|| testCaseName.contains("_SelectedHandlesOrderVariation");
	}

	/** True (case-insensitive) for scenarios that send or reshape {@code selectedHandles} on update. */
	private static boolean mutatesSelectedHandlesStructure(String testCaseName) {
		String n = testCaseName.toLowerCase();
		return n.contains("_withnullselectedhandles")
				|| n.contains("_withinvalidselectedhandletype")
				|| n.contains("_withselectedhandlesspace")
				|| n.contains("_withemptyselectedhandle")
				|| n.contains("_withemptyhandles")
				|| n.contains("_withouthandles")
				|| n.contains("_withoutselectedhandles")
				|| n.contains("_replaceselectedhandles")
				|| n.contains("_withonehandle")
				|| n.contains("_updatewithphoneemail")
				|| n.contains("_removeselectedhandles")
				|| n.contains("_updateselectedhandles")
				|| n.contains("_withupdatedselectedhandle")
				|| n.contains("_removeselectedhandle_updatephone")
				|| n.contains("_withinvaliddhandle")
				|| n.contains("_withupdatedhandlewhichisnotinschema")
				|| n.contains("_withusedhandlevaluewithoutselectedhandles");
	}

	/** A schema field that exists but is NOT a handle (for the not-applicable-selectedHandle test). */
	private static String resolveNonHandleSchemaField() {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		for (String fieldName : props.keySet()) {
			if (fieldName.equals("UIN") || fieldName.equals("selectedHandles")
					|| fieldName.equals("IDSchemaVersion")) {
				continue;
			}
			if (!props.getJSONObject(fieldName).optBoolean("handle", false)) {
				return fieldName;
			}
		}
		return "fullName";
	}

	/** A field name generated and verified NOT present in the live schema (for the unknown-field test). */
	private static String resolveFieldNotInSchema() {
		JSONObject props = AdminTestUtil.getIdentitySchemaProperties();
		String candidate;
		do {
			candidate = "notASchemaField" + BaseTestCase.generateRandomNumberString(6);
		} while (props.has(candidate));
		return candidate;
	}

	// A value engineered to violate the typical handle validator (email / phone / alphanumeric-ID
	// regexes) — it contains spaces and special characters that anchored patterns reject.
	private static final String INVALID_HANDLE_VALUE = "in valid!@# value";

	/** Removes a handle field from both the identity body and the selectedHandles array. */
	private static void removeHandleFromIdentity(JSONObject identity, String handle) {
		if (handle == null) {
			return;
		}
		identity.remove(handle);
		if (identity.has("selectedHandles") && identity.get("selectedHandles") instanceof JSONArray) {
			JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
			JSONArray remaining = new JSONArray();
			for (int i = 0; i < selectedHandles.length(); i++) {
				if (!handle.equals(selectedHandles.getString(i))) {
					remaining.put(selectedHandles.getString(i));
				}
			}
			identity.put("selectedHandles", remaining);
		}
	}

	/** Sets a handle's value to one that violates its validator, honouring the schema's string/array shape. */
	private static void setInvalidHandleValue(JSONObject identity, String handle) {
		if (handle == null) {
			return;
		}
		writeHandleValue(identity, handle, INVALID_HANDLE_VALUE);
	}

	/** Reads a handle's value regardless of schema shape (string, or array of {value,tags}); null if absent. */
	private static String readHandleValue(JSONObject identity, String handle) {
		Object handleObj = identity.opt(handle);
		if (handleObj instanceof JSONArray) {
			JSONArray handleArray = (JSONArray) handleObj;
			for (int i = 0; i < handleArray.length(); i++) {
				JSONObject entry = handleArray.optJSONObject(i);
				if (entry != null && entry.has("value")) {
					return entry.getString("value");
				}
			}
			return null;
		}
		return handleObj instanceof String ? (String) handleObj : null;
	}

	/** Counterpart to {@link #readHandleValue}: writes into whichever shape the schema defines. */
	private static void writeHandleValue(JSONObject identity, String handle, String value) {
		Object handleObj = identity.opt(handle);
		if (handleObj instanceof JSONArray) {
			JSONArray handleArray = (JSONArray) handleObj;
			for (int i = 0; i < handleArray.length(); i++) {
				JSONObject entry = handleArray.optJSONObject(i);
				if (entry != null) {
					entry.put("value", value);
				}
			}
		} else if (handleObj != null) {
			identity.put(handle, value);
		}
	}

	/** Records the identity's current handle values for a later test to replay. */
	private static void saveHandleValues(JSONObject identity) {
		for (String handle : resolveSchemaHandleFields()) {
			String value = readHandleValue(identity, handle);
			if (value != null) {
				savedHandleValues.put(handle, value);
			}
		}
	}

	/** Replays {@link #savedHandleValues} onto this identity so each handle duplicates an existing one. */
	private static void applySavedHandleValues(JSONObject identity) {
		for (Map.Entry<String, String> saved : savedHandleValues.entrySet()) {
			if (identity.has(saved.getKey())) {
				writeHandleValue(identity, saved.getKey(), saved.getValue());
			}
		}
	}

	/** Replays only the saved email value onto this identity, to prove a freed handle value is reusable. */
	private static void applySavedEmailValueOnly(JSONObject identity) {
		String emailField = resolveEmailFieldName();
		String savedEmail = savedHandleValues.get(emailField);
		if (savedEmail == null) {
			throw new IllegalStateException("No saved email handle value is available for reuse");
		}
		if (!identity.has(emailField)) {
			throw new IllegalStateException("Email handle field is absent from the identity");
		}
		writeHandleValue(identity, emailField, savedEmail);
	}

	/** Email property name from the auth actuator ({@code json-property.emailId}), e.g. {@code email}. */
	private static String resolveEmailFieldName() {
		String email = AdminTestUtil.getValueFromAuthActuator("json-property", "emailId");
		return email.replaceAll("\\[\"|\"]", "");
	}

	/** Adds a non-schema {@code values} array (three strings) to every entry; the server must reject it. */
	private static void putMultipleValues(JSONArray handleArray) {
		JSONArray valuesArray = new JSONArray();
		valuesArray.put("mosip501724826584965_modified_1");
		valuesArray.put("mosip501724826584965_modified_2");
		valuesArray.put("mosip501724826584965_modified_3");
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).put("values", valuesArray);
		}
	}

	/** Appends two more untagged values to an existing array-typed handle, to prove tags don't gate status. */
	private static void applyAppendUntaggedValues(JSONArray handleArray, String handle) {
		JSONObject second = new JSONObject();
		second.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
		JSONObject third = new JSONObject();
		third.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
		handleArray.put(second);
		handleArray.put(third);
	}

	// ===== AddIdentity Private Handlers =====

	/** {@code _withmultiplevaluesandwithouttags}: adds a {@code values} array and drops {@code tags} from every entry. */
	private static void applyMultipleValuesAndWithoutTags(JSONArray handleArray) {
		JSONArray valuesArray = new JSONArray();
		valuesArray.put("mosip501724826584965_modified_1");
		valuesArray.put("mosip501724826584965_modified_2");
		valuesArray.put("mosip501724826584965_modified_3");
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject obj = handleArray.getJSONObject(j);
			obj.put("values", valuesArray);
			obj.remove("tags");
		}
	}

	/** {@code _onlywithtags}: drops {@code value}, leaving entries with tags only. */
	private static void applyOnlyWithTags(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("value");
		}
	}

	/** {@code _withouttags}: drops {@code tags} from every entry. */
	private static void applyWithoutTags(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("tags");
		}
	}

	/** {@code _withtagwithoutselectedhandles}: removes any entry-level {@code selectedHandles} key (normally a no-op). */
	private static void applyWithTagWithoutSelectedHandles(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("selectedHandles");
		}
	}

	/** {@code _withinvalidtag}: suffixes every tag with {@code _invalid<randomId>}. */
	private static void applyWithInvalidTag(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject obj = handleArray.getJSONObject(j);
			JSONArray tags = obj.optJSONArray("tags");
			if (tags != null) {
				for (int k = 0; k < tags.length(); k++) {
					tags.put(k, tags.getString(k) + "_invalid" + nextRandomId());
				}
				obj.put("tags", tags);
			}
		}
	}

	/** {@code _withmultiplehandleswithoutvalue}: replaces the array with two entries that have tags but no value. */
	private static void applyMultipleHandlesWithoutValue(JSONArray handleArray) {

	    // Remove existing entries
	    while (handleArray.length() > 0) {
	        handleArray.remove(0);
	    }

	    JSONObject handleObj = new JSONObject();
	    handleObj.put("tags", new JSONArray().put("handle"));

	    JSONObject notificationObj = new JSONObject();
	    notificationObj.put("tags", new JSONArray().put("notification"));

	    handleArray.put(handleObj);
	    handleArray.put(notificationObj);
	}
	
	/** {@code _WithEmailHandleAndNotification}: tags every email entry {@code [handle, notification]}. */
	private static void applyEmailHandleAndNotification(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject emailObj = handleArray.getJSONObject(j);
	        emailObj.put("tags",
	                new JSONArray()
	                        .put("handle")
	                        .put("notification"));
	    }
	}
	
	/**
	 * {@code _withfunctionalIdsUsedFirstTwoValueOutOfFive}: appends four schema-valid values (five in
	 * total); only the first appended one is tagged {@code handle}.
	 */
	private static void applyFunctionalIdsUsedFirstTwoValueOutOfFive(JSONArray handleArray, String handle) {
		// Positive: five entries, only the first tagged; values generated to satisfy the handle validator.
		for (int j = 0; j < 4; j++) {
			JSONObject obj = new JSONObject();
			obj.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
			if (j < 1) {
				obj.put("tags", new JSONArray().put("handle"));
			}
			handleArray.put(obj);
		}
	}
	
	/** {@code _WithMultipleEmail_And_OneEmail_As_Handle}: one email tagged {@code handle} plus one untagged email. */
	private static void applyMultipleEmailOneHandle(JSONArray handleArray) {
	    while (handleArray.length() > 0) {
	        handleArray.remove(0);
	    }
	    JSONObject handleEmail = new JSONObject();
	    handleEmail.put("value", "handle_" + BaseTestCase.generateRandomNumberString(6) + "@mail.com");
	    handleEmail.put("tags", new JSONArray().put("handle"));
	    JSONObject normalEmail = new JSONObject();
	    normalEmail.put("value", "notification_" + BaseTestCase.generateRandomNumberString(6) + "@mail.com");
	    handleArray.put(handleEmail);
	    handleArray.put(normalEmail);
	}
	
	/** {@code _SelectedHandlesOrderVariation}: tags every email entry {@code [notification, handle]} (reversed order). */
	private static void applySelectedHandlesOrderVariation(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject emailObj = handleArray.getJSONObject(j);
	        JSONArray tags = new JSONArray();
	        tags.put("notification");
	        tags.put("handle");
	        emailObj.put("tags", tags);
	    }
	}
	
	/** {@code _OnlyNotificationTag}: tags every email entry {@code [notification]} only (no {@code handle}). */
	private static void applyOnlyNotificationTag(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject emailObj = handleArray.getJSONObject(j);
	        JSONArray tags = new JSONArray();
	        tags.put("notification");
	        emailObj.put("tags", tags);
	    }
	}
	
	/** {@code _WithLengthyEmailArray}: replaces the array with 20 unique emails; only the first is tagged {@code handle}. */
	private static void applyLengthyEmailArray(JSONArray handleArray) {
		while (handleArray.length() > 0) {
			handleArray.remove(0);
		}
		for (int i = 1; i <= 20; i++) {
			JSONObject emailObj = new JSONObject();
			emailObj.put("value", "email" + i + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");

			if (i == 1) {
				emailObj.put("tags", new JSONArray().put("handle"));
			}

			handleArray.put(emailObj);
		}
	}
	
	/** {@code _WithMultipleHandlesDifferentDomains}: three emails on different domains, all tagged {@code handle}. */
	private static void applyMultipleHandlesDifferentDomains(JSONArray handleArray) {
	    while (handleArray.length() > 0) {
	        handleArray.remove(0);
	    }
	    JSONObject email1 = new JSONObject();
	    email1.put("value", "user1_" + BaseTestCase.generateRandomNumberString(6) + "@gmail.com");
	    email1.put("tags", new JSONArray().put("handle"));

	    JSONObject email2 = new JSONObject();
	    email2.put("value", "user2_" + BaseTestCase.generateRandomNumberString(6) + "@yahoo.com");
	    email2.put("tags", new JSONArray().put("handle"));

	    JSONObject email3 = new JSONObject();
	    email3.put("value", "user3_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");
	    email3.put("tags", new JSONArray().put("handle"));

	    handleArray.put(email1);
	    handleArray.put(email2);
	    handleArray.put(email3);
	}
	
	/** {@code _WithMultipleEmailWithoutHandle}: two unique emails with no tags. */
	private static void applyMultipleEmailWithoutHandle(JSONArray handleArray) {
		while (handleArray.length() > 0) {
			handleArray.remove(0);
		}
		JSONObject email1 = new JSONObject();
		email1.put("value", "email01_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");
		JSONObject email2 = new JSONObject();
		email2.put("value", "email02_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");
		handleArray.put(email1);
		handleArray.put(email2);
	}
	
	/** {@code _WithMultipleEmailHandlesTagged}: two unique emails, both tagged {@code handle}. */
	private static void applyMultipleEmailHandlesTagged(JSONArray handleArray) {
	    // Clear existing email entries
	    while (handleArray.length() > 0) {
	        handleArray.remove(0);
	    }
	    JSONObject email1 = new JSONObject();
	    email1.put("value", "email03_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");
	    email1.put("tags", new JSONArray().put("handle"));
	    JSONObject email2 = new JSONObject();
	    email2.put("value", "email04_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");
	    email2.put("tags", new JSONArray().put("handle"));
	    handleArray.put(email1);
	    handleArray.put(email2);
	}
	
	/** {@code _RetainTaggedEmail}: keeps the existing entries and appends one new untagged email. */
	private static void applyRetainTaggedEmail(JSONArray handleArray) {

	    JSONObject existingEmail = handleArray.getJSONObject(0);

	    JSONObject newEmail = new JSONObject();
	    newEmail.put("value",
	        "newemail_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");

	    handleArray.put(newEmail);
	}

	/**
	 * {@code _withfunctionalIdsUsedFirstTwoValue}: when fewer than three entries exist, appends a
	 * tagged and an untagged schema-valid value.
	 */
	private static void applyFunctionalIdsUsedFirstTwoValue(JSONArray handleArray, String handle) {
		if (handleArray.length() < 3) {
			// Positive: appended values generated to satisfy the handle's own validator.
			JSONObject secondValue = new JSONObject();
			secondValue.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
			secondValue.put("tags", new JSONArray().put("handle"));
			JSONObject thirdValue = new JSONObject();
			thirdValue.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
			handleArray.put(secondValue);
			handleArray.put(thirdValue);
		}
	}

	/** {@code _withfunctionalIdsandPhoneWithoutTags}: drops {@code tags} from every entry. */
	private static void applyFunctionalIdsAndPhoneWithoutTags(JSONArray handleArray) {
		// Gated [functionalId, phone]; both are already handles in the body — just strip the tags.
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("tags");
		}
	}

	/** {@code _withfunctionalIds}: drops {@code tags} from every entry of an array-typed handle. */
	private static void applyFunctionalIds(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("tags");
		}
	}

	/**
	 * {@code _removeexceptfirsthandle} / {@code _withinvaliddemofield_inupdate}: removes the visited
	 * handle field when more than one handle is selected and trims {@code selectedHandles} to its
	 * first entry.
	 */
	private static void applyRemoveExceptFirstHandle(JSONObject identity, JSONArray selectedHandles, String handle) {
		if (identity.has("selectedHandles") && selectedHandles.length() > 0) {
			for (int j = 1; j < selectedHandles.length(); j++) {
				if (identity.has(handle)) {
					identity.remove(handle);
				}
			}
			while (selectedHandles.length() > 1) {
				selectedHandles.remove(1);
			}
		}
	}

	/**
	 * {@code _withonedemofield}: removes the visited array handle when more than one handle is
	 * selected and sets {@code selectedHandles} to its first entry.
	 */
	private static void applyWithOneDemoField(JSONObject identity, JSONArray selectedHandles, String handle) {
		if (identity.has("selectedHandles")) {
			String firstHandle = selectedHandles.getString(0);
			for (int j = 1; j < selectedHandles.length(); j++) {
				if (identity.has(handle) && identity.get(handle) instanceof JSONArray) {
					identity.remove(handle);
				}
			}
			identity.put("selectedHandles", new JSONArray().put(firstHandle));
		}
	}

	/** {@code _withcasesensitivehandles}: rewrites any {@code handles} tag (any case) as {@code HANDLES}. */
	private static void applyWithCaseSensitiveHandles(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject obj = handleArray.getJSONObject(j);

	        JSONArray tags = obj.getJSONArray("tags");
	        for (int k = 0; k < tags.length(); k++) {
	            if ("handles".equalsIgnoreCase(tags.getString(k))) {
	                tags.put(k, "HANDLES");
	            }
	        }
	    }
	}

	/** {@code _withselectedhandlephone}: sets {@code selectedHandles} to the phone field only. */
	private static void applyWithSelectedHandlePhone(JSONObject identity) {
		// Gated [phone]; reduce selectedHandles to just phone (resolved name, not a literal).
		identity.put("selectedHandles", new JSONArray().put(resolvePhoneFieldName()));
	}

	/** {@code _withmultipledublicatevalue}: appends the saved value of this handle as a second tagged entry (no-op if none saved). */
	private static void applyWithMultipleDuplicateValue(JSONArray handleArray, String handle) {
		String savedValue = savedHandleValues.get(handle);
		if (savedValue == null) {
			return;
		}
		JSONObject secondValue = new JSONObject();
		secondValue.put("value", savedValue);
		secondValue.put("tags", new JSONArray().put("handle"));
		handleArray.put(secondValue);
	}

	/** {@code _removevalueaddexistingvalue}: overwrites every entry's value with this handle's saved value (no-op if none saved). */
	private static void applyRemoveValueAddExistingValue(JSONArray handleArray, String handle) {
		String savedValue = savedHandleValues.get(handle);
		if (savedValue == null) {
			return;
		}
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject obj = handleArray.getJSONObject(j);
			obj.remove("value");
			obj.put("value", savedValue);
		}
	}

	// ===== UpdateIdentity Private Handlers =====

	/**
	 * {@code _withupdatevalues}: gives every entry a new value, {@code mosip_update_<RANDOM_ID>@mosip.net}
	 * for the email field and a schema-valid generated value for any other handle.
	 */
	private static void applyWithUpdateValues(JSONArray handleArray, String handle, String emailFieldName) {
		for (int j = 0; j < handleArray.length(); j++) {
			if (handle.equals(emailFieldName)) {
				handleArray.getJSONObject(j).put("value", "mosip_update_" + RANDOM_ID + "@mosip.net");
			} else {
				// Positive update — new value generated to satisfy the handle's own validator.
				handleArray.getJSONObject(j).put("value", IdRepoUtil.generateSchemaFieldValue(handle));
			}
		}
	}

	/** {@code _withupdatetags}: suffixes every tag with {@code _invalid<RANDOM_ID>}. */
	private static void applyWithUpdateTags(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject handleObj = handleArray.getJSONObject(j);
			JSONArray tags = handleObj.optJSONArray("tags");
			if (tags != null) {
				for (int k = 0; k < tags.length(); k++) {
					tags.put(k, tags.getString(k) + "_invalid" + RANDOM_ID);
				}
			}
		}
	}

	/** {@code _withupdatetagsandhandles}: suffixes every tag, and every element of an array-valued {@code value}, with {@code _invalid<RANDOM_ID>}. */
	private static void applyWithUpdateTagsAndHandles(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject handleObj = handleArray.getJSONObject(j);
			JSONArray tags = handleObj.optJSONArray("tags");
			if (tags != null) {
				for (int k = 0; k < tags.length(); k++) {
					tags.put(k, tags.getString(k) + "_invalid" + RANDOM_ID);
				}
			}
			JSONArray values = handleObj.optJSONArray("value");
			if (values != null) {
				for (int k = 0; k < values.length(); k++) {
					values.put(k, values.getString(k) + "_invalid" + RANDOM_ID);
				}
			}
		}
	}

	/** {@code _withdeletehandlefromrecord}: removes every selected handle field and {@code selectedHandles} itself. */
	private static void applyDeleteHandleFromRecord(JSONObject identity) {
		JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
		for (int j = 0; j < selectedHandles.length(); j++) {
			String handleToDelete = selectedHandles.getString(j);
			if (identity.has(handleToDelete)) {
				identity.remove(handleToDelete);
			}
		}
		identity.remove("selectedHandles");
	}

	/** {@code _withupdatedselectedhandle}: renames the first selected handle to an unknown name ({@code <name><RANDOM_ID>}). */
	private static void applyWithUpdatedSelectedHandle(JSONArray selectedHandles) {
		String firstHandle = selectedHandles.getString(0);
		selectedHandles.put(0, firstHandle + RANDOM_ID);
	}

	/**
	 * {@code _withupdatedselectedhandleanddemo}: renames the first selected handle and moves its
	 * array under the new name, with each value suffixed by {@code RANDOM_ID}.
	 */
	private static void applyWithUpdatedSelectedHandleAndDemo(JSONObject identity, JSONArray selectedHandles,
			int outerIndex) {
		if (selectedHandles.length() > 0) {
			String originalHandle = selectedHandles.getString(0);
			String updatedHandle = originalHandle + RANDOM_ID;
			selectedHandles.put(0, updatedHandle);
			if (identity.has(originalHandle) && identity.get(originalHandle) instanceof JSONArray) {
				JSONArray originalHandleArray = identity.getJSONArray(originalHandle);
				for (int J = 0; J < originalHandleArray.length(); J++) {
					JSONObject handleObject = originalHandleArray.getJSONObject(outerIndex);
					String originalValue = handleObject.optString("value", "");
					handleObject.put("value", originalValue + RANDOM_ID);
					originalHandleArray.put(J, handleObject);
				}
				identity.remove(originalHandle);
				identity.put(updatedHandle, originalHandleArray);
			}
		}
	}

	/**
	 * {@code _withupdatedselectedhandleandfirstattribute}: makes the first array-valued identity
	 * attribute the first selected handle and appends {@code 123} to its values and tags.
	 */
	private static void applyWithUpdatedSelectedHandleAndFirstAttribute(JSONObject identity,
			JSONArray selectedHandles) {
		// Pick the first array-valued attribute to corrupt; skip selectedHandles and any string/scalar
		// field (e.g. the string-typed handle licenseNo), which would throw on getJSONArray.
		Iterator<String> keys = identity.keys();
		while (keys.hasNext()) {
			String firstKey = keys.next();
			if (firstKey.equals("selectedHandles") || !(identity.get(firstKey) instanceof JSONArray)) {
				continue;
			}
			selectedHandles.put(0, firstKey);
			JSONArray originalArray = identity.getJSONArray(firstKey);
			for (int j = 0; j < originalArray.length(); j++) {
				JSONObject handleObject = originalArray.getJSONObject(j);
				if (handleObject.has("value")) {
					handleObject.put("value", handleObject.getString("value") + "123");
				}
				if (handleObject.has("tags")) {
					JSONArray tagsArray = handleObject.getJSONArray("tags");
					for (int k = 0; k < tagsArray.length(); k++) {
						tagsArray.put(k, tagsArray.getString(k) + "123");
					}
					handleObject.put("tags", tagsArray);
				}
				originalArray.put(j, handleObject);
			}
			identity.remove(firstKey);
			identity.put(firstKey, originalArray);
			break;
		}
	}

	/** {@code _withremovedtaggedattribute}: strips {@code tags} from the entries of every selected array handle. */
	private static void applyWithRemovedTaggedAttribute(JSONObject identity, JSONArray selectedHandles,
			String handle, JSONArray handleArray) {
		for (int j = 0; j < selectedHandles.length(); j++) {
			String handle1 = selectedHandles.getString(j);
			if (identity.has(handle1) && identity.get(handle1) instanceof JSONArray) {
				JSONArray handleArray1 = identity.getJSONArray(handle1);
				for (int k = 0; k < handleArray1.length(); k++) {
					JSONObject handleObject = handleArray1.getJSONObject(k);
					if (handleObject.has("tags")) {
						handleObject.remove("tags");
					}
				}
				identity.put(handle, handleArray);
			}
		}
	}

	/** {@code _withouthandlesattr}: removes every selected handle field and {@code selectedHandles}, if present. */
	private static void applyWithoutHandlesAttr(JSONObject identity) {
		if (identity.has("selectedHandles")) {
			JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
			for (int j = 0; j < selectedHandles.length(); j++) {
				String h = selectedHandles.getString(j);
				if (identity.has(h)) {
					identity.remove(h);
				}
			}
			identity.remove("selectedHandles");
		}
	}

	/**
	 * {@code _withinvaliddemofield}: prefixes the visited handle's value(s) with {@code invalid_}
	 * and renames its {@code selectedHandles} entry at {@code outerIndex} to {@code invalid_<handle>}.
	 */
	private static void applyWithInvalidDemoField(JSONObject identity, JSONArray selectedHandles, String handle,
			int outerIndex) {
		if (identity.has("selectedHandles")) {
			for (int j = 0; j < selectedHandles.length(); j++) {
				if (identity.has(handle)) {
					Object currentValue = identity.get(handle);
					if (currentValue instanceof String) {
						identity.put(handle, "invalid_" + currentValue);
					} else if (currentValue instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray) currentValue;
						for (int k = 0; k < jsonArray.length(); k++) {
							JSONObject obj = jsonArray.getJSONObject(k);
							if (obj.has("value")) {
								obj.put("value", "invalid_" + obj.getString("value"));
							}
						}
						identity.put(handle, jsonArray);
					}
				}
				selectedHandles.put(outerIndex, "invalid_" + handle);
			}
			identity.put("selectedHandles", selectedHandles);
		}
	}

	/** {@code _withoutselectedhandlesandattri}: removes every selected handle field and {@code selectedHandles}. */
	private static void applyWithoutSelectedHandlesAndAttri(JSONObject identity) {
		JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
		for (int j = 0; j < selectedHandles.length(); j++) {
			String h = selectedHandles.getString(j);
			if (identity.has(h)) {
				identity.remove(h);
			}
		}
		identity.remove("selectedHandles");
	}

	/** {@code _withalldemofieldsremoved}: removes the visited handle field, leaving {@code selectedHandles} intact. */
	private static void applyWithAllDemoFieldsRemoved(JSONObject identity, JSONArray selectedHandles, String handle) {
		for (int j = 0; j < selectedHandles.length(); j++) {
			if (identity.has(handle)) {
				identity.remove(handle);
			}
		}
	}

	/** {@code _witharandomnonhandleattr}: appends the first identity attribute not already selected to {@code selectedHandles}. */
	private static void applyWithARandomNonHandleAttr(JSONObject identity, JSONArray selectedHandles) {
		if (identity.has("selectedHandles")) {
			List<String> existingHandles = new ArrayList<>();
			for (int j = 0; j < selectedHandles.length(); j++) {
				existingHandles.add(selectedHandles.getString(j));
			}
			Iterator<String> keys = identity.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (key.equals("selectedHandles")) continue;
				if (!existingHandles.contains(key)) {
					selectedHandles.put(key);
					break;
				}
			}
		}
	}

	/** {@code _updateselectedhandleswithscehmaattrwhichisnothandle}: appends the first unselected string attribute to {@code selectedHandles}. */
	private static void applyUpdateSelectedHandlesWithSchemaAttrWhichIsNotHandle(JSONObject identity,
			JSONArray selectedHandles) {
		Iterator<String> keys = identity.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (!selectedHandles.toList().contains(key) && identity.optString(key) != null
					&& identity.get(key) instanceof String) {
				selectedHandles.put(key);
				break;
			}
		}
	}

	/** {@code _removeselectedhandle_updatephone}: drops {@code selectedHandles} and sets a new random 10-digit phone. */
	private static void applyRemoveSelectedHandleUpdatePhone(JSONObject identity, String phoneFieldName) {
		identity.remove("selectedHandles");
		if (identity.has(phoneFieldName)) {
			identity.put(phoneFieldName, BaseTestCase.generateRandomNumberString(10));
		}
	}

	// ===== Utility =====

	/** {@code _removealltagshandles}: recursively removes every {@code tags} array that is exactly {@code ["handles"]}. */
	private static void removeTagsHandles(JSONObject jsonObj) {
		for (String key : jsonObj.keySet()) {
			Object value = jsonObj.get(key);
			if (value instanceof JSONObject) {
				JSONObject nestedObject = (JSONObject) value;
				if (nestedObject.has("tags")) {
					JSONArray tagsArray = nestedObject.getJSONArray("tags");
					if (tagsArray.length() == 1 && "handles".equals(tagsArray.getString(0))) {
						nestedObject.remove("tags");
					}
				}
				removeTagsHandles(nestedObject);
			} else if (value instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) value;
				for (int i = 0; i < jsonArray.length(); i++) {
					Object arrayElement = jsonArray.get(i);
					if (arrayElement instanceof JSONObject) {
						removeTagsHandles((JSONObject) arrayElement);
					}
				}
			}
		}
	}
}
