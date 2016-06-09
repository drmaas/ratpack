package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.function.Consumer;

/**
 *
 */
public class PoolingChannelPool implements BootstrappingChannelPool {

  protected final ChannelPool channelPool;
  protected final Bootstrap bootstrap;
  protected final ChannelPoolHandler handler;

  public PoolingChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, int maxConnections) {
    this.bootstrap = bootstrap;
    this.handler = handler;
    this.channelPool = new FixedChannelPool(bootstrap, handler, maxConnections);
  }

  @Override
  public Future<Channel> acquire(Consumer<Channel> consumer) {
//    Bootstrap b = bootstrap.clone().handler(new ChannelInitializer<Channel>() {
//      @Override
//      protected void initChannel(Channel ch) throws Exception {
//        handler.channelCreated(ch);
//        consumer.accept(ch);
//      }
//    });
//    return b.connect();
    return acquire().addListener(f -> {
      if (f.isDone()) {
        consumer.accept((Channel)f.getNow());
      } else {
        f.addListener(f1 -> consumer.accept((Channel)f1.getNow()));
      }
    });
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
