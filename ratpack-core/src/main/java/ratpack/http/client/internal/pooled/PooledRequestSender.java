package ratpack.http.client.internal.pooled;

import com.google.common.net.HostAndPort;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import ratpack.exec.Downstream;
import ratpack.func.Action;
import ratpack.http.MutableHeaders;
import ratpack.http.client.PooledRequestSpec;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.internal.RequestParams;
import ratpack.http.client.internal.RequestSpecBacking;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;

import java.net.URI;

import static ratpack.util.Exceptions.uncheck;

public class PooledRequestSender {
    private final Action<? super RequestSpec> requestConfigurer;
    private ChannelPool channelPool;
    private final MutableHeaders headers;
    private final RequestSpecBacking requestSpecBacking;
    private final ByteBufAllocator byteBufAllocator;
    private final URI uri;
    private final RequestParams requestParams;
    private final boolean finalUseSsl;
    private final String host;
    private final int port;

    public PooledRequestSender(Action<? super RequestSpec> requestConfigurer, ChannelPool channelPool, URI uri, ByteBufAllocator byteBufAllocator) {
        this.requestConfigurer = requestConfigurer;
        this.channelPool = channelPool;
        this.byteBufAllocator = byteBufAllocator;
        this.uri = uri;
        this.requestParams = new RequestParams();
        this.headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
        this.requestSpecBacking = new RequestSpecBacking(headers, uri, byteBufAllocator, requestParams);

        try {
            requestConfigurer.execute(requestSpecBacking.asSpec());
        } catch (Exception e) {
            throw uncheck(e);
        }

        String scheme = uri.getScheme();
        boolean useSsl = false;
        if (scheme.equals("https")) {
            useSsl = true;
        } else if (!scheme.equals("http")) {
            throw new IllegalArgumentException(String.format("URL '%s' is not a http url", uri.toString()));
        }
        this.finalUseSsl = useSsl;

        this.host = uri.getHost();
        this.port = uri.getPort() < 0 ? (useSsl ? 443 : 80) : uri.getPort();
    }

    public void send(Downstream<? super ReceivedResponse> downstream) {
        Future<Channel> connectFuture = this.channelPool.acquire();
        connectFuture.addListener(f1 -> {
            PooledHttpClientHandler pooledHttpClientHandler = new PooledHttpClientHandler(channelPool, downstream, byteBufAllocator);

            if (!connectFuture.isSuccess()) {
                pooledHttpClientHandler.error(downstream, connectFuture.cause());
            } else {
                Channel channel = connectFuture.getNow();
                channel.pipeline().addLast(pooledHttpClientHandler);

                String fullPath = getFullPath(uri);
                FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(requestSpecBacking.getMethod()), fullPath, requestSpecBacking.getBody());
                if (headers.get(HttpHeaderConstants.HOST) == null) {
                    HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
                    headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
                }
                headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                int contentLength = request.content().readableBytes();
                if (contentLength > 0) {
                    headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength));
                }

                HttpHeaders requestHeaders = request.headers();
                requestHeaders.set(headers.getNettyHeaders());

                channel.writeAndFlush(request);
            }
        });
    }

    private static String getFullPath(URI uri) {
        StringBuilder sb = new StringBuilder(uri.getRawPath());
        String query = uri.getRawQuery();
        if (query != null) {
            sb.append("?").append(query);
        }

        return sb.toString();
    }
}
