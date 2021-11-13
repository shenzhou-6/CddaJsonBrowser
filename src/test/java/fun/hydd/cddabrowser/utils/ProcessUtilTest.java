package fun.hydd.cddabrowser.utils;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class ProcessUtilTest {

  @Test
  void execute(Vertx vertx, VertxTestContext testContext) {
    boolean test = true;
    ProcessBuilder processBuilder = new ProcessBuilder("pwd");
    processBuilder.directory(new File("/"));
    ProcessUtil.execute(processBuilder, vertx)
      .onComplete(testContext.succeeding(unused -> {
        assertThat(true).isTrue();
        testContext.completeNow();
      }));
  }

}
