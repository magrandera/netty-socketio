/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.handler;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.namespace.HttpNamespace;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.messages.HttpErrorMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.AuthPacket;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.ConnectMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

@Sharable
public class AuthorizeHandler extends ChannelInboundHandlerAdapter implements Disconnectable {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeHandler.class);

    private final CancelableScheduler disconnectScheduler;

    private final String connectPath;
    private final Configuration configuration;
    private final NamespacesHub namespacesHub;
    private final HttpNamespace httpNamespace;
    private final StoreFactory storeFactory;
    private final DisconnectableHub disconnectable;
    private final AckManager ackManager;
    private final ClientsBox clientsBox;

    public AuthorizeHandler(String connectPath, CancelableScheduler scheduler, Configuration configuration, NamespacesHub namespacesHub, HttpNamespace httpNamespace, StoreFactory storeFactory,
                            DisconnectableHub disconnectable, AckManager ackManager, ClientsBox clientsBox) {
        super();
        this.connectPath = connectPath;
        this.configuration = configuration;
        this.disconnectScheduler = scheduler;
        this.namespacesHub = namespacesHub;
        this.httpNamespace = httpNamespace;
        this.storeFactory = storeFactory;
        this.disconnectable = disconnectable;
        this.ackManager = ackManager;
        this.clientsBox = clientsBox;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, ctx.channel());
        disconnectScheduler.schedule(key, new Runnable() {
            @Override
            public void run() {
                ctx.channel().close();
                log.debug("Client with ip {} opened channel but doesn't send any data! Channel closed!", ctx.channel().remoteAddress());
            }
        }, configuration.getFirstDataTimeout(), TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, ctx.channel());
        disconnectScheduler.cancel(key);

        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            Channel channel = ctx.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());

            if (!httpNamespace.hasListeners() && !queryDecoder.path().startsWith(connectPath)) {
                io.netty.handler.codec.http.HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
                req.release();
                return;
            }

            List<String> sid = queryDecoder.parameters().get("sid");
            if (queryDecoder.path().equals(connectPath)
                    && sid == null) {
                String origin = req.headers().get(HttpHeaderNames.ORIGIN);
                if (!authorize(ctx, channel, origin, queryDecoder.parameters(), req)) {
                    req.release();
                    return;
                }
                // forward message to polling or websocket handler to bind channel
            }
        }
        ctx.fireChannelRead(msg);
    }

    private boolean authorize(ChannelHandlerContext ctx, Channel channel, String origin, Map<String, List<String>> params, FullHttpRequest req)
            throws IOException {
        Map<String, List<String>> headers = new HashMap<String, List<String>>(req.headers().names().size());
        for (String name : req.headers().names()) {
            List<String> values = req.headers().getAll(name);
            headers.put(name, values);
        }

        HandshakeData handshakeData = new HandshakeData(req.headers(), params,
                (InetSocketAddress)channel.remoteAddress(),
                (InetSocketAddress)channel.localAddress(),
                req.uri(), origin != null && !origin.equalsIgnoreCase("null"));

        AuthorizationResponse authorizationResponse = null;
        try {
            authorizationResponse = configuration.getAuthorizationListener().isAuthorized(handshakeData);
        } catch (Exception e) {
            log.error("AuthorizationListener error", e);
        }

        if (authorizationResponse == null) {
            authorizationResponse = UnauthorizedResponse.UNAUTHORIZED();
        }

        if (authorizationResponse instanceof UnauthorizedResponse) {
            UnauthorizedResponse unauthorizedResponse = (UnauthorizedResponse) authorizationResponse;

            DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, unauthorizedResponse.getHttpResponseStatus());
            if (unauthorizedResponse.getBody() != null) {
                ByteBuf buf = Unpooled.copiedBuffer(unauthorizedResponse.getBody(), unauthorizedResponse.getCharset());
                res.content().writeBytes(buf);
                buf.release();
                res.headers().set(HttpHeaderNames.CONTENT_TYPE, unauthorizedResponse.getContentType() + "; charset=" + unauthorizedResponse.getCharset().displayName().toLowerCase());
                res.headers().set(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
            }
            if (unauthorizedResponse.getHeaders() != null) {
                res.headers().add(unauthorizedResponse.getHeaders());
            }

            channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            log.debug("Handshake UNAUTHORIZED, query params: {} headers: {}", params, headers);
            return false;
        }

        // authorized
        AuthorizedResponse authorizedResponse = (AuthorizedResponse) authorizationResponse;
        Map<String, Object> storeData = authorizedResponse.getClientData();

        UUID sessionId = null;
        if (configuration.isRandomSession()) {
            sessionId = UUID.randomUUID();
        } else {
            sessionId = this.generateOrGetSessionIdFromRequest(req.headers());
        }

        List<String> transportValue = params.get("transport");
        if (transportValue == null) {
            log.error("Got no transports for request {}", req.uri());
            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        Transport transport = Transport.byName(transportValue.get(0));
        if (!configuration.getTransports().contains(transport)) {
            Map<String, Object> errorData = new HashMap<String, Object>();
            errorData.put("code", 0);
            errorData.put("message", "Transport unknown");

            channel.attr(EncoderHandler.ORIGIN).set(origin);
            channel.writeAndFlush(new HttpErrorMessage(errorData));
            return false;
        }

        ClientHead client = new ClientHead(sessionId, ackManager, disconnectable, storeFactory, storeData, handshakeData, clientsBox, transport, disconnectScheduler, configuration);
        channel.attr(ClientHead.CLIENT).set(client);
        clientsBox.addClient(client);

        String[] transports = {};
        if (configuration.getTransports().contains(Transport.WEBSOCKET)) {
            transports = new String[]{"websocket"};
        }

        AuthPacket authPacket = new AuthPacket(sessionId, transports, configuration.getPingInterval(),
                configuration.getPingTimeout());
        Packet packet = new Packet(PacketType.OPEN);
        packet.setData(authPacket);
        client.send(packet);

        client.schedulePingTimeout();
        log.debug("Handshake authorized for sessionId: {}, query params: {} headers: {}", sessionId, params, headers);
        return true;
    }

    /**
     * This method will either generate a new random sessionId or will retrieve the value stored
     * in the "io" cookie.  Failures to parse will cause a logging warning to be generated and a
     * random uuid to be generated instead (same as not passing a cookie in the first place).
     */
    private UUID generateOrGetSessionIdFromRequest(HttpHeaders headers) {
        List<String> values = headers.getAll("io");
        if (values.size() == 1) {
            try {
                return UUID.fromString(values.get(0));
            } catch (IllegalArgumentException iaex) {
                log.warn("Malformed UUID received for session! io=" + values.get(0));
            }
        }

        for (String cookieHeader : headers.getAll(HttpHeaderNames.COOKIE)) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieHeader);

            for (Cookie cookie : cookies) {
                if (cookie.name().equals("io")) {
                    try {
                        return UUID.fromString(cookie.value());
                    } catch (IllegalArgumentException iaex) {
                        log.warn("Malformed UUID received for session! io=" + cookie.value());
                    }
                }
            }
        }

        return UUID.randomUUID();
    }

    public void connect(UUID sessionId) {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
        disconnectScheduler.cancel(key);
    }

    public void connect(ClientHead client) {
        Namespace ns = namespacesHub.get(Namespace.DEFAULT_NAME);

        if (!client.getNamespaces().contains(ns)) {
            Packet packet = new Packet(PacketType.MESSAGE);
            packet.setSubType(PacketType.CONNECT);
            client.send(packet);

            configuration.getStoreFactory().pubSubStore().publish(PubSubType.CONNECT, new ConnectMessage(client.getSessionId()));

            SocketIOClient nsClient = client.addNamespaceClient(ns);
            ns.onConnect(nsClient);
        }
    }

    @Override
    public void onDisconnect(ClientHead client) {
        clientsBox.removeClient(client.getSessionId());
    }

}
