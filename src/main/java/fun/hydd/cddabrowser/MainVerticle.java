package fun.hydd.cddabrowser;

import fun.hydd.cddabrowser.entity.Version;
import fun.hydd.cddabrowser.exception.NoNeedUpdateException;
import fun.hydd.cddabrowser.utils.VersionUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MainVerticle extends AbstractVerticle {
  public static final String PROJECT_NAME = "cdda-browser";
  private static final String MONGODB_URL = "mongodb://127.0.0.1:27017";
  private static final String COLLECTION_TEST = "cdda_browser";

  @Override
  public void start(Promise<Void> startPromise) {
    final JsonObject mongoConfig = new JsonObject()
      .put("connection_string", MONGODB_URL)
      .put("db_name", COLLECTION_TEST);
    MongoClient.createShared(this.vertx, mongoConfig);
    startPromise.complete();
  }

  private Future<Version> judgeIsNeedUpdate(Version version) {
    MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject());
    return VersionUtil.findLatestVersionByBranch(mongoClient, version.getBranch())
      .compose(dbVersion -> {
        if (dbVersion == null || version.getCreatedAt().after(dbVersion.getCreatedAt())) {
          return Future.succeededFuture(version);
        } else {
          return Future.failedFuture(new NoNeedUpdateException());
        }
      });
  }
}
