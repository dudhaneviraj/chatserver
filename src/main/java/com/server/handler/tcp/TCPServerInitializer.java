package com.server.handler.tcp;


import com.server.Main;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class TCPServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public TCPServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null)
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        pipeline.addLast("idleStateHandler", new IdleStateHandler(Main.CHANNEL_CLOSE_MINUTES, 0, 0, TimeUnit.MINUTES));
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new TCPHandler());
    }



}