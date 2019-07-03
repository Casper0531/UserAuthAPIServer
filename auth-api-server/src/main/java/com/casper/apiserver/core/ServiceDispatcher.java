package com.casper.apiserver.core;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ServiceDispatcher {

	private static ApplicationContext springContext;
	
	// 직접 할당할 수 없으므로 Autowired annotation을 사용한 간접할당
	@Autowired
	public void init(ApplicationContext springContext) {
		ServiceDispatcher.springContext = springContext;
	}
	
	protected Logger logger = LogManager.getLogger(this.getClass());
	
	public static ApiRequest dispatch(Map<String, String> requestMap) {
		String serviceUri = requestMap.get("REQUEST_URI");
		String beanName = null;
		
		if (serviceUri == null) {
			beanName = "notFound";
		}
		
		if (serviceUri.startsWith("/tokens")) {
			String httpMethod = requestMap.get("REQUEST_METHOD");
			
			switch(httpMethod) {
			case "POST":
				beanName = "tokenIssue";
				break;
			case "DELETE":
				beanName = "tokenExpire";
				break;
			case "GET":
				beanName = "tokenVerify";
				break;
			default:
				beanName = "notFound";
				break;
			}
		}
		else if(serviceUri.startsWith("/users")) {
			beanName = "users";
		}
		else {
			beanName = "notFound";
		}
		
		ApiRequest service = null;
		
		try {
			service = (ApiRequest)springContext.getBean(beanName, requestMap);
		}
		catch(Exception e) {
			e.printStackTrace();
			// 문제 발생시 기본 API서비스 클래스 생성
			service = (ApiRequest)springContext.getBean("notFound", requestMap);
		}
		
		return service;
	}
}
