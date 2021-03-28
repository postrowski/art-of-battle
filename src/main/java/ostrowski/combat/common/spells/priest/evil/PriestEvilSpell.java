package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.demonic.SpellSummonHellHounds;

import java.util.ArrayList;
import java.util.List;

public class PriestEvilSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellDarkness(PriestEvilSpell.class, 1));
      spells.add(new SpellDetectGood(PriestEvilSpell.class, 1));
      spells.add(new SpellCurse(PriestEvilSpell.class, 2));
      spells.add(new SpellEnrage(PriestEvilSpell.class, 3));

      spells.add(new SpellFear(PriestEvilSpell.class, 4));
      spells.add(new SpellWeakenEnemy(PriestEvilSpell.class, 4));
      spells.add(new SpellParalyze(PriestEvilSpell.class, 5));
      spells.add(new SpellSummonHellHounds(PriestEvilSpell.class, 6));
      spells.add(new SpellCreateZombie(PriestEvilSpell.class, 6));
      
      spells.add(new SpellMassFear(PriestEvilSpell.class, 7));
      spells.add(new SpellBanishGood(PriestEvilSpell.class, 7));
      spells.add(new SpellPossession(PriestEvilSpell.class, 8));
      spells.add(new SpellDestroyGood(PriestEvilSpell.class, 9));
      spells.add(new SpellOpenGateToHell(PriestEvilSpell.class, 10));
   }
}
