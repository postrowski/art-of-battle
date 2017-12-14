package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellWallOfWind extends ExpiringPriestSpell
{
   public static final String NAME = "Wall of Wind";

   public SpellWallOfWind() {
      this(null, 0);
   }

   public SpellWallOfWind(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }
   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes a wall of wind to fill several hexes." +
      		" The wind is strong enough to deflect missile weapons, giving a PD bonus to any target that is on the other side of the wall of wind." +
      		" At one power point, the spell covers 3 hexes (usually the three hexes in front of the caster), and increases PD of any target by 3." +
      		" Each additional point of power doubles the size of the affected area." +
      		" Additional power points can also be spent to further increase the PD bonus, each point of power doubling the PD provided." +
      		" So a 4-power spell could cover 6 hexes with a PD bonus of 12, or it could cover 24 hexes with a PD bonus of 3." +
      		" Due to their speed, bullets are less affected by this spell, offering 1/3 the PD bonus afforded to other missile weapons." +
      		" The caster of the spell is able to move the wall of wind 1 hex in any direction on any round, by spending one action.";
   }

   @Override
   public byte getPassiveDefenseModifier(boolean vsRangedWeapons, DamageType vsDamageType) {
      if (vsRangedWeapons) {
         if (!isExpired()) {
            return (byte) (getPower() * 2);
         }
      }
      return 0;
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
