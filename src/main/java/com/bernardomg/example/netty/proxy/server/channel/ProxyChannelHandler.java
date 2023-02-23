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
public final class ProxyChannelHandler extends SimpleChannelInboundHandler<String> {

    private final Channel clientChannel;

    public ProxyChannelHandler(final Channel channel) {
        super();

        clientChannel = Objects.requireNonNull(channel);
    }

    @Override
    public final void channelRead0(final ChannelHandlerContext ctx, final String message) throws Exception {
        log.debug("Handling server request");

        log.debug("Received server request: {}", message);

        // listener.onServerReceive(message);

        // Redirec to the target client
        clientChannel.writeAndFlush(Unpooled.wrappedBuffer(message.getBytes()))
            .addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Successful client channel future");
                    // listener.onServerSend(message);
                } else {
                    log.debug("Failed client channel future");
                }
            });

        log.debug("Handling client response");

        log.debug("Received client response: {}", message);

        // listener.onClientReceive(message);

        // Redirec to the source server
        ctx.writeAndFlush(Unpooled.wrappedBuffer(message.getBytes()))
            .addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Successful server channel future");
                    // listener.onClientSend(message);
                } else {
                    log.debug("Failed server channel future");
                }
            });
    }

}
