package fun.hydd.cddabrowser;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(VertxExtension.class)
class TestConfig {
  @Test
  void configTest(Vertx vertx, VertxTestContext testContext) {
    ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "config.json"))));
    retriever.getConfig(json -> {
      JsonObject result = json.result();
      assertThat(result.getString("test")).isEqualTo("Hello world");
      testContext.completeNow();
    });
  }
}
