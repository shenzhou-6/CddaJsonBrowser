package fun.hydd.cddabrowser.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilTest {

  @Test
  void bufferToJsonObjectList() {
    JsonObject jsonObject = new JsonObject()
      .put("id", "test id")
      .put("type", "test type");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(jsonObject);
    Buffer buffer = jsonArray.toBuffer();

    assertThat(JsonUtil.bufferToJsonObjectList(buffer))
      .hasSize(1)
      .contains(jsonObject);
  }
}
