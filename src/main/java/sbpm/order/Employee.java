package sbpm.order;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import net.orolle.verticleflow.sbpm.WebSubject;
import net.orolle.verticleflow.sbpm.WebSubjectState;

public class Employee extends WebSubject<Employee> {

  @Override
  public void start() {
    super.start();

    currentState = new WebSubjectState("no-pending-order.jade", new JsonObject(), new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject req) {

      }
    });

    input.port("order").messageHandler(new Handler<String>() {
      @Override
      public void handle(String event) {
        currentState = new WebSubjectState("decide-order.jade", new JsonObject().putString("order", event), new Handler<JsonObject>() {
          @Override
          public void handle(JsonObject req) {
            String decision = req.getString("decision", "");
            if("accept".equals(decision) || "deny".equals(decision)) {
              currentState = new WebSubjectState("decided-order.jade", new JsonObject().putString("decision", decision), new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject req) {

                }
              });

              if("accept".equals(decision)) {
                output.port("accept").send("");
              }

              if("deny".equals(decision)) {
                output.port("deny").send("");
              }
            }
          }
        });
      }
    });
  }
}
