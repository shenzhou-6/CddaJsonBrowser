package fun.hydd.cddabrowser.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilTest {
  String string;
  String nullString;
  String emptyString;
  String blankString;
  String blankString1;

  @BeforeEach
  void start() {
    string = "content";
    nullString = null;
    emptyString = "";
    blankString = " ";
    blankString1 = "  ";
  }

  @Test
  void isEmpty() {
    assertThat(StringUtil.isEmpty(string)).isFalse();
    assertThat(StringUtil.isEmpty(nullString)).isTrue();
    assertThat(StringUtil.isEmpty(emptyString)).isTrue();
    assertThat(StringUtil.isEmpty(blankString)).isFalse();
    assertThat(StringUtil.isEmpty(blankString1)).isFalse();
  }

  @Test
  void isNotEmpty() {
    assertThat(StringUtil.isNotEmpty(string)).isTrue();
    assertThat(StringUtil.isNotEmpty(nullString)).isFalse();
    assertThat(StringUtil.isNotEmpty(emptyString)).isFalse();
    assertThat(StringUtil.isNotEmpty(blankString)).isTrue();
    assertThat(StringUtil.isNotEmpty(blankString1)).isTrue();
  }
}
