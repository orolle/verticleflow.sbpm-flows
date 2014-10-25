package net.orolle.verticleflow.eventbus;

import org.vertx.java.core.Handler;

import net.kuujo.vertigo.java.ComponentVerticle;

public class EventBusSend extends ComponentVerticle {

  @Override
  public void start() {
    final String address = container.config().getString("address");
    super.start();
    
    input.port("in").messageHandler(new Handler<Object>() {
      @Override
      public void handle(Object o) {
        vertx.eventBus().send(address, o);
      }
    });
  }
}
