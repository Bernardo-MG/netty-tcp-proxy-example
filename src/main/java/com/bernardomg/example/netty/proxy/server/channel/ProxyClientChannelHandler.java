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

import com.bernardomg.example.netty.proxy.server.ProxyListener;

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
public final class ProxyClientChannelHandler extends ChannelInboundHandlerAdapter {

    /**
     * Proxy listener. Extension hook which allows reacting to the server events.
     */
    private final ProxyListener listener;

    /**
     * Embedded server connection.
     */
    private final Channel       serverChannel;

    public ProxyClientChannelHandler(final Channel channel, final ProxyListener lstn) {
        super();

        serverChannel = Objects.requireNonNull(channel);
        listener = Objects.requireNonNull(lstn);
    }

    @Override
    public final void channelActive(final ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public final void channelInactive(final ChannelHandlerContext ctx) {
        if (serverChannel.isActive()) {
            log.debug("Closing server");
            serverChannel.close();
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        serverChannel.writeAndFlush(msg)
            .addListener(future -> {
                if (future.isSuccess()) {
                    log.debug("Successful client channel future");
                    listener.onClientSend(msg);
                    ctx.channel()
                        .read();
                } else {
                    log.debug("Failed client channel future");
                }
            });
    }

}
