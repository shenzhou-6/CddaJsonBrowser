package fun.hydd.cddabrowser;

import fun.hydd.cddabrowser.entity.JsonEntry;
import fun.hydd.cddabrowser.entity.Version;
import fun.hydd.cddabrowser.exception.NoNeedUpdateException;
import fun.hydd.cddabrowser.utils.FileUtil;
import fun.hydd.cddabrowser.utils.HttpUtil;
import fun.hydd.cddabrowser.utils.JsonEntryUtil;
import fun.hydd.cddabrowser.utils.JsonUtil;
import fun.hydd.cddabrowser.utils.MongoDBUtil;
import fun.hydd.cddabrowser.utils.VersionUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcuessVerticle extends AbstractVerticle {
  public static final String PROJECT_NAME = "cdda-browser";
  private static final String COLLECTION_TEST = "test";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private String translateDirPath;
  private Version latestVersion;

  private MongoClient mongoClient;
  private JsonObject config;
  private JsonEntryUtil jsonEntryUtil;
  @Override
  public void start(Promise<Void> startPromise) {
    config = config();
    mongoClient = MongoClient.createShared(this.vertx, config().getJsonObject("mongo"));
    vertx.setTimer(1000L, aLong -> timedUpdateGameData());
    jsonEntryUtil = new JsonEntryUtil(mongoClient);
    startPromise.complete();
  }

  private void timedUpdateGameData() {
//    VersionUtil.catchLatestVersionFromGithub(vertx)
//      .onSuccess(version -> latestVersion = version)
//      .compose(this::judgeIsNeedUpdate)
//      .onSuccess(version -> FileUtil.clearUnzipDirAndTranslateDir())
//      .compose(this::downloadGameZip)
//      .compose(gameZipPath -> ProcessUtil.unzip(vertx, gameZipPath, FileUtil.getUnzipDirPath()))
//      .onSuccess(unused -> gameRootDirPath = FileUtil.findGameRootDirPath())
//      .compose(unused -> ProcessUtil.compileMo(vertx, gameRootDirPath))
//      .compose(unused -> FileUtil.copyEnJsonFile(vertx, gameRootDirPath, translateDirPath))
//      .compose(unused -> ProcessUtil.translateJsonFile(vertx, FileUtil.getTranslatePythonShellPath(), gameRootDirPath
//        , translateDirPath))
    latestVersion = config.getJsonObject("version").mapTo(Version.class);
    translateDirPath = config.getString("HOME")+latestVersion.getTagName();
    this.processOriginalJsonEntry()
      .onSuccess(unused -> logger.info("SAVE VERSION"))
      .onFailure(throwable -> {
        if (throwable instanceof NoNeedUpdateException) {
          this.logger.warn(throwable.getMessage());
        } else {
          this.logger.error("TimedUpdateVersion() is fail:\n", throwable);
        }
      });
  }

  private Future<Void> processOriginalJsonEntry() {
    return jsonEntryUtil.getAllOriginalCollection()
      .compose(this::processCollectionListOriginalJsonEntry);
  }

  private Future<Void> processCollectionListOriginalJsonEntry(List<String> collectionList) {
    if (collectionList.isEmpty()) {
      return Future.succeededFuture();
    }
    String collection = collectionList.remove(0);
    return jsonEntryUtil.getSortedMod(collection)
      .compose(strings -> processCollectionListOriginalJsonEntryByMod(collection, strings));
  }

  private Future<Void> processCollectionListOriginalJsonEntryByMod(String collection,
                                                                   List<String> sortedModList) {
    if (sortedModList.isEmpty()) {
      return Future.succeededFuture();
    }
    String mod = sortedModList.remove(0);
    logger.info("start process {},mod is {}", collection, mod);
    return jsonEntryUtil.getNeedProcessInheritJsonObjectByMod(collection, latestVersion, mod)
      .compose(jsonObjectList -> Future.succeededFuture(jsonObjectList.stream()
        .map(jsonObject -> {
          JsonEntry jsonEntry = jsonObject.mapTo(JsonEntry.class);
          jsonEntry.setData(jsonObject.getJsonObject("data"));
          return jsonEntry;
        })
        .collect(Collectors.toList())))
      .compose(jsonEntryList -> jsonEntryUtil.processInheritJsonEntryList(jsonEntryList, sortedModList))
      .compose(jsonEntryList -> jsonEntryUtil.processNewJsonEntryList(jsonEntryList))
      .compose(bulkOperationListMap -> MongoDBUtil.bulkWriteBulkOperationListMap(mongoClient,
        bulkOperationListMap))
      .compose(unused -> processCollectionListOriginalJsonEntryByMod(collection, sortedModList));
  }

  private Future<Void> processGameJsonFile() {
    List<File> fileList = new ArrayList<>();
    fileList.add(new File(translateDirPath));
    return FileUtil.scanDirectory(fileList, this::processFile);
  }

  private Future<Void> processFile(File file) {
    if (file.isDirectory() || !JsonUtil.isJsonFile(file) || JsonUtil.isIgnoreJsonFile(file)) {
      return Future.succeededFuture();
    }
    logger.info("process file is {}", file);
    MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject());
    return vertx.fileSystem().readFile(file.getAbsolutePath())
      .compose(buffer -> MongoDBUtil.escapeBsonField(buffer, vertx))
      .compose(buffer -> {
        List<JsonObject> jsonObjectList = JsonUtil.bufferToJsonObjectList(buffer);
        return jsonEntryUtil.processNewJsonEntryListByJsonObjectList(
            JsonEntryUtil.parserLanguage(file),
            latestVersion,
            JsonEntryUtil.parserRelativePath(file, latestVersion),
            jsonObjectList)
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
