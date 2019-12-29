package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums.RANGE;

public interface IRangedSpell
{
   short getMaxRange(Character caster);
   short getMinRange(Character caster);
   RANGE getRange(short distanceInHexes);
   byte getRangeDefenseAdjustmentToPD(short distanceInHexes);
   byte getRangeDefenseAdjustmentPerAction(short distanceInHexes);
   byte getRangeTNAdjustment(short distanceInHexes);
   short getRangeBase();
}
