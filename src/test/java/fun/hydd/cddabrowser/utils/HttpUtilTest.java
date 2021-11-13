package fun.hydd.cddabrowser.utils;

import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUtilTest {

  @Test
  void request() {
    HttpUtil.request(Vertx.vertx(), new RequestOptions()
        .setAbsoluteURI("https://www.baidu.com")
        .setSsl(true)
        .putHeader("User-Agent", "cdda-browser-test"))
      .onFailure(throwable -> assertThat(true).isFalse());
  }
}
