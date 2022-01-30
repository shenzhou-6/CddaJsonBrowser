package fun.hydd.cddabrowser.utils;

import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class HttpUtilTest {

  @Test
  void request(Vertx vertx, VertxTestContext testContext) {
    HttpUtil.request(vertx, new RequestOptions()
        .setAbsoluteURI("https://www.baidu.com")
        .setSsl(true)
        .putHeader("User-Agent", "cdda-browser-test"))
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer.toString()).startsWith("<!DOCTYPE html>");
        testContext.completeNow();
      }));
  }

  @Test
  void downloadFile(Vertx vertx, VertxTestContext testContext) {
    String url = "https://www.baidu.com";
    String path = this.getClass().getResource("/").getPath() + "downloadFile/baidu.html";
    HttpUtil.downloadFile(vertx, url, path)
      .onComplete(testContext.succeeding(s -> {
        assertThat(new File(s)).exists().isFile();
        testContext.completeNow();
      }));
  }
}
