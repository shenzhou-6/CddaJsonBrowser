package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.Version;

import java.util.regex.Pattern;

public class VersionUtil {
  private VersionUtil() {
  }

  public static int parseBranchByTag(final String tag) {
    if (tag == null) {
      return Version.EXPERIMENTAL;
    }
    final String pattern = "0\\.[A-Z](-[0-9])?";
    if (Pattern.matches(pattern, tag)) {
      return Version.STABLE;
    }
    return Version.EXPERIMENTAL;
  }
}
