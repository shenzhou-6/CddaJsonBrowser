package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.JsonEntry;
import fun.hydd.cddabrowser.entity.Version;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEntryUtilTest {
  final static JsonEntry jsonEntry = new JsonEntry();

  @BeforeAll
  static void setup() {
    Version version = new Version();
    version.setName("test name");
    version.setTagName("test tag");
    version.setBranch(Version.EXPERIMENTAL);
    version.setTargetCommitish("test commit");
    version.setCreatedAt(new Date());
    jsonEntry.setData(new JsonObject());
    jsonEntry.setStartVersion(version);
    jsonEntry.setEndVersion(version);
    jsonEntry.setId("test id");
    jsonEntry.setLanguage("en");
    jsonEntry.setMod("test mod");
    jsonEntry.setOriginal(true);
    jsonEntry.setPath("test path");
    jsonEntry.setType("test type");
  }

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

  @Test
  void generateEqualJsonEntryQuery() {
    JsonObject result = JsonEntryUtil.generateEqualJsonEntryQuery(jsonEntry);
    assertThat(result).isEqualTo(new JsonObject()
      .put("id", jsonEntry.getId())
      .put("type", jsonEntry.getType())
      .put("language", jsonEntry.getLanguage())
      .put("path", jsonEntry.getPath()
      )
    );
  }

  @Test
  void generateCurrentEffectiveVersionJsonEntryQuery() {
    JsonObject result = JsonEntryUtil.generateCurrentEffectiveVersionJsonEntryQuery(jsonEntry);
    assertThat(result.containsKey("startVersion.created_at")).isTrue();
    assertThat(result.containsKey("endVersion.created_at")).isTrue();
  }

  @Test
  void generateAfterVersionJsonEntryQuery() {
    JsonObject result = JsonEntryUtil.generateAfterVersionJsonEntryQuery(jsonEntry);
    assertThat(result.containsKey("startVersion.created_at")).isTrue();
  }

  @Test
  void generateBeforeVersionJsonEntryQuery() {
    JsonObject result = JsonEntryUtil.generateBeforeVersionJsonEntryQuery(jsonEntry);
    assertThat(result.containsKey("endVersion.created_at")).isTrue();
  }
}
