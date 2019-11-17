package ostrowski.combat.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterGenerator;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.Door;
import ostrowski.combat.common.things.Thing;
import ostrowski.protocol.ObjectChanged;
import ostrowski.util.SemaphoreAutoTracker;

public class ArenaEvent implements Cloneable
{
   public static final String EVENT_TYPE_DISPLAY_MESSAGE_PRIVATELY = "Private Message";
   public static final String EVENT_TYPE_DISPLAY_MESSAGE_PUBLICLY  = "Public Message";
   public static final String EVENT_TYPE_OPEN_DOOR                 = "Open Door";
   public static final String EVENT_TYPE_CLOSE_DOOR                = "Close Door";
   public static final String EVENT_TYPE_ENTER_CHARACTER           = "Enter Character";
   public static final String EVENT_TYPE_TRAP                      = "Trap";
   public static final String EVENT_TYPE_NEW_MAP                   = "New Map";
   public static final String EVENT_TYPE_ENABLE_TRIGGER            = "Enable Named Trigger";
   public static final String EVENT_TYPE_DISABLE_TRIGGER           = "Disable Named Trigger";
   public static final String EVENT_TYPE_DISABLE_THIS_TRIGGER      = "Disable This Trigger";
   public static final String EVENT_TYPE_TELEPORT                  = "Teleport";

   static final ArrayList<String> EVENT_TYPES = new ArrayList<>();
   static {
      EVENT_TYPES.add(EVENT_TYPE_DISPLAY_MESSAGE_PRIVATELY);
      EVENT_TYPES.add(EVENT_TYPE_DISPLAY_MESSAGE_PUBLICLY);
      EVENT_TYPES.add(EVENT_TYPE_OPEN_DOOR);
      EVENT_TYPES.add(EVENT_TYPE_CLOSE_DOOR);
      EVENT_TYPES.add(EVENT_TYPE_ENTER_CHARACTER);
      EVENT_TYPES.add(EVENT_TYPE_TRAP);
      EVENT_TYPES.add(EVENT_TYPE_NEW_MAP);
      EVENT_TYPES.add(EVENT_TYPE_ENABLE_TRIGGER);
      EVENT_TYPES.add(EVENT_TYPE_DISABLE_TRIGGER);
      EVENT_TYPES.add(EVENT_TYPE_DISABLE_THIS_TRIGGER);
      EVENT_TYPES.add(EVENT_TYPE_TELEPORT);
   }

   String                   _eventName                           = "";
   String                   _eventType                           = EVENT_TYPE_DISPLAY_MESSAGE_PUBLICLY;
   String                   _eventData                           = "";
   ArrayList<ArenaCoordinates> _eventLocations                   = null;

   public ArenaEvent(String name) {
      _eventName = name;
   }

   public String getData() {
      if (_eventData == null) {
         return _eventData = "";
      }
      return _eventData;
   }
   public void setData(String data) {
      _eventData = data;
   }
   public String getType() {
      return _eventType;
   }
   public void setType(String type) {
      _eventType = type;
   }

   public boolean usesLocation() {
      if (_eventType.equals(EVENT_TYPE_CLOSE_DOOR)      ||
          _eventType.equals(EVENT_TYPE_OPEN_DOOR)       ||
          _eventType.equals(EVENT_TYPE_ENTER_CHARACTER) ||
          _eventType.equals(EVENT_TYPE_TRAP)) {
         return true;
      }
      return false;
   }
   public Element getXmlNode(Document mapDoc, String newLine) {
      Element eventElement = mapDoc.createElement("event");
      eventElement.setAttribute("name", String.valueOf(_eventName));
      eventElement.setAttribute("type", String.valueOf(_eventType));
      eventElement.setAttribute("data", String.valueOf(_eventData));
      if ((_eventLocations != null) && (_eventLocations.size() > 0)) {
         for (ArenaCoordinates location : _eventLocations) {
            Element locationElement = mapDoc.createElement("location");
            locationElement.setAttribute("x", String.valueOf(location._x));
            locationElement.setAttribute("y", String.valueOf(location._y));
            eventElement.appendChild(mapDoc.createTextNode(newLine + "  "));
            eventElement.appendChild(locationElement);
         }
         eventElement.appendChild(mapDoc.createTextNode(newLine));
      }
      return eventElement;
   }

