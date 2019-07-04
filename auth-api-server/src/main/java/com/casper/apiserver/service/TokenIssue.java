package com.casper.apiserver.service;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.casper.apiserver.core.ApiRequestTemplate;
import com.casper.apiserver.core.JedisHelper;
import com.casper.apiserver.core.KeyMaker;
import com.casper.apiserver.service.dao.TokenKey;
import com.google.gson.JsonObject;

import redis.clients.jedis.Jedis;

/*
 * 인증토큰발급 API : 입력으로 사용자번호와 패스워드를 받아 db에 저장된 사용자 정보와 비교한 뒤 토큰을 발급하여
 * 응답코드와 응답 메시지를 포함하여 돌려준
 * 
 */
@Service("tokenIssue")
@Scope("prototype")
public class TokenIssue extends ApiRequestTemplate {

	// Redis에 접근하기 위한 Jedis 헬퍼 클래스
	private static final JedisHelper helper = JedisHelper.getInstance();
	
	@Autowired
	private SqlSession sqlSession;
	
	public TokenIssue(Map<String, String> reqData) {
		super(reqData);
	}
	
	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(this.reqData.get("userNo"))) {
			throw new RequestParamException("userNo가 없습니다");
		}
		
		if (StringUtils.isEmpty(this.reqData.get("password"))) {
			throw new RequestParamException("password가 없습니다");
		}
	}

	@Override
	public void service() throws ServiceException {
		Jedis jedis = null;
		
		try {
			Map<String, Object> result = sqlSession.selectOne("users.userInfoByPassword", this.reqData);
			
			if (result != null) {
				final long threeHour = 60 * 60 * 3;
				long issueDate = System.currentTimeMillis();
				String email = String.valueOf(result.get("USERID"));
				
				JsonObject token = new JsonObject();
				token.addProperty("issueDate", issueDate);
				token.addProperty("expireDate", issueDate + threeHour);
				token.addProperty("email", email);
				token.addProperty("userNo", reqData.get("userNo"));
				
				// TOKEN 저장
				// 발급된 토큰을 레디스에 저장하고 조회하고자 KeyMaker 인터페이스를 사용
				KeyMaker tokenKey = new TokenKey(email,issueDate);
				jedis = helper.getConnection();
				// jedis의 setex메소드를 사용하여 지정된 시간(3시간) 이후에 데이터를 자동으로 삭제
				jedis.setex(tokenKey.getKey(), 60 * 60 * 3, token.toString());
				
				// helper
				this.apiResult.addProperty("resultCode", "200");
				this.apiResult.addProperty("message", "Success");
				this.apiResult.addProperty("token", tokenKey.getKey());
			}
			else {
				// 데이터 없음
				this.apiResult.addProperty("resultCode", "404");
			}
			
			helper.returnResource(jedis);
	
		}
		catch (Exception e) {
			helper.returnResource(jedis);
		}
	}

}
