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
import java.util.function.Supplier;

import com.bernardomg.example.netty.proxy.server.ProxyListener;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Message listener channel handler. Will send any message to the contained listener.
 *
 * @author Bernardo Mart&iacute;nez Garrido
 *
 */
@Slf4j
public final class ProxyServerChannelHandler extends SimpleChannelInboundHandler<String> {

    private Channel                 clientChannel;

    private final Supplier<Channel> clientChannelSupplier;

    /**
     * Proxy listener. Extension hook which allows reacting to the server events.
     */
    private final ProxyListener     listener;

    public ProxyServerChannelHandler(final ProxyListener lstn, final Supplier<Channel> clientChannelSup) {
        super();

        listener = Objects.requireNonNull(lstn);
        clientChannelSupplier = Objects.requireNonNull(clientChannelSup);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        clientChannel = clientChannelSupplier.get();
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
    }

}
