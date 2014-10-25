package net.orolle.verticleflow.sbpm;

import java.util.Collections;
import java.util.HashMap;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;

import com.jetdrone.vertx.yoke.engine.Jade4JEngine;

public abstract class WebSubject<K extends WebSubject> extends Subject<K>{
  private Jade4JEngine renderer;
  public WebSubjectState currentState;
  
  
  @Override
  public void start() {
    super.start();
    
    renderer = new Jade4JEngine(this.container.config().getString("folder", ""));
    renderer.setVertx(getVertx());
    
    this.input.port("web").messageHandler(new Handler<JsonObject>() {
      @Override
      public void handle(JsonObject request) {
        currentState.webRequest().handle(request.getObject("request"));
        
        jade(currentState.filename(), currentState.parameters(), request.getObject("closure"), request.getObject("request"), new Handler<String>() {
          @Override
          public void handle(String html) {
            output.port("web").send(html);
          }
        });
      }
    });
  }

  private void jade(final String file, final JsonObject params, final JsonObject closure,  final JsonObject freeVars, final Handler<String> html) {
    System.out.println("render "+file+" with "+params+" in closure "+closure+" with free variables "+freeVars);
    
    renderer.render(file, params.toMap(), new Handler<AsyncResult<Buffer>> () {
      @Override
      public void handle(AsyncResult<Buffer> res) {
        if(res.succeeded()) {
          html.handle(res.result().toString());
        } else {
          html.handle("<html><body>" +
              "render "+file+" with "+params+" in closure "+closure+"<br><br>" +
              res.cause().getMessage().replace("\n", "<br>")+"</body><html>");
        }
      }
    });
  }
}
