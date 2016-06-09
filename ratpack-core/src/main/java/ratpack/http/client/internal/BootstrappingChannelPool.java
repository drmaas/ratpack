package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;

import java.util.function.Consumer;

/**
 *
 */
public abstract class BootstrappingChannelPool implements ChannelPool {

  protected final Bootstrap bootstrap;
  protected final ChannelPoolHandler handler;

  public BootstrappingChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler) {
    this.handler = handler;
    this.bootstrap = bootstrap;
  }

  public ChannelFuture acquire(Consumer<Channel> consumer) {
    Bootstrap b = bootstrap.clone().handler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        handler.channelCreated(ch);
        consumer.accept(ch);
      }
    });
    return b.connect();
  }

}
