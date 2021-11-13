package fun.hydd.cddabrowser.utils;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessUtilTest {

  @Test
  void execute() {
    boolean test = true;
    ProcessBuilder processBuilder = new ProcessBuilder("pwd");
    processBuilder.directory(new File("/"));
    ProcessUtil.execute(processBuilder, Vertx.vertx())
      .onSuccess(unused -> assertThat(true).isTrue())
      .onFailure(throwable -> {
        LoggerFactory.getLogger(getClass().getName()).error("test is fail", throwable);
        assertThat(true).isFalse();
      });
  }

}
