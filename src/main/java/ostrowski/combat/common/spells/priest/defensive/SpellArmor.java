/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.server.Arena;

/**
 * This spell increases the target Health attribute by an amount equal to twice the effective power of the spell
 */
public class SpellArmor extends ExpiringPriestSpell implements ICastInBattle
{
   public static final String NAME = "Armor";
   public SpellArmor() {
      this(null, 0);
   }
   public SpellArmor(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + "'s skin becomes like armor (BLD increased by " + getHealthModifier() + ").";
      }
      return "(HT incresed by " + getHealthModifier() + ").";
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell increases the subject's Build characteristic by an amount equal to the effective power of the spell."
               + " This spell may be cast on yourself or others. Multiple castings on the same subject do not accumulate."
               + " The highest power casting (of multiple castings) is the only one that has an effect.";
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Health, (byte) (getTarget().getAttributeLevel(Attribute.Health) + getHealthModifier()), false/*containInLimits*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Health, (byte) (getTarget().getAttributeLevel(Attribute.Health) - getHealthModifier()), false/*containInLimits*/);
   }

   public byte getHealthModifier() {
      return getPower();
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }

   @Override
   public boolean isIncompatibleWith(Spell spell) {
      if (spell.getClass() == SpellImprovedArmor.class) {
         return true;
      }
      return super.isIncompatibleWith(spell);
   }

   @Override
   public boolean takesPrecedenceOver(Spell spell) {
      if (spell.getClass() == SpellImprovedArmor.class) {
         return false;
      }
      return super.takesPrecedenceOver(spell);
   }

}
