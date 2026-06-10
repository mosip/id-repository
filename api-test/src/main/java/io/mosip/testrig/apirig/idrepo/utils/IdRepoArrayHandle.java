package io.mosip.testrig.apirig.idrepo.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.AdminTestUtil;

public class IdRepoArrayHandle {
	private static String selectedHandlesValue = null;
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
		    selectedHandles.put("fullName");
		    identity.put("selectedHandles", selectedHandles);
		    return jsonObj.toString();
		}
		if (testCaseName.contains("_withUnknownSelectedHandles")) {
		    JSONArray selectedHandles = new JSONArray();
		    selectedHandles.put("passport");
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
		} else if (testCaseName.contains("_withfunctionalIdsUsedFirstTwoValueOutOfFive") && handle.equals("functionalId")) {
			applyFunctionalIdsUsedFirstTwoValueOutOfFive(handleArray);
		} else if (testCaseName.contains("_withfunctionalIdsUsedFirstTwoValue") && handle.equals("functionalId")) {
			applyFunctionalIdsUsedFirstTwoValue(handleArray);
		} else if (testCaseName.contains("_withfunctionalIdsandPhoneWithoutTags")) {
			applyFunctionalIdsAndPhoneWithoutTags(identity, selectedHandles, handleArray);
		} else if (testCaseName.contains("_withfunctionalIds") && handle.equals("functionalId")) {
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
			applyWithMultipleDuplicateValue(handleArray);
		} else if (testCaseName.contains("_withdublicatevalue")) {
			applyWithDuplicateValue(testCaseName, handleArray);
		} else if (testCaseName.contains("_removevalueaddexistingvalue")) {
			applyRemoveValueAddExistingValue(handleArray);
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
		} else if (testCaseName.contains("_withusedphone")) {
			if (identity.has(phoneFieldName)) {
				identity.put(phoneFieldName,
						"$ID:AddIdentity_array_handle_value_smoke_Pos_withphonenumber_PHONE$");
			}
		} else if (testCaseName.contains("_withphonevalue")) {
			if (identity.has(phoneFieldName)) {
				identity.put(phoneFieldName,
						"$ID:AddIdentity_array_handle_value_smoke_Pos_withselectedhandlephone_PHONE$");
			}
		}
	}

	// ===== Shared Helpers =====

	private static String resolvePhoneFieldName() {
		String phone = AdminTestUtil.getValueFromAuthActuator("json-property", "phone_number");
		return phone.replaceAll("\\[\"|\"]", "");
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
	
	private static void applyFunctionalIdsUsedFirstTwoValueOutOfFive(JSONArray handleArray) {
		String baseValue = handleArray.length() > 0 ? handleArray.getJSONObject(0).getString("value") : "";
		for (int j = 0; j < 4; j++) {
			JSONObject obj = new JSONObject();
			obj.put("value", baseValue + j);
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
	    handleEmail.put("value", "handle_" + BaseTestCase.generateRandomNumberString(2) + "@mail.com");
	    handleEmail.put("tags", new JSONArray().put("handle"));
	    JSONObject normalEmail = new JSONObject();
	    normalEmail.put("value", "notification_" + BaseTestCase.generateRandomNumberString(2) + "@mail.com");
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
			emailObj.put("value", "email" + i + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");

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
	    email1.put("value", "user1_" + BaseTestCase.generateRandomNumberString(2) + "@gmail.com");
	    email1.put("tags", new JSONArray().put("handle"));

	    JSONObject email2 = new JSONObject();
	    email2.put("value", "user2_" + BaseTestCase.generateRandomNumberString(2) + "@yahoo.com");
	    email2.put("tags", new JSONArray().put("handle"));

	    JSONObject email3 = new JSONObject();
	    email3.put("value", "user3_" + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");
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
		email1.put("value", "email01_" + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");
		JSONObject email2 = new JSONObject();
		email2.put("value", "email02_" + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");
		handleArray.put(email1);
		handleArray.put(email2);
	}
	
	private static void applyMultipleEmailHandlesTagged(JSONArray handleArray) {
	    // Clear existing email entries
	    while (handleArray.length() > 0) {
	        handleArray.remove(0);
	    }
	    JSONObject email1 = new JSONObject();
	    email1.put("value", "email03_" + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");
	    email1.put("tags", new JSONArray().put("handle"));
	    JSONObject email2 = new JSONObject();
	    email2.put("value", "email04_" + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");
	    email2.put("tags", new JSONArray().put("handle"));
	    handleArray.put(email1);
	    handleArray.put(email2);
	}
	
	private static void applyRetainTaggedEmail(JSONArray handleArray) {

	    JSONObject existingEmail = handleArray.getJSONObject(0);

	    JSONObject newEmail = new JSONObject();
	    newEmail.put("value",
	        "newemail_" + BaseTestCase.generateRandomNumberString(2) + "@mosip.net");

	    handleArray.put(newEmail);
	}

	private static void applyFunctionalIdsUsedFirstTwoValue(JSONArray handleArray) {
		if (handleArray.length() < 3) {
			JSONObject secondValue = new JSONObject();
			secondValue.put("value", "RANDOM_ID_2" + 12);
			secondValue.put("tags", new JSONArray().put("handle"));
			JSONObject thirdValue = new JSONObject();
			thirdValue.put("value", "RANDOM_ID_2" + 34);
			handleArray.put(secondValue);
			handleArray.put(thirdValue);
		}
	}

	private static void applyFunctionalIdsAndPhoneWithoutTags(JSONObject identity, JSONArray selectedHandles,
			JSONArray handleArray) {
		String phoneFieldName = resolvePhoneFieldName();
		boolean containsPhone = false;
		for (int j = 0; j < selectedHandles.length(); j++) {
			if (phoneFieldName.equalsIgnoreCase(selectedHandles.getString(j))) {
				containsPhone = true;
				break;
			}
		}
		if (!containsPhone) {
			selectedHandles.put(phoneFieldName);
			JSONObject phoneEntry = new JSONObject();
			phoneEntry.put("value", "$PHONENUMBERFORIDENTITY$");
			identity.put(phoneFieldName, new JSONArray().put(phoneEntry));
		}
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
		JSONArray updatedHandles = new JSONArray();
		boolean containsPhone = false;
		if (identity.has("selectedHandles")) {
			JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
			for (int j = 0; j < selectedHandles.length(); j++) {
				String h = selectedHandles.getString(j);
				if (h.equalsIgnoreCase("phone")) {
					containsPhone = true;
					updatedHandles.put("phone");
				}
			}
		}
		if (!containsPhone) {
			updatedHandles.put("phone");
		}
		identity.put("selectedHandles", updatedHandles);
	}

	private static void applyWithMultipleDuplicateValue(JSONArray handleArray) {
		JSONObject secondValue = new JSONObject();
		secondValue.put("value", selectedHandlesValue);
		secondValue.put("tags", new JSONArray().put("handle"));
		handleArray.put(secondValue);
	}

	private static void applyWithDuplicateValue(String testCaseName, JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject obj = handleArray.getJSONObject(j);
			if (testCaseName.contains("_save_withdublicatevalue")) {
				selectedHandlesValue = obj.getString("value");
			}
			obj.put("value", selectedHandlesValue);
		}
	}

	private static void applyRemoveValueAddExistingValue(JSONArray handleArray) {
		for (int j = 0; j < handleArray.length(); j++) {
			JSONObject obj = handleArray.getJSONObject(j);
			obj.remove("value");
			obj.put("value", selectedHandlesValue);
		}
	}

	// ===== UpdateIdentity Private Handlers =====

	private static void applyWithUpdateValues(JSONArray handleArray, String handle, String emailFieldName) {
		for (int j = 0; j < handleArray.length(); j++) {
			if (handle.equals(emailFieldName)) {
				handleArray.getJSONObject(j).put("value", "mosip_update_" + RANDOM_ID + "@mosip.net");
			} else {
				handleArray.getJSONObject(j).put("value", "mosip" + RANDOM_ID + "_" + j);
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
		Iterator<String> keys = identity.keys();
		if (keys.hasNext()) {
			String firstKey = keys.next();
			if (!firstKey.equals("selectedHandles")) {
				selectedHandles.put(0, firstKey);
				if (identity.has(firstKey)) {
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
				}
			}
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
