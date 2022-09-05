package com.onfido.qa.client;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class LoggingInterceptor implements Interceptor {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final Level level;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    public static class Logger {

        private final StringBuilder sb;

        public Logger() {
            sb = new StringBuilder();
        }

        public void log(String s) {
            sb.append(s).append("\n");
        }

        public void flush() {
            log.warn(sb.toString());
        }

    }

    public enum Level {
        NONE,
        BASIC,
        HEADERS,
        BODY
    }

    public LoggingInterceptor(Level level) {
        this.level = level;
    }


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        boolean logBody = level == Level.BODY;
        boolean logHeaders = logBody || level == Level.HEADERS;
        Connection connection = chain.connection();
        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        String requestStartMessage = "--> " + request.method() + ' ' + request.url() + (connection != null ? " " + connection.protocol() : "");
        if (!logHeaders && hasRequestBody) {
            requestStartMessage = requestStartMessage + " (" + requestBody.contentLength() + "-byte body)";
        }

        var logger = new Logger();

        logger.log(requestStartMessage);
        if (logHeaders) {
            if (hasRequestBody) {
                if (requestBody.contentType() != null) {
                    logger.log("Content-Type: " + requestBody.contentType());
                }

                if (requestBody.contentLength() != -1L) {
                    logger.log("Content-Length: " + requestBody.contentLength());
                }
            }

            Headers headers = request.headers();
            int i = 0;

            for (int count = headers.size(); i < count; ++i) {
                String name = headers.name(i);
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    logger.log(name + ": " + headers.value(i));
                }
            }

            if (logBody && hasRequestBody) {
                if (this.bodyEncoded(request.headers())) {
                    logger.log("--> END " + request.method() + " (encoded body omitted)");
                } else {
                    Buffer buffer = new Buffer();
                    requestBody.writeTo(buffer);
                    Charset charset = UTF8;
                    MediaType contentType = requestBody.contentType();
                    if (contentType != null) {
                        charset = contentType.charset(UTF8);
                    }

                    logger.log("");
                    if (isPlaintext(buffer)) {
                        logger.log(buffer.readString(charset));
                        logger.log("--> END " + request.method() + " (" + requestBody.contentLength() + "-byte body)");
                    } else {
                        logger.log("--> END " + request.method() + " (binary " + requestBody.contentLength() + "-byte body omitted)");
                    }
                }
            } else {
                logger.log("--> END " + request.method());
            }
        }

        long startNs = System.nanoTime();

        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logger.log("<-- HTTP FAILED: " + e);
            throw e;
        }

        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1L ? contentLength + "-byte" : "unknown-length";
        logger.log("<-- " + response.code() + (response.message().isEmpty() ? "" : ' ' + response.message()) + ' ' + response.request()
                                                                                                                             .url() + " (" + tookMs + "ms" + (!logHeaders ? ", " + bodySize + " body" : "") + ')');
        if (logHeaders) {
            Headers headers = response.headers();
            int i = 0;

            for (int count = headers.size(); i < count; ++i) {
                logger.log(headers.name(i) + ": " + headers.value(i));
            }

            if (logBody && HttpHeaders.hasBody(response)) {
                if (this.bodyEncoded(response.headers())) {
                    logger.log("<-- END HTTP (encoded body omitted)");
                } else {
                    BufferedSource source = responseBody.source();
                    source.request(9223372036854775807L);
                    Buffer buffer = source.buffer();
                    Charset charset = UTF8;
                    MediaType contentType = responseBody.contentType();
                    if (contentType != null) {
                        charset = contentType.charset(UTF8);
                    }

                    if (!isPlaintext(buffer)) {
                        logger.log("");
                        logger.log("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
                        return response;
                    }

                    if (contentLength != 0L) {
                        logger.log("");
                        logger.log(buffer.clone().readString(charset));
                    }

                    logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
                }
            } else {
                logger.log("<-- END HTTP");
            }
        }

        if (!response.isSuccessful()) {
            logger.flush();
        }

        return response;
    }

    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = Math.min(buffer.size(), 64L);
            buffer.copyTo(prefix, 0L, byteCount);

            for (int i = 0; i < 16 && !prefix.exhausted(); ++i) {
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }

            return true;
        } catch (EOFException var6) {
            return false;
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }


}
