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
      public final byte      _team;
      public final String    _name;
      public final Character _character;
      public final byte      _teamPosition;
      public final boolean   _available;

      public TeamMember(byte team, String name, Character character, byte teamPosition, boolean available) {
         _team = team;
         if ((name == null) || name.trim().isEmpty()) {
            _name = Enums.TEAM_NAMES[team] + (teamPosition+1);
            //_name = "Any";
         }
         else {
            _name = name;
         }
         _character = character;
         _teamPosition = teamPosition;
         _available = available;
      }

      @Override
      public TeamMember clone() {
         return new TeamMember(_team, _name, ((_character == null) ? null : _character.clone()), _teamPosition, _available);
      }
   }

   private final Map<Byte, List<TeamMember>> _mapTeamToListTeamMembers = new HashMap<>();
   private Character _newCharacter;

   public RequestArenaEntrance() {
      super();
      _message = "Please select your character.";
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
         Set<Entry<Byte, List<TeamMember>>> entrySet = _mapTeamToListTeamMembers.entrySet();
         writeToStream(entrySet.size(), out);
         for (Map.Entry<Byte, List<TeamMember>> e : entrySet) {
            writeToStream(e.getKey().byteValue(), out);
            List<TeamMember> listCharacterNamesAndAvailability = e.getValue();
            writeToStream(listCharacterNamesAndAvailability.size(), out);
            for (TeamMember teamMember : listCharacterNamesAndAvailability) {
               writeToStream(teamMember._name,         out);
               writeToStream((teamMember._character != null), out);
               if (teamMember._character != null) {
                  teamMember._character.serializeToStream(out);
               }
               writeToStream(teamMember._teamPosition, out);
               writeToStream(teamMember._available,    out);
            }
         }
         writeToStream((_newCharacter != null), out);
         if (_newCharacter != null) {
            _newCharacter.serializeToStream(out);
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
            _mapTeamToListTeamMembers.put(team, teamMembers);
         }
         if (readBoolean(in)) {
            _newCharacter = (Character) SerializableFactory.readObject(readString(in), in);
         }
         else {
            _newCharacter = null;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestArenaEntrance) {
            RequestArenaEntrance newReq = ((RequestArenaEntrance)newObj);
            for (Map.Entry<Byte, List<TeamMember>> team : _mapTeamToListTeamMembers.entrySet()) {
               List<TeamMember> newTeam = new ArrayList<>();
               for (TeamMember teamMember : team.getValue()) {
                  newTeam.add(teamMember.clone());
               }
               newReq._mapTeamToListTeamMembers.put(team.getKey(), newTeam);
            }
            newReq._newCharacter = _newCharacter.clone();
         }
      }
   }
   private void addCharacterByTeam(Byte team, TeamMember teamMember) {
      List<TeamMember> teamMembers = _mapTeamToListTeamMembers.computeIfAbsent(team, k -> new ArrayList<>());
      teamMembers.add(teamMember.clone());
   }
   public void rebuildQuestion() {
      _options.clear();
      _answer = null;
      boolean separatorNeededNextTeam = false;
      int i = 0;
      for (Map.Entry<Byte, List<TeamMember>> team : _mapTeamToListTeamMembers.entrySet()) {
         if (separatorNeededNextTeam) {
            addSeparatorOption();
         }
         for (TeamMember teamMember : team.getValue()) {
            addOption(new RequestOption(teamMember._name, i++, teamMember._available));
            separatorNeededNextTeam = true;
         }
      }
   }
   public List<TeamMember> getCharacterNamesByTeam(Byte team) {
      return new ArrayList<>(_mapTeamToListTeamMembers.get(team));
   }
   public Byte getSelectedCharacterTeam() {
      int i = 0;
      for (Map.Entry<Byte, List<TeamMember>> team : _mapTeamToListTeamMembers.entrySet()) {
         for (TeamMember teamMember : team.getValue()) {
            if (i++ == _answer.getIntValue()) {
               return team.getKey();
            }
         }
      }
      return null;
   }
   public String getSelectedCharacterName() {
      TeamMember teamMember = getSelectedTeamMember();
      return (teamMember == null) ? null : teamMember._name;
   }
   public byte getSelectedCharacterTeamPosition() {
      TeamMember teamMember = getSelectedTeamMember();
      return (teamMember == null) ? -1 : teamMember._teamPosition;
   }
   public Character getSelectedCharacter() {
      TeamMember teamMember = getSelectedTeamMember();
      return (teamMember == null) ? null : teamMember._character;
   }
   private TeamMember getSelectedTeamMember() {
      if (_answer != null) {
         int i = 0;
         for (Map.Entry<Byte, List<TeamMember>> team : _mapTeamToListTeamMembers.entrySet()) {
            for (TeamMember teamMember : team.getValue()) {
               if (i++ == _answer.getIntValue()) {
                  return teamMember;
               }
            }
         }
      }
      return null;
   }
   public Character getSelectedNewCharacter() {
      return _newCharacter;
   }
   public void setSelectedCharacter(Character character) {
      _newCharacter = character;
   }
}
