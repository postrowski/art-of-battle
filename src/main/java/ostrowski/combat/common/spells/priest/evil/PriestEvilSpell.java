package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.demonic.SpellSummonHellHounds;

import java.util.ArrayList;
import java.util.List;

public class PriestEvilSpell implements IPriestGroup
{
   static public final List<PriestSpell> _spells = new ArrayList<>();
   static {
      _spells.add(new SpellDarkness(PriestEvilSpell.class, 1));
      _spells.add(new SpellDetectGood(PriestEvilSpell.class, 1));
      _spells.add(new SpellCurse(PriestEvilSpell.class, 2));
      _spells.add(new SpellEnrage(PriestEvilSpell.class, 3));

      _spells.add(new SpellFear(PriestEvilSpell.class, 4));
      _spells.add(new SpellWeakenEnemy(PriestEvilSpell.class, 4));
      _spells.add(new SpellParalyze(PriestEvilSpell.class, 5));
      _spells.add(new SpellSummonHellHounds(PriestEvilSpell.class, 6));
      _spells.add(new SpellCreateZombie(PriestEvilSpell.class, 6));
      
      _spells.add(new SpellMassFear(PriestEvilSpell.class, 7));
      _spells.add(new SpellBanishGood(PriestEvilSpell.class, 7));
      _spells.add(new SpellPossession(PriestEvilSpell.class, 8));
      _spells.add(new SpellDestroyGood(PriestEvilSpell.class, 9));
      _spells.add(new SpellOpenGateToHell(PriestEvilSpell.class, 10));
   }
}
