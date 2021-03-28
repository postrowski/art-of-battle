/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.Condition;
import ostrowski.combat.protocol.*;
import ostrowski.combat.protocol.request.RequestArenaEntrance;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.Configuration;
import ostrowski.protocol.*;
import ostrowski.util.sockets.SocketConnector;

import java.util.HashMap;

public class ServerConnection extends CombatSocket
{
   final CharacterDisplay display;
   StatusChit statusChit = null;
   public static final HashMap<Integer, Character> CHARACTERS_MAP = new HashMap<>();

   public ServerConnection(CharacterDisplay display) {
      super("ServerConnection");
      this.display = display;
   }

   @Override
   public void processReceivedObject(final SerializableObject inObj)
   {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!display.shell.isDisposed()) {
         Display display = this.display.shell.getDisplay();
         display.asyncExec(() -> processReceivedObjectInUIThread(inObj));
      }
   }

   public void processReceivedObjectInUIThread(SerializableObject inObj) {
      if (display.shell.isDisposed()) {
         return;
      }
      // First check for events that apply to the display:
      if (inObj instanceof Character) {
         Character newChar = (Character) inObj;
         Integer uniqueID = newChar.uniqueID;
         Character existingChar = CHARACTERS_MAP.get(uniqueID);
         if (existingChar == null) {
            CHARACTERS_MAP.put(uniqueID, newChar);
         }
         else {
            existingChar.copyData(newChar);
         }
         display.updateCharacter(newChar);
      }
      else if (inObj instanceof ServerStatus) {
         ServerStatus status = (ServerStatus) inObj;
         display.updateServerStatus(status);
      }
      else if (inObj instanceof CombatMap) {
         CombatMap map = (CombatMap) inObj;
         display.updateMap(map);
      }
      else if (inObj instanceof Condition) {
         display.charWidget.character.setCondition((Condition) inObj);
      }
      else if (inObj instanceof BeginBattle) {
         BeginBattle battleMsg = (BeginBattle) inObj;
         display.beginBattle(battleMsg);
      }
      else if (inObj instanceof ClientID) {
         ClientID msgIn = (ClientID) inObj;
         display.setUniqueConnectionID(msgIn.getID());
      }
      else if (inObj instanceof MessageText) {
         MessageText msgIn = (MessageText) inObj;
         if (msgIn.isPopUp()) {
            MessageDialog msgDlg = new MessageDialog(display.shell, SWT.ICON | SWT.MODELESS);
            msgDlg.open(msgIn.getText(), msgIn.isPublic());
         }
         if (msgIn.getText().trim().isEmpty()){
            display.appendMessage("");
         }
         else {
            display.appendMessage("<b>" + msgIn.getSource() + ":</b> " + msgIn.getText());
         }
         return;
      }

      // then tell the AI about everything:
      if (display.charWidget.ai != null) {
         if (display.charWidget.ai.processObject(inObj, null/*arena*/, display.getCombatMap(), display)) {
            sendObject(inObj, "server");
            return;
         }
      }
      // If the AI didn't handle it, deal with it ourselves:
      if (inObj instanceof SyncRequest) {
         SyncRequest req = (SyncRequest) inObj;
         if (req instanceof RequestAttackStyle) {
            if (display.requestAttackStyle((RequestAttackStyle)req)) {
               Response resp = new Response(req);
               resp.setFullAnswerID(req.getFullAnswerID());
               resp.setAnswerStr(req.getAnswer());
               sendObject(resp, "server");
               return;
            }
         }
         if (req instanceof RequestMovement) {
            display.requestMovement((RequestMovement)req);
            // the response will be sent after the user selects a location.
            return;
         }
         if (req instanceof RequestLocation) {
            display.requestLocation((RequestLocation)req);
            // the response will be sent after the user selects a location.
            return;
         }
         if (Configuration.showChit())
         {
            if (!(req instanceof RequestArenaEntrance)) {
               if (statusChit == null) {
                  statusChit = new StatusChit(display.shell, SWT.MODELESS | SWT.NO_TRIM);
                  statusChit.open();
               }
               if (display.charWidget.character != null) {
                  statusChit.updateFromCharacter(display.charWidget.character);
               }
            }
         }

         if ((statusChit != null) && !Configuration.showChit()) {
            statusChit.close();
            statusChit = null;
         }
         RequestUserInput reqUI = new RequestUserInput(display.shell,
                                                       SWT.ICON_QUESTION | SWT.MODELESS,
                                                       req, false/*showChit*/);
         reqUI.setDefault(req.getDefaultIndex());
         reqUI.setTitle("Question from the Server");
         // While we have a modal dialog up, refresh the browser display, so we know its up to date
         Object answer = reqUI.open();
         if (answer != null) {
            if (answer instanceof Integer) {
               req.setAnswerByOptionIndex(((Integer) answer));
            }
            else if (answer instanceof String) {
               req.setCustAnswer((String) answer);
            }
         }
         if (req instanceof RequestArenaEntrance) {
            RequestArenaEntrance entReq = (RequestArenaEntrance) req;
            Character chr = entReq.getSelectedCharacter();
            if (chr != null) {
               display.setCharacter(chr);
            }
            else {
               chr = display.charWidget.character;
               chr.uniqueID = display.uniqueConnectionID;
               entReq.setSelectedCharacter(chr);
            }
            EnterArena enterMsg = new EnterArena(chr, true, entReq.getSelectedCharacterTeam(), entReq.getSelectedCharacterTeamPosition());
            sendObject(enterMsg, "server");
            return;
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
            display.updateArenaLocation(arenaLoc);
         }
         else if (modObject instanceof MapVisibility) {
            MapVisibility mapVisibility = (MapVisibility) modObject;
            display.updateMapVisibility(mapVisibility);
         }
      }
      else if (inObj instanceof ObjectInfo) {
         ObjectInfo objInfo = (ObjectInfo) inObj;
         SerializableObject newObject = objInfo.getObject();
         if (newObject instanceof ArenaLocation) {
            ArenaLocation arenaLoc = (ArenaLocation) newObject;
            display.updateArenaLocation(arenaLoc);
         }
      }
      else if (inObj instanceof ObjectDelete) {
         ObjectDelete objDelete = (ObjectDelete) inObj;
         SerializableObject newObject = objDelete.getObject();
         if (newObject instanceof ArenaLocation) {
            ArenaLocation arenaLoc = (ArenaLocation) newObject;
            arenaLoc.setVisible(false, null, null, display.charWidget.character.uniqueID, true/*basedOnFacing*/);
            display.updateArenaLocation(arenaLoc);
         }
      }
   }

   @Override
   public void handleConnect(SocketConnector connectedConnection) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!display.shell.isDisposed()) {
         Display display = this.display.shell.getDisplay();
         display.asyncExec(this.display::handleConnect);
      }
   }

   @Override
   public void handleDisconnect(SocketConnector diconnectedConnection) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!display.shell.isDisposed()) {
         Display display = this.display.shell.getDisplay();
         display.asyncExec(this.display::handleDisconnect);
      }
   }

}
