package ratpack.http.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;

import java.util.function.Consumer;

/**
 *
 */
public interface BootstrappingChannelPool extends ChannelPool {

  Future<Channel> acquire(Consumer<Channel> consumer);

}
