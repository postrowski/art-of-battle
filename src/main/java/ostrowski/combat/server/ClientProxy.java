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

   public final MonitoringObject _monitoringObject;
   Arena _arena;
   public         String _name         = "";
   private final  int    _clientID;
   static private int    _nextServerID = 1;

    static public void setNextServerID(int nextServerID) { _nextServerID = nextServerID;}
    static public int getNextServerID() { return _nextServerID++;}
    public boolean sendObject(SerializableObject objToSend)
    {
        return super.sendObject(objToSend, _name);
    }

    public ClientProxy(Arena arena)
    {
        super("ClientProxy-"+_nextServerID);
        _arena = arena;
        _clientID = getNextServerID();
        _monitoringObject = new MonitoringObject("ClientProxy");
    }
    @Override
    public void setSocket(Socket socket)
    {
        super.setSocket(socket);
    }

    public int getClientID() {
       return _clientID;
    }

    public void setClientName(String clientName) { _name = clientName; setName(_name+"-ClientProxy");}

    @Override
   public void processReceivedObject(SerializableObject inObj)
    {
       Rules.diag("received msg from " + _name + ": " + inObj);
       if (inObj instanceof MessageText) {
          MessageText msgText = (MessageText)inObj;
          _arena.sendEventToAllClients(msgText);
       }
       else if (inObj instanceof EnterArena) {
          EnterArena msgEnter = (EnterArena) inObj;
          if (msgEnter.isEntering()) {
             msgEnter.getCharacter()._teamID = msgEnter.getTeam();
             _arena.addCombatant(msgEnter.getCharacter(), msgEnter.getTeam(),
                                 msgEnter.getIndexOnTeam(), this, true/*checkForAutoStart*/);
          }
          else {
             _arena.removeCombatant(msgEnter.getCharacter(), this);
          }
       }
       else if (inObj instanceof BeginBattle) {
          _arena.beginBattle();
       }
       else if (inObj instanceof TargetPriorities) {
          _arena.handleTargetPriorities((TargetPriorities)inObj, this);
       }
    }

    @Override
   public void handleConnect(SocketConnector connectedConnection) {
        // once a new client has connected, send it's clientID immediately.
        sendObject(new ClientID(_clientID));
        _arena.connectClient(this);
    }

    @Override
   public void handleDisconnect(SocketConnector diconnectedConnection) {
        _arena.disconnectClient(this);
    }
   @Override
   public String getObjectIDString()
   {
      return _monitoringObject.getObjectIDString();
   }
   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects()
   {
      return _monitoringObject.getSnapShotOfWatchedObjects();
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
      return _monitoringObject.registerMonitoredObject(watchedObject, diag);
   }
   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return _monitoringObject.unregisterMonitoredObject(watchedObject, diag);
   }
   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return _monitoringObject.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
}
