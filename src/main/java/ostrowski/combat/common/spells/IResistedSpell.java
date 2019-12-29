package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Enums.TargetType;

public interface IResistedSpell
{
   short getMaxRange(Character caster);
   boolean isDefendable();
   byte getResistanceAttribute(Character target);
   byte getResistanceActions();
   DiceSet getResistanceDice(Character target);
   TargetType getTargetType();
   String getResistanceAttributeName();
   byte getRangeTNAdjustment(short distanceInHexes);
}
