package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.Version;

import static org.assertj.core.api.Assertions.assertThat;

class VersionUtilTest {

  void parseBranchByTag() {
    String tag = "0.F-2 ";
    String tag1 = "0.F";
    String tag2 = "cdda-experimental-2021-10-10-1829";
    String tag3 = "cdda-experimental-2021-5-10-1829";
    assertThat(VersionUtil.parseBranchByTag(tag)).isEqualTo(Version.STABLE);
    assertThat(VersionUtil.parseBranchByTag(tag1)).isEqualTo(Version.STABLE);
    assertThat(VersionUtil.parseBranchByTag(tag2)).isEqualTo(Version.EXPERIMENTAL);
    assertThat(VersionUtil.parseBranchByTag(tag3)).isEqualTo(Version.EXPERIMENTAL);
  }

}
