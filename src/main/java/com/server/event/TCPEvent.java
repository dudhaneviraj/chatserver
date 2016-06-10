package com.server.event;


import com.server.config.Config;
import com.server.handler.tcp.TCPServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPEvent implements IEvent, Runnable {
    private static final TCPEvent tcpEvent = new TCPEvent();
    boolean sslEnabled = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private TCPEvent() {
    }

    public static TCPEvent getEvent() {
        return tcpEvent;
    }

    @Override
    public void build(Config config, boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    @Override
    public void start() throws Exception {
        executorService.submit(tcpEvent);
    }

    @Override
    public void stop() throws Exception {
        executorService.shutdownNow();
    }

    @Override
    public void run() {
        try {
            final SslContext sslCtx;
            if (sslEnabled) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else
                sslCtx = null;

            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new TCPServerInitializer(sslCtx));

                Channel ch = b.bind(9000).sync().channel();
                ch.closeFuture().sync();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
