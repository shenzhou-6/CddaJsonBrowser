package fun.hydd.cddabrowser.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
  public static final String INHERIT_FIELD_COPY_FROM = "copy-from";
  public static final String INHERIT_FIELD_ABSTRACT = "abstract";
  public static final String INHERIT_FIELD_EXTEND = "extend";
  public static final String INHERIT_FIELD_RELATIVE = "relative";
  public static final String INHERIT_FIELD_PROPORTIONAL = "proportional";
  public static final String INHERIT_FIELD_DELETE = "delete";
  static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

  private JsonUtil() {
  }

  public static List<JsonObject> bufferToJsonObjectList(final Buffer buffer) {
    final JsonArray jsonArray = buffer.toJsonArray();
    final List<JsonObject> jsonObjects = new ArrayList<>();
    if (jsonArray != null) {
      for (int i = 0; i < jsonArray.size(); i++) {
        jsonObjects.add(jsonArray.getJsonObject(i));
      }
    }
    return jsonObjects;
  }

  public static List<JsonObject> convertJsonArray(JsonArray jsonArray) {
    final List<JsonObject> jsonObjectList = new ArrayList<>();
    for (int i = 0; i < jsonArray.size(); i++) {
      jsonObjectList.add(jsonArray.getJsonObject(i));
    }
    return jsonObjectList;
  }

  public static boolean isNotEmpty(JsonObject jsonObject) {
    return !isEmpty(jsonObject);
  }

  public static boolean isEmpty(JsonObject jsonObject) {
    return jsonObject == null || jsonObject.isEmpty();
  }

  public static boolean isNotEmpty(JsonArray jsonArray) {
    return !isEmpty(jsonArray);
  }

  public static boolean isEmpty(JsonArray jsonArray) {
    return jsonArray == null || jsonArray.isEmpty();
  }

  public static JsonArray convertObjectToJsonArray(Object object) {
    if (object instanceof JsonArray) {
      return (JsonArray) object;
    } else {
      return new JsonArray().add(object);
    }
  }

  public static boolean isJsonFile(File file) {
    return isJsonFile(file.getAbsolutePath());
  }

  public static boolean isJsonFile(String path) {
    return path.endsWith(".json");
  }
}
