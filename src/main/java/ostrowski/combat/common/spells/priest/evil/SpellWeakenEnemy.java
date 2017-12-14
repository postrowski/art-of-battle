package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.server.Arena;

public class SpellWeakenEnemy extends ResistedPriestSpell
{
   public static final String NAME = "Weaken Enemy";
   public SpellWeakenEnemy() {
   }
   public SpellWeakenEnemy(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Health, (byte)2/*resistedActions*/, true /*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime)
   {
      if (firstTime) {
         return getTargetName() + "'s STR decreases by " + getStrReduction() + " points.";
      }
      return " (STR decreased by " + getStrReduction() + " points.)";
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell decreases the subject's STR by one point for every two full points of effective power." +
            " This will affect the damage done with weapons, and may put the subject into a higher encumbrance level." +
            " Subjects that use bows made for their original strength, will find that they are no longer strong enough to draw their own bow!";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   private byte getStrReduction() {
      return (byte) (getEffectivePower() / 2);
   }
   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) - getStrReduction()), false/*containInLimits*/);
   }

   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Strength, (byte) (getTarget().getAttributeLevel(Attribute.Strength) + getStrReduction()), false/*containInLimits*/);
   }

}
