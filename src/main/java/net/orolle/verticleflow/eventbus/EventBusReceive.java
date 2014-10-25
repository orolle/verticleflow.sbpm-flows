package net.orolle.verticleflow.eventbus;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import net.kuujo.vertigo.java.ComponentVerticle;

public class EventBusReceive extends ComponentVerticle {

  @Override
  public void start() {
    final String address = container.config().getString("address");
    super.start();
    
    vertx.eventBus().registerHandler(address, new Handler<Message<Object>>() {
      @Override
      public void handle(Message<Object> m) {
        output.port("out").send(m.body());
      }
    });
  }
}
