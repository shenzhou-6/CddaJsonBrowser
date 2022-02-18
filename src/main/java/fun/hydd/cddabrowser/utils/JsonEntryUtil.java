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
import java.util.*;
import java.util.stream.Collectors;

public class JsonEntryUtil {
  public static final String START_VERSION_CREATED_AT = "startVersion.created_at";
  public static final String END_VERSION_CREATED_AT = "endVersion.created_at";
  static final Logger logger = LoggerFactory.getLogger(JsonEntryUtil.class);
  public static Map<String, JsonEntry> jsonEntryMap = new HashMap<>();

  private MongoClient mongoClient;

  public JsonEntryUtil(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }


  public Future<List<JsonEntry>> processInheritJsonEntryList(List<JsonEntry> jsonEntryList,
                                                                    List<String> sortedModList) {
    @SuppressWarnings("rawtypes") List<Future> futureList = new ArrayList<>();
    List<JsonEntry> newJsonEntryList = new ArrayList<>();
    for (JsonEntry jsonEntry : jsonEntryList) {
      futureList.add(processInheritJsonEntry(jsonEntry, sortedModList, jsonEntry.getMod())
        .onSuccess(newJsonEntryList::add));
    }
    return CompositeFuture.all(futureList)
      .compose(compositeFuture -> Future.succeededFuture(newJsonEntryList));
  }

  private Future<JsonEntry> findSuperJsonEntry(JsonEntry subJsonEntry,
                                                      List<String> sortedModList, String currentMod,
                                                      JsonEntry superJsonEntry) {
    if (superJsonEntry != null || sortedModList.isEmpty()) {
      if (sortedModList.isEmpty()) {
        logger.warn("no find all supper data for {}", subJsonEntry);
      }
      return Future.succeededFuture(subJsonEntry);
    }
    String mod = "dda";
    int index = sortedModList.indexOf(currentMod);
    if (index > 1) {
      mod = sortedModList.get(index - 1);
    }
    JsonObject data = subJsonEntry.getData();
    subJsonEntry.setOriginal(false);
    if (!data.containsKey(JsonUtil.INHERIT_FIELD_COPY_FROM)) {
      return Future.succeededFuture(subJsonEntry);
    }
    String copyFrom = data.getString(JsonUtil.INHERIT_FIELD_COPY_FROM);
    JsonObject query = new JsonObject()
      .put("id", copyFrom)//todo type can diff but common is eq
      .put("mod", mod);
    String collectionName = subJsonEntry.getLanguage() + "_" + ("dda".equals(subJsonEntry.getMod()) ? "original" :
      "process");
    return getCurrentEffectiveJsonEntry(query, collectionName)
      .compose(jsonObject -> {
        if (JsonUtil.isNotEmpty(jsonObject)) {
          return Future.succeededFuture(jsonObject.mapTo(JsonEntry.class));
        }
        return Future.succeededFuture();
      })
      .compose(jsonEntry1 -> findSuperJsonEntry(subJsonEntry, sortedModList, currentMod, jsonEntry1));
  }

  public Future<JsonEntry> processInheritJsonEntry(JsonEntry jsonEntry,
                                                          List<String> sortedModList, String currentMod) {
    if (jsonEntryMap.containsKey(jsonEntry.getId())) {
      logger.info("in map find {}", jsonEntry.getId());
      return Future.succeededFuture(jsonEntryMap.get(jsonEntry.getId()));
    }
    return findSuperJsonEntry(jsonEntry, sortedModList, currentMod, null)
      .compose(jsonEntry1 -> {
        JsonObject superData = jsonEntry1.getData();
        if (superData.containsKey(JsonUtil.INHERIT_FIELD_COPY_FROM)) {
          logger.info("Start process super inheritJsonEntry {}", jsonEntry1.getId());
          return processInheritJsonEntry( jsonEntry1, sortedModList, currentMod)
            .compose(jsonEntry2 -> {
              jsonEntry.setData(processInheritJsonObject(jsonEntry.getData(), jsonEntry2.getData()));
              return Future.succeededFuture(jsonEntry);
            });
        }
        jsonEntry.setData(processInheritJsonObject(jsonEntry.getData(), superData));
        return Future.succeededFuture(jsonEntry);
      })
      .onSuccess(jsonEntry1 -> jsonEntryMap.put(jsonEntry1.getId(), jsonEntry1));
  }

  public Future<List<String>> getSortedMod(String collection) {
    return getAllModInfoDataJsonObject(collection)
      .compose(jsonObjectList -> {
        Map<String, List<String>> map = getAllModInfoMap(jsonObjectList);
        List<String> sortKeyList = topologySort(map);
        return Future.succeededFuture(sortKeyList);
      });
  }

