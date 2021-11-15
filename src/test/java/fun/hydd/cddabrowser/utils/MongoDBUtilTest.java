package fun.hydd.cddabrowser.utils;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(VertxExtension.class)
class MongoDBUtilTest {
  Buffer oldBuffer;
  Buffer oldBuffer1;
  Buffer oldBuffer2;
  Buffer oldBuffer3;

  Buffer newBuffer;
  Buffer newBuffer1;
  Buffer newBuffer2;
  Buffer newBuffer3;

  @BeforeEach
  void setBuffer() {
    oldBuffer = Buffer.buffer("\".\": test");
    oldBuffer1 = Buffer.buffer("\"test.test\": test");
    oldBuffer2 = Buffer.buffer("\"te12st.te12st\": test");
    oldBuffer3 = Buffer.buffer("\"$\": test");

    newBuffer = Buffer.buffer("\"{point}\": test");
    newBuffer1 = Buffer.buffer("\"test{point}test\": test");
    newBuffer2 = Buffer.buffer("\"te12st{point}te12st\": test");
    newBuffer3 = Buffer.buffer("\"{$}\": test");
  }

  @Test
  void escapeBsonField(Vertx vertx, VertxTestContext testContext) {
    MongoDBUtil.escapeBsonField(oldBuffer, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(newBuffer);
        testContext.completeNow();
      }));
    MongoDBUtil.escapeBsonField(oldBuffer1, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(newBuffer1);
        testContext.completeNow();
      }));
    MongoDBUtil.escapeBsonField(oldBuffer1, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(newBuffer1);
        testContext.completeNow();
      }));
    MongoDBUtil.escapeBsonField(oldBuffer1, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(newBuffer1);
        testContext.completeNow();
      }));
  }

  @Test
  void unescapeBsonField(Vertx vertx, VertxTestContext testContext) {
    MongoDBUtil.unescapeBsonField(newBuffer, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(oldBuffer);
        testContext.completeNow();
      }));
    MongoDBUtil.unescapeBsonField(newBuffer1, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(oldBuffer1);
        testContext.completeNow();
      }));
    MongoDBUtil.unescapeBsonField(newBuffer2, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(oldBuffer2);
        testContext.completeNow();
      }));
    MongoDBUtil.unescapeBsonField(newBuffer3, vertx)
      .onComplete(testContext.succeeding(buffer -> {
        assertThat(buffer).isEqualTo(oldBuffer3);
        testContext.completeNow();
      }));

  }
}
