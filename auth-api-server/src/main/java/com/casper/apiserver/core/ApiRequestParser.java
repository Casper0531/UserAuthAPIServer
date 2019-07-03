package com.casper.apiserver.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

/*
 * 디코딩이 완료된 FullHttpMessage객체의 값을 추출하고
 * 요청 종류에 따라 적당한 API 처리 클래스를 실행 및 처리 결과를 apiResult변수에 저장
 * channelRead0이벤트가 정상적으로 실행된다면 API호출에 필요한 모든 값을 reqData 저장 및 처리 클래스로 전달 
 * 
 */
// 채널 파이프라인의 모든 디코더를 거치고 난 뒤에 호출 되는 이벤트핸들러 (FullHttpMessage msg는 인터페이스가 구현된 HTTP프로토콜의 모든 데이터가 포함된 객)
public class ApiRequestParser extends SimpleChannelInboundHandler<FullHttpMessage> {
	
	private static final Logger logger = LogManager.getLogger(ApiRequestParser.class);
	private HttpRequest request;
	// API요청에 따라 업무 처리 클래스를 호출하고 그 결과를 JsonObject객체 
	private JsonObject apiResult;
	
	private static final HttpDataFactory factory = 
			new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
	
	// 사용자가 전송한 HTTP본문을 추출할 디코더 
	private HttpPostRequestDecoder decoder;
	
	// 본문 추출 후 요청의 파라미터를 전송할 Map객체 
	private Map<String, String> reqData = new HashMap<String, String>();
	
	// 클라이언트가 전송한 헤더중에서 사용할 헤더의 이름 목록 (token, email사용)
	private static final Set<String> usingHeader = new HashSet<String>();
	
	static {
		usingHeader.add("token");
		usingHeader.add("email");
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception {
		/*
		 * Request헤더 처리
		 * HttpRequestDecoder는 HTTP프로토콜의 데이터를 HttpRequest, HttpContent, LastHttpContent의 순으로 디코딩하여 FullHttpMessage객체로 생성
		 * msg가 FullHttpMessage의 구현클래스 객체라면 True리턴
		 */
		if(msg instanceof HttpRequest) {
			this.request = (HttpRequest)msg;
			
			if(HttpHeaders.is100ContinueExpected(request)) {
				send100Continue(ctx);
			}
			
			// HTTP요청의 헤더정보추출
			HttpHeaders headers = request.headers();
			
			if(!headers.isEmpty()) {
				
				for(Map.Entry<String, String> h : headers) {
					String key = h.getKey();
					
					// usingHeader에 지정한 key의 value만 추출 
					if(usingHeader.contains(key)) {
						reqData.put(key, h.getValue());
					}
				}
			}
			
			// 클라이언트의 요청 URI 및 Method추출
			reqData.put("REQUEST_URI", request.getUri());
			reqData.put("REQUEST_METHOD", request.getMethod().name());
		}
		
		if(msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent)msg;
			
			ByteBuf content = httpContent.content();
			
			// HTTPCONTENT의 상위 인터페이스. 모든 HTTP메시지가 디코딩되었고 HTTP의 마지막 데이터임을 알리는 인터페이스 
			if(msg instanceof LastHttpContent) {
				logger.debug("LastHttpContent message received!!" + request.getUri());
				
				LastHttpContent trailer = (LastHttpContent)msg;
				
				// HTTP본문에서 http post데이터를 추출 
				readPostData();
				
				// reqData의 method를 확인하여 요청에 맞는 api를 실
				ApiRequest service = ServiceDispatcher.dispatch(reqData);
				
				try {
					service.executeService();
					// api 클래스의 수행 결과를 저장 
					apiResult = service.getApiResult();
				}
				finally {
					reqData.clear();
				}
				
				//apiResult인 처리결과를 클라이언트 채널의 송신 버퍼에 기록 
				if (!writeResponse(trailer, ctx)) {
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				}
				reset();
			}
		}
		
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info("요청 처리 완료");
		// channelRead0이벤트 메서드의 수행이 완료된 이후 channelReadComplete가 수행되면 클라이언트 채널에 기록된 내용 전송 
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(cause);
		ctx.close();
	}
	
	private void reset() {
		request = null;
	}
	
	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
		ctx.write(response);
	}
	
	// 디코딩된 HttpRequest객체에서 HTTP본문 데이터를 추출 
	private void readPostData() {
		try {
			//post메서드로 수신된 데이터를 추출하기 위한 디코더를 생성 
			decoder = new HttpPostRequestDecoder(factory, request);
			
			for(InterfaceHttpData data : decoder.getBodyHttpDatas()) {
				if(HttpDataType.Attribute == data.getHttpDataType()) {
					try {
						//디코더로 추출된 데이터 목록을 attribute객체로 캐스팅 
						Attribute attribute = (Attribute)data;
						// 사용자가 form엘리먼트를 사용하여 전송한 데이터를 추출 
						reqData.put(attribute.getName(), attribute.getValue());
					}
					catch(IOException e) {
						logger.error("BODY Attribute: " + data.getHttpDataType().name(), e);
						return;
					}
				}
				else {
					logger.info("BODY data : " + data.getHttpDataType().name() + ": " + data);
				}
			}
		}
		catch (ErrorDataDecoderException e) {
			logger.error(e);
		}
		finally {
			if(decoder != null) {
				decoder.destroy();
			}
		}
	}
	
	private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
		
		// 헤더의 keepAlive를 확인하여 커넥션 유지 유무를 확인
		boolean keepAlive = HttpHeaders.isKeepAlive(request);
		
		// response객체 생성 
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST, Unpooled.copiedBuffer(apiResult.toString(), CharsetUtil.UTF_8));
		
		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		
		if (keepAlive) {
			response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		
		ctx.write(response);
		
		return keepAlive;
	}

}
