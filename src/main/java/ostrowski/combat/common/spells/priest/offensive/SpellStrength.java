/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.server.Arena;

public class SpellStrength extends ExpiringPriestSpell
{
   public static final String NAME = "Strength";
   public SpellStrength() {
      this(null, 0);
   }
   public SpellStrength(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)10 /*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + "'s STR increases by " + getPower() + " points.";
      }
      return " (STR increased by " + getPower() + " points.)";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell increases the subject's STR by an amount equal to the effective power of the spell.";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) + getPower()), false/*containInLimits*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) - getPower()), false/*containInLimits*/);
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }
}
