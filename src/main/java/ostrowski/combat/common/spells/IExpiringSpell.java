package ostrowski.combat.common.spells;

public interface IExpiringSpell
{
   short getDuration();
   short getBaseExpirationTimeInTurns();
   short getBonusTimeInTurnsPerPower();
   String describeActiveSpell();
}
