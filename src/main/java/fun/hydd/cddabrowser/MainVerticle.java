package fun.hydd.cddabrowser;

import fun.hydd.cddabrowser.entity.Version;
import fun.hydd.cddabrowser.exception.NoNeedUpdateException;
import fun.hydd.cddabrowser.utils.FileUtil;
import fun.hydd.cddabrowser.utils.HttpUtil;
import fun.hydd.cddabrowser.utils.JsonEntryUtil;
import fun.hydd.cddabrowser.utils.JsonUtil;
import fun.hydd.cddabrowser.utils.MongoDBUtil;
import fun.hydd.cddabrowser.utils.ProcessUtil;
import fun.hydd.cddabrowser.utils.VersionUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MainVerticle extends AbstractVerticle {
  public static final String PROJECT_NAME = "cdda-browser";
  private static final String MONGODB_URL = "mongodb://127.0.0.1:27017";
  private static final String COLLECTION_TEST = "cdda_browser";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private String gameRootDirPath;
  private String translateDirPath;

  @Override
  public void start(Promise<Void> startPromise) {
    final JsonObject mongoConfig = new JsonObject()
      .put("connection_string", MONGODB_URL)
      .put("db_name", COLLECTION_TEST);
    MongoClient.createShared(this.vertx, mongoConfig);
    translateDirPath = FileUtil.getTranslateDirPath();
    startPromise.complete();
  }

  private void timedUpdateGameData() {
    VersionUtil.catchLatestVersionFromGithub(vertx)
      .compose(this::judgeIsNeedUpdate)
      .compose(this::downloadGameZip)
      .compose(gameZipPath -> ProcessUtil.unzip(vertx, gameZipPath, FileUtil.getUnzipDirPath()))
      .onSuccess(unused -> gameRootDirPath = FileUtil.findGameRootDirPath())
      .compose(unused -> ProcessUtil.compileMo(vertx, gameRootDirPath))
      .compose(unused -> FileUtil.copyEnJsonFile(vertx, gameRootDirPath, translateDirPath))
      .compose(unused -> ProcessUtil.translateJsonFile(vertx, FileUtil.getTranslatePythonShellPath(), gameRootDirPath
        , translateDirPath))
      .compose(unused -> this.processOriginalJsonData())
      .onSuccess(unused -> logger.info("SAVE VERSION"))
      .onFailure(throwable -> {
        if (throwable.getMessage().startsWith("no need update")) {
          this.logger.warn(throwable.getMessage());
        } else {
          this.logger.error("TimedUpdateVersion() is fail:", throwable);
        }
      });


  }

  private Future<Void> processOriginalJsonData() {
    return FileUtil.scanDirectory(List.of(new File(translateDirPath)), this::processFile);
  }

  private Future<Void> processFile(File file) {
    MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject());
    return vertx.fileSystem().readFile(file.getAbsolutePath())
      .compose(buffer -> {
        List<JsonObject> jsonObjectList = JsonUtil.bufferToJsonObjectList(buffer);
        return JsonEntryUtil.processNewJsonEntryListByJsonObjectList(mongoClient, jsonObjectList)
          .compose(stringListMap -> MongoDBUtil.bulkWriteBulkOperationListMap(mongoClient, stringListMap));
      });
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

  private Future<String> downloadGameZip(Version version) {
    final String tagName = version.getTagName();
    String downloadUrl = HttpUtil.GITHUB_DOWNLOAD_SOURCE_ZIP_URL_PREFIX + tagName;
    String targetFilePath = FileUtil.getApplicationHomePath() + tagName;
    return HttpUtil.downloadFile(vertx, downloadUrl, targetFilePath);
  }

}
