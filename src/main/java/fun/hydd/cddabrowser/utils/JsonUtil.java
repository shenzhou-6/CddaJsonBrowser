package fun.hydd.cddabrowser.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
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
}
