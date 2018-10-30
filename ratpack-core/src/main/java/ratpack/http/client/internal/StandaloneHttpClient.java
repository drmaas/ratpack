package ratpack.http.client.internal;

import ratpack.exec.ExecController;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClientSpec;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;

import java.net.URI;

public class StandaloneHttpClient extends DefaultHttpClientInternal {

  private ExecController execController;

  @Override
  public HttpClient copyWith(Action<? super HttpClientSpec> action) throws Exception {
    return of(execController, new DefaultHttpClientSpec(getSpec()), action);
  }

  private StandaloneHttpClient(ExecController execController, DefaultHttpClientSpec spec) {
    super(spec);
    this.execController = execController;
  }

  public static HttpClient of(ExecController execController, Action<? super HttpClientSpec> action) throws Exception {
    DefaultHttpClientSpec spec = new DefaultHttpClientSpec();
    return of(execController, spec, action);
  }

  private static HttpClient of(ExecController execController, DefaultHttpClientSpec spec, Action<? super HttpClientSpec> action) throws Exception {
    action.execute(spec);
    return new StandaloneHttpClient(execController, spec);
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action);
  }

  @Override
  public Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action.prepend(RequestSpec::post));
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return intercept(
      Promise.async(downstream ->
          execController.fork().start((e) ->
            new ContentAggregatingRequestAction(uri, this, 0, e, requestConfigurer.append(getSpec().getRequestInterceptor())).connect(downstream))
        ),
      getSpec().getResponseInterceptor(),
      getSpec().getErrorInterceptor()
    );
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return intercept(
      Promise.async(downstream ->
          execController.fork().start((e) ->
            new ContentStreamingRequestAction(uri, this, 0, e, requestConfigurer.append(getSpec().getRequestInterceptor())).connect(downstream))
      ),
      getSpec().getResponseInterceptor(),
      getSpec().getErrorInterceptor()
    );
  }

}
