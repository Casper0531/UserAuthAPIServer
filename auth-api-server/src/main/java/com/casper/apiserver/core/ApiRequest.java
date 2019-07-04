package com.casper.apiserver.core;

import com.casper.apiserver.service.RequestParamException;
import com.casper.apiserver.service.ServiceException;
import com.google.gson.JsonObject;

/*
 * API 서비스 클래스는 ApiRequest인터페이스와 ApiRequestTemplate 추상 클래스를 사용한 템플릿 메서드 패턴을 구현 
 * 
 */
public interface ApiRequest {

	// API를 호출하는 HTTP의 요청의 파라미터값이 입력되었는지 검
	public void requestParamValidation() throws RequestParamException;
	
	// 각 API서비스에 따른 개별 구현 메소드
	public void service() throws ServiceException;
	
	// 서비스 API의 호출 시작 메서드 
	public void executeService();
	
	// api서비스의 처리 결과를 조회하는 메서드 
	public JsonObject getApiResult();
}
