package ratpack.http.client.internal;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.exec.Operation;
import ratpack.func.Action;
import ratpack.http.client.HttpClientSpec;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.RequestSpec;
import ratpack.server.ServerConfig;

import java.time.Duration;

public class DefaultHttpClientSpec implements HttpClientSpec {

  private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
  private int poolSize;
  private int poolQueueSize = Integer.MAX_VALUE;
  private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
  private int responseMaxChunkSize = 8192;
  private Duration readTimeout = Duration.ofSeconds(30);
  private Duration connectTimeout = Duration.ofSeconds(30);
  private Action<? super RequestSpec> requestInterceptor = Action.noop();
  private Action<? super HttpResponse> responseInterceptor = Action.noop();
  private Action<? super Throwable> errorInterceptor = Action.noop();
  private boolean enableMetricsCollection;

  public DefaultHttpClientSpec() {
  }

  public DefaultHttpClientSpec(HttpClientSpec spec) {
    this.byteBufAllocator = spec.getByteBufAllocator();
    this.poolSize = spec.getPoolSize();
    this.poolQueueSize = spec.getPoolQueueSize();
    this.maxContentLength = spec.getMaxContentLength();
    this.responseMaxChunkSize = spec.getResponseMaxChunkSize();
    this.readTimeout = spec.getReadTimeout();
    this.connectTimeout = spec.getConnectTimeout();
    this.requestInterceptor = spec.getRequestInterceptor();
    this.responseInterceptor = spec.getResponseInterceptor();
    this.enableMetricsCollection = spec.getEnableMetricsCollection();
  }

  @Override
  public HttpClientSpec poolSize(int poolSize) {
    this.poolSize = poolSize;
    return this;
  }

  @Override
  public HttpClientSpec poolQueueSize(int poolQueueSize) {
    this.poolQueueSize = poolQueueSize;
    return this;
  }

  @Override
  public HttpClientSpec byteBufAllocator(ByteBufAllocator byteBufAllocator) {
    this.byteBufAllocator = byteBufAllocator;
    return this;
  }

  @Override
  public HttpClientSpec maxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  @Override
  public HttpClientSpec responseMaxChunkSize(int numBytes) {
    this.responseMaxChunkSize = numBytes;
    return this;
  }

  @Override
  public HttpClientSpec readTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  @Override
  public HttpClientSpec connectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  @Override
  public HttpClientSpec requestIntercept(Action<? super RequestSpec> interceptor) {
    requestInterceptor = requestInterceptor.append(interceptor);
    return this;
  }

  @Override
  public HttpClientSpec responseIntercept(Action<? super HttpResponse> interceptor) {
    responseInterceptor = responseInterceptor.append(interceptor);
    return this;
  }

  @Override
  public HttpClientSpec responseIntercept(Operation operation) {
    responseInterceptor = responseInterceptor.append(response -> operation.then());
    return this;
  }

  @Override
  public HttpClientSpec errorIntercept(Action<? super Throwable> interceptor) {
    errorInterceptor = errorInterceptor.append(interceptor);
    return this;
  }

  @Override
  public HttpClientSpec enableMetricsCollection(boolean enableMetricsCollection) {
    this.enableMetricsCollection = enableMetricsCollection;
    return this;
  }

  @Override
  public ByteBufAllocator getByteBufAllocator() {
    return byteBufAllocator;
  }

  @Override
  public int getPoolSize() {
    return poolSize;
  }

  @Override
  public int getPoolQueueSize() {
    return poolQueueSize;
  }

  @Override
  public int getMaxContentLength() {
    return maxContentLength;
  }

  @Override
  public int getResponseMaxChunkSize() {
    return responseMaxChunkSize;
  }

  @Override
  public Duration getReadTimeout() {
    return readTimeout;
  }

  @Override
  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  @Override
  public Action<? super RequestSpec> getRequestInterceptor() {
    return requestInterceptor;
  }

  @Override
  public Action<? super HttpResponse> getResponseInterceptor() {
    return responseInterceptor;
  }

  @Override
  public Action<? super Throwable> getErrorInterceptor() {
    return errorInterceptor;
  }

  @Override
  public boolean getEnableMetricsCollection() {
    return enableMetricsCollection;
  }
}
