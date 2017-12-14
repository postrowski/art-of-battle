package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Enums.TargetType;

public interface IResistedSpell
{
   public short getMaxRange(Character caster);
   public boolean isDefendable();
   public byte getResistanceAttribute(Character target);
   public byte getResistanceActions();
   public DiceSet getResistanceDice(Character target);
   public TargetType getTargetType();
   public String getResistanceAttributeName();
   public byte getRangeTNAdjustment(short distanceInHexes);
}
