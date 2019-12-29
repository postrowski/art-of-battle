/*
 * Created on Mar 4, 2007
 *
 */
package ostrowski.combat.protocol.request;

import ostrowski.combat.common.Character;
import ostrowski.combat.server.Arena;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;

import java.util.List;

public class RequestSpellTypeSelection extends SyncRequest
{
   public static final String                SPELL_TYPE_MAGE        = "Mage spells";
   public              RequestSpellSelection _spellSelectionRequest = null;

   public RequestSpellTypeSelection() {}
   public RequestSpellTypeSelection(boolean mage, List<String> priestAffinities, Character caster)
   {
      StringBuilder message = new StringBuilder();
      message.append(caster.getName()).append(", select which type of spell you would like to cast.");
      message.append(" You have ");
      int magePoints = caster.getCondition().getMageSpellPointsAvailable();
      int priestPoints = caster.getCondition().getPriestSpellPointsAvailable();
      if (magePoints > 0) {
         message.append(magePoints).append(" mage points available");
         if (priestPoints > 0) {
            message.append(" and");
         }
      }
      if (priestPoints > 0) {
         message.append(priestPoints).append(" priest points available");
      }
      message.append(", select which type of spell you would like to cast.");
      setMessage(message.toString());

      if (mage) {
         addOption(new RequestOption(SPELL_TYPE_MAGE, 0, mage/*enabled*/));
      }
      for (int index=0 ; index<priestAffinities.size() ; index++) {
         addOption(new RequestOption(priestAffinities.get(index), index+1, true/*enabled*/));
      }
   }

   @Override
   public void init() {
      super.init();
      _spellSelectionRequest = null;
   }

   public SyncRequest getNextQuestion(Character actor, List<Character> combatants, Arena arena) {
      if (_spellSelectionRequest == null) {
         _spellSelectionRequest = actor.getSpellSelectionRequest(getAnswer());
      }
      return _spellSelectionRequest;
   }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      if (optionID == 0) {
         return "mM";
      }
      for (ostrowski.protocol.IRequestOption option : _options) {
         if (option.getIntValue() == optionID) {
            String name = option.getName();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
               char ch = name.charAt(i);
               sb.append(java.lang.Character.toLowerCase(ch));
               sb.append(java.lang.Character.toUpperCase(ch));
            }
            return sb.toString();
         }
      }
      return "0123456789";
   }

}
