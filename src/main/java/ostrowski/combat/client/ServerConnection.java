/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.client;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.Condition;
import ostrowski.combat.protocol.BeginBattle;
import ostrowski.combat.protocol.CombatSocket;
import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.protocol.MessageText;
import ostrowski.combat.protocol.ServerStatus;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.Configuration;
import ostrowski.protocol.ClientID;
import ostrowski.protocol.ObjectChanged;
import ostrowski.protocol.ObjectDelete;
import ostrowski.protocol.ObjectInfo;
import ostrowski.protocol.Response;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.sockets.SocketConnector;

public class ServerConnection extends CombatSocket
{
   CharacterDisplay _display;
   StatusChit _statusChit = null;

   public ServerConnection(CharacterDisplay display) {
      super("ServerConnection");
      _display = display;
   }

   @Override
   public void processRecievedObject(final SerializableObject inObj)
   {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!_display._shell.isDisposed()) {
         Display display = _display._shell.getDisplay();
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               processRecievedObjectInUIThread(inObj);
            }
         });
      }
   }

   public static HashMap<Integer, Character> _charactersMap = new HashMap<>();
   public void processRecievedObjectInUIThread(SerializableObject inObj) {
      if (_display._shell.isDisposed()) {
         return;
      }
      // First check for events that apply to the display:
      if (inObj instanceof Character) {
         Character newChar = (Character) inObj;
         Integer uniqueID = new Integer(newChar._uniqueID);
         Character existingChar = _charactersMap.get(uniqueID);
         if (existingChar == null) {
            _charactersMap.put(uniqueID, newChar);
         }
         else {
            existingChar.copyData(newChar);
         }
         _display.updateCharacter(newChar);
      }
      else if (inObj instanceof ServerStatus) {
         ServerStatus status = (ServerStatus) inObj;
         _display.updateServerStatus(status);
      }
      else if (inObj instanceof CombatMap) {
         CombatMap map = (CombatMap) inObj;
         _display.updateMap(map);
      }
      else if (inObj instanceof Condition) {
         _display._charWidget._character.setCondition((Condition) inObj);
      }
      else if (inObj instanceof BeginBattle) {
         BeginBattle battleMsg = (BeginBattle) inObj;
         _display.beginBattle(battleMsg);
      }
      else if (inObj instanceof ClientID) {
         ClientID msgIn = (ClientID) inObj;
         _display.setUniqueConnectionID(msgIn.getID());
      }
      else if (inObj instanceof MessageText) {
         MessageText msgIn = (MessageText) inObj;
         if (msgIn.isPopUp()) {
            MessageDialog msgDlg = new MessageDialog(_display._shell, SWT.ICON | SWT.MODELESS);
            msgDlg.open(msgIn.getText(), msgIn.isPublic());
         }
         if (msgIn.getText().trim().isEmpty()){
            _display.appendMessage("");
         }
         else {
            _display.appendMessage("<b>" + msgIn.getSource() + ":</b> " + msgIn.getText());
         }
         return;
      }

      // then tell the AI about everything:
      if (_display._charWidget._ai != null) {
         if (_display._charWidget._ai.processObject(inObj, null/*arena*/, _display.getCombatMap(), _display)) {
            sendObject(inObj, "server");
            return;
         }
      }
      // If the AI didn't handle it, deal with it ourselves:
      if (inObj instanceof SyncRequest) {
         SyncRequest req = (SyncRequest) inObj;
         if (req instanceof RequestAttackStyle) {
            if (_display.requestAttackStyle((RequestAttackStyle)req)) {
               Response resp = new Response(req);
               resp.setFullAnswerID(req.getFullAnswerID());
               resp.setAnswerStr(req.getAnswer());
               sendObject(resp, "server");
               return;
            }
         }
         if (req instanceof RequestMovement) {
            _display.requestMovement((RequestMovement)req);
            // the response will be sent after the user selects a location.
            return;
         }
         if (Configuration.showChit())
         {
            if (_statusChit == null) {
               _statusChit = new StatusChit(_display._shell, SWT.MODELESS | SWT.NO_TRIM);
               _statusChit.open();
            }
            if (_display._charWidget._character != null) {
               _statusChit.updateFromCharacter(_display._charWidget._character);
            }
         }

         if ((_statusChit != null) && !Configuration.showChit()) {
            _statusChit.close();
            _statusChit = null;
         }
         RequestUserInput reqUI = new RequestUserInput(_display._shell,
                                                       SWT.ICON_QUESTION | SWT.MODELESS,
                                                       req, false/*showChit*/);
         reqUI.setDefault(req.getDefaultIndex());
         reqUI.setTitle("Question from the Server");
         // While we have a modal dialog up, refresh the browser display, so we know its up to date
         Object answer = reqUI.open();
         if (answer != null) {
            if (answer instanceof Integer) {
               req.setAnswerByOptionIndex((((Integer) answer).intValue()));
            }
            else if (answer instanceof String) {
               req.setCustAnswer((String) answer);
            }
         }
         Response resp = new Response(req);
         resp.setAnswerKey(req.getFullAnswerID());
         resp.setAnswerStr(req.getAnswer());
         sendObject(resp, "server");
      }
      else if (inObj instanceof ostrowski.protocol.ObjectChanged) {
         ObjectChanged objChanged = (ObjectChanged) inObj;
         Object modObject = objChanged.getModifiedObj();
         if (modObject instanceof ArenaLocation) {
            ArenaLocation arenaLoc = (ArenaLocation) modObject;
            _display.updateArenaLocation(arenaLoc);
         }
         else if (modObject instanceof MapVisibility) {
            MapVisibility mapVisibility = (MapVisibility) modObject;
            _display.updateMapVisibility(mapVisibility);
         }
      }
      else if (inObj instanceof ObjectInfo) {
         ObjectInfo objInfo = (ObjectInfo) inObj;
         SerializableObject newObject = objInfo.getObject();
         if (newObject instanceof ArenaLocation) {
            ArenaLocation arenaLoc = (ArenaLocation) newObject;
            _display.updateArenaLocation(arenaLoc);
         }
      }
      else if (inObj instanceof ObjectDelete) {
         ObjectDelete objDelete = (ObjectDelete) inObj;
         SerializableObject newObject = objDelete.getObject();
         if (newObject instanceof ArenaLocation) {
            ArenaLocation arenaLoc = (ArenaLocation) newObject;
            arenaLoc.setVisible(false, null, null, _display._charWidget._character._uniqueID, true/*basedOnFacing*/);
            _display.updateArenaLocation(arenaLoc);
         }
      }
   }

   @Override
   public void handleConnect(SocketConnector connectedConnection) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!_display._shell.isDisposed()) {
         Display display = _display._shell.getDisplay();
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               _display.handleConnect();
            }
         });
      }
   }

   @Override
   public void handleDisconnect(SocketConnector diconnectedConnection) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!_display._shell.isDisposed()) {
         Display display = _display._shell.getDisplay();
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               _display.handleDisconnect();
            }
         });
      }
   }

}
