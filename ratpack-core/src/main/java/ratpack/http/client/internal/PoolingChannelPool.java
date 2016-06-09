package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 *
 */
public class PoolingChannelPool extends BootstrappingChannelPool {

  protected final ChannelPool channelPool;

  public PoolingChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, int maxConnections) {
    super(bootstrap, handler);
    this.channelPool = new FixedChannelPool(bootstrap, handler, maxConnections);
  }

  @Override
  public Future<Channel> acquire() {
    return channelPool.acquire();
  }

  @Override
  public Future<Channel> acquire(Promise<Channel> promise) {
    return channelPool.acquire(promise);
  }

  @Override
  public Future<Void> release(Channel channel) {
    return channelPool.release(channel);
  }

  @Override
  public Future<Void> release(Channel channel, Promise<Void> promise) {
    return channelPool.release(channel, promise);
  }

  @Override
  public void close() {
    channelPool.close();
  }
}
