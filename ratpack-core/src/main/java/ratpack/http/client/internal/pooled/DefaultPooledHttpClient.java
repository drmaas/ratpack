package ratpack.http.client.internal.pooled;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.*;
import ratpack.util.internal.ChannelImplDetector;

import java.net.URI;

public class DefaultPooledHttpClient implements PooledHttpClient {

  private ChannelPool channelPool;
  private int maxContentLengthBytes;
  private PooledHttpConfig config;
  private ByteBufAllocator byteBufAllocator;

  public DefaultPooledHttpClient(PooledHttpConfig config, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
    this.config = config;
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(Execution.current().getEventLoop())
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeoutMillis())
            .channel(ChannelImplDetector.getSocketChannelImpl());


    channelPool = new FixedChannelPool(bootstrap, new AbstractChannelPoolHandler() {
        @Override
        public void channelCreated(Channel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          //TODO - Figure out how to add SSL handler
          p.addLast(new HttpClientCodec());
          //TODO - ReadTimeoutHandler
          //TODO - Redirector Handler
          p.addLast(new HttpContentDecompressor());
          //TODO - Add config options
          p.addLast(new HttpObjectAggregator(maxContentLengthBytes));
        }
      }, config.getMaxConnections());
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action) {
    return null;
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return Promise.of(f -> new PooledRequestSender(requestConfigurer, this.channelPool, uri, this.byteBufAllocator).send(f));
  }
}
