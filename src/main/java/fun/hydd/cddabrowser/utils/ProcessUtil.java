package fun.hydd.cddabrowser.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProcessUtil {

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
}
