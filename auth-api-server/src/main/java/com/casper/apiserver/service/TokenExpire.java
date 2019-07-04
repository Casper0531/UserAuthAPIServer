package com.casper.apiserver.service;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.casper.apiserver.core.ApiRequestTemplate;
import com.casper.apiserver.core.JedisHelper;

import redis.clients.jedis.Jedis;

@Service("tokenExpier")
@Scope("prototype")
public class TokenExpire extends ApiRequestTemplate {
	private static final JedisHelper helper = JedisHelper.getInstance();
	
	public TokenExpire(Map<String, String> reqData) {
		super(reqData);
	}
	
	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(this.reqData.get("token"))) {
			throw new RequestParamException("Token이 없습니다");
		}

	}

	@Override
	public void service() throws ServiceException {
		Jedis jedis = null;
		
		try {
			// token 삭제
			jedis = helper.getConnection();
			long result = jedis.del(this.reqData.get("token"));
			System.out.println(result);
			
			// helper
			this.apiResult.addProperty("resultCode", "200");
			this.apiResult.addProperty("message", "Success");
			this.apiResult.addProperty("token", this.reqData.get("token"));
		}
		catch (Exception e) {
			helper.returnResource(jedis);
		}

	}

}
