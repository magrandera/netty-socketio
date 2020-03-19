package com.corundumstudio.socketio;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class UnauthorizedResponse extends AuthorizationResponse {

    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private String body;
    private String contentType = "text/plain";
    private Charset charset = CharsetUtil.UTF_8;

    public UnauthorizedResponse(HttpResponseStatus httpResponseStatus) {
        super(validateHttpResponseStatus(httpResponseStatus));
    }

    private static HttpResponseStatus validateHttpResponseStatus(HttpResponseStatus httpResponseStatus) {
        if (HttpResponseStatus.OK.equals(httpResponseStatus)) {
            throw new RuntimeException("Use 'AuthorizedResponse' for httpResponseStatus 'OK'.");
        }
        return httpResponseStatus;
    }

    public static UnauthorizedResponse TEMPORARY_REDIRECT(String locationUrl) {
        UnauthorizedResponse authorizationResponse = new UnauthorizedResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        authorizationResponse.getHeaders().add(HttpHeaderNames.LOCATION, locationUrl);
        return authorizationResponse;
    }

    public static UnauthorizedResponse UNAUTHORIZED() {
        return new UnauthorizedResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static UnauthorizedResponse INTERNAL_SERVER_ERROR() {
        return new UnauthorizedResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public UnauthorizedResponse setHeader(AsciiString name, String value) {
        httpHeaders.add(name, value);
        return this;
    }

    public UnauthorizedResponse setHeaders(HttpHeaders headers) {
        httpHeaders.setAll(headers);
        return this;
    }

    public UnauthorizedResponse setHeaders(Map<AsciiString, List<String>> headers) {
        for (Map.Entry<AsciiString, List<String>> header : headers.entrySet()) {
            AsciiString name = header.getKey();
            List<String> values = header.getValue();
            for (String value : values) {
                httpHeaders.set(name, value);
            }
        }
        return this;
    }

    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    public UnauthorizedResponse setBody(String body) {
        this.body = body;
        return this;
    }

    public UnauthorizedResponse setBody(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
        return this;
    }

    public UnauthorizedResponse setBody(String body, String contentType, Charset charset) {
        this.body = body;
        this.contentType = contentType;
        this.charset = charset;
        return this;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public Charset getCharset() {
        return charset;
    }

}
