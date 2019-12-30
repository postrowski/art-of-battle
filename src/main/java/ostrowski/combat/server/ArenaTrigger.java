package ostrowski.combat.server;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;

public class ArenaTrigger implements Cloneable
{
   private String                      _name               = "";
   private List<ArenaCoordinates> _triggerCoordinates = new ArrayList<>();
   private boolean                     _onlyAffectsPlayers = true;
   private boolean                     _requiresEntireTeam = false;
   private boolean                     _enabled            = true;
   private List<ArenaEvent>       _events             = new ArrayList<>();

   public ArenaTrigger(String name) {
      _name = name;
   }
   public boolean isArmed() {
      return _enabled;
   }

   public Element getXmlNode(Document mapDoc, String newLine) {
      Element triggerElement = mapDoc.createElement("trigger");
      triggerElement.setAttribute("name", String.valueOf(_name));
      triggerElement.setAttribute("enabled", String.valueOf(_enabled));
      triggerElement.setAttribute("requiresEntireTeam", String.valueOf(_requiresEntireTeam));
      triggerElement.setAttribute("onlyAffectsPlayers", String.valueOf(_onlyAffectsPlayers));
      if ((_triggerCoordinates != null) && (_triggerCoordinates.size() > 0)) {
         for (ArenaCoordinates coord : _triggerCoordinates) {
            if (coord == null) {
               continue;
            }
            Element locationElement = mapDoc.createElement("location");
            locationElement.setAttribute("x", String.valueOf(coord._x));
            locationElement.setAttribute("y", String.valueOf(coord._y));
            triggerElement.appendChild(mapDoc.createTextNode(newLine + "  "));
            triggerElement.appendChild(locationElement);
         }
      }
      for (ArenaEvent event : _events) {
         Element eventElement = event.getXmlNode(mapDoc, newLine + "  ");
         triggerElement.appendChild(mapDoc.createTextNode(newLine + "  "));
         triggerElement.appendChild(eventElement);
      }
      triggerElement.appendChild(mapDoc.createTextNode(newLine));
      return triggerElement;
   }

   public static ArenaTrigger getArenaTrigger(Node node) {
      if (!node.getNodeName().equals("trigger")) {
         return null;
      }

      NamedNodeMap attributes = node.getAttributes();
      if (attributes == null) {
         return null;
      }

      String name = attributes.getNamedItem("name").getNodeValue();
      ArenaTrigger trigger = new ArenaTrigger(name);
      trigger._enabled = Boolean.parseBoolean(attributes.getNamedItem("enabled").getNodeValue());
      trigger._requiresEntireTeam = Boolean.parseBoolean(attributes.getNamedItem("requiresEntireTeam").getNodeValue());
      trigger._onlyAffectsPlayers = Boolean.parseBoolean(attributes.getNamedItem("onlyAffectsPlayers").getNodeValue());
      trigger._triggerCoordinates = new ArrayList<>();
      trigger._events = new ArrayList<>();
      NodeList children = node.getChildNodes();
      for (int teamIndex=0 ; teamIndex<children.getLength() ; teamIndex++) {
         Node child = children.item(teamIndex);
         if (child.getNodeName().equals("location")) {
            NamedNodeMap childAttrs = child.getAttributes();
            if (childAttrs != null) {
               try {
                  short locX = Short.parseShort(childAttrs.getNamedItem("x").getNodeValue());
                  short locY = Short.parseShort(childAttrs.getNamedItem("y").getNodeValue());
                  trigger._triggerCoordinates.add(new ArenaCoordinates(locX, locY));
               }
               catch (NumberFormatException e) {
               }
            }
         }
         else if (child.getNodeName().equals("event")) {
            ArenaEvent event = ArenaEvent.getArenaEvent(child);
            if (event != null) {
               trigger._events.add(event);
            }
         }
      }
      return trigger;
   }

   public String getName() {
      return _name;
   }
   public void setName(String name) {
      _name = name;
   }

