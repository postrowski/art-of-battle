/*
 * Created on May 25, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.InstantaneousPriestSpell;

public class SpellBlockSpell extends InstantaneousPriestSpell
{
   public static final String NAME = "Block Spell";
   public SpellBlockSpell() {}
   public SpellBlockSpell(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell is used defensively when the priest is the target of an unwanted resisted spell." +
             " This spell increases the caster's resistance "+
             "by an amount equal to the caster's divine affinity times the actions spent on the spell." +
             " For each action spent casting this spell, the priest expends one power point against his daily limit." +
             " The number of actions spent may never exceed the priest's Divine Power level." +
             " This spell is effective against priest and mage spells equally.";
   }

   @Override
   public byte getActiveDefensiveTN(byte actionsSpent, Character caster) {
      if ((_caster != null) && (_caster != caster)) {
         DebugBreak.debugBreak();
      }
      _caster = caster;
      return (byte) (getCastingLevel() * actionsSpent);
   }
   @Override
   public boolean canDefendAgainstSpells() {
      return true;
   }
}
