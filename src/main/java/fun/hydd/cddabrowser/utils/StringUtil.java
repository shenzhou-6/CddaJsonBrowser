package fun.hydd.cddabrowser.utils;

public final class StringUtil {
  private StringUtil() {
  }

  public static boolean isEmpty(final String s) {
    return s == null || s.isEmpty();
  }

  public static boolean isNotEmpty(final String s) {
    return !isEmpty(s);
  }
}
