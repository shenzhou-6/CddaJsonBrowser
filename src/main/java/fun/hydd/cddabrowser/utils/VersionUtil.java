package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.MainVerticle;
import fun.hydd.cddabrowser.entity.Version;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class VersionUtil {
  public static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/CleverRaven/Cataclysm-DDA/releases";
  private static final Logger logger = LoggerFactory.getLogger(VersionUtil.class);

  private VersionUtil() {
  }

  public static Future<Version> catchLatestVersionFromGithub(Vertx vertx) {
    return HttpUtil.request(vertx,
        new RequestOptions()
          .setAbsoluteURI(GITHUB_RELEASES_URL)
          .putHeader("User-Agent", MainVerticle.PROJECT_NAME)
          .setSsl(true))
      .compose(buffer -> {
        final JsonArray jsonArray = buffer.toJsonArray();
        if (jsonArray.isEmpty()) {
          return Future.failedFuture("catchLatestVersion(),jsonArray is Empty");
        }
        final JsonObject jsonObject = jsonArray.getJsonObject(0);
        final Version version = jsonObject.mapTo(Version.class);
        version.setBranch(parseBranchByTag(version.getTagName()));
        logger.info("catchLatestVersion:\n" +
          "{}", Json.encodePrettily(version));
        return Future.succeededFuture(version);
      });
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
