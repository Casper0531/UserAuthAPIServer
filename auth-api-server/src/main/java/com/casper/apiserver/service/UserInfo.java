package com.casper.apiserver.service;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.casper.apiserver.core.ApiRequestTemplate;

/*
 * 사용자 번호(userNo) 조회 API : 사용자의 email을 인자로 저장된 사용자의 번호를 apiResult로 리턴
 * @Service : Spring Context가 UserInfo 클래스를 생성할 수 있도록 해줌. getBean의 호출 인자
 * @Scope : Spring Context가 객체를 싱글톤으로 생성할지 유무 지정. prototype은 매번 새로 생성(<-> 싱글톤_
 */
@Service("users")
@Scope("prototype")
public class UserInfo extends ApiRequestTemplate {

	// HSQLDB와 Mybatis스프링 설정을 기초로 sqlSession객체 할당 
	@Autowired
	private SqlSession sqlSession;
	
	// 요청의 파라미터값을 인자로 객체 생성
	public UserInfo(Map<String, String> reqData) {
		super(reqData);
	}

	// 필요 파라미터가 정상적으로 입력되었는지 확인
    @Override
    public void requestParamValidation() throws RequestParamException {
        if (StringUtils.isEmpty(this.reqData.get("email"))) {
            throw new RequestParamException("email이 없습니다.");
        }
    }

	@Override
	public void service() throws ServiceException {
		// users.xml에 정의된 userInfoByEmail 쿼리 실행. 쿼리 입력 파라미터는 HTTP요청에서 입력된 필드와 매칭
		Map<String, Object> result = 
				sqlSession.selectOne("users.userInfoByEmail", this.reqData);
		
		if (result != null) {
			String userNo = String.valueOf(result.get("USERNO"));
			
			this.apiResult.addProperty("resultCode", 200);
			this.apiResult.addProperty("message", "Success");
			this.apiResult.addProperty("userNo", userNo);
		}
		else {
			this.apiResult.addProperty("resultCode", "404");
			this.apiResult.addProperty("message", "Fail");
		}
		
	}
}
