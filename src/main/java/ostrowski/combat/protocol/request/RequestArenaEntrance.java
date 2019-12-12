/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ostrowski.DebugBreak;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestArenaEntrance extends SyncRequest
{
   private final Map<Byte, List<String>> _mapTeamToListCharacterNames = new HashMap<>();

   public RequestArenaEntrance() {
      _message = "Please select your character.";
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
   public synchronized void copyDataInto(SyncRequest newObj) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestArenaEntrance) {
            RequestArenaEntrance newReq = ((RequestArenaEntrance)newObj);
            for (Map.Entry<Byte, List<String>> team : _mapTeamToListCharacterNames.entrySet()) {
               newReq.setCharacterNamesByTeam(team.getKey(), team.getValue());
            }
         }
      }
   }
   public void setCharacterNamesByTeam(Byte team, List<String> characterNames) {
      List<String> charNames = _mapTeamToListCharacterNames.get(team);
      if (charNames == null) {
         charNames = new ArrayList<>();
         _mapTeamToListCharacterNames.put(team, charNames);
      }
      charNames.addAll(characterNames);
      rebuildQuestion();
   }
   public void rebuildQuestion() {
      boolean separatorNeededNextTeam = false;
      int i = 0;
      for (Map.Entry<Byte, List<String>> team : _mapTeamToListCharacterNames.entrySet()) {
         if (separatorNeededNextTeam) {
            addSeparatorOption();
         }
         for (String charName : team.getValue()) {
            addOption(new RequestOption(charName, i++, true/*enabled*/));
            separatorNeededNextTeam = true;
         }
      }
   }
   public List<String> getCharacterNamesByTeam(Byte team) {
      List<String> characterNames = new ArrayList<>();
      characterNames.addAll(_mapTeamToListCharacterNames.get(team));
      return characterNames;
   }
   public String getSelectedCharacterName() {
      int i = 0;
      for (Map.Entry<Byte, List<String>> team : _mapTeamToListCharacterNames.entrySet()) {
         for (String charName : team.getValue()) {
            if (i++ == _answer.getIntValue()) {
               return charName;
            }
         }
      }
      return null;
   }
   public Byte getSelectedCharacterTeam() {
      int i = 0;
      for (Map.Entry<Byte, List<String>> team : _mapTeamToListCharacterNames.entrySet()) {
         for (String charName : team.getValue()) {
            if (i++ == _answer.getIntValue()) {
               return team.getKey();
            }
         }
      }
      return null;
   }
}
