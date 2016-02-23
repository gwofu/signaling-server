package com.devry.videochat;

import java.util.Set;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class VideoChatVerticle extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(VideoChatVerticle.class);
    
    private static final String UPDATE_CALLER_COUNT_EVENT = "update.caller.count.event";
    private static final String CALLER_WAITING_MAP = "caller_waiting_map";

    @Override
    public void start(Future<Void> fut) throws Exception {
        logger.debug("Verticle started " + Thread.currentThread().getName());
        System.out.println("Verticle started " + Thread.currentThread().getName());

        // Create a router object.
       Router router = Router.router(vertx);
         
        // Bind "/" to our hello message - so we are still compatible.
       router.route("/").handler(routingContext -> {
           HttpServerResponse response = routingContext.response();
           response.putHeader("content-type", "text/html")
               .end("<h1>Hello from DeVry Video Chat Signaling Server!</h1>");
       });
       router.route("/test").handler(routingContext -> {
           HttpServerResponse response = routingContext.response();
           response.putHeader("content-type", "text/html")
                  .end("Served from " + Thread.currentThread().getName() + "\n");
      });

        // Serve static resources from the /assets directory
      router.route("/assets/*").handler(StaticHandler.create("assets"));
        
      HttpServerOptions options = new HttpServerOptions().setSsl(true).setWebsocketSubProtocols("wss").setKeyStoreOptions(
          new JksOptions().setPath("keystore.jks").setPassword("password"));
      
      vertx.createHttpServer(options)
          .websocketHandler(webSocket -> {
              logger.debug("New client connected: " + ((ServerWebSocket) webSocket).remoteAddress());
              logger.debug("path= " + webSocket.path());
              logger.debug("uri= " + webSocket.uri());
              logger.debug("localAddress= " + webSocket.localAddress());
              logger.debug("textHandlerID= " + webSocket.textHandlerID());
              
              webSocket.handler(new Handler<Buffer>() {
                  public void handle(Buffer data) {
                      JsonObject jsonObj = data.toJsonObject();                       
                      processMessage(webSocket, jsonObj);
                  }
              });
              
              webSocket.closeHandler(new Handler<Void>() {
                  @Override
                  public void handle(Void arg0) {
                      logger.debug("Close handler. arg0=" + arg0);                                                
                  }
              });
              
          })
          .requestHandler(router::accept)
          .requestHandler(req -> {
              if (req.uri().equals("/")) {
                  req.response().sendFile("ws.html");
              }
              else if (req.uri().equals("/test")) {
                  req.response().end("Served from " + Thread.currentThread().getName() + "\n");
              }
          })
          .listen(config().getInteger("http.port", 8443), result -> {
              if (result.succeeded()) {
                  fut.complete();
              } else {
                  fut.fail(result.cause());
              }
          });
    }

    @Override
    public void stop(Future<Void> stopFuture) {
    }
    
    private void processMessage(ServerWebSocket socket, JsonObject jsonObj) {
        String type = jsonObj.containsKey("type") ? jsonObj.getString("type") : "";
        String username = jsonObj.containsKey("username") ? jsonObj.getString("username") : "";
        String callerId = jsonObj.containsKey("callerId") ? jsonObj.getString("callerId") : System.currentTimeMillis() + "-" + username;
        LocalMap<String, String> callerWaitingMap = vertx.sharedData().getLocalMap(CALLER_WAITING_MAP);

        if (username.length() == 0) {
            jsonObj.put("type", "error").put("message", "Missing username");
            socket.writeFinalTextFrame(jsonObj.toString());
            return;
        }

        EventBus eventBus = vertx.eventBus();
        LocalMap<String, String> callerMap = vertx.sharedData().getLocalMap(callerId);

        switch (type) {

        case "advisorLogin":
            eventBus.consumer(UPDATE_CALLER_COUNT_EVENT, message -> {
                JsonObject json = new JsonObject();
                json.put("type", "updateWaitingCallerCount");
                json.put("waitingCallerCount", callerWaitingMap.size());
                socket.writeFinalTextFrame(json.toString());
            });

            eventBus.consumer(socket.textHandlerID(), message -> {
                socket.writeFinalTextFrame(message.body().toString());
            });         
            
            jsonObj.put("waitingCallerCount", callerWaitingMap.size());
            socket.writeFinalTextFrame(jsonObj.toString());
            break;
            
        case "call":            
            callerMap.put(socket.textHandlerID(), "_host_");
            callerWaitingMap.put(callerId, socket.textHandlerID());
                        
            eventBus.publish(UPDATE_CALLER_COUNT_EVENT, callerId);                      
            eventBus.consumer(socket.textHandlerID(), message -> {
                socket.writeFinalTextFrame(message.body().toString());
            });         
            eventBus.consumer(callerId + "LEAVE_EVENT", message -> {
                logger.debug("Leave: " + message.body());                       
                socket.writeFinalTextFrame(message.body().toString());
            });         

            jsonObj.put("success", "true");
            jsonObj.put("callerId", callerId);
            socket.writeFinalTextFrame(jsonObj.toString());
            break;

        case "callList":
            jsonObj.put("value", getCallList(callerWaitingMap));
            socket.writeFinalTextFrame(jsonObj.toString());
            break;

        case "join":
            if (callerMap.isEmpty()) {
                jsonObj.put("success", "false").put("value", "Call does not exist");    
            }
            else {
                // Keep the caller and remove joined socket since it is no longer needed, 
                callerMap.keySet().forEach(key -> {
                    if (!callerMap.get(key).startsWith("_host_")) {
                        callerMap.remove(key);
                    }
                });;
                callerMap.put(socket.textHandlerID(), username);
                eventBus.consumer(callerId + "LEAVE_EVENT", message -> {
                    logger.debug("Leave: " + message.body());                       
                    socket.writeFinalTextFrame(message.body().toString());
                });         
                jsonObj.put("success", "true");             
            }
            
            if (callerWaitingMap.remove(callerId) != null) {
                eventBus.publish(UPDATE_CALLER_COUNT_EVENT, callerId);              
            }
            
            socket.writeFinalTextFrame(jsonObj.toString());         
            break;

        case "leave":
            if (callerWaitingMap.remove(callerId) != null) {
                eventBus.publish(UPDATE_CALLER_COUNT_EVENT, callerId);
            }
            eventBus.publish(callerId + "LEAVE_EVENT", jsonObj.toString());
            break;
            
        case "offer":
        case "answer":
        case "candidate":
            processMessage(callerMap, socket, callerId, jsonObj);           
            break;
        
        default:
            socket.writeFinalTextFrame("{\"type\":\"error\", \"message\":\"Unrecognized command: " + type + "\"}");     
                    
        }   
    }

    private void processMessage(LocalMap<String, String> callerMap, ServerWebSocket currentSocket, String callerId, JsonObject jsonObj) {       
        String currentSocketId = currentSocket.textHandlerID();
        
        callerMap.keySet().forEach(targetSocketId -> {
            if (currentSocketId != targetSocketId) {
                vertx.eventBus().send(targetSocketId, jsonObj.toString());
            }
        }); 
    }
    
    private JsonObject getCallList(LocalMap<String, String> callerWaitingMap) {
        Set<String> callerWaitingSet = callerWaitingMap.keySet();
        JsonObject jsonObj = new JsonObject();
        
        callerWaitingSet.forEach(callerId -> {
            jsonObj.put(callerId, new JsonArray().add(callerId.split("-")[1]));
        });
        
        return jsonObj;
    }


}