package net.orolle.verticleflow.sbpm;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class WebSubjectState {
  private String filename;
  private JsonObject params;
  private Handler<JsonObject> webRequest;
  
  public WebSubjectState(String filename, JsonObject params, Handler<JsonObject> req) {
    this.filename(filename);
    this.parameters(params);
    this.webRequest(req);
  }
  
  public String filename() {
    return filename;
  }
  public WebSubjectState filename(String filename) {
    this.filename = filename;
    return this;
  }
  public JsonObject parameters() {
    return params;
  }
  public WebSubjectState parameters(JsonObject params) {
    this.params = params;
    return this;
  }
  public Handler<JsonObject> webRequest() {
    return webRequest;
  }
  public WebSubjectState webRequest(final Handler<JsonObject> h) {
    this.webRequest = new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject arg0) {
        if(h != null) {
          h.handle(arg0); 
        }
      }
    };
    return this;
  }
}
