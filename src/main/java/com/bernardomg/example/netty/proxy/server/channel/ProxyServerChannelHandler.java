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

import com.bernardomg.example.netty.proxy.server.ChannelProducer;
import com.bernardomg.example.netty.proxy.server.ProxyListener;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Channel handler ready to proxy requests. Will move request between a server, which is the owner of this listener, and
 * an embedded client.
 *
 * @author Bernardo Mart&iacute;nez Garrido
 *
 */
@Slf4j
public final class ProxyServerChannelHandler extends SimpleChannelInboundHandler<String> {

    /**
     * Embedded client connection.
     */
    private Channel               clientChannel;

    /**
     * Supplier to acquire the client connection.
     */
    private final ChannelProducer clientChannelSupplier;

    /**
     * Proxy listener. Extension hook which allows reacting to the server events.
     */
    private final ProxyListener   listener;

    /**
     * Server request context. Required to redirect messages received by the client.
     */
    private ChannelHandlerContext serverContext;

    public ProxyServerChannelHandler(final String hst, final Integer prt, final ProxyListener lstn) {
        super();

        listener = Objects.requireNonNull(lstn);
        clientChannelSupplier = new ChannelProducer(hst, prt, this::handleClientResponse);
    }

    @Override
    public final void channelActive(final ChannelHandlerContext ctx) {
        clientChannel = clientChannelSupplier.get();
    }

    @Override
    public final void channelInactive(final ChannelHandlerContext ctx) {
        if (clientChannel.isActive()) {
            clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public final void channelRead0(final ChannelHandlerContext ctx, final String message) throws Exception {
        log.debug("Handling server request");

        log.debug("Received server request: {}", message);

        listener.onServerReceive(message);

        // Redirect to the target client
        clientChannel.writeAndFlush(Unpooled.wrappedBuffer(message.getBytes()))
            .addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Successful client channel future");
                    listener.onServerSend(message);
                } else {
                    log.debug("Failed client channel future");
                }
            });

        serverContext = ctx;
    }

    private final void handleClientResponse(final ChannelHandlerContext ctx, final String message) {
        log.debug("Handling client response");

        log.debug("Received client response: {}", message);

        listener.onClientReceive(message);

        // Redirect to the source server
        serverContext.writeAndFlush(Unpooled.wrappedBuffer(message.getBytes()))
            .addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Successful server channel future");
                    listener.onClientSend(message);
                } else {
                    log.debug("Failed server channel future");
                }
            });
    }

}
