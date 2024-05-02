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

import java.nio.charset.Charset;
import java.util.Objects;

import com.bernardomg.example.netty.proxy.server.ProxyListener;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Channel handler ready to proxy requests. Will move request between a server, which is the owner of this listener, and
 * an embedded client.
 *
 * @author Bernardo Mart&iacute;nez Garrido
 *
 */
@Slf4j
public final class ProxyServerChannelHandler extends ChannelInboundHandlerAdapter {

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
        clientChannel = clientChannelSupplier.apply(ctx);
    }

    @Override
    public final void channelInactive(final ChannelHandlerContext ctx) {
        if (clientChannel.isActive()) {
            log.debug("Closing client");
            clientChannel.close();
        }
    }

    @Override
    public final void channelRead(final ChannelHandlerContext ctx, final Object message) throws Exception {
        log.debug("Handling request to server");

        log.debug("Received server request: {}", message);

        if (message instanceof ByteBuf) {
            listener.onRequest(((ByteBuf) message).toString(Charset.defaultCharset()));
        } else {
            listener.onRequest(message.toString());
        }

        if (!clientChannel.isActive()) {
            log.error("Client channel inactive");
        }

        // Redirect to the target client
        clientChannel.writeAndFlush(message);

        serverContext = ctx;
    }

    private final void handleClientResponse(final ChannelHandlerContext ctx, final Object message) {
        log.debug("Handling client response");

        log.debug("Received client response: {}", message);

        if (message instanceof ByteBuf) {
            listener.onResponse(((ByteBuf) message).toString(Charset.defaultCharset()));
        } else {
            listener.onResponse(message.toString());
        }

        // Redirect to the source server
        serverContext.writeAndFlush(message);
    }

}
