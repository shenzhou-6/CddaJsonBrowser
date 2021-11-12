package fun.hydd.cddabrowser.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProcessUtil {

  private ProcessUtil() {
  }

  /**
   * @param processBuilder processBuilder
   * @param vertx          current vertx
   * @return no return
   */
  public static Future<Void> execute(final ProcessBuilder processBuilder, final Vertx vertx) {
    processBuilder.inheritIO();
    try {
      final Process process = processBuilder.start();
      return vertx.executeBlocking(voidPromise -> {
        try {
          final boolean result = process.waitFor(4L, TimeUnit.MINUTES);
          process.destroy();
          if (result) {
            voidPromise.complete();
          } else {
            final StringBuilder stringBuilder = new StringBuilder();
            for (final String command : processBuilder.command()) {
              stringBuilder.append(command).append("\n");
            }
            voidPromise.fail("process execute fail\n" + stringBuilder);
          }
        } catch (final InterruptedException e) {
          voidPromise.fail(e);
          Thread.currentThread().interrupt();
        }
      }, true);
    } catch (final IOException e) {
      return Future.failedFuture(e);
    }
  }
}
