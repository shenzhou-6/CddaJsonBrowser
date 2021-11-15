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

  @Test
  void isNotEmpty() {
    JsonObject jsonObjectNull = null;
    JsonObject jsonObjectEmpty = new JsonObject();
    JsonObject jsonObjectBlank = new JsonObject().put("", "");
    JsonObject jsonObjectTrue = new JsonObject().put("test", "test");

    assertThat(JsonUtil.isNotEmpty(jsonObjectNull)).isFalse();
    assertThat(JsonUtil.isNotEmpty(jsonObjectEmpty)).isFalse();
    assertThat(JsonUtil.isNotEmpty(jsonObjectBlank)).isTrue();
    assertThat(JsonUtil.isNotEmpty(jsonObjectTrue)).isTrue();
  }

  @Test
  void isEmpty() {
    JsonObject jsonObjectNull = null;
    JsonObject jsonObjectEmpty = new JsonObject();
    JsonObject jsonObjectBlank = new JsonObject().put("", "");
    JsonObject jsonObjectTrue = new JsonObject().put("test", "test");

    assertThat(JsonUtil.isEmpty(jsonObjectNull)).isTrue();
    assertThat(JsonUtil.isEmpty(jsonObjectEmpty)).isTrue();
    assertThat(JsonUtil.isEmpty(jsonObjectBlank)).isFalse();
    assertThat(JsonUtil.isEmpty(jsonObjectTrue)).isFalse();
  }

  @Test
  void isEmptyJsonArray() {
    JsonArray jsonArrayNull = null;
    JsonArray jsonArrayEmpty = new JsonArray();
    JsonArray jsonArrayBlank = new JsonArray().add("");
    JsonArray jsonArrayTrue = new JsonArray().add("test");

    assertThat(JsonUtil.isEmpty(jsonArrayNull)).isTrue();
    assertThat(JsonUtil.isEmpty(jsonArrayEmpty)).isTrue();
    assertThat(JsonUtil.isEmpty(jsonArrayBlank)).isFalse();
    assertThat(JsonUtil.isEmpty(jsonArrayTrue)).isFalse();
  }

  @Test
  void isNotEmptyJsonArray() {
    JsonArray jsonArrayNull = null;
    JsonArray jsonArrayEmpty = new JsonArray();
    JsonArray jsonArrayBlank = new JsonArray().add("");
    JsonArray jsonArrayTrue = new JsonArray().add("test");

    assertThat(JsonUtil.isNotEmpty(jsonArrayNull)).isFalse();
    assertThat(JsonUtil.isNotEmpty(jsonArrayEmpty)).isFalse();
    assertThat(JsonUtil.isNotEmpty(jsonArrayBlank)).isTrue();
    assertThat(JsonUtil.isNotEmpty(jsonArrayTrue)).isTrue();
  }

  @Test
  void convertJsonArray() {
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray1 = new JsonArray()
      .add(new JsonObject())
      .add(new JsonObject().put("test", "test"));
    JsonArray jsonArray2 = new JsonArray()
      .add(new JsonObject());
    JsonArray jsonArray3 = new JsonArray()
      .add(new JsonArray());

    assertThat(JsonUtil.convertJsonArray(jsonArray)).hasSize(0);
    assertThat(JsonUtil.convertJsonArray(jsonArray1)).hasSize(2);
    assertThat(JsonUtil.convertJsonArray(jsonArray2)).hasSize(1);
    assertThat(JsonUtil.convertJsonArray(jsonArray2)).hasSize(1);
  }

  @Test
  void convertObjectToJsonArray() {
    Object object = new JsonArray().add("test").add("test1");
    Object object1 = "test1";

    assertThat(JsonUtil.convertObjectToJsonArray(object)).isEqualTo(new JsonArray().add("test").add("test1"));
    assertThat(JsonUtil.convertObjectToJsonArray(object1)).isEqualTo(new JsonArray().add("test1"));
  }

  @Test
  void isJsonFile() {
    String test = "test1.json";
    String test1 = "test/sb/test.json";
    String test2 = "test/sb/test.jsn";
    String test3 = "test/sb/json";

    assertThat(JsonUtil.isJsonFile(test)).isTrue();
    assertThat(JsonUtil.isJsonFile(test1)).isTrue();
    assertThat(JsonUtil.isJsonFile(test2)).isFalse();
    assertThat(JsonUtil.isJsonFile(test3)).isFalse();
  }
}
