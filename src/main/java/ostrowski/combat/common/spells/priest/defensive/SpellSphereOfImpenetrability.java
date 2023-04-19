/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;

public class SpellSphereOfImpenetrability extends ExpiringPriestSpell
{
   public static final String NAME = "Sphere of Impenetrability";
   public SpellSphereOfImpenetrability() {
      this(null, 0);
   }
   public SpellSphereOfImpenetrability(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)25/*baseExpirationTimeInTurns*/, (short)5 /*bonusTimeInTurnsPerPower*/,
            group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " is surrounded by a sphere of impenetrability, through which nothing may pass weapons.";
      }
      return " (surrounded by a sphere of impenetrability)";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell creates a sphere around the caster through which nothing may pass in or out."+
             " The power of the spell determines the radius of the sphere.";
   }

   @Override
   public byte getPassiveDefenseModifier(boolean vsRangedWeapons, DamageType vsDamageType) {
      if (!isExpired()) {
         return 127;
      }
      return 0;
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_SELF;
   }
}
