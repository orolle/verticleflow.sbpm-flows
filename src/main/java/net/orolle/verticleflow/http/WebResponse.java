package net.orolle.verticleflow.http;

import net.kuujo.vertigo.io.group.InputGroup;
import net.kuujo.vertigo.java.ComponentVerticle;

import org.vertx.java.core.Handler;

public class WebResponse extends ComponentVerticle {
  @Override
  public void start() {
    
    input.port("response").groupHandler(new Handler<InputGroup>() {
      @Override
      public void handle(InputGroup group) {
        final String requestId = group.name();
        
        group.messageHandler(new Handler<String>() {
          @Override
          public void handle(String event) {
            vertx.eventBus().send(requestId, event);
          }
        });
      }
    });
  }
}
