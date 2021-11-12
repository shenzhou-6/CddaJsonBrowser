package fun.hydd.cddabrowser.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.io.File;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonEntry {
  private String id;
  private String type;
  private boolean isOriginal;
  private Version startVersion;
  private Version endVersion;
  private String language;
  private String path;
  @JsonIgnoreProperties(ignoreUnknown = true)
  private JsonObject data;
  private String mod;

  public static String extractId(final JsonObject data) {
    String id = null;
    if (data.containsKey("id")) {
      id = data.getString("id");
    } else if (data.containsKey("abstract")) {
      id = data.getString("abstract");
    } else if ("recipe".equals(data.getString("type")) || "uncraft".equals(data.getString("type"))) {
      if (data.containsKey("result")) {
        id = data.getString("result");
      } else if (data.containsKey("copy-from")) {
        id = data.getString("copy-from");
      }
      if (data.containsKey("id_suffix")) {
        id = id + data.getString("id_suffix");
      }
    } else if ("dream".equals(data.getString("type"))) {
      if (data.containsKey("category")) {
        id = data.getString("category");
        if (data.containsKey("strength")) {
          id = id + data.getString("strength");
        }
      }
    } else if ("MONSTER_BLACKLIST".equals(data.getString("type"))) {
      id = data.getString("monsters");
    } else {
      id = data.encode();
    }
    return id;
  }

  public static Future<JsonEntry> generatedJsonEntry(final JsonObject data, final String language,
                                                     final Version currentVersion, final String path) {
    if (!data.containsKey("type")) {
      return Future.failedFuture("generatedJsonEntry():no find type\n" +
        data.encodePrettily());
    }
    final String id = extractId(data);
    if (id == null) {
      return Future.failedFuture("generatedJsonEntry():no find id\n" +
        data.encodePrettily());
    }
    final JsonEntry jsonEntry = new JsonEntry();
    jsonEntry.setData(data);
    jsonEntry.setLanguage(language);
    jsonEntry.setStartVersion(currentVersion);
    jsonEntry.setEndVersion(currentVersion);
    jsonEntry.setId(id);
    jsonEntry.setOriginal(true);
    jsonEntry.setType(data.getString("type"));
    jsonEntry.setPath(path);
    jsonEntry.setMod(getMod(path));
    return Future.succeededFuture(jsonEntry);
  }

  public static String getRelativePath(final File file, final Version version) {
    final String filePath = file.getAbsolutePath();
    final String tag = version.getTagName();
    return filePath.substring(filePath.indexOf("/", filePath.indexOf(tag) + tag.length() + 1));
  }

  public static String getMod(final String path) {
    if (path.startsWith("/data/mods/")) {
      return path.split("/", 3)[2];
    } else {
      return "dda";
    }
  }

  public String getCollectionName() {
    return this.type + "_" + (this.isOriginal ? "original" : "process");
  }

  public String getMod() {
    return this.mod;
  }

  public void setMod(final String mod) {
    this.mod = mod;
  }

  public String getId() {
    return this.id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public boolean isOriginal() {
    return this.isOriginal;
  }

  public void setOriginal(final boolean original) {
    this.isOriginal = original;
  }

  public Version getStartVersion() {
    return this.startVersion;
  }

  public void setStartVersion(final Version startVersion) {
    this.startVersion = startVersion;
  }

  public Version getEndVersion() {
    return this.endVersion;
  }

  public void setEndVersion(final Version endVersion) {
    this.endVersion = endVersion;
  }

  public String getLanguage() {
    return this.language;
  }

  public void setLanguage(final String language) {
    this.language = language;
  }

  public String getType() {
    return this.type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public JsonObject getData() {
    return this.data;
  }

  public void setData(final JsonObject data) {
    this.data = data;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

}
