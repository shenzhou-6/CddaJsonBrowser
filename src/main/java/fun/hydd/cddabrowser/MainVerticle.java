package fun.hydd.cddabrowser;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
      readConfig()
        .compose(this::deployVerticle)
        .onSuccess(res -> startPromise.complete())
        .onFailure(startPromise::fail);

  }

  public Future<JsonObject> readConfig(){
    JsonObject filePathConfig = new JsonObject();
    filePathConfig.put("path","conf/config.json");
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setConfig(filePathConfig);
    ConfigStoreOptions envStore = new ConfigStoreOptions()
      .setType("env");
    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
      .addStore(fileStore)
      .addStore(envStore);
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    return retriever.getConfig();
  }

  public Future<String> deployVerticle(JsonObject config){
    DeploymentOptions options = new DeploymentOptions();
    options.setConfig(config);
    return vertx.deployVerticle("fun.hydd.cddabrowser.ProcuessVerticle",options);
  }
}
