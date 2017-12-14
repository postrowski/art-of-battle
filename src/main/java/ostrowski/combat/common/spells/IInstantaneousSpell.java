package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;

public interface IInstantaneousSpell
{
   public byte getIncantationTime();
   public boolean canDefendAgainstMeleeAttacks();
   public boolean canDefendAgainstRangedAttacks();
   public boolean canDefendAgainstSpells();
   public String getName();
   public byte getLevel();
   public byte getActiveDefensiveTN(byte actions, Character caster);
}
