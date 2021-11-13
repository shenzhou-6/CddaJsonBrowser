package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.JsonEntry;
import fun.hydd.cddabrowser.entity.Version;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;

import java.io.File;

public class JsonEntryUtil {

  private JsonEntryUtil() {
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
