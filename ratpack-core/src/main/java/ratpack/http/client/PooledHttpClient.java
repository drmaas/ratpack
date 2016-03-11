package ratpack.http.client;

import ratpack.exec.Promise;
import ratpack.func.Action;

import java.net.URI;

public interface PooledHttpClient {

  /**
   * An asynchronous method to do a GET HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec, but the method will be defaulted to a GET.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ratpack.http.client.ReceivedResponse}
   */
  Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action);

  Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer);
}
