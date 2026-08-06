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

public class IdRepoArrayHandle {
	/** Handle values captured from a "_save_" test's identity, replayed to trigger IDR-IDC-014. */
	private static final Map<String, String> savedHandleValues = new LinkedHashMap<>();
	public static String RANDOM_ID = "mosip" + BaseTestCase.generateRandomNumberString(2)
			+ Calendar.getInstance().getTimeInMillis();

	// ===== Public Entry Points =====

	public static String replaceArrayHandleValues(String inputJson, String testCaseName) {
		JSONObject jsonObj = new JSONObject(inputJson);
		JSONObject request = jsonObj.getJSONObject("request");
		JSONObject identity = request.getJSONObject("identity");
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
		// Replays only the previously saved email value (not other saved handles) onto a brand-new
		// identity, so a still-active handle on another field (e.g. phone) on the original identity
		// cannot cause an unrelated IDR-IDC-014 collision here.
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

	public static String replaceArrayHandleValuesForUpdateIdentity(String inputJson, String testCaseName) {
		JSONObject jsonObj = new JSONObject(inputJson);
		JSONObject request = jsonObj.getJSONObject("request");
		JSONObject identity = request.getJSONObject("identity");
		String phoneFieldName = resolvePhoneFieldName();
		String emailFieldName = resolveEmailFieldName();

		// Handle cases that exit early or modify identity structure globally.
		// More-specific patterns checked before substrings they contain.
		if (testCaseName.contains("_withdeletehandlefromrecord")) {
			applyDeleteHandleFromRecord(identity);
			return jsonObj.toString();
		}
		// Removes only the email handle (value + selectedHandles entry), leaving any other handle
		// (e.g. phone) untouched, so a later identity can safely reuse just the freed email value.
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
		} else if (testCaseName.contains("_RetainTaggedEmail")) {
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

	/** Replays only the saved email value onto this identity - used to prove a handle value becomes
	 * reusable once its original holder's association with it (and only it) is removed. */
	private static void applySavedEmailValueOnly(JSONObject identity) {
		String emailField = resolveEmailFieldName();
		String savedEmail = savedHandleValues.get(emailField);
		if (savedEmail != null && identity.has(emailField)) {
			writeHandleValue(identity, emailField, savedEmail);
		}
	}

	private static String resolveEmailFieldName() {
		String email = AdminTestUtil.getValueFromAuthActuator("json-property", "emailId");
		return email.replaceAll("\\[\"|\"]", "");
	}

	private static void putMultipleValues(JSONArray handleArray) {
		JSONArray valuesArray = new JSONArray();
		valuesArray.put("mosip501724826584965_modified_1");
		valuesArray.put("mosip501724826584965_modified_2");
		valuesArray.put("mosip501724826584965_modified_3");
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).put("values", valuesArray);
		}
	}

	/** Appends two more untagged, validator-satisfying values to an existing array-typed handle
	 * (field-agnostic — targets whichever handle IdRepoUtil.resolveHandleOfType("array") resolves to).
	 * Used to prove that handle status is driven by selectedHandles membership, not per-item tags. */
	private static void applyAppendUntaggedValues(JSONArray handleArray, String handle) {
		JSONObject second = new JSONObject();
		second.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
		JSONObject third = new JSONObject();
		third.put("value", IdRepoUtil.generateSchemaFieldValue(handle));
		handleArray.put(second);
		handleArray.put(third);
	}

	// ===== AddIdentity Private Handlers =====

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

	private static void applyOnlyWithTags(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("value");
		}
	}

	private static void applyWithoutTags(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("tags");
		}
	}

	private static void applyWithTagWithoutSelectedHandles(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("selectedHandles");
		}
	}

	private static void applyWithInvalidTag(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject obj = handleArray.getJSONObject(j);
			JSONArray tags = obj.optJSONArray("tags");
			if (tags != null) {
				for (int k = 0; k < tags.length(); k++) {
					tags.put(k, tags.getString(k) + "_invalidRANDOM_ID");
				}
				obj.put("tags", tags);
			}
		}
	}

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
	
	private static void applyEmailHandleAndNotification(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject emailObj = handleArray.getJSONObject(j);
	        emailObj.put("tags",
	                new JSONArray()
	                        .put("handle")
	                        .put("notification"));
	    }
	}
	
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
	
	private static void applySelectedHandlesOrderVariation(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject emailObj = handleArray.getJSONObject(j);
	        JSONArray tags = new JSONArray();
	        tags.put("notification");
	        tags.put("handle");
	        emailObj.put("tags", tags);
	    }
	}
	
	private static void applyOnlyNotificationTag(JSONArray handleArray) {
	    for (int j = 0; j < handleArray.length(); j++) {
	        JSONObject emailObj = handleArray.getJSONObject(j);
	        JSONArray tags = new JSONArray();
	        tags.put("notification");
	        emailObj.put("tags", tags);
	    }
	}
	
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
	
	private static void applyRetainTaggedEmail(JSONArray handleArray) {

	    JSONObject existingEmail = handleArray.getJSONObject(0);

	    JSONObject newEmail = new JSONObject();
	    newEmail.put("value",
	        "newemail_" + BaseTestCase.generateRandomNumberString(6) + "@mosip.net");

	    handleArray.put(newEmail);
	}

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

	private static void applyFunctionalIdsAndPhoneWithoutTags(JSONArray handleArray) {
		// Gated [functionalId, phone]; both are already handles in the body — just strip the tags.
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("tags");
		}
	}

	private static void applyFunctionalIds(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			handleArray.getJSONObject(j).remove("tags");
		}
	}

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

	private static void applyWithSelectedHandlePhone(JSONObject identity) {
		// Gated [phone]; reduce selectedHandles to just phone (resolved name, not a literal).
		identity.put("selectedHandles", new JSONArray().put(resolvePhoneFieldName()));
	}

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

	private static void applyWithUpdatedSelectedHandle(JSONArray selectedHandles) {
		String firstHandle = selectedHandles.getString(0);
		selectedHandles.put(0, firstHandle + RANDOM_ID);
	}

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

	private static void applyWithAllDemoFieldsRemoved(JSONObject identity, JSONArray selectedHandles, String handle) {
		for (int j = 0; j < selectedHandles.length(); j++) {
			if (identity.has(handle)) {
				identity.remove(handle);
			}
		}
	}

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

	private static void applyRemoveSelectedHandleUpdatePhone(JSONObject identity, String phoneFieldName) {
		identity.remove("selectedHandles");
		if (identity.has(phoneFieldName)) {
			identity.put(phoneFieldName, BaseTestCase.generateRandomNumberString(10));
		}
	}

	// ===== Utility =====

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
