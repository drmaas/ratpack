package ratpack.http.client.internal.pooled;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.http.Headers;
import ratpack.http.Status;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.internal.DefaultReceivedResponse;
import ratpack.http.internal.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class PooledHttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final AtomicBoolean fired = new AtomicBoolean();
    private Downstream<? super ReceivedResponse> fulfiller;
    private ByteBufAllocator byteBufAllocator;
    private boolean isKeepAlive;
    private final ChannelPool channelPool;

    public PooledHttpClientHandler(ChannelPool channelPool, Downstream<? super ReceivedResponse> fulfiller, ByteBufAllocator byteBufAllocator) {
        this.channelPool = channelPool;
        this.fulfiller = fulfiller;
        this.byteBufAllocator = byteBufAllocator;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        if (!(isKeepAlive = HttpUtil.isKeepAlive(msg))) {
            ctx.close();
        } else {
            ctx.pipeline().remove(this);
            channelPool.release(ctx.channel());
        }
        success(fulfiller, toReceivedResponse(msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.pipeline().remove(this);
        if (!isKeepAlive) {
            ctx.close();
        } else {
            channelPool.release(ctx.channel());
        }
        error(fulfiller, cause);
    }

    protected void success(Downstream<? super ReceivedResponse> downstream, ReceivedResponse value) {
        if (fired.compareAndSet(false, true)) {
            downstream.success(value);
        }
    }

    protected void error(Downstream<?> downstream, Throwable error) {
        if (fired.compareAndSet(false, true)) {
            downstream.error(error);
        }
    }

    protected ReceivedResponse toReceivedResponse(FullHttpResponse msg) {
        return toReceivedResponse(msg, initBufferReleaseOnExecutionClose(msg.content(), Execution.current()));
    }

    protected ReceivedResponse toReceivedResponse(HttpResponse msg) {
        return toReceivedResponse(msg, byteBufAllocator.buffer(0, 0));
    }

    private ReceivedResponse toReceivedResponse(HttpResponse msg, ByteBuf responseBuffer) {
        final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
        String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE.toString());
        final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(responseBuffer, DefaultMediaType.get(contentType));
        final Status status = new DefaultStatus(msg.status());
        return new DefaultReceivedResponse(status, headers, typedData);
    }

    private static ByteBuf initBufferReleaseOnExecutionClose(final ByteBuf responseBuffer, Execution execution) {
        execution.onComplete(responseBuffer::release);
        return responseBuffer;
    }
}
