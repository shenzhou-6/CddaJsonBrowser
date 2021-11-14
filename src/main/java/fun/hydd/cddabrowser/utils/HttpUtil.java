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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class HttpUtil {
  public static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
  public static final String GITHUB_DOWNLOAD_SOURCE_ZIP_URL_PREFIX = "https://codeload.github.com/" +
    "CleverRaven/Cataclysm-DDA/zip/refs/tags/";

  private HttpUtil() {
  }

  public static Future<String> downloadFile(Vertx vertx, String url, String target) {
    final File file = new File(target);
    if (file.exists()) {
      try {
        Files.delete(file.toPath());
      } catch (final IOException e) {
        return Future.failedFuture(e);
      }
    }
    if (!file.getParentFile().exists()) {
      logger.info("mkdirs is {}", file.getParentFile().mkdirs());
    }
    return request(vertx, new RequestOptions().setAbsoluteURI(url))
      .compose(buffer -> vertx.fileSystem().writeFile(target, buffer))
      .compose(unused -> Future.succeededFuture(target))
      .onSuccess(s -> logger.info("success downloadFile:{}", s))
      .recover(throwable -> {
        logger.error("download file is fail\n" +
          "\tdownload url is {}\n", url);
        logger.error("download is fail", throwable);
        return Future.failedFuture(throwable);
      });
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
