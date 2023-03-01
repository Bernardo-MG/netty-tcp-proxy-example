/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2023 the original author or authors.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bernardomg.example.netty.proxy.server;

import java.util.Objects;

import com.bernardomg.example.netty.proxy.server.channel.ProxyChannelInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty based TCP server.
 *
 * @author Bernardo Mart&iacute;nez Garrido
 *
 */
@Slf4j
public final class NettyTcpProxyServer implements Server {

    /**
     * Group storing the server channel.
     */
    private ChannelGroup        channelGroup;

    /**
     * Server secondary event loop group.
     */
    private EventLoopGroup      childGroup;

    /**
     * Proxy listener. Extension hook which allows reacting to the proxy events.
     */
    private final ProxyListener listener;

    /**
     * Server main event loop group.
     */
    private EventLoopGroup      parentGroup;

    /**
     * Port which the server will listen to.
     */
    private final Integer       port;

    /**
     * Host for the server to which this client will connect.
     */
    private final String        targetHost;

    /**
     * Port for the server to which this client will connect.
     */
    private final Integer       targetPort;

    public NettyTcpProxyServer(final Integer prt, final String trgtHost, final Integer trgtPort,
            final ProxyListener lst) {
        super();

        port = Objects.requireNonNull(prt);
        targetHost = Objects.requireNonNull(trgtHost);
        targetPort = Objects.requireNonNull(trgtPort);
        listener = Objects.requireNonNull(lst);
    }

    @Override
    public final void start() {
        final Channel serverChannel;

        log.trace("Starting proxy");

        listener.onStart();

        // Initializes groups
        parentGroup = new NioEventLoopGroup();
        channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        childGroup = new NioEventLoopGroup();

        serverChannel = connectoToServer();

        channelGroup.add(serverChannel);

        log.trace("Started proxy");
    }

    @Override
    public final void stop() {
        log.trace("Stopping proxy");

        listener.onStop();

        // Stop server
        channelGroup.close();
        parentGroup.shutdownGracefully();
        childGroup.shutdownGracefully();

        log.trace("Stopped proxy");
    }

    /**
     * Starts a server connection and returns a channel.
     * 
     * @return channel for the server
     */
    private final Channel connectoToServer() {
        final ServerBootstrap bootstrap;
        final ChannelFuture   channelFuture;

        bootstrap = new ServerBootstrap()
            // Registers groups
            .group(parentGroup, childGroup)
            // Defines channel
            .channel(NioServerSocketChannel.class)
            // Configuration
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.AUTO_CLOSE, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            // Child handler
            .childHandler(new ProxyChannelInitializer(targetHost, targetPort, listener));

        try {
            // Binds to the port
            log.debug("Binding port {}", port);
            channelFuture = bootstrap.bind(port)
                .sync();
        } catch (final InterruptedException e) {
            log.error(e.getLocalizedMessage(), e);
            stop();

            // Rethrows exception
            throw new RuntimeException(e);
        }

        if (channelFuture.isSuccess()) {
            log.debug("Bound correctly to port {}", port);
        }

        return channelFuture.channel();
    }

}