   public static ArenaEvent getArenaEvent(Node node) {
      if (!node.getNodeName().equals("event")) {
         return null;
      }

      NamedNodeMap attributes = node.getAttributes();
      if (attributes == null) {
         return null;
      }

      String name = attributes.getNamedItem("name").getNodeValue();
      ArenaEvent event = new ArenaEvent(name);
      event._eventType = attributes.getNamedItem("type").getNodeValue();
      event._eventData = attributes.getNamedItem("data").getNodeValue();
      event._eventLocations = new ArrayList<>();
      NodeList children = node.getChildNodes();
      for (int teamIndex=0 ; teamIndex<children.getLength() ; teamIndex++) {
         Node child = children.item(teamIndex);
         NamedNodeMap childAttrs = child.getAttributes();
         if (childAttrs != null) {
            if (child.getNodeName().equals("location")) {
               short locX = Short.parseShort(childAttrs.getNamedItem("x").getNodeValue());
               short locY = Short.parseShort(childAttrs.getNamedItem("y").getNodeValue());
               event._eventLocations.add(new ArenaCoordinates(locX, locY));
            }
         }
      }
      return event;
   }

   public String getName() {
      return _eventName;
   }

   public void setName(String name) {
      _eventName = name;
   }

   public boolean isEventAtLocation(ArenaCoordinates loc) {
      return (_eventLocations != null) && _eventLocations.contains(loc);
   }