   public void setRequiresEntireTeam(boolean requiresEntireTeam) {
      _requiresEntireTeam = requiresEntireTeam;
   }
   public boolean getRequiresEntireTeam() {
      return _requiresEntireTeam;
   }
   public void setOnlyAffectsPlayers(boolean onlyAffectsPlayers) {
      _onlyAffectsPlayers = onlyAffectsPlayers;
   }
   public boolean getOnlyAffectsPlayers() {
      return _onlyAffectsPlayers;
   }
   public void setEnabled(boolean enabled) {
      _enabled = enabled;
   }
   public boolean getEnabled() {
      return _enabled;
   }
   public List<ArenaEvent> getEvents() {
      return _events;
   }

   public List<ArenaCoordinates> getTriggerCoordinates() {
      return _triggerCoordinates;
   }
   public boolean isTriggerAtLocation(ArenaCoordinates coord, Character mover) {
      if (_onlyAffectsPlayers && (mover != null) && (mover.isAIPlayer())) {
         return false;
      }
      return _triggerCoordinates.contains(coord);
   }
   public boolean isTriggerAtLocation(ArenaLocation loc, Character mover, CombatMap map) {
      if (isTriggerAtLocation(loc, mover)) {
         if (getRequiresEntireTeam()) {
            for (Character combatant : map.getCombatants()) {
               if (combatant._teamID == mover._teamID) {
                  if (combatant.stillFighting()) {
                     if (!_onlyAffectsPlayers || !combatant.isAIPlayer()) {
                        boolean atTriggerLoc = false;
                        for (ArenaCoordinates combatantCoord : combatant.getCoordinates()) {
                           if (isTriggerAtLocation(combatantCoord, combatant)) {
                              atTriggerLoc = true;
                              break;
                           }
                        }
                        if (!atTriggerLoc) {
                           return  false;
                        }
                     }
                  }
               }
            }
         }
         return true;
      }
      return false;
   }
   public boolean addTriggerAtLocation(ArenaLocation loc) {
      return _triggerCoordinates.add(loc);
   }
   public boolean removeTriggerAtLocation(ArenaCoordinates loc) {
      return _triggerCoordinates.remove(loc);
   }

   /**
    * returns true to indicate that this TriggerEvent caused the mover to stop moving.
    * @param triggeringCharacter
    * @return true if this TriggerEvent causes the mover to stop moving.
    */
   public boolean trigger(Character triggeringCharacter) {
      boolean newMapInvoked = false;
      if (isArmed()) {
         for (ArenaEvent event : _events) {
            if (event.fireEvent(triggeringCharacter)) {
               newMapInvoked = true;
            }
         }
      }
      return newMapInvoked;
   }
   public boolean hasExitEvent() {
      for (ArenaEvent event : _events) {
         if (event.isExitEvent()) {
            return true;
         }
      }
      return false;
   }

   @Override
   public ArenaTrigger clone() {
      try {
         ArenaTrigger dup = (ArenaTrigger) super.clone();
         dup._triggerCoordinates = new ArrayList<>();
         for (ArenaCoordinates coord : _triggerCoordinates) {
            if (coord != null) {
               dup._triggerCoordinates.add(coord.clone());
            }
         }
         dup._events = new ArrayList<>();
         for (ArenaEvent event : _events) {
            dup._events.add(event.clone());
         }
         return dup;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
   public boolean equals(ArenaTrigger other) {
      if (other == null) {
         return false;
      }
      if (!_name.equals(other._name)) {
         return false;
      }
      if (_triggerCoordinates.size() != other._triggerCoordinates.size()) {
         return false;
      }
      for (int i=0 ; i<_triggerCoordinates.size(); i++) {
         if (!_triggerCoordinates.get(i).equals(other._triggerCoordinates.get(i))) {
            return false;
         }
      }
      if (_onlyAffectsPlayers != other._onlyAffectsPlayers) {
         return false;
      }
      if (_requiresEntireTeam != other._requiresEntireTeam) {
         return false;
      }
      if (_enabled != other._enabled) {
         return false;
      }
      if (_events.size() != other._events.size()) {
         return false;
      }
      for (int i=0 ; i<_events.size(); i++) {
         if (!_events.get(i).equals(other._events.get(i))) {
            return false;
         }
      }
      return true;
   }
}
