package net.orolle.verticleflow.http;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map.Entry;
import java.util.UUID;

import net.kuujo.vertigo.io.group.OutputGroup;
import net.kuujo.vertigo.java.ComponentVerticle;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;

import com.jetdrone.vertx.yoke.engine.Jade4JEngine;

public class WebRequest extends ComponentVerticle {
  int requestId = 0;

  @Override
  public void start() {
    final RouteMatcher m = new RouteMatcher();
    final Jade4JEngine renderer = new Jade4JEngine("web/index/");
    renderer.setVertx(getVertx());

    m.all("/", new Handler<HttpServerRequest>() {
      final SecureRandom ssid = new SecureRandom();
      
      @Override
      public void handle(final HttpServerRequest req) {
        final String file = "index.jade";
        final JsonObject params = new JsonObject().putString("id", "instance"+new BigInteger(130, ssid).toString());
        
        renderer.render(file, params.toMap(), new Handler<AsyncResult<Buffer>> () {
          @Override
          public void handle(AsyncResult<Buffer> res) {
            if(res.succeeded()) {
              req.response().end(res.result());
            } else {
              req.response().end("<html><body>" +
                  "render "+file+" with "+params+"<br><br>" +
                  res.cause().getMessage().replace("\n", "<br>")+"</body><html>");
            }
          }
        });
      }
    });
    
    m.all("/process/:instance/:subject", new Handler<HttpServerRequest>() {
      @Override
      public void handle(final HttpServerRequest req) {
        final String id = UUID.randomUUID().toString();
        //System.out.println("WebRequest["+id+"]");

        vertx.eventBus().registerHandler(id, new Handler<Message<String>>() {
          @Override
          public void handle(Message<String> msg) {
            vertx.eventBus().unregisterHandler(id, this);

            req.response().end(msg.body());
          }
        });

        if(req.method().equals("POST") || req.method().equals("PUT")) {
          req.expectMultiPart(true);
        }
        
        req.bodyHandler(new Handler<Buffer>() {
          @Override
          public void handle(Buffer body) {
            final JsonObject params = new JsonObject();

            for (Entry<String, String> e : req.params().entries()) {
              params.putString(e.getKey(), e.getValue());
            }

            if(req.method().equals("POST") || req.method().equals("PUT")) {
              for (Entry<String, String> e : req.formAttributes().entries()) {
                params.putString(e.getKey(), e.getValue());
              }
            }

            //System.out.println(params.encodePrettily());
            output.port("request").group(id, new Handler<OutputGroup>() {
              @Override
              public void handle(OutputGroup group) {
                group.send(params).end();
              }
            });
          }
        });
      }
    });

    vertx.createHttpServer()
    .requestHandler(m)
    .listen(8080, new Handler<AsyncResult<HttpServer>>() {
      @Override
      public void handle(AsyncResult<HttpServer> event) {
        System.out.println("start web server on 0.0.0.0:8080");
      }
    });
  }
}
