/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SerializableFactory;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class RequestArenaEntrance extends SyncRequest
{
   public static class TeamMember implements Cloneable {
      public final byte      team;
      public final String    name;
      public final Character character;
      public final byte      teamPosition;
      public final boolean   available;

      public TeamMember(byte team, String name, Character character, byte teamPosition, boolean available) {
         this.team = team;
         if ((name == null) || name.trim().isEmpty()) {
            this.name = Enums.TEAM_NAMES[team] + (teamPosition + 1);
            //_name = "Any";
         }
         else {
            this.name = name;
         }
         this.character = character;
         this.teamPosition = teamPosition;
         this.available = available;
      }

      @Override
      public TeamMember clone() {
         return new TeamMember(team, name, ((character == null) ? null : character.clone()), teamPosition, available);
      }
   }

   private final Map<Byte, List<TeamMember>> mapTeamToListTeamMembers = new HashMap<>();
   private Character                         newCharacter;

   public RequestArenaEntrance() {
      super();
      message = "Please select your character.";
   }
   public RequestArenaEntrance(Map<Byte, List<TeamMember>> availableCombatantNamesByTeams) {
      this();
      for (Map.Entry<Byte, List<TeamMember>> teamMap : availableCombatantNamesByTeams.entrySet()) {
         for (TeamMember teamMember : teamMap.getValue()) {
            addCharacterByTeam(teamMap.getKey(), teamMember);
         }
      }
      rebuildQuestion();
   }

   @Override
   public boolean isCancelable() {
      return false;
   }
   @Override
   public void addOption(int optionID, String optionStr, boolean enabled)
   {
      DebugBreak.debugBreak();
      throw new NullPointerException();
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         Set<Entry<Byte, List<TeamMember>>> entrySet = mapTeamToListTeamMembers.entrySet();
         writeToStream(entrySet.size(), out);
         for (Map.Entry<Byte, List<TeamMember>> e : entrySet) {
            writeToStream(e.getKey().byteValue(), out);
            List<TeamMember> listCharacterNamesAndAvailability = e.getValue();
            writeToStream(listCharacterNamesAndAvailability.size(), out);
            for (TeamMember teamMember : listCharacterNamesAndAvailability) {
               writeToStream(teamMember.name, out);
               writeToStream((teamMember.character != null), out);
               if (teamMember.character != null) {
                  teamMember.character.serializeToStream(out);
               }
               writeToStream(teamMember.teamPosition, out);
               writeToStream(teamMember.available, out);
            }
         }
         writeToStream((newCharacter != null), out);
         if (newCharacter != null) {
            newCharacter.serializeToStream(out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         int size = readInt(in);
         for (int i=0 ; i<size ; i++) {
            byte team = readByte(in);
            int teamSize = readInt(in);
            List<TeamMember> teamMembers = new ArrayList<>();
            for (int t=0 ; t<teamSize ; t++) {
               String name = readString(in);
               boolean hasChar = readBoolean(in);
               Character chr = null;
               if (hasChar) {
                  chr = new Character();
                  chr.serializeFromStream(in);
               }
               byte teamPosition = readByte(in);
               boolean avail = readBoolean(in);
               teamMembers.add(new TeamMember(team, name, chr, teamPosition, avail));
            }
            mapTeamToListTeamMembers.put(team, teamMembers);
         }
         if (readBoolean(in)) {
            newCharacter = (Character) SerializableFactory.readObject(readString(in), in);
         }
         else {
            newCharacter = null;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestArenaEntrance) {
            RequestArenaEntrance newReq = ((RequestArenaEntrance)newObj);
            for (Map.Entry<Byte, List<TeamMember>> team : mapTeamToListTeamMembers.entrySet()) {
               List<TeamMember> newTeam = new ArrayList<>();
               for (TeamMember teamMember : team.getValue()) {
                  newTeam.add(teamMember.clone());
               }
               newReq.mapTeamToListTeamMembers.put(team.getKey(), newTeam);
            }
            newReq.newCharacter = newCharacter.clone();
         }
      }
   }
   private void addCharacterByTeam(Byte team, TeamMember teamMember) {
      List<TeamMember> teamMembers = mapTeamToListTeamMembers.computeIfAbsent(team, k -> new ArrayList<>());
      teamMembers.add(teamMember.clone());
   }
   public void rebuildQuestion() {
      options.clear();
      answer = null;
      boolean separatorNeededNextTeam = false;
      int i = 0;
      for (Map.Entry<Byte, List<TeamMember>> team : mapTeamToListTeamMembers.entrySet()) {
         if (separatorNeededNextTeam) {
            addSeparatorOption();
         }
         for (TeamMember teamMember : team.getValue()) {
            addOption(new RequestOption(teamMember.name, i++, teamMember.available));
            separatorNeededNextTeam = true;
         }
      }
   }

   public Byte getSelectedCharacterTeam() {
      int i = 0;
      for (Map.Entry<Byte, List<TeamMember>> team : mapTeamToListTeamMembers.entrySet()) {
         for (TeamMember teamMember : team.getValue()) {
            if (i++ == answer.getIntValue()) {
               return team.getKey();
            }
         }
      }
      return null;
   }

   public byte getSelectedCharacterTeamPosition() {
      TeamMember teamMember = getSelectedTeamMember();
      return (teamMember == null) ? -1 : teamMember.teamPosition;
   }
   public Character getSelectedCharacter() {
      TeamMember teamMember = getSelectedTeamMember();
      return (teamMember == null) ? null : teamMember.character;
   }
   private TeamMember getSelectedTeamMember() {
      if (answer != null) {
         int i = 0;
         for (Map.Entry<Byte, List<TeamMember>> team : mapTeamToListTeamMembers.entrySet()) {
            for (TeamMember teamMember : team.getValue()) {
               if (i++ == answer.getIntValue()) {
                  return teamMember;
               }
            }
         }
      }
      return null;
   }

   public void setSelectedCharacter(Character character) {
      newCharacter = character;
   }
}
