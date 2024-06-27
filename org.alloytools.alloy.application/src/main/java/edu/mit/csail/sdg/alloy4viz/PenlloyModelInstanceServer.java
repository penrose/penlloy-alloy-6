package edu.mit.csail.sdg.alloy4viz;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

public class PenlloyModelInstanceServer extends WebSocketServer {

  private static PenlloyModelInstanceServer serverInstance;
  private static int                        port = 1549;

  public static PenlloyModelInstanceServer getServerInstance() {
    return serverInstance;
  }

  public static int getServerPort() {
    return port;
  }

  public static void setServerPort(int p) {
    port = p;
  }

  public static boolean startServer() {
    try {
      System.out.println("starting Penlloy server at port " + port);
      serverInstance = new PenlloyModelInstanceServer(port);
      serverInstance.start();
      return true;
    } catch (Exception e) {
      serverInstance = null;
      System.err.println("could not start Penlloy server at port " + port);
      return false;
    }
  }

  public static void stopServer() {
    try {
      if (serverInstance != null) {
        serverInstance.stop();
        serverInstance = null;
        System.out.println("Penlloy WebSocket server stopped");
      }
    } catch (Exception e) {
    } finally {
      serverInstance = null;
    }
  }

  public static void changePort(int port) {
    System.out.println("setting port = " + port);
    stopServer();
    setServerPort(port);
    startServer();
  }

  private PenlloyModelInstanceServer(int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    conn.send("{\"kind\": \"connected\"}");
    System.out.println("new connection: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());

    // send the current model and instance (if available) to the new connection
    sendCurrent(conn);
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    System.out.println("closed connection: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    System.out.println("received: " + message);
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    System.err.println("server encountered an error");
    ex.printStackTrace();
    stopServer();
  }

  @Override
  public void onStart() {
    System.out.println("server started successfully at port " + getPort());
  }

  private JSONObject currentModelJson;
  private JSONObject currentInstanceJson;

  public static boolean broadcastNewModelAndInstance(AlloyModel model, AlloyInstance instance) {
    System.out.println("setting current model and instance for broadcast by server");

    PenlloyModelInstanceServer sInst = getServerInstance();

    if (sInst == null) {
      System.err.println("failed to set current model and instance because server is not running");
      return false;
    }


    

    sInst.currentModelJson = model.toJson();
    sInst.currentInstanceJson = instance.toJson();

    return broadcastCurrentModelAndInstance();
  }

  public static boolean broadcastCurrentModelAndInstance() {
    System.out.println("broadcasting current model and instance");

    PenlloyModelInstanceServer sInst = getServerInstance();

    if (sInst == null) {
      System.err.println("failed to set new model and instance because server is not running");
      return false;
    }

    boolean status = sInst.sendCurrent(null);

    if (status) {
      System.out.println("broadcasted current model and instance");
    } else {
      System.err.println("failed to broadcast current model and instance");
    }
    return status;
  }

  private boolean sendCurrent(WebSocket conn) {
    if (currentModelJson != null && currentInstanceJson != null) {
      String jsonMsg = "{\"kind\":\"ModelAndInstance\",\"model\":" + currentModelJson + ", \"instance\":" + currentInstanceJson + "}";
      if (conn == null) {
        broadcast(jsonMsg);
        System.out.println(jsonMsg); //for testing
      } else {
        conn.send(jsonMsg);
        System.out.println(jsonMsg); //for testing

      }
      return true;
    } else {
      return false;
    }
  }
}
