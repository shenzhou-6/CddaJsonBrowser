package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.JsonEntry;
import fun.hydd.cddabrowser.entity.Version;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonEntryUtil {

  private JsonEntryUtil() {
  }

  public static Future<Map<String, List<BulkOperation>>> processNewJsonEntryListByJsonObjectList(MongoClient mongoClient,
                                                                                                 List<JsonObject> jsonObjectList) {
    List<JsonEntry> jsonEntryList = new ArrayList<>();
    for (JsonObject jsonObject : jsonObjectList) {
      jsonEntryList.add(jsonObject.mapTo(JsonEntry.class));
    }
    return processNewJsonEntryList(mongoClient, jsonEntryList);
  }

  public static Future<Map<String, List<BulkOperation>>> processNewJsonEntryList(MongoClient mongoClient,
                                                                                 List<JsonEntry> jsonEntryList) {
    Map<String, List<BulkOperation>> bulkOperationListMap = new HashMap<>();
    //noinspection rawtypes
    List<Future> futureList = new ArrayList<>();
    for (JsonEntry jsonEntry : jsonEntryList) {
      futureList.add(processNewJsonEntry(mongoClient, jsonEntry)
        .onSuccess(bulkOperation -> {
          final String collectionName = jsonEntry.getCollectionName();
          if (bulkOperationListMap.containsKey(collectionName)) {
            bulkOperationListMap.get(collectionName).add(bulkOperation);
          } else {
            bulkOperationListMap.put(collectionName, List.of(bulkOperation));
          }
        }));
    }
    return CompositeFuture.all(futureList)
      .compose(compositeFuture -> Future.succeededFuture(bulkOperationListMap));
  }

  public static Future<BulkOperation> processNewJsonEntry(MongoClient mongoClient, JsonEntry jsonEntry) {
    return judgeCurrentEffectiveVersionJsonEntry(mongoClient, null, jsonEntry)
      .compose(bulkOperation -> judgeAfterVersionVersionJsonEntry(mongoClient, bulkOperation, jsonEntry))
      .compose(bulkOperation -> judgeBeforeVersionVersionJsonEntry(mongoClient, bulkOperation, jsonEntry));
  }

  public static Future<BulkOperation> judgeCurrentEffectiveVersionJsonEntry(MongoClient mongoClient,
                                                                            BulkOperation bulkOperation,
                                                                            JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateCurrentEffectiveVersionJsonEntryQuery(jsonEntry);
    return mongoClient.findOne(jsonEntry.getCollectionName(), queryCondition, new JsonObject())
      .compose(jsonObject -> {
        if (JsonUtil.isNotEmpty(jsonObject)) {
          JsonEntry dbJsonEntry = jsonObject.mapTo(JsonEntry.class);
          if (!dbJsonEntry.getData().equals(jsonEntry.getData()) &&
            dbJsonEntry.getEndVersion().equals(dbJsonEntry.getStartVersion())) {
            return Future.succeededFuture(generateReplaceJsonEntryBulkOperation(dbJsonEntry, jsonEntry));
          }
        }
        return Future.succeededFuture();
      });
  }

  public static Future<BulkOperation> judgeAfterVersionVersionJsonEntry(MongoClient mongoClient,
                                                                        BulkOperation bulkOperation,
                                                                        JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateAfterVersionJsonEntryQuery(jsonEntry);
    return mongoClient.findOne(jsonEntry.getCollectionName(), queryCondition, new JsonObject())
      .compose(jsonObject -> {
        if (JsonUtil.isNotEmpty(jsonObject)) {
          JsonEntry dbJsonEntry = jsonObject.mapTo(JsonEntry.class);
          if (dbJsonEntry.getData().equals(jsonEntry.getData())) {
            return Future.succeededFuture(generateUpdateAfterDBJsonEntryBulkOperation(dbJsonEntry,
              jsonEntry.getEndVersion()));
          } else {
            return Future.succeededFuture(generateInsertJsonEntryBulkOperation(jsonEntry));
          }
        }
        return Future.succeededFuture();
      });
  }

  public static Future<BulkOperation> judgeBeforeVersionVersionJsonEntry(MongoClient mongoClient,
                                                                         BulkOperation bulkOperation,
                                                                         JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateBeforeVersionJsonEntryQuery(jsonEntry);
    return mongoClient.findOne(jsonEntry.getCollectionName(), queryCondition, new JsonObject())
      .compose(jsonObject -> {
        if (JsonUtil.isNotEmpty(jsonObject)) {
          JsonEntry dbJsonEntry = jsonObject.mapTo(JsonEntry.class);
          if (dbJsonEntry.getData().equals(jsonEntry.getData())) {
            return Future.succeededFuture(generateUpdateBeforeDBJsonEntryBulkOperation(dbJsonEntry,
              jsonEntry.getStartVersion()));
          } else {
            return Future.succeededFuture(generateInsertJsonEntryBulkOperation(jsonEntry));
          }
        }
        return Future.succeededFuture();
      });
  }

  public static JsonObject generateCurrentEffectiveVersionJsonEntryQuery(final JsonEntry jsonEntry) {
    return generateEqualJsonEntryQuery(jsonEntry)
      .put("startVersion.created_at", new JsonObject()
        .put("$lte", jsonEntry.getStartVersion().getCreatedAt()))
      .put("endVersion.created_at", new JsonObject()
        .put("$gte", jsonEntry.getEndVersion().getCreatedAt()));
  }

  public static JsonObject generateAfterVersionJsonEntryQuery(final JsonEntry jsonEntry) {
    return generateEqualJsonEntryQuery(jsonEntry)
      .put("startVersion.created_at", new JsonObject()
        .put("$gt", jsonEntry.getEndVersion().getCreatedAt()));
  }

  public static JsonObject generateBeforeVersionJsonEntryQuery(final JsonEntry jsonEntry) {
    return generateEqualJsonEntryQuery(jsonEntry)
      .put("endVersion.created_at", new JsonObject()
        .put("$lt", jsonEntry.getStartVersion().getCreatedAt()));
  }

  public static JsonObject generateEqualJsonEntryQuery(JsonEntry jsonEntry) {
    return new JsonObject()
      .put("id", jsonEntry.getId())
      .put("type", jsonEntry.getType())
      .put("language", jsonEntry.getLanguage())
      .put("path", jsonEntry.getPath());
  }

  public static String parserId(JsonObject data) {
    String type = data.getString("type");
    String id = "";
    switch (type) {
      case "uncraft":
      case "recipe":
        if (data.containsKey("result")) {
          id = data.getString("result");
        } else if (data.containsKey("copy-from")) {
          id = data.getString("copy-from");
        }
        if (data.containsKey("id_suffix")) {
          id = id + data.getString("id_suffix");
        }
        return id;
      default:
        if (data.containsKey("id")) {
          id = data.getString("id");
        } else if (data.containsKey("abstract")) {
          id = data.getString("abstract");
        } else {
          id = type;
        }
        return id;
    }
  }

  public static String parserRelativePath(final File file, final Version version) {
    final String filePath = file.getAbsolutePath();
    final String tag = version.getTagName();
    return parserRelativePath(filePath, tag);
  }

  public static String parserRelativePath(String absolutePath, String tag) {
    int tagIndex = absolutePath.indexOf(tag);
    if (tagIndex == -1) {
      return "";
    }
    return absolutePath.substring(absolutePath.indexOf("/", tagIndex + tag.length()));
  }

  public static String parserModByPath(final String path) {
    if (path.startsWith("/data/mods/")) {
      return path.split("/")[3];
    } else {
      return "dda";
    }
  }

  public static BulkOperation generateInsertJsonEntryBulkOperation(final JsonEntry jsonEntry) {
    return BulkOperation.createInsert(JsonObject.mapFrom(jsonEntry));
  }

  public static BulkOperation generateReplaceJsonEntryBulkOperation(JsonEntry dbJsonEntry, JsonEntry newJsonEntry) {
    return BulkOperation.createReplace(JsonObject.mapFrom(dbJsonEntry), JsonObject.mapFrom(newJsonEntry));
  }

  public static BulkOperation generateUpdateBeforeDBJsonEntryBulkOperation(final JsonEntry jsonEntry,
                                                                           final Version newVersion) {
    final JsonObject queryCondition = JsonObject.mapFrom(jsonEntry);
    final JsonObject update = new JsonObject()
      .put("endVersion", JsonObject.mapFrom(newVersion));
    return BulkOperation.createUpdate(queryCondition, update);
  }

  public static BulkOperation generateUpdateAfterDBJsonEntryBulkOperation(final JsonEntry jsonEntry,
                                                                          final Version newVersion) {
    final JsonObject queryCondition = JsonObject.mapFrom(jsonEntry);
    final JsonObject update = new JsonObject()
      .put("startVersion", JsonObject.mapFrom(newVersion));
    return BulkOperation.createUpdate(queryCondition, update);
  }
}
