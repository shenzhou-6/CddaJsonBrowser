package fun.hydd.cddabrowser.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProcessUtil {
  static Logger logger = LoggerFactory.getLogger(ProcessUtil.class);

  private ProcessUtil() {
  }

  public static Future<Void> execute(final ProcessBuilder processBuilder, final Vertx vertx) {
    return execute(processBuilder, vertx, 4L, true);
  }

  public static Future<Void> execute(final ProcessBuilder processBuilder, final Vertx vertx, long timeout,
                                     boolean order) {
    processBuilder.inheritIO();
    try {
      final Process process = processBuilder.start();
      return vertx.executeBlocking(voidPromise -> {
        try {
          final boolean result = process.waitFor(timeout, TimeUnit.MINUTES);
          process.destroy();
          if (result) {
            voidPromise.complete();
          } else {
            final StringBuilder stringBuilder = new StringBuilder();
            for (final String command : processBuilder.command()) {
              stringBuilder.append(command).append("\n");
            }
            voidPromise.fail("process execute fail,commands is\n" + stringBuilder + "exit code is " + process.exitValue());
          }
        } catch (final InterruptedException e) {
          voidPromise.fail(e);
          Thread.currentThread().interrupt();
        }
      }, order);
    } catch (final IOException e) {
      return Future.failedFuture(e);
    }
  }

  public static Future<Void> unzip(Vertx vertx, String zipFilePath, String unzipDirPath) {
    logger.info("start unzip {}", zipFilePath);
    final ProcessBuilder p = new ProcessBuilder("unzip", "-qqo", zipFilePath, "-d", unzipDirPath);
    p.directory(new File(new File(zipFilePath).getParent()));
    return ProcessUtil.execute(p, vertx)
      .onSuccess(event -> logger.info("unzip {} success", zipFilePath));
  }

  public static Future<Void> compileMo(Vertx vertx, String gameDirPath) {
    final String compileMoShell = gameDirPath + "/lang/compile_mo.sh";
    final ProcessBuilder processBuilder = new ProcessBuilder("sh", compileMoShell);
    processBuilder.directory(new File(gameDirPath));
    return ProcessUtil.execute(processBuilder, vertx)
      .onSuccess(event -> logger.info("compileMo success,game dir is {}", gameDirPath));
  }

  public static Future<Void> translateJsonFile(Vertx vertx, String translatePythonShellPath, String gameDirPath,
                                               String translateDirPath) {
    logger.info("start translate,translate python shell is {}", translatePythonShellPath);
    final ProcessBuilder processBuilder = new ProcessBuilder("python", translatePythonShellPath, "-o",
      translateDirPath);
    processBuilder.directory(new File(gameDirPath));
    return ProcessUtil.execute(processBuilder, vertx)
      .onSuccess(event -> logger.info("translate success,translate dir is {}", translateDirPath));
  }
}
