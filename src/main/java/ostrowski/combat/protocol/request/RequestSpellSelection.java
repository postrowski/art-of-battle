/*
 * Created on Mar 4, 2007
 *
 */
package ostrowski.combat.protocol.request;

import java.util.List;

import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.protocol.SyncRequest;

public class RequestSpellSelection extends SyncRequest
{
   public RequestSpellSelection() {}

   public RequestSpellSelection(List<? extends Spell> list, ostrowski.combat.common.Character caster)
   {
      StringBuilder message = new StringBuilder();
      message.append(caster.getName());
      message.append(", select which spell you would like to cast.");
      message.append(" You have ");
      if (list.get(0) instanceof MageSpell) {
         message.append(caster.getCondition().getMageSpellPointsAvailable()).append(" mage points available.");
      }
      else {
         message.append(caster.getCondition().getPriestSpellPointsAvailable()).append(" priest points available.");
      }
      setMessage(message.toString());

      boolean newColumn = false;
      for (int index=0 ; index<list.size() ; index++) {
         Spell spell = list.get(index);
         if (spell == null) {
            // add a column break
            if (!newColumn) {
               addSeparatorOption();
               newColumn = true;
            }
         }
         else if (spell.isCastInBattle()) {
            String name;
            byte incantationTime = spell.getIncantationTime();
            if (incantationTime == 0) {
               name = spell.getName() + " (instantaneous)";
            }
            else {
               name = spell.getName() + " (" + incantationTime + " rounds)";
            }
            addOption(index, name, true/*enabled*/);
            newColumn = false;
         }
      }
   }
   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      return "abcdefghijklmnopqrstuvwxyz" +
      		 "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   }

}
