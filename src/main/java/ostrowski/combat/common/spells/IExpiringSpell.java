package ostrowski.combat.common.spells;

public interface IExpiringSpell
{
   public short getDuration();
   public boolean isExpired();
   public short getBaseExpirationTimeInTurns();
   public short getBonusTimeInTurnsPerPower();
   public String describeActiveSpell();
}