  private Future<List<JsonObject>> getAllModInfoDataJsonObject(String collection) {
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
        if (superData.containsKey(key)) {
          JsonArray oldJsonArray = superData.getJsonArray(key);
          JsonArray newJsonArray = JsonUtil.convertObjectToJsonArray(entry.getValue());
          for (Object object : newJsonArray) {
            oldJsonArray.remove(object);
          }
          superData.put(key, oldJsonArray);
        }
      }
      data.remove(JsonUtil.INHERIT_FIELD_DELETE);
    }
    if (data.containsKey(JsonUtil.INHERIT_FIELD_RELATIVE)) {
      JsonObject jsonObject = data.getJsonObject(JsonUtil.INHERIT_FIELD_RELATIVE);
      for (Map.Entry<String, Object> entry : jsonObject) {
        final String key = entry.getKey();
        if (superData.containsKey(key)) {
          superData.put(key, superData.getFloat(key) + Double.parseDouble(String.valueOf(entry.getValue())));
        } else {
          superData.put(key, Double.parseDouble(String.valueOf(entry.getValue())));
        }
      }
      data.remove(JsonUtil.INHERIT_FIELD_RELATIVE);
    }
    if (data.containsKey(JsonUtil.INHERIT_FIELD_PROPORTIONAL)) {
      JsonObject jsonObject = data.getJsonObject(JsonUtil.INHERIT_FIELD_PROPORTIONAL);
      for (Map.Entry<String, Object> entry : jsonObject) {
        final String key = entry.getKey();
        if (superData.containsKey(key)) {
          superData.put(key, superData.getFloat(key) * Double.parseDouble(String.valueOf(entry.getValue())));
        }
      }
      data.remove(JsonUtil.INHERIT_FIELD_PROPORTIONAL);
    }
    for (Map.Entry<String, Object> entry : data) {
      superData.put(entry.getKey(), entry.getValue());
    }
    return superData;
  }

  public Future<List<JsonObject>> getNeedProcessInheritJsonObjectByMod(String collection,
                                                                              Version version,
                                                                              String mod) {
    JsonObject query = new JsonObject()
      .put("endVersion", JsonObject.mapFrom(version))
      .put("mod", mod);
    return mongoClient.find(collection, query);
  }

  public Future<List<String>> getAllOriginalCollection() {
    return mongoClient.getCollections()
      .compose(strings ->
        Future.succeededFuture(
          strings.stream().filter(s -> s.endsWith("_original")).collect(Collectors.toList())
        )
      );
  }

  public Future<Map<String, List<BulkOperation>>> processNewJsonEntryListByJsonObjectList(String language,
                                                                                                 Version version,
                                                                                                 String path,
                                                                                                 List<JsonObject> jsonObjectList) {
    List<JsonEntry> jsonEntryList = new ArrayList<>();
    for (JsonObject jsonObject : jsonObjectList) {
      jsonEntryList.add(new JsonEntry(jsonObject, language, version, path));
    }
    return processNewJsonEntryList(jsonEntryList);
  }

  public Future<Map<String, List<BulkOperation>>> processNewJsonEntryList(List<JsonEntry> jsonEntryList) {
    Map<String, List<BulkOperation>> bulkOperationListMap = new HashMap<>();
    //noinspection rawtypes
    List<Future> futureList = new ArrayList<>();
    for (JsonEntry jsonEntry : jsonEntryList) {
      futureList.add(processNewJsonEntry(jsonEntry)
        .onSuccess(bulkOperation -> {
          final String collectionName = jsonEntry.getCollectionName();
          if (bulkOperationListMap.containsKey(collectionName)) {
            bulkOperationListMap.get(collectionName).add(bulkOperation);
          } else {
            bulkOperationListMap.put(collectionName, Collections.singletonList(bulkOperation));
          }
        }));
    }
    return CompositeFuture.all(futureList)
      .compose(compositeFuture -> Future.succeededFuture(bulkOperationListMap));
  }

  public Future<BulkOperation> processNewJsonEntry(JsonEntry jsonEntry) {
    return judgeCurrentEffectiveVersionJsonEntry(null, jsonEntry)
      .compose(bulkOperation -> judgeAfterVersionVersionJsonEntry(bulkOperation, jsonEntry))
      .compose(bulkOperation -> judgeBeforeVersionVersionJsonEntry(bulkOperation, jsonEntry))
      .compose(bulkOperation -> judgeIsNewJsonEntry(bulkOperation, jsonEntry));
  }

  public Future<JsonObject> getCurrentEffectiveJsonEntry(JsonObject queryCondition,
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

  public Future<JsonObject> getAfterJsonEntry(JsonObject queryCondition,
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

  public Future<JsonObject> getBeforeJsonEntry(JsonObject queryCondition,
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

  public Future<BulkOperation> judgeCurrentEffectiveVersionJsonEntry(BulkOperation bulkOperation,
                                                                            JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateCurrentEffectiveVersionJsonEntryQuery(jsonEntry);
    return getCurrentEffectiveJsonEntry(queryCondition, jsonEntry.getCollectionName())
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

  public Future<BulkOperation> judgeAfterVersionVersionJsonEntry(BulkOperation bulkOperation,
                                                                        JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateAfterVersionJsonEntryQuery(jsonEntry);
    return getAfterJsonEntry(queryCondition, jsonEntry.getCollectionName())
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

  public Future<BulkOperation> judgeBeforeVersionVersionJsonEntry(BulkOperation bulkOperation,
                                                                         JsonEntry jsonEntry) {
    if (bulkOperation != null) {
      return Future.succeededFuture(bulkOperation);
    }
    JsonObject queryCondition = generateBeforeVersionJsonEntryQuery(jsonEntry);
    return getBeforeJsonEntry(queryCondition, jsonEntry.getCollectionName())
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
