package fun.hydd.cddabrowser.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

  public static void clearUnzipDirAndTranslateDir() {
    File unzipDirFile = new File(getUnzipDirPath());
    if (unzipDirFile.exists()) {
      for (File file : Objects.requireNonNull(unzipDirFile.listFiles())) {
        try {
          FileUtils.deleteDirectory(file);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    File translateDirFile = new File(getTranslateDirPath());
    if (translateDirFile.exists()) {
      for (File file : Objects.requireNonNull(translateDirFile.listFiles())) {
        try {
          logger.info("delete have {}", file);
          FileUtils.forceDeleteOnExit(file);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static Future<Void> copyEnJsonFile(Vertx vertx, final String gameRootDirPath,
                                            final String translateGameDirPath) {
    logger.info("start copyEnJsonFile,start:{},to:{}", gameRootDirPath,
      translateGameDirPath);
    final FileSystem fileSystem = vertx.fileSystem();
    @SuppressWarnings("rawtypes") final List<Future> futureList = new ArrayList<>();
    final String toDirPath = translateGameDirPath + "en";
    final File toDirFile = new File(toDirPath);
    if (toDirFile.exists()) {
      try {
        FileUtils.deleteDirectory(toDirFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
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
    List<Future> futureList = new ArrayList<>();
    for (int i = 0; i <= 4 && !fileList.isEmpty(); i++) {
      File directory = fileList.remove(0);
      if (directory.isDirectory()) {
        fileList.addAll(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
      }
      futureList.add(handler.apply(directory));
    }
    return CompositeFuture.any(futureList).compose(o -> scanDirectory(fileList, handler));
  }

  public static Future<Void> scanDirectoryAsync(List<File> fileList, Function<File, Future> handler) {
    List<File> fileList1 = scanJsonDir(fileList.get(0));
    List<Future> futureList = new ArrayList<>();
    for (File file : fileList1) {
      futureList.add(handler.apply(file));
    }
    return CompositeFuture.all(futureList)
      .compose(compositeFuture -> Future.succeededFuture());
  }

  public static List<File> scanJsonDir(final File jsonDir) {
    if (!jsonDir.exists()) {
      throw new NullPointerException("jsonDir not exists");
    }
    if (!jsonDir.isDirectory()) {
      throw new NullPointerException("jsonDir not directory");
    }
    final File[] subFiles = jsonDir.listFiles();
    final List<File> jsonFiles = new ArrayList<>();
    if (subFiles == null || subFiles.length == 0) {
      return jsonFiles;
    }
    for (final File subFile : subFiles) {
      if (subFile.isFile() && subFile.getName().endsWith("json")) {
        jsonFiles.add(subFile);
      } else if (subFile.isDirectory()) {
        jsonFiles.addAll(scanJsonDir(subFile));
      }
    }
    return jsonFiles;
  }
}
