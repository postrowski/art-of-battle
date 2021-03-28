package ostrowski.combat.server;

import ostrowski.combat.common.Rules;
import ostrowski.combat.protocol.*;
import ostrowski.protocol.ClientID;
import ostrowski.protocol.SerializableObject;
import ostrowski.util.Diagnostics;
import ostrowski.util.IMonitorableObject;
import ostrowski.util.IMonitoringObject;
import ostrowski.util.MonitoringObject;
import ostrowski.util.sockets.SocketConnector;

import java.net.Socket;
import java.util.Vector;

public class ClientProxy extends CombatSocket implements IMonitoringObject {

   public final MonitoringObject monitoringObject;
   Arena arena;
   public         String name = "";
   private final  int clientID;
   static private int nextServerID = 1;

    static public void setNextServerID(int nextServerID) { ClientProxy.nextServerID = nextServerID;}
    static public int getNextServerID() { return nextServerID++;}
    public boolean sendObject(SerializableObject objToSend)
    {
        return super.sendObject(objToSend, name);
    }

    public ClientProxy(Arena arena)
    {
        super("ClientProxy-" + nextServerID);
        this.arena = arena;
        clientID = getNextServerID();
        monitoringObject = new MonitoringObject("ClientProxy");
    }
    @Override
    public void setSocket(Socket socket)
    {
        super.setSocket(socket);
    }

    public int getClientID() {
       return clientID;
    }

    public void setClientName(String clientName) { name = clientName; setName(name + "-ClientProxy");}

    @Override
   public void processReceivedObject(SerializableObject inObj)
    {
       Rules.diag("received msg from " + name + ": " + inObj);
       if (inObj instanceof MessageText) {
          MessageText msgText = (MessageText)inObj;
          arena.sendEventToAllClients(msgText);
       }
       else if (inObj instanceof EnterArena) {
          EnterArena msgEnter = (EnterArena) inObj;
          if (msgEnter.isEntering()) {
             msgEnter.getCharacter().teamID = msgEnter.getTeam();
             arena.addCombatant(msgEnter.getCharacter(), msgEnter.getTeam(),
                                msgEnter.getIndexOnTeam(), this, true/*checkForAutoStart*/);
          }
          else {
             arena.removeCombatant(msgEnter.getCharacter(), this);
          }
       }
       else if (inObj instanceof BeginBattle) {
          arena.beginBattle();
       }
       else if (inObj instanceof TargetPriorities) {
          arena.handleTargetPriorities((TargetPriorities)inObj, this);
       }
    }

    @Override
   public void handleConnect(SocketConnector connectedConnection) {
        // once a new client has connected, send it's clientID immediately.
        sendObject(new ClientID(clientID));
        arena.connectClient(this);
    }

    @Override
   public void handleDisconnect(SocketConnector diconnectedConnection) {
        arena.disconnectClient(this);
    }
   @Override
   public String getObjectIDString()
   {
      return monitoringObject.getObjectIDString();
   }
   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects()
   {
      return monitoringObject.getSnapShotOfWatchedObjects();
   }
   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject,
                                      IMonitorableObject modifiedWatchedObject,
                                      Object changeNotification,
                                      Vector<IMonitoringObject> skipList,
                                      Diagnostics diag)
   {
      if (changeNotification instanceof SerializableObject) {
         if ((skipList == null) || (!skipList.contains(this))) {
            sendObject((SerializableObject) changeNotification);
         }
      }
   }
   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return monitoringObject.registerMonitoredObject(watchedObject, diag);
   }
   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return monitoringObject.unregisterMonitoredObject(watchedObject, diag);
   }
   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return monitoringObject.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
}
