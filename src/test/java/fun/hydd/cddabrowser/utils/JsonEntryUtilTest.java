package fun.hydd.cddabrowser.utils;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEntryUtilTest {

  @Test
  void parserModByPath() {
    String path = "/data/mods/testMod/test/test";
    assertThat(JsonEntryUtil.parserModByPath(path)).isEqualTo("testMod");
  }

  @Test
  void getRelativePath() {
    String absolutePath = "/home/hydd/unzip/cataclysm-2021-10-1/data/core/test.json";
    String absolutePath1 = "/home/hydd/unzip/cataclysm-2021-10-1/data/core/testDir/test.json";
    String absolutePath2 = "/home/hydd/unzip/cataclysm-2021-10-1/data/row/testDir/tesDir2/test.json";
    String absolutePathFail = "/home/hydd/unzip/cataclysm-2021-1-fail-0-1/data/row/testDir/tesDir2/test.json";
    String tag = "cataclysm-2021-10-1";
    assertThat(JsonEntryUtil.parserRelativePath(absolutePath, tag)).isEqualTo("/data/core/test.json");
    assertThat(JsonEntryUtil.parserRelativePath(absolutePath1, tag)).isEqualTo("/data/core/testDir/test.json");
    assertThat(JsonEntryUtil.parserRelativePath(absolutePath2, tag)).isEqualTo("/data/row/testDir/tesDir2/test.json");
    assertThat(JsonEntryUtil.parserRelativePath(absolutePathFail, tag)).isEmpty();
  }

  @Test
  void parserId() {
    JsonObject jsonObject = new JsonObject()
      .put("id", "testId")
      .put("type", "tool");
    JsonObject jsonObject1 = new JsonObject()
      .put("abstract", "testId1")
      .put("type", "tool");
    JsonObject jsonObject2 = new JsonObject()
      .put("result", "testId2")
      .put("type", "recipe");
    JsonObject jsonObject3 = new JsonObject()
      .put("copy-from", "testId3")
      .put("type", "recipe");
    assertThat(JsonEntryUtil.parserId(jsonObject)).isEqualTo("testId");
    assertThat(JsonEntryUtil.parserId(jsonObject1)).isEqualTo("testId1");
    assertThat(JsonEntryUtil.parserId(jsonObject2)).isEqualTo("testId2");
    assertThat(JsonEntryUtil.parserId(jsonObject3)).isEqualTo("testId3");
  }
}
