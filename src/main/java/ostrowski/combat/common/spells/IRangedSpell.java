package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums.RANGE;

public interface IRangedSpell
{
   public short getMaxRange(Character caster);
   public short getMinRange(Character caster);
   public RANGE getRange(short distanceInHexes);
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes);
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes);
   public byte getRangeTNAdjustment(short distanceInHexes);
   public short getRangeBase();
}
