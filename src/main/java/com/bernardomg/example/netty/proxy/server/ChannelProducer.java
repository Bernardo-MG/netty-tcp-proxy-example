
package com.bernardomg.example.netty.proxy.server;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.bernardomg.example.netty.proxy.server.channel.MessageListenerChannelInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ChannelProducer implements Function<BiConsumer<ChannelHandlerContext, String>, Channel> {

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    private final String         host;

    private final Integer        port;

    public ChannelProducer(final String hst, final Integer prt) {
        super();

        host = Objects.requireNonNull(hst);
        port = Objects.requireNonNull(prt);
    }

    @Override
    public final Channel apply(final BiConsumer<ChannelHandlerContext, String> lstn) {
        final Bootstrap     bootstrap;
        final ChannelFuture channelFuture;

        log.trace("Starting client");

        log.debug("Connecting to {}:{}", host, port);

        bootstrap = new Bootstrap();
        bootstrap
            // Registers groups
            .group(eventLoopGroup)
            // Defines channel
            .channel(NioSocketChannel.class)
            // Configuration
            .option(ChannelOption.SO_KEEPALIVE, true)
            // Sets channel initializer which listens for responses
            .handler(new MessageListenerChannelInitializer(lstn));

        try {
            log.debug("Connecting to {}:{}", host, port);
            channelFuture = bootstrap.connect(host, port)
                .sync();
        } catch (final InterruptedException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        }

        if (channelFuture.isSuccess()) {
            log.debug("Connected correctly to {}:{}", host, port);
        }

        log.trace("Started client");

        return channelFuture.channel();
    }

}
