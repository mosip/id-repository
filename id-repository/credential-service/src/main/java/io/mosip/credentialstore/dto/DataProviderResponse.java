package io.mosip.credentialstore.dto;

import java.time.LocalDateTime;

import org.json.simple.JSONObject;

public class DataProviderResponse {
	private String  credentialId;
	private LocalDateTime  issuanceDate;

	private JSONObject JSON;

	public String getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}

	public LocalDateTime getIssuanceDate() {
		return issuanceDate;
	}

	public void setIssuanceDate(LocalDateTime issuanceDate) {
		this.issuanceDate = issuanceDate;
	}

	public JSONObject getJSON() {
		return JSON;
	}

	public void setJSON(JSONObject jSON) {
		JSON = jSON;
	}

}
