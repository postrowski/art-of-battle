package ostrowski.combat.common.spells;

public interface IExpiringSpell
{
   short getDuration();
   boolean isExpired();
   short getBaseExpirationTimeInTurns();
   short getBonusTimeInTurnsPerPower();
   String describeActiveSpell();
}
