package fun.hydd.cddabrowser.utils;

import fun.hydd.cddabrowser.entity.Tag;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@ExtendWith(VertxExtension.class)
class GitManagerTest {
  Logger logger = LoggerFactory.getLogger(GitManagerTest.class);

  @Test
  void getLatestTag(Vertx vertx, VertxTestContext testContext) throws Exception {
    GitManager gitManager = new GitManager();
    Tag tag = gitManager.getLatestTag();

    VersionUtil.getReleaseByTagName(vertx, tag.getName())
      .onComplete(testContext.succeeding(release -> {
        System.out.println(release.getName());
        testContext.completeNow();
      }));
  }

  @Test
  void getHeadTag(Vertx vertx, VertxTestContext testContext) throws Exception {
    GitManager gitManager = new GitManager();
    Tag tag = gitManager.getHeadTag();

    VersionUtil.getReleaseByTagName(vertx, tag.getName())
      .onComplete(testContext.succeeding(release -> {
        System.out.println(release.getName());
        testContext.completeNow();
      }));
  }

  @Test
  void getLocalNoHasRemoteTagRefList() throws Exception {
    GitManager gitManager = new GitManager();
    List<Ref> remoteTagRefs = gitManager.getLocalNoHasRemoteTagRefList();
    for (Ref remoteTagRef : remoteTagRefs) {
      logger.info("name is {}, id is {}", remoteTagRef.getName(), remoteTagRef.getObjectId().getName());
    }
  }

  @Test
  void update() throws Exception {
    GitManager gitManager = new GitManager();
    gitManager.update();
  }

  @Test
  void reset() throws GitAPIException, IOException {
    GitManager gitManager = new GitManager();
    gitManager.reset("0.F-3");
  }
}
