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

package com.bernardomg.example.netty.proxy.server.channel;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ChannelProducer implements Function<ChannelHandlerContext, Channel> {

    private final String                                    host;

    private final BiConsumer<ChannelHandlerContext, String> listener;

    private final Integer                                   port;

    public ChannelProducer(final String hst, final Integer prt, final BiConsumer<ChannelHandlerContext, String> lstn) {
        super();

        host = Objects.requireNonNull(hst);
        port = Objects.requireNonNull(prt);
        listener = Objects.requireNonNull(lstn);
    }

    @Override
    public final Channel apply(final ChannelHandlerContext ctx) {
        final Bootstrap bootstrap;
        final Channel   contextChannel;

        contextChannel = ctx.channel();

        log.trace("Starting client");

        log.debug("Connecting to {}:{}", host, port);

        bootstrap = new Bootstrap();
        bootstrap
            // Registers groups
            .group(contextChannel.eventLoop())
            // Defines channel
            .channel(ctx.channel()
                .getClass())
            // Configuration
            .option(ChannelOption.AUTO_READ, false)
            // Sets channel initializer which listens for responses
            .handler(new MessageListenerChannelInitializer(listener));

        return bootstrap.connect(host, port)
            .channel();
    }

}
