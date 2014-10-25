package net.orolle.verticleflow.sbpm;

import net.kuujo.vertigo.java.ComponentVerticle;

public abstract class Subject<S extends Subject> extends ComponentVerticle {
  private String internalState;

  public String internalState() {
    return internalState;
  }
  
  public S internalState(String state) {
    this.internalState = state;
    return (S) this;
  }
}
