package fun.hydd.cddabrowser.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongoDBUtil {
  private MongoDBUtil() {
  }

  public static Future<Void> bulkWriteBulkOperationListMap(MongoClient mongoClient, Map<String,
    List<BulkOperation>> bulkOperationListMap) {
    //noinspection rawtypes
    final List<Future> futureList = new ArrayList<>();
    for (Map.Entry<String, List<BulkOperation>> entry : bulkOperationListMap.entrySet()) {
      futureList.add(mongoClient.bulkWrite(entry.getKey(), entry.getValue()));
    }
    return CompositeFuture.all(futureList)
      .compose(compositeFuture -> Future.succeededFuture());
  }

  public static Future<Buffer> escapeBsonField(final Buffer buffer, final Vertx vertx) {
    return vertx.executeBlocking(event -> {
      final Buffer newBuffer = Buffer.buffer(buffer.toString()
        .replaceAll("(\"\\w*)\\.(\\w*\":)", "$1{point}$2").
        replace("\"$\":", "\"{$}\":"));
      event.complete(newBuffer);
    }, true);
  }

  public static Future<Buffer> unescapeBsonField(final Buffer buffer, final Vertx vertx) {
    return vertx.executeBlocking(event -> {
      final Buffer newBuffer = Buffer.buffer(buffer.toString()
        .replaceAll("(\"\\w*)\\{point}(\\w*\":)", "$1.$2").
        replace("\"{$}\":", "\"$\":"));
      event.complete(newBuffer);
    }, true);
  }
}
