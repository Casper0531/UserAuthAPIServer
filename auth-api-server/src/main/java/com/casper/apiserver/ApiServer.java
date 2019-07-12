package com.casper.apiserver;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

@Component
public final class ApiServer {

	@Autowired
	@Qualifier("tcpSocketAddress")
	private InetSocketAddress address;
	
	@Autowired
	@Qualifier("httpsSocketAddress")
	private InetSocketAddress httpsAddress;
	
	@Autowired
	@Qualifier("workerThreadCount")
	private int workerThreadCount;
	
	@Autowired
	@Qualifier("bossThreadCount")
	private int bossThreadCount;
	
	public void start() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadCount);
		EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreadCount);
		ChannelFuture channelFuture = null;
	
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.handler(new LoggingHandler(LogLevel.INFO))
			// ApiServerInitializer의 인자는 SSL컨텍스트이지만 HTTP만을 다루는 서버이므로 null로 초기화 
			.childHandler(new ApiServerInitializer(null));
			
			Channel ch = b.bind(address).sync().channel();
			channelFuture = ch.closeFuture();
			// 서버 채널의 closeFuture객체를 가져와서 닫힘 이벤트가 발생할 때까지 대기. 메인스레드의 일시정지부분 
			// 보안 채널 추가 및 해당 부분에서 sync() 메서드로 블로킹되는 것을 막기 위해 주석처리
			//channelFuture.sync();
			
			// SSL연결을 지원하기 위해 자기 스스로 서명한 인증서를 생성하는 SelfSignedCertificate객체 생성
			// 일반 브라우저에서 접근한다면 경고 생성
			final SslContext sslCtx;
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
			
			// 보안 채널을 위한 HTTPS 연결 수신 부트스트랩 설정 추가
			ServerBootstrap b2 = new ServerBootstrap();
			// 이벤트 루프는 b1과 공유
			b2.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.handler(new LoggingHandler(LogLevel.INFO))
			.childHandler(new ApiServerInitializer(sslCtx));
			
			// 하나의 프로세스에서 두 개의 서비스 포트를 수신하도록 변경
			Channel ch2 = b2.bind(httpsAddress).sync().channel();
			
			channelFuture = ch2.closeFuture();
			channelFuture.sync();
			
		}
		catch (InterruptedException | CertificateException e){
			e.printStackTrace();
		}
		catch (SSLException e) {
			e.printStackTrace();
		}
		finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
}
