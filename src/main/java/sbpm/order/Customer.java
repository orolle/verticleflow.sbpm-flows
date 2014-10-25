package sbpm.order;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import net.orolle.verticleflow.sbpm.WebSubject;
import net.orolle.verticleflow.sbpm.WebSubjectState;

public class Customer extends WebSubject<Customer> {
  @Override
  public void start() {
    super.start();
    
    doEnterOrder();
    
    input.port("accept-order").messageHandler(new Handler<Object>() {
      @Override
      public void handle(Object order) {
        currentState = new WebSubjectState("accept-order.jade", new JsonObject(), new Handler<JsonObject>() {
          @Override
          public void handle(JsonObject arg0) {
            
          }
        });
      }
    });
    
    input.port("deny-order").messageHandler(new Handler<Object>() {
      @Override
      public void handle(Object order) {
        currentState = new WebSubjectState("deny-order.jade", new JsonObject(), new Handler<JsonObject>() {
          @Override
          public void handle(JsonObject arg0) {
            
          }
        });
      }
    });
  }

  private void doEnterOrder() {
    currentState  = new WebSubjectState("enter-order.jade", new JsonObject(), new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject req) {
        if(req.containsField("order")) {
          String product = req.getString("order");
          output.port("order").send(product);
          
          currentState = new WebSubjectState("pending-order.jade", new JsonObject().putString("order", product), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject arg0) {
              
            }
          });
        }
      }
    });
  }  
}
