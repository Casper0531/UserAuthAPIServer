package com.casper.apiserver;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/*
 * ComponentScan : Spring이 컴포넌트를 검색할 위치를 지정. ApiServer클래스의 위치와 토큰 발급 및 사용자 정보 조회 클래스의 위치 지정 
 * PropertySource : API서버의 설정 프로퍼티 파일인 api-server.properties의 위치를 지정 
 */
@Configuration
@ImportResource("classpath:spring/hsqlApplicationContext.xml")
@ComponentScan("com.casper.apiserver, com.casper.apiserver.service, com.casper.apiserver.core")
@PropertySource("classpath:api-server.properties")
public class ApiServerConfig {
	
	@Value("${boss.thread.count}")
	private int bossThreadCount;
	
	@Value("${worker.thread.count}")
	private int workerThreadCount;
	
	@Value("${tcp.port}")
	private int tcpPort;
	
	@Value("${https.port}")
	private int httpsPort;
	
	@Bean(name= "bossThreadCount")
	public int getBossThreadCount() {
		return bossThreadCount;
	}
	
	@Bean(name = "workerThreadCount")
	public int getWorkerThreadCount() {
		return workerThreadCount;
	}
	
	public int getTcpPort() {
		return tcpPort;
	}
	
	public int getHttpsPort() {
		return httpsPort;
	}
	
	@Bean(name = "tcpSocketAddress")
	public InetSocketAddress tcpPort() {
		return new InetSocketAddress(tcpPort);
	}
	
	@Bean(name = "httpsSocketAddress")
	public InetSocketAddress httpsPort() {
		return new InetSocketAddress(httpsPort);
	}
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlacehodlerConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
}
