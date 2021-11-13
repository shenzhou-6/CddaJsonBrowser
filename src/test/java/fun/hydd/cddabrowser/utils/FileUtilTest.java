package fun.hydd.cddabrowser.utils;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileUtilTest {

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
}
