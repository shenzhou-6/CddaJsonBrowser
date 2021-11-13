package fun.hydd.cddabrowser.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {
  public static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

  private HttpUtil() {
  }

  public static Future<Buffer> request(Vertx vertx, RequestOptions requestOptions) {
    return request(vertx, requestOptions, 0);
  }

  public static Future<Buffer> request(Vertx vertx, RequestOptions requestOptions, int replyCount) {
    final HttpClient client = vertx.createHttpClient();
    return client.request(
        requestOptions
      )
      .compose(HttpClientRequest::send)
      .compose(HttpClientResponse::body)
      .onSuccess(buffer -> logger.info("request {} is success", requestOptions.getURI()))
      .recover(throwable -> {
        if (replyCount < 10) {
          logger.warn("Request reply:\n" +
            "\treply count is {}\n" +
            "\turl: {}", replyCount, requestOptions.getURI());
          return request(vertx, requestOptions, replyCount + 1);
        } else {
          return Future.failedFuture(throwable);
        }
      });
  }
}
