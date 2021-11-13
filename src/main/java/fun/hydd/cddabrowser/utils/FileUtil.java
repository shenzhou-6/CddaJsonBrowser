package fun.hydd.cddabrowser.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
  static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
  static final String[] JSON_DIR_PATHS = new String[]{
    "/data/core/",
    "/data/help/",
    "/data/json/",
    "/data/mods/",
    "/data/raw/"};

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

  private Future<Void> copyEnJsonFile(Vertx vertx, final String gameRootDirPath, final String translateGameDirPath) {
    logger.info("start copyEnJsonFile,start:{},to:{}", gameRootDirPath,
      translateGameDirPath);
    final FileSystem fileSystem = vertx.fileSystem();
    @SuppressWarnings("rawtypes") final List<Future> futureList = new ArrayList<>();
    final String toDirPath = translateGameDirPath + "/en";
    for (final String jsonDirPath : JSON_DIR_PATHS) {
      final String jsonDir = gameRootDirPath + jsonDirPath;
      final String toDetailedDirPath = toDirPath + jsonDirPath;
      final File toDirDetailedFile = new File(toDetailedDirPath);
      if (!toDirDetailedFile.exists() && !toDirDetailedFile.mkdirs()) {
        return Future.failedFuture("mkdirs " + toDirPath + " fail");
      }
      futureList.add(fileSystem.copyRecursive(jsonDir, toDetailedDirPath, true));
    }
    return CompositeFuture.join(futureList)
      .compose(compositeFuture -> Future.succeededFuture());
  }
}
