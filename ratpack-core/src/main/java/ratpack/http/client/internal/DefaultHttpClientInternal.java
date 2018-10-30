package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.exec.internal.ExecControllerInternal;
import ratpack.func.Action;
import ratpack.http.client.HttpClientSpec;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.util.internal.ChannelImplDetector;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class DefaultHttpClientInternal implements HttpClientInternal {

  private static final ChannelHealthChecker ALWAYS_UNHEALTHY = channel ->
    channel.eventLoop().newSucceededFuture(Boolean.FALSE);

  private final Map<String, ChannelPoolStats> hostStats = new ConcurrentHashMap<>();

  private final HttpChannelPoolMap channelPoolMap = new HttpChannelPoolMap() {
    @Override
    protected ChannelPool newPool(HttpChannelKey key) {
      Bootstrap bootstrap = new Bootstrap()
        .remoteAddress(key.host, key.port)
        .group(key.execution.getEventLoop())
        .channel(ChannelImplDetector.getSocketChannelImpl())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) key.connectTimeout.toMillis())
        .option(ChannelOption.ALLOCATOR, spec.getByteBufAllocator())
        .option(ChannelOption.AUTO_READ, false)
        .option(ChannelOption.SO_KEEPALIVE, isPooling());

      if (isPooling()) {
        InstrumentedChannelPoolHandler channelPoolHandler = getPoolingHandler(key);
        hostStats.put(key.host, channelPoolHandler);
        ChannelPool channelPool = new FixedChannelPool(bootstrap, channelPoolHandler, getPoolSize(), getPoolQueueSize());
        ((ExecControllerInternal) key.execution.getController()).onClose(() -> {
          remove(key);
          channelPool.close();
        });
        return channelPool;
      } else {
        InstrumentedChannelPoolHandler channelPoolHandler = getSimpleHandler(key);
        hostStats.put(key.host, channelPoolHandler);
        return new SimpleChannelPool(bootstrap, channelPoolHandler, ALWAYS_UNHEALTHY);
      }
    }
  };

  private final DefaultHttpClientSpec spec;

  protected DefaultHttpClientInternal(DefaultHttpClientSpec spec) {
    this.spec = spec;
  }

  private InstrumentedChannelPoolHandler getPoolingHandler(HttpChannelKey key) {
    if (spec.getEnableMetricsCollection()) {
      return new InstrumentedFixedChannelPoolHandler(key, getPoolSize());
    }
    return new NoopFixedChannelPoolHandler(key);
  }

  private InstrumentedChannelPoolHandler getSimpleHandler(HttpChannelKey key) {
    if (spec.getEnableMetricsCollection()) {
      return new InstrumentedSimpleChannelPoolHandler(key);
    }
    return new NoopSimpleChannelPoolHandler(key);
  }

  @Override
  public int getPoolSize() {
    return spec.getPoolSize();
  }

  @Override
  public int getPoolQueueSize() {
    return spec.getPoolQueueSize();
  }

  private boolean isPooling() {
    return getPoolSize() > 0;
  }

  @Override
  public HttpClientSpec getSpec() {
    return spec;
  }

  @Override
  public HttpChannelPoolMap getChannelPoolMap() {
    return channelPoolMap;
  }

  @Override
  public Action<? super RequestSpec> getRequestInterceptor() {
    return spec.getRequestInterceptor();
  }

  @Override
  public Action<? super HttpResponse> getResponseInterceptor() {
    return spec.getResponseInterceptor();
  }

  public ByteBufAllocator getByteBufAllocator() {
    return spec.getByteBufAllocator();
  }

  public int getMaxContentLength() {
    return spec.getMaxContentLength();
  }

  @Override
  public int getMaxResponseChunkSize() {
    return spec.getResponseMaxChunkSize();
  }

  public Duration getReadTimeout() {
    return spec.getReadTimeout();
  }

  public Duration getConnectTimeout() {
    return spec.getConnectTimeout();
  }

  @Override
  public void close() {
    channelPoolMap.close();
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action);
  }

  @Override
  public Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action.prepend(RequestSpec::post));
  }

  protected <T extends HttpResponse> Promise<T> intercept(Promise<T> promise, Action<? super HttpResponse> action, Action<? super Throwable> errorAction) {
    return promise.wiretap(r -> {
      if (r.isError()) {
        ExecController.require()
          .fork()
          .eventLoop(Execution.current().getEventLoop())
          .start(e ->
            errorAction.execute(r.getThrowable())
          );
      }
    })
      .next(r ->
        ExecController.require()
          .fork()
          .eventLoop(Execution.current().getEventLoop())
          .start(e ->
            action.execute(r)
          )
      );
  }

  public HttpClientStats getHttpClientStats() {
    return new HttpClientStats(
      hostStats.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> e.getValue().getHostStats()
      ))
    );
  }

}
