package fun.hydd.cddabrowser.utils;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.Objects;

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

  @Test
  void unzip(Vertx vertx, VertxTestContext testContext) {
    final String ResourceRootPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath();
    String unzipDirPath = ResourceRootPath + "unzip/";
    String unzipGameDirPath = ResourceRootPath + "unzip/cdda-experimental-2021-10-1-1000/";
    String jsonPath = unzipGameDirPath + "test.json";
    String zipFilePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("testZip.zip")).getPath();
    if (new File(unzipDirPath).exists()) {
      //noinspection ResultOfMethodCallIgnored
      new File(unzipDirPath).delete();
    }
    ProcessUtil.unzip(vertx, zipFilePath, unzipDirPath)
      .onComplete(testContext.succeeding(unused -> {
        assertThat(new File(unzipGameDirPath)).exists().isDirectory();
        assertThat(new File(jsonPath)).exists().isFile();
        testContext.completeNow();
      }));
  }

}