   /**
    * returns true to indicate that this TriggerEvent caused the mover to stop moving.
    * @param triggeringCharacter
    * @return true if this TriggerEvent causes the mover to stop moving.
    */
   public boolean fireEvent(final Character triggeringCharacter) {
      if (!CombatServer._isServer) {
         DebugBreak.debugBreak();
         return false;
      }
      final Arena arena = CombatServer._this.getArena();
      if (_eventType.equals(EVENT_TYPE_DISPLAY_MESSAGE_PUBLICLY)) {
         arena.sendMessageTextToAllClients(_eventData, true/*popUp*/);
      }
      else if (_eventType.equals(EVENT_TYPE_DISPLAY_MESSAGE_PRIVATELY)) {
         arena.sendMessageTextToClient(_eventData, triggeringCharacter, true/*popUp*/);
      }
      else if ((_eventType.equals(EVENT_TYPE_CLOSE_DOOR)) ||
               (_eventType.equals(EVENT_TYPE_OPEN_DOOR))) {
         for (ArenaCoordinates eventLoc : _eventLocations) {
            ArenaLocation loc = arena.getLocation(eventLoc);
            ArenaLocation origLoc = loc.clone();
            boolean locChanged = false;
            synchronized (loc) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc._lock_this)) {
                  for (Door door : loc.getDoors()) {
                     if (_eventType.equals(EVENT_TYPE_CLOSE_DOOR) && door.isOpen()) {
                        if (door.close()) {
                           locChanged = true;
                        }
                        if (door.lock()) {
                           locChanged = true;
                        }
                     }
                     if (_eventType.equals(EVENT_TYPE_OPEN_DOOR) && !door.isOpen()) {
                        if (door.unlock()) {
                           locChanged = true;
                        }
                        if (door.open()) {
                           locChanged = true;
                        }
                     }
                  }
               }
            }
            if (locChanged) {
               ObjectChanged changeNotification = new ObjectChanged(origLoc, loc);
               loc.notifyWatchers(origLoc, loc, changeNotification , null/*skipList*/, null/*diag*/);
            }
         }
      }
      else if (_eventType.equals(EVENT_TYPE_ENTER_CHARACTER)) {
         AI_Type aiEngineType =  AI_Type.NORM;
         String lowerCaseData = _eventData.toLowerCase();
         String nonAiData = _eventData;
         for (AI_Type aiType : AI_Type.values()) {
            String aiTypeLowerCase = aiType.name.toLowerCase();
            if (lowerCaseData.contains(" " + aiTypeLowerCase + " ")) {
               int index = lowerCaseData.indexOf(" " + aiTypeLowerCase + " ");
               nonAiData = _eventData.substring(0, index) + _eventData.substring(index + aiTypeLowerCase.length() + 1);
               aiEngineType = aiType;
               break;
            }
            if (lowerCaseData.startsWith(aiTypeLowerCase + " ")) {
               nonAiData = _eventData.substring(aiTypeLowerCase.length() + 1).trim();
               aiEngineType = aiType;
               break;
            }
            if (lowerCaseData.endsWith(" " + aiTypeLowerCase)) {
               nonAiData = _eventData.substring(0, _eventData.length() - (aiTypeLowerCase.length() + 1)).trim();
               aiEngineType = aiType;
               break;
            }
         }
         Position position = Position.STANDING;
         String nonAiDataLowerCase = nonAiData.toLowerCase();
         if (lowerCaseData.contains("position")) {
            for (Position pos : Position.values()) {
               String posName = pos.name.toLowerCase();
               String positionName = "position:" + posName.replace(' ', '_');
               int locWithinSource = nonAiDataLowerCase.indexOf(positionName);
               if (locWithinSource > -1) {
                  position = pos;
                  nonAiData = (nonAiData.substring(0, locWithinSource) + nonAiData.substring(locWithinSource + positionName.length())).trim();
                  break;
               }
            }
         }
         Character newChar = CombatServer._this._charFile.getCharacter(nonAiData);
         if (newChar == null) {
            newChar = CharacterGenerator.generateRandomCharacter(nonAiData, arena, true/*printCharacter*/);
         }
         else {
            // Remove the character name from the remaining tokens:
            String charNameLower = newChar.getName().toLowerCase();
            int index = nonAiData.toLowerCase().indexOf(" " + charNameLower + " ");
            if (index >= 0) {
               nonAiData = nonAiData.substring(0, index) + nonAiData.substring(index + charNameLower.length() + 1);
            }
            else if (nonAiData.toLowerCase().startsWith(charNameLower + " ")) {
               nonAiData = nonAiData.substring(charNameLower.length() + 1).trim();
            }
            else if (nonAiData.toLowerCase().endsWith(" " + charNameLower)) {
               nonAiData = nonAiData.substring(0, nonAiData.length() - (charNameLower.length() + 1)).trim();
            }
            String[] tokens = nonAiData.split(" ");
            for (String token : tokens) {
               newChar.addEquipment(Thing.getThing(token, newChar.getRace()));
            }
         }
         // If the event specified the team ID, it will be reflected in the generated character
         byte team = newChar._teamID;
         if (team == -1) {
            // Pick the opposing team
            if (triggeringCharacter._teamID == Enums.TEAM_ALPHA) {
               team = Enums.TEAM_BETA;
            }
            else {
               team = Enums.TEAM_ALPHA;
            }
         }
         // try each of the locations in the location list until we find one that is unoccupied
         for (ArenaCoordinates location : _eventLocations) {
            if (arena.addCombatant(newChar, team, location._x, location._y, aiEngineType)) {
               newChar.getCondition().setPosition(position, arena.getCombatMap(), newChar);
               return false;
            }
         }
         // search progressively larger and larger locations until we find an unoccupied location:
         for (int searchDistance=1 ; searchDistance<=4 ; searchDistance++) {
            for (int xOffset = 0-searchDistance ; xOffset <= searchDistance ; xOffset++) {
               for (int yOffset = 0-(searchDistance*2) ; yOffset <= (searchDistance*2) ; yOffset++) {
                  // don't consider illegal locations:
                  if ((xOffset%2) == (yOffset%2)) {
                     // don't consider the location we have already checked:
                     if ((Math.abs(xOffset) < searchDistance) && (Math.abs(yOffset) < searchDistance)) {
                        continue;
                     }
                     for (ArenaCoordinates location : _eventLocations) {
                        if (arena.addCombatant(newChar, team, (short)(location._x + xOffset), (short)(location._y + yOffset), aiEngineType)) {
                           newChar.getCondition().setPosition(position, arena.getCombatMap(), newChar);
                           return false;
                        }
                     }
                  }
               }
            }
         }
         // If we got here, then we never found a location
         DebugBreak.debugBreak();
      }
      else if (_eventType.equals(EVENT_TYPE_TELEPORT)) {
         int locIndex = 0;
         CombatMap map = arena.getCombatMap();
         while (locIndex < _eventLocations.size()) {
            ArenaLocation eventLoc = map.getLocation(_eventLocations.get(locIndex));
            if (eventLoc.getCharacters().size() == 0) {
               if (triggeringCharacter.setHeadLocation(eventLoc, triggeringCharacter.getFacing(), map, null/*diag*/)) {
                  break;
               }
            }
            locIndex++;
         }
      }
      else if (_eventType.equals(EVENT_TYPE_DISABLE_THIS_TRIGGER)){
         CombatMap map = arena.getCombatMap();
         for (ArenaTrigger trigger : map.getTriggers()) {
            if (trigger.getEvents().contains(this)) {
               trigger.setEnabled(false);
            }
         }
      }
      else if ((_eventType.equals(EVENT_TYPE_ENABLE_TRIGGER)) ||
               (_eventType.equals(EVENT_TYPE_DISABLE_TRIGGER))){
         boolean enable = _eventType.equals(EVENT_TYPE_ENABLE_TRIGGER);
         CombatMap map = arena.getCombatMap();

         String triggerName = getData();
         for (ArenaTrigger trigger : map.getTriggers()) {
            if ((triggerName != null) && (triggerName.length()>0)) {
               if (trigger.getName().equalsIgnoreCase(triggerName)) {
                  trigger.setEnabled(enable);
               }
            }
            else {
               if (trigger.getEvents().contains(this)) {
                  trigger.setEnabled(enable);
               }
            }
         }
      }
      else if (_eventType.equals(EVENT_TYPE_NEW_MAP)) {
         // save the current map
         CombatMap map = arena.getCombatMap();
         // if we come back to this map, make sure we don't re-create the original players
         map.clearCharacterStartingLocations();
         final ArrayList<Character> team = new ArrayList<>();
         final byte teamID = triggeringCharacter._teamID;
         for (Character combatant : arena.getCombatants()) {
            if (combatant._teamID == teamID) {
               if (combatant.stillFighting()) {
                  team.add(combatant);
                  // remove the characters that are leaving this map, so they aren't there when they might return.
                  map.removeCharacter(combatant);
               }
            }
         }
         // keep this map for later, in case we come back.
         _savedMaps.put(map.getName(), map.clone());
         map.removeAllCombatants();
         if (!CombatServer._this.getShell().isDisposed()) {
            Display display = CombatServer._this.getShell().getDisplay();
            if (!display.isDisposed()) {
               // This object allows us to wait for the main thread to finish its processing of the new map before we return
               final Object waitObj = new Object();
               synchronized(waitObj) {
                  // Since CombatServer.setMap(...) require UI-thread access permissions,
                  // we have to defer this to the UI thread
                  display.asyncExec(new Runnable() {
                     @Override
                     public void run() {
                        arena.removeAllCombatants();
                        CombatMap newMap = _savedMaps.get(_eventData);
                        if (newMap == null) {
                           CombatServer._this.setMap(_eventData);
                        }
                        else {
                           arena.setCombatMap(newMap, true/*clearCombatants*/);
                        }
                        arena.addStockCombatants();
                        ArrayList<ArenaCoordinates> locations = new ArrayList<>();
                        for (Character teamMember : team) {
                           // make sure we never run out of locations, even if we re-use them twice.
                           if (locations.isEmpty()) {
                              locations.addAll(_eventLocations);
                           }

                           ArenaCoordinates loc = locations.remove(0);
                           arena.addCombatant(teamMember, teamID, loc._x, loc._y, teamMember.getAIType());
                        }
                        arena.terminateBattle();
                        arena.beginBattle();
                        synchronized(waitObj) {
                           // Let the non-UI thread know we are done, so it can resume.
                           waitObj.notifyAll();
                        }
                     }
                  });
                  try {
                     // Wait for the main thread to finish its processing of the new map before we return
                     waitObj.wait();
                  } catch (InterruptedException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
         // return true to indicate that this TriggerEvent caused the mover to stop moving.
         return true;
      }
      else if (_eventType.equals(EVENT_TYPE_TRAP)) {
      }
      return false;
   }
   static public HashMap<String, CombatMap> _savedMaps = new HashMap<>();

   public boolean isExitEvent() {
      return _eventType.equals(EVENT_TYPE_NEW_MAP);
   }

   public boolean equals(ArenaEvent other) {
      if (!_eventName.equals(other._eventName)) {
         return false;
      }
      if (!_eventType.equals(other._eventType)) {
         return false;
      }
      if (!_eventData.equals(other._eventData)) {
         return false;
      }
      if ((_eventLocations != null) && (other._eventLocations != null)) {
         if (_eventLocations.size() != other._eventLocations.size()) {
            return false;
         }
         for (int i=0 ; i<_eventLocations.size(); i++) {
            if (!_eventLocations.get(i).sameCoordinates(other._eventLocations.get(i))) {
               return false;
            }
         }
      }
      else if ((_eventLocations != null) || (other._eventLocations != null)) {
         return false;
      }
      return true;
   }

   @Override
   public ArenaEvent clone() {
      try {
         ArenaEvent dup = (ArenaEvent) super.clone();
         if (_eventLocations != null) {
            dup._eventLocations = new ArrayList<>();
            dup._eventLocations.addAll(_eventLocations);
         }
         return dup;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }

   public Collection<ArenaCoordinates> getLocations() {
      return _eventLocations;
   }
}
