package ostrowski.combat.common.spells.priest.good;

import java.util.ArrayList;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.healing.SpellResurrection;

public class PriestGoodSpell implements IPriestGroup
{
   static public ArrayList<PriestSpell> _spells = new ArrayList<>();
   static {
      _spells.add(new SpellDetectEvil(PriestGoodSpell.class, 1));
      _spells.add(new SpellLight(PriestGoodSpell.class, 1));
      _spells.add(new SpellDetectLies(PriestGoodSpell.class, 2));
      _spells.add(new SpellBless(PriestGoodSpell.class, 2));
      _spells.add(new SpellPacify(PriestGoodSpell.class, 3));
      _spells.add(new SpellForceTruth(PriestGoodSpell.class, 4));
      _spells.add(new SpellCharmPerson(PriestGoodSpell.class, 4));
      _spells.add(new SpellTurnUndead(PriestGoodSpell.class, 5));
      _spells.add(new SpellCharmMonster(PriestGoodSpell.class, 6));
      _spells.add(new SpellBanishEvil(PriestGoodSpell.class, 7));
      _spells.add(new SpellDestroyEvil(PriestGoodSpell.class, 8));
      _spells.add(new SpellResurrection(PriestGoodSpell.class, 9));
   }
}
