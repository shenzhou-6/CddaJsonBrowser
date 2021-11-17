package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.JsonEntry;
import fun.hydd.cddabrowser.entity.Version;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonEntryUtil {
  public static final String START_VERSION_CREATED_AT = "startVersion.created_at";
  public static final String END_VERSION_CREATED_AT = "endVersion.created_at";
  static final Logger logger = LoggerFactory.getLogger(JsonEntryUtil.class);

  private JsonEntryUtil() {
  }

  public static Future<List<JsonEntry>> processInheritJsonEntryList(MongoClient mongoClient,
                                                                    List<JsonEntry> jsonEntryList,
                                                                    List<String> sortedModList) {
    List<JsonEntry> newJsonEntryList = new ArrayList<>();
    return CommonUtil
      .chainCall(jsonEntryList, jsonEntry ->
        processInheritJsonEntry(mongoClient, jsonEntry, sortedModList, jsonEntry.getMod(), null)
          .onSuccess(newJsonEntryList::add))
      .compose(compositeFuture -> Future.succeededFuture(newJsonEntryList));
  }

  public static Future<JsonEntry> processInheritJsonEntry(MongoClient mongoClient, JsonEntry jsonEntry,
                                                          List<String> sortedModList, String currentMod,
                                                          JsonEntry newJsonEntry) {
    if (newJsonEntry != null || sortedModList.isEmpty()) {
      if (sortedModList.isEmpty()) {
        logger.warn("no find all supper data for {}", jsonEntry);
      } else {
        logger.warn("in mod {} no find all supper data for {}", currentMod, jsonEntry);
      }
      return Future.succeededFuture(jsonEntry);
    }
    String mod = "dda";
    int index = sortedModList.indexOf(currentMod);
    if (index > 1) {
      mod = sortedModList.get(index - 1);
    }
    JsonObject data = jsonEntry.getData();
    jsonEntry.setOriginal(false);
    if (!data.containsKey(JsonUtil.INHERIT_FIELD_COPY_FROM)) {
      return Future.succeededFuture(jsonEntry);
    }
    String copyFrom = data.getString(JsonUtil.INHERIT_FIELD_COPY_FROM);
    JsonObject query = new JsonObject()
      .put("id", copyFrom)
      .put("mod", mod);
    return getCurrentEffectiveJsonEntry(mongoClient, query, jsonEntry.getCollectionName())
      .compose(jsonObject -> {
        if (JsonUtil.isNotEmpty(jsonObject)) {
          JsonObject superData = jsonObject.getJsonObject("data");
          jsonEntry.setData(processInheritJsonObject(data, superData));
        }
        return Future.succeededFuture(jsonEntry);
      })
      .compose(jsonEntry1 -> processInheritJsonEntry(mongoClient, jsonEntry, sortedModList, currentMod, jsonEntry1));
  }

  public static Future<List<String>> getSortedMod(MongoClient mongoClient, String collection) {
    return getAllModInfoDataJsonObject(mongoClient, collection)
      .compose(jsonObjectList -> {
        Map<String, List<String>> map = getAllModInfoMap(jsonObjectList);
        List<String> sortKeyList = topologySort(map);
        return Future.succeededFuture(sortKeyList);
      });
  }

  private static Future<List<JsonObject>> getAllModInfoDataJsonObject(MongoClient mongoClient, String collection) {
    JsonObject query = new JsonObject()
      .put("type", "MOD_INFO");
    return mongoClient.find(collection, query)
      .compose(jsonObjectList -> Future.succeededFuture(
        jsonObjectList.stream().map(jsonObject -> jsonObject.getJsonObject("data")).collect(Collectors.toList())));
  }

  private static Map<String, List<String>> getAllModInfoMap(List<JsonObject> dataJsonObject) {
    Map<String, List<String>> map = new HashMap<>();
    for (JsonObject jsonObject : dataJsonObject) {
      List<String> dependencies = new ArrayList<>();
      if (jsonObject.containsKey("dependencies")) {
        for (Object o : jsonObject.getJsonArray("dependencies")) {
          dependencies.add(String.valueOf(o));
        }
      }
      map.put(jsonObject.getString("id"), dependencies);
    }
    return map;
  }

  private static List<String> topologySort(Map<String, List<String>> map) {
    List<String> result = new ArrayList<>();
    while (!map.isEmpty()) {
      for (Map.Entry<String, List<String>> entry : map.entrySet()) {
        if (entry.getValue().isEmpty()) {
          result.add(entry.getKey());
        }
      }
      for (String mod : result) {
        map.remove(mod);
      }
      for (Map.Entry<String, List<String>> entry : map.entrySet()) {
        entry.getValue().removeIf(result::contains);
      }
    }
    return result;
  }

  public static JsonObject processInheritJsonObject(JsonObject data, JsonObject superData) {
    superData.remove(JsonUtil.INHERIT_FIELD_ABSTRACT);
    if (data.containsKey(JsonUtil.INHERIT_FIELD_EXTEND)) {
      JsonObject jsonObject = data.getJsonObject(JsonUtil.INHERIT_FIELD_EXTEND);
      for (Map.Entry<String, Object> entry : jsonObject) {
        final String key = entry.getKey();
        JsonArray oldJsonArray;
        if (superData.containsKey(key)) {
          oldJsonArray = superData.getJsonArray(key);
          JsonArray newJsonArray = JsonUtil.convertObjectToJsonArray(entry.getValue());
          for (Object object : newJsonArray) {
            oldJsonArray.add(object);
          }
        } else {
          oldJsonArray = JsonUtil.convertObjectToJsonArray(entry.getValue());
        }
        superData.put(key, oldJsonArray);
      }
      data.remove(JsonUtil.INHERIT_FIELD_EXTEND);
    }
    if (data.containsKey(JsonUtil.INHERIT_FIELD_DELETE)) {
      JsonObject jsonObject = data.getJsonObject(JsonUtil.INHERIT_FIELD_DELETE);
      for (Map.Entry<String, Object> entry : jsonObject) {
        final String key = entry.getKey();
        JsonArray oldJsonArray = superData.getJsonArray(key);
        JsonArray newJsonArray = JsonUtil.convertObjectToJsonArray(entry.getValue());
        for (Object object : newJsonArray) {
          oldJsonArray.remove(object);
        }
        superData.put(key, oldJsonArray);
      }
      data.remove(JsonUtil.INHERIT_FIELD_DELETE);
    }
    if (data.containsKey(JsonUtil.INHERIT_FIELD_RELATIVE)) {
      JsonObject jsonObject = data.getJsonObject(JsonUtil.INHERIT_FIELD_RELATIVE);
      for (Map.Entry<String, Object> entry : jsonObject) {
        final String key = entry.getKey();
        superData.put(key, superData.getFloat(key) + Double.parseDouble(String.valueOf(entry.getValue())));
      }
      data.remove(JsonUtil.INHERIT_FIELD_RELATIVE);
    }
    if (data.containsKey(JsonUtil.INHERIT_FIELD_PROPORTIONAL)) {
      JsonObject jsonObject = data.getJsonObject(JsonUtil.INHERIT_FIELD_PROPORTIONAL);
      for (Map.Entry<String, Object> entry : jsonObject) {
        final String key = entry.getKey();
        superData.put(key, superData.getFloat(key) * Double.parseDouble(String.valueOf(entry.getValue())));
      }
      data.remove(JsonUtil.INHERIT_FIELD_PROPORTIONAL);
    }
    for (Map.Entry<String, Object> entry : data) {
      superData.put(entry.getKey(), entry.getValue());
    }
    return superData;
  }

  public static Future<List<JsonObject>> getNeedProcessInheritJsonObjectByMod(MongoClient mongoClient,
                                                                              String collection,
                                                                              Version version,
                                                                              String mod) {
    JsonObject query = new JsonObject()
      .put("endVersion", JsonObject.mapFrom(version))
      .put("mod", mod);
    return mongoClient.find(collection, query);
  }

  public static Future<List<String>> getAllOriginalCollection(MongoClient mongoClient) {
    return mongoClient.getCollections()
      .compose(strings ->
        Future.succeededFuture(
          strings.stream().filter(s -> s.endsWith("_original")).collect(Collectors.toList())
        )
      );
  }

  public static Future<Map<String, List<BulkOperation>>> processNewJsonEntryListByJsonObjectList(MongoClient mongoClient,
                                                                                                 String language,
                                                                                                 Version version,
                                                                                                 String path,
                                                                                                 List<JsonObject> jsonObjectList) {
    List<JsonEntry> jsonEntryList = new ArrayList<>();
    for (JsonObject jsonObject : jsonObjectList) {
      jsonEntryList.add(new JsonEntry(jsonObject, language, version, path));
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
            bulkOperationListMap.put(collectionName, new ArrayList<>(List.of(bulkOperation)));
          }
        }));
    }
    return CompositeFuture.all(futureList)
      .compose(compositeFuture -> Future.succeededFuture(bulkOperationListMap));
  }

  public static Future<BulkOperation> processNewJsonEntry(MongoClient mongoClient, JsonEntry jsonEntry) {
    return judgeCurrentEffectiveVersionJsonEntry(mongoClient, null, jsonEntry)
      .compose(bulkOperation -> judgeAfterVersionVersionJsonEntry(mongoClient, bulkOperation, jsonEntry))
      .compose(bulkOperation -> judgeBeforeVersionVersionJsonEntry(mongoClient, bulkOperation, jsonEntry))
      .compose(bulkOperation -> judgeIsNewJsonEntry(bulkOperation, jsonEntry));
  }

  public static Future<JsonObject> getCurrentEffectiveJsonEntry(MongoClient mongoClient, JsonObject queryCondition,
                                                                String collection) {
    final FindOptions findOptions = new FindOptions()
      .setSort(new JsonObject().put(START_VERSION_CREATED_AT, -1))
      .setLimit(1);
    return mongoClient
      .findWithOptions(collection, queryCondition, findOptions)
      .compose(jsonObjectList -> {
        if (jsonObjectList == null || jsonObjectList.isEmpty()) {
          return Future.succeededFuture();
        } else {
          return Future.succeededFuture(jsonObjectList.get(0));
        }
      });
  }

  public static Future<JsonObject> getAfterJsonEntry(MongoClient mongoClient, JsonObject queryCondition,
                                                     String collection) {
    final FindOptions findOptions = new FindOptions()
      .setSort(new JsonObject().put(START_VERSION_CREATED_AT, 1))
      .setLimit(1);
    return mongoClient
      .findWithOptions(collection, queryCondition, findOptions)
      .compose(jsonObjectList -> {
        if (jsonObjectList == null || jsonObjectList.isEmpty()) {
          return Future.succeededFuture();
        } else {
          return Future.succeededFuture(jsonObjectList.get(0));
        }
      });
  }

  public static Future<JsonObject> getBeforeJsonEntry(MongoClient mongoClient, JsonObject queryCondition,
                                                      String collection) {
    final FindOptions findOptions = new FindOptions()
      .setSort(new JsonObject().put(END_VERSION_CREATED_AT, -1))
      .setLimit(1);
    return mongoClient
      .findWithOptions(collection, queryCondition, findOptions)
      .compose(jsonObjectList -> {
        if (jsonObjectList == null || jsonObjectList.isEmpty()) {
          return Future.succeededFuture();
        } else {
          return Future.succeededFuture(jsonObjectList.get(0));
        }
      });
  }

  public static Future<BulkOperation> judgeCurrentEffectiveVersionJsonEntry(MongoClient mongoClient,
                                                                            BulkOperation bulkOperation,
                                                                            JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateCurrentEffectiveVersionJsonEntryQuery(jsonEntry);
    return getCurrentEffectiveJsonEntry(mongoClient, queryCondition, jsonEntry.getCollectionName())
      .compose(jsonObject -> {
        if (JsonUtil.isNotEmpty(jsonObject)) {
          JsonEntry dbJsonEntry = jsonObject.mapTo(JsonEntry.class);
          if (!dbJsonEntry.getData().equals(jsonEntry.getData()) &&
            dbJsonEntry.getEndVersion().equals(dbJsonEntry.getStartVersion())) {
            return Future.succeededFuture(generateReplaceJsonEntryBulkOperation(dbJsonEntry,
              jsonEntry));
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
    return getAfterJsonEntry(mongoClient, queryCondition, jsonEntry.getCollectionName())
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
    return getBeforeJsonEntry(mongoClient, queryCondition, jsonEntry.getCollectionName())
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

  public static Future<BulkOperation> judgeIsNewJsonEntry(BulkOperation bulkOperation, JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    } else {
      return Future.succeededFuture(generateInsertJsonEntryBulkOperation(jsonEntry));
    }
  }

  public static JsonObject generateCurrentEffectiveVersionJsonEntryQuery(final JsonEntry jsonEntry) {
    return generateEqualJsonEntryQuery(jsonEntry)
      .put(START_VERSION_CREATED_AT, new JsonObject()
        .put("$lte", jsonEntry.getStartVersion().getCreatedAt().toInstant()))
      .put(END_VERSION_CREATED_AT, new JsonObject()
        .put("$gte", jsonEntry.getEndVersion().getCreatedAt().toInstant()));
  }

  public static JsonObject generateAfterVersionJsonEntryQuery(final JsonEntry jsonEntry) {
    return generateEqualJsonEntryQuery(jsonEntry)
      .put(START_VERSION_CREATED_AT, new JsonObject()
        .put("$gt", jsonEntry.getEndVersion().getCreatedAt().toInstant()));
  }

  public static JsonObject generateBeforeVersionJsonEntryQuery(final JsonEntry jsonEntry) {
    return generateEqualJsonEntryQuery(jsonEntry)
      .put(END_VERSION_CREATED_AT, new JsonObject()
        .put("$lt", jsonEntry.getStartVersion().getCreatedAt().toInstant()));
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
        } else if (data.containsKey(JsonUtil.INHERIT_FIELD_COPY_FROM)) {
          id = data.getString(JsonUtil.INHERIT_FIELD_COPY_FROM);
        }
        if (data.containsKey("id_suffix")) {
          id = id + data.getString("id_suffix");
        }
        return id;
      default:
        if (data.containsKey("id")) {
          id = data.getString("id");
        } else if (data.containsKey(JsonUtil.INHERIT_FIELD_ABSTRACT)) {
          id = data.getString(JsonUtil.INHERIT_FIELD_ABSTRACT);
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
    return absolutePath.substring(absolutePath.indexOf("/", tagIndex + tag.length() + 1));
  }

  public static String parserModByPath(final String path) {
    if (path.startsWith("/data/mods/")) {
      return path.split("/")[3];
    } else {
      return "dda";
    }
  }

  public static String parserLanguage(File file) {
    return parserLanguage(file.getAbsolutePath());
  }

  public static String parserLanguage(String path) {
    int startIndex = FileUtil.getTranslateDirPath().length();
    final String[] split = path.substring(startIndex).split("/");
    return split[1];
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
