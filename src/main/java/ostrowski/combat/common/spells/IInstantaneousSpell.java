package ostrowski.combat.common.spells;

import ostrowski.combat.common.Character;

public interface IInstantaneousSpell
{
   boolean canDefendAgainstMeleeAttacks();
   boolean canDefendAgainstRangedAttacks();
   boolean canDefendAgainstSpells();
   String getName();
   byte getLevel();
   byte getActiveDefensiveTN(byte actions, Character caster);
}
