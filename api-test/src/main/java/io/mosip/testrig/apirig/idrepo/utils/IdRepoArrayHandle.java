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
	private static String selectedHandlesValue=null;
	public static  String RANDOM_ID = "mosip" + BaseTestCase.generateRandomNumberString(2)
	+ Calendar.getInstance().getTimeInMillis();

	public static String replaceArrayHandleValues(String inputJson, String testCaseName) {
	    JSONObject jsonObj = new JSONObject(inputJson);
	    JSONObject request = jsonObj.getJSONObject("request");
	    JSONObject identity = request.getJSONObject("identity");
	    JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
	    String email = AdminTestUtil.getValueFromAuthActuator("json-property", "emailId");
        String emailResult = email.replaceAll("\\[\"|\"\\]", "");

	    for (int i = 0; i < selectedHandles.length(); i++) {
	        String handle = selectedHandles.getString(i);

	        if (identity.has(handle)) {
	            Object handleObj = identity.get(handle); // Dynamically get the handle object

	            // Check if the handle is an array
	            if (handleObj instanceof JSONArray) {
	                JSONArray handleArray = (JSONArray) handleObj;
	                
	                if (testCaseName.contains("_onlywithtags")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.remove("value");
	                    }
	                } else if (testCaseName.contains("_withouttags")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.remove("tags");
	                    }
	                } else if (testCaseName.contains("_withtagwithoutselectedhandles")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.remove("selectedHandles");
	                    }
	                } else if (testCaseName.contains("_withinvalidtag")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        JSONArray tags = obj.optJSONArray("tags");
	                        if (tags != null) {
	                            for (int k = 0; k < tags.length(); k++) {
	                                String tag = tags.getString(k);
	                                tags.put(k, tag + "_invalid" + "RANDOM_ID");
	                            }
	                            obj.put("tags", tags);
	                        }
	                    }
	                } else if (testCaseName.contains("_withmultiplevalues")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        JSONArray valuesArray = new JSONArray();
	                        valuesArray.put("mosip501724826584965_modified_1");
	                        valuesArray.put("mosip501724826584965_modified_2");
	                        valuesArray.put("mosip501724826584965_modified_3");
	                        obj.put("values", valuesArray);
	                    }
	                } else if (testCaseName.contains("_withmultiplevaluesandwithouttags")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        JSONArray valuesArray = new JSONArray();
	                        valuesArray.put("mosip501724826584965_modified_1");
	                        valuesArray.put("mosip501724826584965_modified_2");
	                        valuesArray.put("mosip501724826584965_modified_3");
	                        obj.put("values", valuesArray);
	                        obj.remove("tags");
	                    }
	                } else if (testCaseName.contains("_withemptyselecthandles")) {
	                    identity.put("selectedHandles", new JSONArray());
	                } else if (testCaseName.contains("_withoutselectedhandles")) {
	                    identity.remove("selectedHandles");
	                    break;
	                } else if (testCaseName.contains("_withmultiplehandleswithoutvalue")) {
	                    String phone = AdminTestUtil.getValueFromAuthActuator("json-property", "phone_number");
	                    String result = phone.replaceAll("\\[\"|\"\\]", "");
	                    boolean containsPhone = false;
	                    for (int j = 0; j < selectedHandles.length(); j++) {
	                        if (result.equalsIgnoreCase(selectedHandles.getString(j))) {
	                            containsPhone = true;
	                            break;
	                        }
	                    }
	                    if (!containsPhone) {
	                        selectedHandles.put(result);
	                        JSONObject phoneEntry = new JSONObject();
	                        phoneEntry.put("value", "$PHONENUMBERFORIDENTITY$");
	                        JSONArray phoneArray = new JSONArray();
	                        phoneArray.put(phoneEntry);
	                        identity.put(result, phoneArray);
	                    }
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.remove("value");
	                    }
	                } else if (testCaseName.contains("_withfunctionalIds") && handle.equals("functionalId")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.remove("tags");
	                    }
	                } else if (testCaseName.contains("_withfunctionalIdsUsedFirstTwoValue") && handle.equals("functionalId")) {
	                    if (handleArray.length() < 3) {
	                        JSONObject secondValue = new JSONObject();
	                        secondValue.put("value", "RANDOM_ID_2" + 12);
	                        secondValue.put("tags", new JSONArray().put("handle"));
	                        JSONObject thirdValue = new JSONObject();
	                        thirdValue.put("value", "RANDOM_ID_2" + 34);
	                        handleArray.put(secondValue);
	                        handleArray.put(thirdValue);
	                    }
	                } else if (testCaseName.contains("_withfunctionalIdsandPhoneWithoutTags")) {
	                    String phone = AdminTestUtil.getValueFromAuthActuator("json-property", "phone_number");
	                    String result = phone.replaceAll("\\[\"|\"\\]", "");
	                    boolean containsPhone = false;
	                    for (int j = 0; j < selectedHandles.length(); j++) {
	                        if (result.equalsIgnoreCase(selectedHandles.getString(j))) {
	                            containsPhone = true;
	                            break;
	                        }
	                    }
	                    if (!containsPhone) {
	                        selectedHandles.put(result);
	                        JSONObject phoneEntry = new JSONObject();
	                        phoneEntry.put("value", "$PHONENUMBERFORIDENTITY$");
	                        JSONArray phoneArray = new JSONArray();
	                        phoneArray.put(phoneEntry);
	                        identity.put(result, phoneArray);
	                    }
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.remove("tags");
	                    }
	                } else if (testCaseName.contains("_withfunctionalIdsUsedFirstTwoValueOutOfFive")) {
	                    String baseValue = "";
	                    if (handleArray.length() > 0) {
	                        baseValue = handleArray.getJSONObject(0).getString("value");
	                    }
	                    for (int j = 0; j < 4; j++) {
	                        JSONObject obj = new JSONObject();
	                        if (j < 1) {
	                            obj.put("value", baseValue + j);
	                            obj.put("tags", new JSONArray().put("handle"));
	                        } else {
	                            obj.put("value", baseValue + j);
	                        }
	                        handleArray.put(obj);
	                    }
	                }
	                //43 in update identity
	                else if (testCaseName.contains("_removeexceptfirsthandle")) {
	                    if (identity.has("selectedHandles")) {

	                        if (selectedHandles.length() > 0) {
//	                            String firstHandleToKeep = selectedHandles.getString(0);

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
	                }
	              //44 in update identity
	                else if (testCaseName.contains("_withinvaliddemofield_inupdate")) {
	                    if (identity.has("selectedHandles")) {

	                        if (selectedHandles.length() > 0) {
//	                            String firstHandleToKeep = selectedHandles.getString(0);

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
	                }
	                //50
	                else if (testCaseName.contains("_withonedemofield")) {
	                    if (identity.has("selectedHandles")) {
	                        String firstHandle = selectedHandles.getString(0);
	                        for (int j = 1; j < selectedHandles.length(); j++) {
	                            if (identity.has(handle)) {
	                                Object handleValue = identity.get(handle);
	                                if (handleValue instanceof JSONArray) {
	                                    identity.remove(handle);
	                                }
	                            }
	                        }
	                        JSONArray newSelectedHandles = new JSONArray();
	                        newSelectedHandles.put(firstHandle);
	                        identity.put("selectedHandles", newSelectedHandles);
	                    }
	                }
	                
	                //82
	                
	                else if (testCaseName.contains("_withcasesensitivehandles")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.put("value", "HANDLES");
	                    }
	                }
	                //77
	                else if (testCaseName.contains("_replaceselectedhandles")) {
	                	identity.put("selectedHandles", new JSONArray().put(emailResult));
	                }
	                //76
	                else if (testCaseName.contains("_onlywithemail")) {
	                	identity.put("selectedHandles", new JSONArray().put(emailResult));
	                }
	                
	                //73
	                else if (testCaseName.contains("_withoutselectedhandlesinidentity")) {
	                	 identity.remove("selectedHandles");
	                }
	              
	                else if (testCaseName.contains("_withdublicatevalue")) {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        if (testCaseName.contains("_save_withdublicatevalue"))
	                        selectedHandlesValue=obj.getString("value");
	                        obj.put("value", selectedHandlesValue);
	                    }
	                }
	                else if (testCaseName.contains("_withmultipledublicatevalue")) {
		                        JSONObject secondValue = new JSONObject();
		                        secondValue.put("value", selectedHandlesValue);
		                        secondValue.put("tags", new JSONArray().put("handle"));
		                        handleArray.put(secondValue);
	                } 
	                else if (testCaseName.contains("_removevalueaddexistingvalue")) {
	                	 for (int j = 0; j < handleArray.length(); j++) {
		                        JSONObject obj = handleArray.getJSONObject(j);
		                        obj.remove("value");
		                        obj.put("value", selectedHandlesValue);
	                	 }
            } 
	                else if (testCaseName.contains("_withselectedhandlephone")) {
	                    if (identity.has("selectedHandles")) {
	                        // Remove "email" and "functionalId", keep only "phone"
	                        JSONArray updatedHandles = new JSONArray();
	                        boolean containsPhone = false;

	                        for (int j = 0; j < selectedHandles.length(); j++) {
	                             handle = selectedHandles.getString(j);
	                            if (handle.equalsIgnoreCase("phone")) {
	                                containsPhone = true;
	                                updatedHandles.put("phone"); // Ensure "phone" is kept
	                            }
	                        }

	                        // Add "phone" if not present
	                        if (!containsPhone) {
	                            updatedHandles.put("phone");
	                        }

	                        // Update the identity with the modified selectedHandles array
	                        identity.put("selectedHandles", updatedHandles);
	                    }else if (testCaseName.contains("_removealltagshandles")) {
		                    removeTagsHandles(jsonObj);
			                   
			                   
		                } else {
	                        // If "selectedHandles" doesn't exist, create it with "phone"
	                        JSONArray newSelectedHandles = new JSONArray();
	                        newSelectedHandles.put("phone");
	                        identity.put("selectedHandles", newSelectedHandles);
	                    }
	                }
	                
	                
	                else {
	                    for (int j = 0; j < handleArray.length(); j++) {
	                        JSONObject obj = handleArray.getJSONObject(j);
	                        obj.put("value", obj.getString("value"));
	                    }
	                }

	                identity.put(handle, handleArray);
	            }
	        }
	    }

	    return jsonObj.toString();
	}
	
	public static String replaceArrayHandleValuesForUpdateIdentity(String inputJson, String testCaseName) {
	    JSONObject jsonObj = new JSONObject(inputJson);
	    JSONObject request = jsonObj.getJSONObject("request");
	    JSONObject identity = request.getJSONObject("identity");
	    JSONArray selectedHandles = identity.getJSONArray("selectedHandles");
	    String phone = AdminTestUtil.getValueFromAuthActuator("json-property", "phone_number");
        String result = phone.replaceAll("\\[\"|\"\\]", "");
        String email = AdminTestUtil.getValueFromAuthActuator("json-property", "emailId");
        String emailResult = email.replaceAll("\\[\"|\"\\]", "");
	    
	   

	    // Iterate over each handle in the selectedHandles array
	    for (int i = 0; i < selectedHandles.length(); i++) {
	        String handle = selectedHandles.getString(i);

	        // Check if the handle exists in identity and if its value is a JSONArray
	        if (identity.has(handle) && identity.get(handle) instanceof JSONArray) {
	            JSONArray handleArray = identity.getJSONArray(handle);

	            if (testCaseName.contains("_withupdatevalues")) {
	                for (int j = 0; j < handleArray.length(); j++) {
	                    JSONObject handleObj = handleArray.getJSONObject(j);
	                    handleObj.put("value", "mosip" + RANDOM_ID + "_" + j);
	                }
	            } else if (testCaseName.contains("_withmultiplevalues")) {
	                for (int j = 0; j < handleArray.length(); j++) {
	                    JSONObject handleObj = handleArray.getJSONObject(j);
	                    JSONArray valuesArray = new JSONArray();
	                    valuesArray.put("mosip501724826584965_modified_1");
	                    valuesArray.put("mosip501724826584965_modified_2");
	                    valuesArray.put("mosip501724826584965_modified_3");
	                    handleObj.put("values", valuesArray);
	                }
	            } else if (testCaseName.contains("_withupdatetags")) {
	                for (int j = 0; j < handleArray.length(); j++) {
	                    JSONObject handleObj = handleArray.getJSONObject(j);
	                    JSONArray tags = handleObj.optJSONArray("tags");
	                    if (tags != null) {
	                        for (int k = 0; k < tags.length(); k++) {
	                            tags.put(k, tags.getString(k) + "_invalid" + RANDOM_ID);
	                        }
	                    }
	                }
	            } else if (testCaseName.contains("_withupdatetagsandhandles")) {
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
	            } else if (testCaseName.contains("_withmultipledemohandles")) {
	                // Handle specific demo handles by checking and adding them to the selectedHandles
	                
	                boolean containsPhone = false;
	                for (int j = 0; j < selectedHandles.length(); j++) {
	                    if (result.equalsIgnoreCase(selectedHandles.getString(j))) {
	                        containsPhone = true;
	                        break;
	                    }
	                }
	                if (!containsPhone) {
	                    selectedHandles.put(result);
	                    JSONObject phoneEntry = new JSONObject();
	                    phoneEntry.put("value", "$PHONENUMBERFORIDENTITY$");
	                    JSONArray phoneArray = new JSONArray();
	                    phoneArray.put(phoneEntry);
	                    identity.put(result, phoneArray);
	                }
	            } else if (testCaseName.contains("_withdeletehandlefromrecord")) {
	                for (int j = 0; j < selectedHandles.length(); j++) {
	                    String handleToDelete = selectedHandles.getString(j);
	                    if (identity.has(handleToDelete)) {
	                        identity.remove(handleToDelete);
	                    }
	                }
	                identity.remove("selectedHandles");
	            } else if (testCaseName.contains("_withupdatedselectedhandle")) {
	                String firstHandle = selectedHandles.getString(0);
	                String updatedHandle = firstHandle + RANDOM_ID;
	                selectedHandles.put(0, updatedHandle);
	            } else if (testCaseName.contains("_withupdatedselectedhandleanddemo")) {
	                if (selectedHandles.length() > 0) {
	                    String originalHandle = selectedHandles.getString(0);
	                    String updatedHandle = originalHandle + RANDOM_ID;
	                    selectedHandles.put(0, updatedHandle);
	                    if (identity.has(originalHandle)) {
	                        JSONArray originalHandleArray = identity.getJSONArray(originalHandle);
	                        for (int J = 0; J < originalHandleArray.length(); J++) {
	                            JSONObject handleObject = originalHandleArray.getJSONObject(i);
	                            String originalValue = handleObject.optString("value", "");
	                            handleObject.put("value", originalValue + RANDOM_ID);
	                            originalHandleArray.put(J, handleObject);
	                        }
	                        identity.remove(originalHandle);
	                        identity.put(updatedHandle, originalHandleArray);
	                    }
	                }
	            } else if (testCaseName.contains("_withupdatedselectedhandleandfirstattribute")) {
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
	                                    String originalValue = handleObject.getString("value");
	                                    handleObject.put("value", originalValue + "123");
	                                }
	                                if (handleObject.has("tags")) {
	                                    JSONArray tagsArray = handleObject.getJSONArray("tags");
	                                    for (int k = 0; k < tagsArray.length(); k++) {
	                                        String tag = tagsArray.getString(k);
	                                        tagsArray.put(k, tag + "123");
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
	            else if (testCaseName.contains("_withremovedtaggedattribute")) {
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
	            else if (testCaseName.contains("_withemptyhandles")) {
	                    identity.remove("selectedHandles");
	                 
	            }
	            
	            else if (testCaseName.contains("_withouthandlesattr")) {
	                if (identity.has("selectedHandles")) {
	                     selectedHandles = identity.getJSONArray("selectedHandles");
	                    for (int j = 0; j < selectedHandles.length(); j++) {
	                         handle = selectedHandles.getString(j);
	                        if (identity.has(handle)) {
	                            identity.remove(handle);
	                        }
	                    }
	                    identity.remove("selectedHandles");
	                }
	            }
	            
	            //44
	            else if (testCaseName.contains("_withinvaliddemofield")) {
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
	                        selectedHandles.put(i, "invalid_" + handle);
	                    }
	                    identity.put("selectedHandles", selectedHandles);
	                }
	            }
	            //49
	            else if (testCaseName.contains("_withoutselectedhandlesandattri")) {

	                for (int j = 0; j < selectedHandles.length(); j++) {

	                    if (identity.has(handle)) {
	                        identity.remove(handle);
	                    }
	                }

	                identity.remove("selectedHandles");
	            }
	            
	            else if (testCaseName.contains("_withalldemofieldsremoved")) {

	                    for (int j = 0; j < selectedHandles.length(); j++) {
	                        if (identity.has(handle)) {
	                            identity.remove(handle);
	                        }
	                    }
	            }
	            
	            else if (testCaseName.contains("_withemptyselectedhandle")) {
	                if (identity.has("selectedHandles")) {
	                    identity.put("selectedHandles", new JSONArray());
	                }
	            }
	            
	            
	            else if (testCaseName.contains("_witharandomnonhandleattr")) {
	                if (identity.has("selectedHandles")) {
	                    List<String> existingHandles = new ArrayList<>();
	                    for (int j = 0; j < selectedHandles.length(); j++) {
	                        existingHandles.add(selectedHandles.getString(j));
	                    }
	                    Iterator<String> keys = identity.keys();
	                    while (keys.hasNext()) {
	                        String key = keys.next();
	                        if (key.equals("selectedHandles")) {
	                            continue;
	                        }
	                        if (!existingHandles.contains(key)) {
	                            selectedHandles.put(key);
	                            break; 
	                        }
	                    }
	                }
	            }
	            
	            else if (testCaseName.contains("_updateselectedhandleswithinvalid")) {
	            	JSONArray updatedHandlesArray = new JSONArray();
	                updatedHandlesArray.put("invalidscehema123");
	                identity.put("selectedHandles", updatedHandlesArray);
	            }
	            
	            else if (testCaseName.contains("_withinvaliddhandle")) {
	            	    selectedHandles.put("newFieldHandle");
	            }
	            
	            else if (testCaseName.contains("_updateselectedhandleswithscehmaattrwhichisnothandle")) {
	                Iterator<String> keys = identity.keys();
	                while (keys.hasNext()) {
	                    String key = keys.next();
	                    if (!selectedHandles.toList().contains(key) && identity.optString(key) != null && identity.get(key) instanceof String) {
	                        selectedHandles.put(key); 
	                        break; 
	                    }
	                }
	            }
	            
	            else if (testCaseName.contains("_removeselectedhandle_updatephone")) {
	                if (identity.has("selectedHandles")) {
	                    identity.remove("selectedHandles");
	                }

	                if (identity.has(result)) {
	                    identity.put(result, BaseTestCase.generateRandomNumberString(10));
	                }
	            }
	            
	            else if (testCaseName.contains("_withupdatedhandlewhichisnotinschema")) {
	            	JSONArray newSelectedHandles = new JSONArray();
	                newSelectedHandles.put("invalid12@@");
	                identity.put("selectedHandles", newSelectedHandles);
	            }
	            
	            else if (testCaseName.contains("_replaceselectedhandles")) {
                	identity.put("selectedHandles", new JSONArray().put(result));
                }
	            
	            else if (testCaseName.contains("_updatewithphoneemail")) {
	                JSONArray updatedHandles = new JSONArray();
	                updatedHandles.put(emailResult);
	                updatedHandles.put(result);
	                
	                identity.put("selectedHandles", updatedHandles);
	            }
	            else if (testCaseName.contains("_withusedphone")) {
	                if (identity.has(result)) {
	                    identity.put(result, "$ID:AddIdentity_array_handle_value_smoke_Pos_withphonenumber_PHONE$" );
	                }
	            }
	            else if (testCaseName.contains("_withphonevalue")) {
	                if (identity.has(result)) {
	                    identity.put(result, "$ID:AddIdentity_array_handle_value_smoke_Pos_withselectedhandlephone_PHONE$" );
	                }
	            }
	            else if (testCaseName.contains("_removeselectedhandlesandupdateemail")) {
	            	 if (identity.has("selectedHandles")) {
		                    identity.remove("selectedHandles");
		                }
	            	if (identity.has(emailResult)) {
	                    identity.put(emailResult, "$ID:AddIdentity_array_handle_value_update_smoke_Pos_withselectedhandlephone_EMAIL$" );
	                }                 
            }

	            identity.put(handle, handleArray);
	        }
	    }

	    // Return the modified JSON as a string
	    return jsonObj.toString();
	}
	
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
	            removeTagsHandles(nestedObject);  // Recursively call for deeper levels
	        } else if (value instanceof JSONArray) {
	            JSONArray jsonArray = (JSONArray) value;
	            for (int i = 0; i < jsonArray.length(); i++) {
	                Object arrayElement = jsonArray.get(i);
	                if (arrayElement instanceof JSONObject) {
	                    removeTagsHandles((JSONObject) arrayElement);  // Recursively handle each element
	                }
	            }
	        }
	    }
	}
}
