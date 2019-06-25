package com.casper.apiserver;

import java.net.InetSocketAddress;

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

@Component
public final class ApiServer {

	@Autowired
	@Qualifier("tcpSocketAddress")
	private InetSocketAddress address;
	
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
			
			Channel ch = b.bind(8080).sync().channel();
			channelFuture = ch.closeFuture();
			// 서버 채널의 closeFuture객체를 가져와서 닫힘 이벤트가 발생할 때까지 대기. 메인스레드의 일시정지부분 
			channelFuture.sync();
				
		}
		catch (InterruptedException e){
			e.printStackTrace();
		}
		finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
}
