package ostrowski.combat.server;

import org.eclipse.swt.widgets.Display;
import org.w3c.dom.*;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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

   static final List<String> EVENT_TYPES = new ArrayList<>();
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

   String eventName;
   String                 eventType       = EVENT_TYPE_DISPLAY_MESSAGE_PUBLICLY;
   String                 eventData      = "";
   List<ArenaCoordinates> eventLocations = null;
   static public HashMap<String, CombatMap> savedMaps = new HashMap<>();

   public ArenaEvent(String name) {
      eventName = name;
   }

   public String getData() {
      if (eventData == null) {
         return eventData = "";
      }
      return eventData;
   }
   public void setData(String data) {
      eventData = data;
   }
   public String getType() {
      return eventType;
   }
   public void setType(String type) {
      eventType = type;
   }

   public boolean usesLocation() {
      return eventType.equals(EVENT_TYPE_CLOSE_DOOR) ||
             eventType.equals(EVENT_TYPE_OPEN_DOOR) ||
             eventType.equals(EVENT_TYPE_ENTER_CHARACTER) ||
             eventType.equals(EVENT_TYPE_TRAP);
   }
   public Element getXmlNode(Document mapDoc, String newLine) {
      Element eventElement = mapDoc.createElement("event");
      eventElement.setAttribute("name", String.valueOf(eventName));
      eventElement.setAttribute("type", String.valueOf(eventType));
      eventElement.setAttribute("data", String.valueOf(eventData));
      if ((eventLocations != null) && (eventLocations.size() > 0)) {
         for (ArenaCoordinates location : eventLocations) {
            Element locationElement = mapDoc.createElement("location");
            locationElement.setAttribute("x", String.valueOf(location.x));
            locationElement.setAttribute("y", String.valueOf(location.y));
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
      event.eventType = attributes.getNamedItem("type").getNodeValue();
      event.eventData = attributes.getNamedItem("data").getNodeValue();
      event.eventLocations = new ArrayList<>();
      NodeList children = node.getChildNodes();
      for (int teamIndex=0 ; teamIndex<children.getLength() ; teamIndex++) {
         Node child = children.item(teamIndex);
         NamedNodeMap childAttrs = child.getAttributes();
         if (childAttrs != null) {
            if (child.getNodeName().equals("location")) {
               short locX = Short.parseShort(childAttrs.getNamedItem("x").getNodeValue());
               short locY = Short.parseShort(childAttrs.getNamedItem("y").getNodeValue());
               event.eventLocations.add(new ArenaCoordinates(locX, locY));
            }
         }
      }
      return event;
   }

   public String getName() {
      return eventName;
   }

   public void setName(String name) {
      eventName = name;
   }

   public boolean isEventAtLocation(ArenaCoordinates loc) {
      return (eventLocations != null) && eventLocations.contains(loc);
   }

   /**
    * returns true to indicate that this TriggerEvent caused the mover to stop moving.
    * @param triggeringCharacter
    * @return true if this TriggerEvent causes the mover to stop moving.
    */
   public boolean fireEvent(final Character triggeringCharacter) {
      if (!CombatServer.isServer) {
         DebugBreak.debugBreak();
         return false;
      }
      final Arena arena = CombatServer._this.getArena();
      if (eventType.equals(EVENT_TYPE_DISPLAY_MESSAGE_PUBLICLY)) {
         arena.sendMessageTextToAllClients(eventData, true/*popUp*/);
      }
      else if (eventType.equals(EVENT_TYPE_DISPLAY_MESSAGE_PRIVATELY)) {
         arena.sendMessageTextToClient(eventData, triggeringCharacter, true/*popUp*/);
      }
      else if ((eventType.equals(EVENT_TYPE_CLOSE_DOOR)) ||
               (eventType.equals(EVENT_TYPE_OPEN_DOOR))) {
         for (ArenaCoordinates eventLoc : eventLocations) {
            ArenaLocation loc = arena.getLocation(eventLoc);
            ArenaLocation origLoc = loc.clone();
            boolean locChanged = false;
            synchronized (loc) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc.lock_this)) {
                  for (Door door : loc.getDoors()) {
                     if (eventType.equals(EVENT_TYPE_CLOSE_DOOR) && door.isOpen()) {
                        if (door.close()) {
                           locChanged = true;
                        }
                        if (door.lock()) {
                           locChanged = true;
                        }
                     }
                     if (eventType.equals(EVENT_TYPE_OPEN_DOOR) && !door.isOpen()) {
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
      else if (eventType.equals(EVENT_TYPE_ENTER_CHARACTER)) {
         AI_Type aiEngineType =  AI_Type.NORM;
         String lowerCaseData = eventData.toLowerCase();
         String nonAiData = eventData;
         for (AI_Type aiType : AI_Type.values()) {
            String aiTypeLowerCase = aiType.name.toLowerCase();
            if (lowerCaseData.contains(" " + aiTypeLowerCase + " ")) {
               int index = lowerCaseData.indexOf(" " + aiTypeLowerCase + " ");
               nonAiData = eventData.substring(0, index) + eventData.substring(index + aiTypeLowerCase.length() + 1);
               aiEngineType = aiType;
               break;
            }
            if (lowerCaseData.startsWith(aiTypeLowerCase + " ")) {
               nonAiData = eventData.substring(aiTypeLowerCase.length() + 1).trim();
               aiEngineType = aiType;
               break;
            }
            if (lowerCaseData.endsWith(" " + aiTypeLowerCase)) {
               nonAiData = eventData.substring(0, eventData.length() - (aiTypeLowerCase.length() + 1)).trim();
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
         Character newChar = CombatServer._this.charFile.getCharacter(nonAiData);
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
         byte team = newChar.teamID;
         if (team == -1) {
            // Pick the opposing team
            if (triggeringCharacter.teamID == Enums.TEAM_ALPHA) {
               team = Enums.TEAM_BETA;
            }
            else {
               team = Enums.TEAM_ALPHA;
            }
            newChar.teamID = team;
         }
         // try each of the locations in the location list until we find one that is unoccupied
         for (ArenaCoordinates location : eventLocations) {
            if (arena.addCombatant(newChar, team, location.x, location.y, aiEngineType)) {
               newChar.getCondition().setPosition(position, arena.getCombatMap(), newChar);
               return false;
            }
         }
         // search progressively larger and larger locations until we find an unoccupied location:
         for (int searchDistance=1 ; searchDistance<=4 ; searchDistance++) {
            for (int xOffset = -searchDistance; xOffset <= searchDistance ; xOffset++) {
               for (int yOffset = -(searchDistance * 2); yOffset <= (searchDistance * 2) ; yOffset++) {
                  // don't consider illegal locations:
                  if ((xOffset%2) == (yOffset%2)) {
                     // don't consider the location we have already checked:
                     if ((Math.abs(xOffset) < searchDistance) && (Math.abs(yOffset) < searchDistance)) {
                        continue;
                     }
                     for (ArenaCoordinates location : eventLocations) {
                        if (arena.addCombatant(newChar, team, (short)(location.x + xOffset), (short)(location.y + yOffset), aiEngineType)) {
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
      else if (eventType.equals(EVENT_TYPE_TELEPORT)) {
         int locIndex = 0;
         CombatMap map = arena.getCombatMap();
         while (locIndex < eventLocations.size()) {
            ArenaLocation eventLoc = map.getLocation(eventLocations.get(locIndex));
            if (eventLoc.getCharacters().size() == 0) {
               if (triggeringCharacter.setHeadLocation(eventLoc, triggeringCharacter.getFacing(), map, null/*diag*/)) {
                  break;
               }
            }
            locIndex++;
         }
      }
      else if (eventType.equals(EVENT_TYPE_DISABLE_THIS_TRIGGER)){
         CombatMap map = arena.getCombatMap();
         for (ArenaTrigger trigger : map.getTriggers()) {
            if (trigger.getEvents().contains(this)) {
               trigger.setEnabled(false);
            }
         }
      }
      else if ((eventType.equals(EVENT_TYPE_ENABLE_TRIGGER)) ||
               (eventType.equals(EVENT_TYPE_DISABLE_TRIGGER))){
         boolean enable = eventType.equals(EVENT_TYPE_ENABLE_TRIGGER);
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
      else if (eventType.equals(EVENT_TYPE_NEW_MAP)) {
         // save the current map
         CombatMap map = arena.getCombatMap();
         // if we come back to this map, make sure we don't re-create the original players
         map.clearCharacterStartingLocations();
         final List<Character> team = new ArrayList<>();
         final byte teamID = triggeringCharacter.teamID;
         for (Character combatant : arena.getCombatants()) {
            if (combatant.teamID == teamID) {
               if (combatant.stillFighting()) {
                  team.add(combatant);
                  // remove the characters that are leaving this map, so they aren't there when they might return.
                  map.removeCharacter(combatant);
               }
            }
         }
         // keep this map for later, in case we come back.
         savedMaps.put(map.getName(), map.clone());
         map.removeAllCombatants();
         if (!CombatServer._this.getShell().isDisposed()) {
            Display display = CombatServer._this.getShell().getDisplay();
            if (!display.isDisposed()) {
               // This object allows us to wait for the main thread to finish its processing of the new map before we return
               final Object waitObj = new Object();
               synchronized(waitObj) {
                  // Since CombatServer.setMap(...) require UI-thread access permissions,
                  // we have to defer this to the UI thread
                  display.asyncExec(() -> {
                     arena.removeAllCombatants();
                     CombatMap newMap = savedMaps.get(eventData);
                     if (newMap == null) {
                        CombatServer._this.setMap(eventData);
                     }
                     else {
                        arena.setCombatMap(newMap, true/*clearCombatants*/);
                     }
                     arena.addStockCombatants();
                     List<ArenaCoordinates> locations = new ArrayList<>();
                     for (Character teamMember : team) {
                        // make sure we never run out of locations, even if we re-use them twice.
                        if (locations.isEmpty()) {
                           locations.addAll(eventLocations);
                        }

                        ArenaCoordinates loc = locations.remove(0);
                        arena.addCombatant(teamMember, teamID, loc.x, loc.y, teamMember.getAIType());
                     }
                     arena.terminateBattle();
                     arena.beginBattle();
                     synchronized(waitObj) {
                        // Let the non-UI thread know we are done, so it can resume.
                        waitObj.notifyAll();
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
//      else if (eventType.equals(EVENT_TYPE_TRAP)) {
//      }
      return false;
   }

   public boolean isExitEvent() {
      return eventType.equals(EVENT_TYPE_NEW_MAP);
   }

   public boolean equals(ArenaEvent other) {
      if (!eventName.equals(other.eventName)) {
         return false;
      }
      if (!eventType.equals(other.eventType)) {
         return false;
      }
      if (!eventData.equals(other.eventData)) {
         return false;
      }
      if ((eventLocations != null) && (other.eventLocations != null)) {
         if (eventLocations.size() != other.eventLocations.size()) {
            return false;
         }
         for (int i = 0; i < eventLocations.size(); i++) {
            if (!eventLocations.get(i).sameCoordinates(other.eventLocations.get(i))) {
               return false;
            }
         }
      }
      else return (eventLocations == null) && (other.eventLocations == null);
      return true;
   }

   @Override
   public ArenaEvent clone() {
      try {
         ArenaEvent dup = (ArenaEvent) super.clone();
         if (eventLocations != null) {
            dup.eventLocations = new ArrayList<>();
            dup.eventLocations.addAll(eventLocations);
         }
         return dup;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }

   public Collection<ArenaCoordinates> getLocations() {
      return eventLocations;
   }
}
