package com.casper.apiserver.core;

import com.google.gson.JsonObject;

public interface ApiRequest {

	public void executeService();
	
	public JsonObject getApiResult();
}
