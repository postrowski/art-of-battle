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
   private String                 name;
   private List<ArenaCoordinates> triggerCoordinates = new ArrayList<>();
   private boolean                onlyAffectsPlayers = true;
   private boolean                requiresEntireTeam = false;
   private boolean                enabled            = true;
   private List<ArenaEvent>       events             = new ArrayList<>();

   public ArenaTrigger(String name) {
      this.name = name;
   }
   public boolean isArmed() {
      return enabled;
   }

   public Element getXmlNode(Document mapDoc, String newLine) {
      Element triggerElement = mapDoc.createElement("trigger");
      triggerElement.setAttribute("name", String.valueOf(name));
      triggerElement.setAttribute("enabled", String.valueOf(enabled));
      triggerElement.setAttribute("requiresEntireTeam", String.valueOf(requiresEntireTeam));
      triggerElement.setAttribute("onlyAffectsPlayers", String.valueOf(onlyAffectsPlayers));
      if ((triggerCoordinates != null) && (triggerCoordinates.size() > 0)) {
         for (ArenaCoordinates coord : triggerCoordinates) {
            if (coord == null) {
               continue;
            }
            Element locationElement = mapDoc.createElement("location");
            locationElement.setAttribute("x", String.valueOf(coord.x));
            locationElement.setAttribute("y", String.valueOf(coord.y));
            triggerElement.appendChild(mapDoc.createTextNode(newLine + "  "));
            triggerElement.appendChild(locationElement);
         }
      }
      for (ArenaEvent event : events) {
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
      trigger.enabled = Boolean.parseBoolean(attributes.getNamedItem("enabled").getNodeValue());
      trigger.requiresEntireTeam = Boolean.parseBoolean(attributes.getNamedItem("requiresEntireTeam").getNodeValue());
      trigger.onlyAffectsPlayers = Boolean.parseBoolean(attributes.getNamedItem("onlyAffectsPlayers").getNodeValue());
      trigger.triggerCoordinates = new ArrayList<>();
      trigger.events = new ArrayList<>();
      NodeList children = node.getChildNodes();
      for (int teamIndex=0 ; teamIndex<children.getLength() ; teamIndex++) {
         Node child = children.item(teamIndex);
         if (child.getNodeName().equals("location")) {
            NamedNodeMap childAttrs = child.getAttributes();
            if (childAttrs != null) {
               try {
                  short locX = Short.parseShort(childAttrs.getNamedItem("x").getNodeValue());
                  short locY = Short.parseShort(childAttrs.getNamedItem("y").getNodeValue());
                  trigger.triggerCoordinates.add(new ArenaCoordinates(locX, locY));
               }
               catch (NumberFormatException e) {
               }
            }
         }
         else if (child.getNodeName().equals("event")) {
            ArenaEvent event = ArenaEvent.getArenaEvent(child);
            if (event != null) {
               trigger.events.add(event);
            }
         }
      }
      return trigger;
   }

   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }

   public void setRequiresEntireTeam(boolean requiresEntireTeam) {
      this.requiresEntireTeam = requiresEntireTeam;
   }
   public boolean getRequiresEntireTeam() {
      return requiresEntireTeam;
   }
   public void setOnlyAffectsPlayers(boolean onlyAffectsPlayers) {
      this.onlyAffectsPlayers = onlyAffectsPlayers;
   }
   public boolean getOnlyAffectsPlayers() {
      return onlyAffectsPlayers;
   }
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
   public boolean getEnabled() {
      return enabled;
   }
   public List<ArenaEvent> getEvents() {
      return events;
   }

   public List<ArenaCoordinates> getTriggerCoordinates() {
      return triggerCoordinates;
   }
   public boolean isTriggerAtLocation(ArenaCoordinates coord, Character mover) {
      if (onlyAffectsPlayers && (mover != null) && (mover.isAIPlayer())) {
         return false;
      }
      return triggerCoordinates.contains(coord);
   }
   public boolean isTriggerAtLocation(ArenaLocation loc, Character mover, CombatMap map) {
      if (isTriggerAtLocation(loc, mover)) {
         if (getRequiresEntireTeam()) {
            for (Character combatant : map.getCombatants()) {
               if (combatant.teamID == mover.teamID) {
                  if (combatant.stillFighting()) {
                     if (!onlyAffectsPlayers || !combatant.isAIPlayer()) {
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
      return triggerCoordinates.add(loc);
   }
   public boolean removeTriggerAtLocation(ArenaCoordinates loc) {
      return triggerCoordinates.remove(loc);
   }

   /**
    * returns true to indicate that this TriggerEvent caused the mover to stop moving.
    * @param triggeringCharacter
    * @return true if this TriggerEvent causes the mover to stop moving.
    */
   public boolean trigger(Character triggeringCharacter) {
      boolean newMapInvoked = false;
      if (isArmed()) {
         for (ArenaEvent event : events) {
            if (event.fireEvent(triggeringCharacter)) {
               newMapInvoked = true;
            }
         }
      }
      return newMapInvoked;
   }
   public boolean hasExitEvent() {
      for (ArenaEvent event : events) {
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
         dup.events = new ArrayList<>();
         dup.triggerCoordinates = new ArrayList<>();

         for (ArenaEvent event : events) {
            dup.events.add(event.clone());
         }
         for (ArenaCoordinates coord : triggerCoordinates) {
            if (coord != null) {
               dup.triggerCoordinates.add(coord.clone());
            }
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
      if (!name.equals(other.name)) {
         return false;
      }
      if (triggerCoordinates.size() != other.triggerCoordinates.size()) {
         return false;
      }
      for (int i = 0; i < triggerCoordinates.size(); i++) {
         if (!triggerCoordinates.get(i).equals(other.triggerCoordinates.get(i))) {
            return false;
         }
      }
      if (onlyAffectsPlayers != other.onlyAffectsPlayers) {
         return false;
      }
      if (requiresEntireTeam != other.requiresEntireTeam) {
         return false;
      }
      if (enabled != other.enabled) {
         return false;
      }
      if (events.size() != other.events.size()) {
         return false;
      }
      for (int i = 0; i < events.size(); i++) {
         if (!events.get(i).equals(other.events.get(i))) {
            return false;
         }
      }
      return true;
   }
}
