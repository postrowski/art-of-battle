package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;

public class SpellTurnUndead extends ResistedPriestSpell implements IRangedSpell
{
   public static final String NAME = "Turn Undead";
   public SpellTurnUndead() {}

   public SpellTurnUndead(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Health, (byte)2/*resistedActions*/, false/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell is a ranged spell that affects all undead creatures in range." +
             " The priest determines a unique resistance TN, based on the effective power at the target’s range, for every undead creature within range." +
             " If an undead creature fails its resistance roll, it is forced to run away from the caster." +
             " If an undead creature fails its resistance roll by 10 points or more, it is destroyed.";
   }
   @Override
   public TargetType getTargetType() {
      // This spell is not cast at a target. However, it only affect the undead creatures around the caster.
      return TargetType.TARGET_NONE;
   }

   @Override
   public short getRangeBase() {
      return 8;
   }
   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return 0;
   }
   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return 0;
   }
}
