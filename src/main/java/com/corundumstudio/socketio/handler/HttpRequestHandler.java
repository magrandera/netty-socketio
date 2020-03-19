package com.corundumstudio.socketio.handler;

import com.corundumstudio.socketio.HttpParams;
import com.corundumstudio.socketio.HttpRequestBody;
import com.corundumstudio.socketio.HttpRequestSignature;
import com.corundumstudio.socketio.HttpResponse;
import com.corundumstudio.socketio.namespace.HttpNamespace;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestHandler.class);

    private final HttpNamespace httpNamespace;

    public HttpRequestHandler(HttpNamespace httpNamespace) {
        this.httpNamespace = httpNamespace;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest && httpNamespace.hasListeners()) {
            FullHttpRequest req = (FullHttpRequest) msg;
            Channel channel = ctx.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());

            HttpMethod method = req.method();
            String path = queryDecoder.path();
            HttpParams params = new HttpParams(queryDecoder.parameters());
            HttpHeaders headers = req.headers();
            HttpRequestBody body = new HttpRequestBody(req);

            HttpRequestSignature httpRequestSignature = new HttpRequestSignature(method, path);
            HttpResponse httpResponse = null;
            try {
                httpResponse = httpNamespace.onRequest(httpRequestSignature, params, headers, body);
            } catch (Exception e) {
                log.warn("HttpListener threw exception.", e);
            }
            if (httpResponse != null) {
                DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, httpResponse.getHttpResponseStatus());
                if (httpResponse.getBody() != null) {
                    ByteBuf buf = Unpooled.wrappedBuffer(httpResponse.getBody().getBytes(httpResponse.getCharset()));
                    res.content().writeBytes(buf);
                    buf.release();
                    res.headers().set(HttpHeaderNames.CONTENT_TYPE, httpResponse.getContentType() + "; charset=" + httpResponse.getCharset().displayName().toLowerCase());
                    res.headers().set(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
                }
                if (httpResponse.getHeaders() != null) {
                    res.headers().add(httpResponse.getHeaders());
                }

                channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);

                log.debug("Http response, query params: {} headers: {}", params, headers);
                return;
            }
        }
        super.channelRead(ctx, msg);
    }
}
