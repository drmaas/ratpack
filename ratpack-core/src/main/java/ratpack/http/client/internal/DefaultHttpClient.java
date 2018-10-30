/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.http.client.internal;

import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClientSpec;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;

import java.net.URI;

public class DefaultHttpClient extends DefaultHttpClientInternal {

  private DefaultHttpClient(DefaultHttpClientSpec spec) {
    super(spec);
  }

  @Override
  public HttpClient copyWith(Action<? super HttpClientSpec> action) throws Exception {
    return of(new DefaultHttpClientSpec(getSpec()), action);
  }

  public static HttpClient of(Action<? super HttpClientSpec> action) throws Exception {
    DefaultHttpClientSpec spec = new DefaultHttpClientSpec();
    return of(spec, action);
  }

  private static HttpClient of(DefaultHttpClientSpec spec, Action<? super HttpClientSpec> action) throws Exception {
    action.execute(spec);
    return new DefaultHttpClient(spec);
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return intercept(
      Promise.async(downstream -> new ContentAggregatingRequestAction(uri, this, 0, Execution.current(), requestConfigurer.append(getSpec().getRequestInterceptor())).connect(downstream)),
      getSpec().getResponseInterceptor(),
      getSpec().getErrorInterceptor()
    );
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return intercept(
      Promise.async(downstream -> new ContentStreamingRequestAction(uri, this, 0, Execution.current(), requestConfigurer.append(getSpec().getRequestInterceptor())).connect(downstream)),
      getSpec().getResponseInterceptor(),
      getSpec().getErrorInterceptor()
    );
  }


}
