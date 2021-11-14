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
import java.util.Objects;
import java.util.function.Function;

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

  public static String getApplicationHomePath() {
    return System.getenv("HOME") + File.separator;
  }

  public static String getUnzipDirPath() {
    return getApplicationHomePath() + "Unzip/";
  }

  public static String getTranslateDirPath() {
    return getApplicationHomePath() + "Translate/";
  }

  public static String getTranslatePythonShellPath() {
    return getApplicationHomePath() + "Documents/translate.py";
  }

  public static String findGameRootDirPath() {
    return findGameRootDirPath(getUnzipDirPath());
  }

  public static String findGameRootDirPath(String unzipDirPath) {
    return findGameRootDirPath(new File(unzipDirPath));
  }

  public static String findGameRootDirPath(File unzipDirFile) {
    final File[] unzips = unzipDirFile.listFiles();
    if (unzips != null && unzips.length == 1 && unzips[0].isDirectory()) {
      return unzips[0].getAbsolutePath();
    }
    return "";
  }

  public static Future<Void> copyEnJsonFile(Vertx vertx, final String gameRootDirPath,
                                            final String translateGameDirPath) {
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

  public static <R> Future<Void> scanDirectory(List<File> fileList, Function<File, Future<R>> handler) {
    if (fileList.isEmpty()) {
      return Future.succeededFuture();
    }
    File directory = fileList.remove(0);
    if (directory.isDirectory()) {
      for (File file : Objects.requireNonNull(directory.listFiles())) {
        if (file.isDirectory()) {
          fileList.add(file);
        }
      }
    }
    return handler.apply(directory).compose(o -> scanDirectory(fileList, handler));
  }
}
