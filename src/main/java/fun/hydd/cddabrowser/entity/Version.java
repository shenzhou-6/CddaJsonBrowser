package fun.hydd.cddabrowser.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {
  public static final int EXPERIMENTAL = 0;
  public static final int STABLE = 1;
  @JsonProperty("name")
  private String name;
  @JsonProperty("tag_name")
  private String tagName;
  @JsonProperty("target_commitish")
  private String targetCommitish;
  @JsonProperty("branch")
  private int branch;
  @JsonProperty("created_at")
  private Date createdAt;

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getTagName() {
    return this.tagName;
  }

  public void setTagName(final String tagName) {
    this.tagName = tagName;
  }

  public String getTargetCommitish() {
    return this.targetCommitish;
  }

  public void setTargetCommitish(final String targetCommitish) {
    this.targetCommitish = targetCommitish;
  }

  public int getBranch() {
    return this.branch;
  }

  public void setBranch(final int branch) {
    this.branch = branch;
  }

  public Date getCreatedAt() {
    return this.createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "Version{" +
      "name='" + name + '\'' +
      ", tagName='" + tagName + '\'' +
      ", targetCommitish='" + targetCommitish + '\'' +
      ", branch=" + branch +
      ", createdAt=" + createdAt +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Version version = (Version) o;
    return branch == version.branch && Objects.equals(name, version.name) && Objects.equals(tagName, version.tagName) && Objects.equals(targetCommitish, version.targetCommitish) && Objects.equals(createdAt, version.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, tagName, targetCommitish, branch, createdAt);
  }
}
