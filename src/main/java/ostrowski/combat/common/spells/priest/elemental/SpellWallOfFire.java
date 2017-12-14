package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;

public class SpellWallOfFire extends ExpiringPriestSpell
{
   public static final String NAME = "Wall of Fire";

   public SpellWallOfFire() {
      this(null, 0);
   }

   public SpellWallOfFire(Class<PriestElementalSpell>group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes a wall of fire to fill several hexes, 6 feet high." +
            " The fire is thigh enough to partially obscure vision of things on the other side." +
            " This obscurity gives a PD bonus of +1 per hex to any target that is on the other side of the fire." +
            " At one power point, the spell covers 3 hexes anywhere the caster chooses." +
            " The closest point away determines the range for effective power determination." +
            " Each additional point of power doubles the size of the affected area." +
            " So a 3-power spell would cover 12 hexes." +
            " Anyone travelling the fire hex will take 1d6 damage." +
            " Anyone passing through multiple hexes, takes 1d6 per hex through which they pass, up to 3d6 maximum." +
            " If anyone is not able to exit the fire by the end of their movement round, they suffer the maximum 3d6 damage total." +
            " Armor protects against this damage, but Racial Build Adjustment does not.";
   }

}
