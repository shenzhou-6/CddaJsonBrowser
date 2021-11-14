package fun.hydd.cddabrowser.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fun.hydd.cddabrowser.utils.JsonEntryUtil;
import io.vertx.core.json.JsonObject;

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

  public JsonEntry() {
  }

  public JsonEntry(final JsonObject data, final String language,
                   final Version version, final String path) {
    if (!data.containsKey("type")) {
      return;
    }
    this.id = JsonEntryUtil.parserId(data);
    this.type = data.getString("type");
    this.isOriginal = true;
    this.startVersion = version;
    this.endVersion = version;
    this.language = language;
    this.path = path;
    this.data = data;
    this.mod = JsonEntryUtil.parserModByPath(path);
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

  @Override
  public String toString() {
    return "JsonEntry{" +
      "id='" + id + '\'' +
      ", type='" + type + '\'' +
      ", isOriginal=" + isOriginal +
      ", startVersion=" + startVersion +
      ", endVersion=" + endVersion +
      ", language='" + language + '\'' +
      ", path='" + path + '\'' +
      ", data=" + data +
      ", mod='" + mod + '\'' +
      '}';
  }
}
