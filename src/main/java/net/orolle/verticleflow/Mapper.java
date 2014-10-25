package net.orolle.verticleflow;

import org.vertx.java.core.Handler;

import net.kuujo.vertigo.java.ComponentVerticle;

public class Mapper extends ComponentVerticle{

  int nr = 0;
  
  @Override
  public void start() {
    super.start();
    
    input.port("in").messageHandler(new Handler<Object>() {
      @Override
      public void handle(Object event) {
        output.port("out").send("Mapper["+(nr++)+"]");
      }
    });
  }
}
