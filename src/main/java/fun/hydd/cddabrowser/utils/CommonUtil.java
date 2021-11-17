package fun.hydd.cddabrowser.utils;

import io.vertx.core.Future;

import java.util.List;
import java.util.function.Function;

public class CommonUtil {

  private CommonUtil() {
  }

  public static <T, R> Future<R> chainCall(List<T> list, Function<T, Future<R>> method) {
    return list.stream().reduce(Future.succeededFuture(),
      (acc, item) -> acc.compose(v -> method.apply(item)),
      (a, b) -> Future.succeededFuture());
  }
}
