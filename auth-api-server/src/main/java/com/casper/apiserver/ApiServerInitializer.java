package com.casper.apiserver;

import com.casper.apiserver.core.ApiRequestParser;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class ApiServerInitializer extends ChannelInitializer<SocketChannel>{

	// 채널 보안을 위한 SSL Context 객체. 아직 미구현 
	private final SslContext sslCtx;
	
	
	public ApiServerInitializer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}


	/*
	 * Data Reception case Handler Flow (2-3-4-5)
	 * Data Transmition case Handler Flow (6-5-4)
	 */
	@Override
	protected void initChannel(SocketChannel ch){
		// 클라이언트 채널로 수신된 Http데이터를 처리하기 위한 채널 파이프라인 객체 
		ChannelPipeline p = ch.pipeline();
		
		if (sslCtx != null) {
			p.addLast(sslCtx.newHandler(ch.alloc()));
		}
		// 클라이언트가 전송한 HTTP프로토콜 데이터를 네티의 바이트버퍼로 변환 
		p.addLast(new HttpRequestDecoder());
		// HTTP에서 발생하는 메시지 파편화 처리 디코더. 데이터가 나뉘어져 전송되는 경우 합쳐주는 역할 
		p.addLast(new HttpObjectAggregator(65536));
		p.addLast(new HttpResponseEncoder());
		// HTTP본문 데이터를 gzip압축을 사용하여 압축/해제 수행
		p.addLast(new HttpContentCompressor());
		// 클라이언트로부터 수신된 http데이터에서 헤더,데이터값을 추출하여 토큰 발급 등의 업무 처리 클래스(api server controller)
		p.addLast(new ApiRequestParser());
	}

}
