package net.orolle.verticleflow;
/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import net.kuujo.vertigo.Vertigo;
import net.kuujo.vertigo.cluster.Cluster;
import net.kuujo.vertigo.io.selector.FairSelector;
import net.kuujo.vertigo.network.ActiveNetwork;
import net.kuujo.vertigo.network.NetworkConfig;
import net.kuujo.vertigo.network.impl.DefaultNetworkConfig;
import net.kuujo.vertigo.util.serialization.SerializerFactory;
import net.orolle.verticleflow.http.WebRequest;
import net.orolle.verticleflow.http.WebResponse; 
import net.orolle.verticleflow.selector.ScopeSelector;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import sbpm.order.Customer;
import sbpm.order.Employee;

/**
 * Main class builds the vertigo network of LibeliumEndpoint as event source and
 * ConnectorUrbanPulse as event sink. In between vertigo organizes event flow.
 */
public class Main extends Verticle {

  private Vertigo vertigo;

  /**
   * vertigos start method
   */
  public void start(final Future<Void> startedResult) {
    final String cluster = "local";
    vertigo = new Vertigo(this);

    // Define Business Process
    JsonObject scopeConfig = defineScopeConfig(cluster);
    // Embed Business Process within Web-Network
    final NetworkConfig webNetwork = defineWebNetwork(scopeConfig);

    /*
     * Deploies vertigo network on vertx
     */
    vertigo.deployCluster(cluster, new Handler<AsyncResult<Cluster>>() {
      @Override
      public void handle(AsyncResult<Cluster> event) {
        if (event.succeeded()) {
          Cluster c = event.result();
          c.deployNetwork(webNetwork, new Handler<AsyncResult<ActiveNetwork>>() {
            @Override
            public void handle(AsyncResult<ActiveNetwork> event) {
              if (event.succeeded()) {
                container.logger().info("STARTED VERTIGO CLUSTER");
                startedResult.setResult(null);
              } else {
                startedResult.setFailure( new IllegalStateException("Deploy network on cluster failed!") );
              }
            }
          });
        }else{
          startedResult.setFailure( new IllegalStateException("Deploy cluster locally failed!") );
        }
      }
    });
  }

  private NetworkConfig defineWebNetwork(JsonObject scopeConfig) {
    NetworkConfig network = vertigo.createNetwork("business-flow");
    // WebRequest receives a HTTP request and forwareds to Scope
    network.addComponent("Request",  WebRequest.class.getName(), new JsonObject(), 1);
    // Scope creates an instance of a Business Process depending on request parameter
    network.addComponent("Scope",    ScopeSelector.class.getName(), scopeConfig, 1);
    // The state of the Business Process is communicated back to the HTTP request
    network.addComponent("Response", WebResponse.class.getName(), new JsonObject(), 1);

    network.createConnection("Request", "request", "Scope", "in", new FairSelector());
    network.createConnection("Scope" ,  "out",  "Response", "response", new FairSelector());

    return network;
  }

  private JsonObject defineScopeConfig(String cluster) {
    // Subject oriented Business Process as a FBP-Network
    NetworkConfig sbpmNetwork = vertigo.createNetwork("sbpm-network");

    // Customers orders a product; folder defines web pages for the Customer to interact with the business process
    sbpmNetwork.addComponent("Customer", Customer.class.getName(), new JsonObject().putString("folder", "web/Customer/"), 1);
    // Employee accepts or denies customer order; folder defines web pages for the Employee to interact with the business process
    sbpmNetwork.addComponent("Employee", Employee.class.getName(), new JsonObject().putString("folder", "web/Employee/"), 1);

    sbpmNetwork.createConnection("Customer", "order",  "Employee", "order", new FairSelector());
    sbpmNetwork.createConnection("Employee", "accept", "Customer", "accept-order", new FairSelector());
    sbpmNetwork.createConnection("Employee", "deny",   "Customer", "deny-order", new FairSelector());

    JsonObject subNetSer = SerializerFactory.getSerializer(NetworkConfig.class).serializeToObject(sbpmNetwork);

    // Configuration for ScopeSelector
    JsonObject scopeConfig = new JsonObject().putString("cluster", cluster)
        .putObject("network", subNetSer)
        // Primary key which identifies the business process instance
        .putArray("scope",   new JsonArray().add("instance"))
        ;

    return scopeConfig;
  }

  @Override
  public void stop() {
    vertigo.undeployNetwork("local", "sbpm-network");
    super.stop();
  }
}
