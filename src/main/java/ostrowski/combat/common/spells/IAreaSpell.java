package ostrowski.combat.common.spells;

import ostrowski.combat.common.enums.Enums.TargetType;
import ostrowski.combat.server.ArenaLocation;

public interface IAreaSpell
{
   public void setTargetLocation(ArenaLocation targetLocation);
   public ArenaLocation getTargetLocation();
   public TargetType getTargetType();
   public short getMaxRange();
   public short getMinRange();
}
