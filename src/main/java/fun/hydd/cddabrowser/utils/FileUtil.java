package fun.hydd.cddabrowser.utils;

import java.io.File;

public class FileUtil {
  private FileUtil() {
  }

  public static String findUnzipGameDirPathByUnzipDir(String unzipDirPath) {
    return findUnzipGameDirPathByUnzipDir(new File(unzipDirPath));
  }

  public static String findUnzipGameDirPathByUnzipDir(File unzipDirFile) {
    final File[] unzips = unzipDirFile.listFiles();
    if (unzips != null && unzips.length == 1 && unzips[0].isDirectory()) {
      return unzips[0].getAbsolutePath();
    }
    return "";
  }
}
