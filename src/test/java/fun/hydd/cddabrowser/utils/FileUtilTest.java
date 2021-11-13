package fun.hydd.cddabrowser.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class FileUtilTest {
  Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  void findUnzipGameDirPathByUnzipDirPath() {
    File unzipDirFile = mock(File.class);
    File unzipGameDirFile = mock(File.class);
    when(unzipGameDirFile.getAbsolutePath()).thenReturn("test unzipGameDirFile");
    when(unzipGameDirFile.isDirectory()).thenReturn(true);
    File[] unzipGameDirFiles = new File[]{unzipGameDirFile};
    when(unzipDirFile.listFiles()).thenReturn(unzipGameDirFiles);

    String result = FileUtil.findUnzipGameDirPathByUnzipDir(unzipDirFile);

    assertThat(result).isEqualTo("test unzipGameDirFile");
  }

  @Test
  void scanDirectory(Vertx vertx, VertxTestContext testContext) {
    List<File> fileList = new ArrayList<>();
    fileList.add(new File(Objects.requireNonNull(this.getClass().getResource("/")).getPath()));
    FileUtil.scanDirectory(fileList, file -> {
      logger.info("{}", file.getAbsolutePath());
      return Future.succeededFuture();
    }).onComplete(testContext.succeeding(o -> testContext.completeNow()));
  }
}
