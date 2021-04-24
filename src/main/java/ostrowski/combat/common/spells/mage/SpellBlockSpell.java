/*
 * Created on May 25, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellBlockSpell extends InstantaneousMageSpell
{
   public static final String NAME = "Block Spell";
   public SpellBlockSpell() {
      super(NAME, new Class[] {SpellCreateForce.class}, new SkillType[] {SkillType.Spellcasting_Energy, SkillType.Spellcasting_Protection});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell is used defensively when attacked by an enemy with a spell." +
             " This spell increases the caster's resistance by an amount equal to the caster's base skill level with the spell" +
             " (not adjusted by IQ), times the number of actions spent on the spell. To be effective in combat, this" +
             " spell must be memorized.";
   }

   @Override
   public byte getActiveDefensiveTN(byte actionsSpent, Character caster) {
      return (byte) (actionsSpent * caster.getSpellSkill(getName()));
   }
   @Override
   public boolean canDefendAgainstSpells() {
      return true;
   }
}
