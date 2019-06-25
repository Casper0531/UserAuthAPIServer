package com.casper.apiserver.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpMessage;

public class ApiRequestParser extends SimpleChannelInboundHandler<FullHttpMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
