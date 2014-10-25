package net.orolle.verticleflow.selector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import net.kuujo.vertigo.component.ComponentConfig;
import net.kuujo.vertigo.component.impl.DefaultVerticleConfig;
import net.kuujo.vertigo.io.group.InputGroup;
import net.kuujo.vertigo.io.group.OutputGroup;
import net.kuujo.vertigo.io.selector.FairSelector;
import net.kuujo.vertigo.java.ComponentVerticle;
import net.kuujo.vertigo.network.ActiveNetwork;
import net.kuujo.vertigo.network.NetworkConfig;
import net.kuujo.vertigo.util.Configs;
import net.orolle.verticleflow.eventbus.EventBusReceive;
import net.orolle.verticleflow.eventbus.EventBusSend;
import net.orolle.verticleflow.sbpm.WebSubject;

class EventBusWebSubject {
  public final String inAddress, outAddress, name;

  public EventBusWebSubject(String name, String inAddress, String outAddress) {
    super();
    this.inAddress = inAddress;
    this.outAddress = outAddress;
    this.name = name;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("EventBusWebSubject(");
    buf.append("name="+name+",");
    buf.append("inAddress="+inAddress+",");
    buf.append("outAddress="+outAddress);
    buf.append(")");
    return buf.toString();
  }
}

class ScopeNetwork {
  private ActiveNetwork network;
  private String translation;
  private HashMap<String, EventBusWebSubject> webSubjects = new HashMap<>();

  public ActiveNetwork getNetwork() {
    return network;
  }
  public void setNetwork(ActiveNetwork network) {
    this.network = network;
  }
  public String getTranslation() {
    return translation;
  }
  public void setTranslation(String translation) {
    this.translation = translation;
  }
  public HashMap<String, EventBusWebSubject> getWebSubjects() {
    return webSubjects;
  }
}

public class ScopeSelector extends ComponentVerticle {
  HashMap<MultiKey, ScopeNetwork> scopes = new HashMap<>();
  List<String> scopeVariables;

  @Override
  public void start() {
    super.start();
    final String cluster = container.config().getString("cluster");
    final JsonObject basic = container.config().getObject("network");
    scopeVariables = setupScopeVariables();

    input.port("in").groupHandler(new Handler<InputGroup>() {
      @Override
      public void handle(final InputGroup group) {
        group.messageHandler(new Handler<JsonObject>() {
          String parentGroup = group.name();

          @Override
          public void handle(final JsonObject scopeRequest) {
            List<Object> list = new ArrayList<Object>(scopeVariables.size());
            for (String scopeVar : scopeVariables) {
              list.add(scopeRequest.getString(scopeVar, group.name()));
            }

            final MultiKey scopeKey = new MultiKey(list.toArray(new Object[list.size()]));

            if(!scopes.containsKey(scopeKey)) {
              scopes.put(scopeKey, new ScopeNetwork());
            }
            scopes.get(scopeKey).setTranslation(parentGroup);

            if(scopes.get(scopeKey).getNetwork() == null) {
              final JsonObject networkInstance = basic.copy().putString("name", basic.getString("name")+"-"+scopeKey.hashCode());
              final NetworkConfig subNet = Configs.createNetwork(networkInstance);

              for (ComponentConfig<?> comp : subNet.getComponents()) {
                if (comp instanceof DefaultVerticleConfig) {
                  DefaultVerticleConfig vComp = (DefaultVerticleConfig) comp;
                  try {
                    // Throws exception if component is not a WebSubject
                    Class.forName(vComp.getMain()).asSubclass(WebSubject.class);
                    
                    final String rand       = UUID.randomUUID().toString();
                    final String inAddress  = rand+scopeKey.hashCode();
                    final String outAddress = rand+scopeKey.hashCode();

                    String IN  = "component-"+UUID.randomUUID().toString();
                    String OUT = "component-"+UUID.randomUUID().toString();

                    subNet.addComponent(IN,  EventBusReceive.class.getName(), new JsonObject().putString("address", inAddress),  1);
                    subNet.addComponent(OUT, EventBusSend.class.getName(),    new JsonObject().putString("address", outAddress), 1);

                    subNet.createConnection(IN, "out", vComp.getName(), "web", new FairSelector());
                    subNet.createConnection(vComp.getName(), "web", OUT, "in", new FairSelector());

                    scopes.get(scopeKey).getWebSubjects().put(vComp.getName(), new EventBusWebSubject(vComp.getName(), inAddress, outAddress));
                    System.out.println(scopes.get(scopeKey).getWebSubjects());

                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              }

              vertigo.deployNetwork(cluster, subNet, new Handler<AsyncResult<ActiveNetwork>>() {

                private void deployOutAddresses(final List<EventBusWebSubject> webSubjects, final Handler<AsyncResult<Void>> handler) {
                  if(webSubjects.isEmpty()) {
                    handler.handle(new DefaultFutureResult<Void>().setResult(null));
                    return;
                  }
                  EventBusWebSubject ws = webSubjects.remove(0);

                  vertx.eventBus().registerHandler(ws.outAddress, new Handler<Message<Object>>() {
                    public void handle(final Message<Object> msg) {
                      String scopeTranslation = scopes.get(scopeKey).getTranslation();
                      scopes.get(scopeKey).setTranslation(null);

                      output.port("out").group(scopeTranslation, new Handler<OutputGroup>() {
                        @Override
                        public void handle(OutputGroup og) {

                          og.send(msg.body().toString()).end();
                        }
                      });
                    };
                  }, new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> r) {
                      if(r.succeeded()) {
                        deployOutAddresses(webSubjects, handler);
                      } else {
                        handler.handle(new DefaultFutureResult<Void>().setFailure(r.cause()));
                      }
                    }
                  });
                }


                @Override
                public void handle(AsyncResult<ActiveNetwork> deploy) {
                  if(deploy.succeeded()) {
                    scopes.get(scopeKey).setNetwork(deploy.result());

                    deployOutAddresses(new ArrayList<EventBusWebSubject>(scopes.get(scopeKey).getWebSubjects().values()), new Handler<AsyncResult<Void>>() {
                      @Override
                      public void handle(AsyncResult<Void> r) {
                        if(r.succeeded()) {
                          pushToNetwork(scopeKey, scopeRequest);
                        } else {
                          throw new IllegalStateException(r.cause());
                        }
                      }
                    });
                  } else {
                    throw new IllegalStateException(deploy.cause());
                  }
                }
              });
            } else {
              pushToNetwork(scopeKey, scopeRequest);
            }
          }
        });
      }
    });
  }

  private void pushToNetwork(MultiKey scopeKey, JsonObject requestParams) {
    System.out.println(requestParams);
    String subject = requestParams.getString("subject");
    JsonObject closure = new JsonObject();
    for (String scopeVar : scopeVariables) {
      closure.putValue(scopeVar, requestParams.removeField(scopeVar));
    }

    vertx.eventBus().send(scopes.get(scopeKey).getWebSubjects().get(subject).inAddress, new JsonObject().putObject("closure", closure).putObject("request", requestParams)); 
  }

  private final List<String> setupScopeVariables() {
    final List<String> scopeVariables = new ArrayList<String>();

    if (container.config().getField("scope") instanceof JsonArray) {
      JsonArray arr = (JsonArray) container.config().getField("scope");
      for (Object object : arr) {
        scopeVariables.add(object.toString());
      }
    } else if (container.config().getField("scope") instanceof String) {
      String str = (String) container.config().getField("scope");
      scopeVariables.add(str);
    }

    if(scopeVariables.isEmpty()) {
      scopeVariables.add("");
    }

    return scopeVariables;
  }
}
