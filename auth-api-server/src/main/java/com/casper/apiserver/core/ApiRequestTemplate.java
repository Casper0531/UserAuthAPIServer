package com.casper.apiserver.core;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.casper.apiserver.service.RequestParamException;
import com.casper.apiserver.service.ServiceException;
import com.google.gson.JsonObject;

public abstract class ApiRequestTemplate implements ApiRequest{
	
	protected Logger logger;
	
	protected Map<String, String> reqData;
	
	protected JsonObject apiResult;
	
	public ApiRequestTemplate(Map<String, String> reqData) {
		this.logger = LogManager.getLogger(this.getClass());
		this.apiResult = new JsonObject();
		this.reqData = reqData;
		
		logger.info("request data : " + this.reqData);
	}
	
	public void executeService() {
		try {
			// 각각의 구현클래스에 맞는 검증 필요 
			this.requestParamValidation();
			
			this.service();
		}
		catch(RequestParamException e) {
			logger.error(e);
			this.apiResult.addProperty("resultCode", "405");
		}
		catch(ServiceException e) {
			logger.error(e);
			this.apiResult.addProperty("resultCode", "501");
		}
	}
	
	public JsonObject getApiResult() {
		return this.apiResult;
	}
}
