package ostrowski.combat.common.spells.priest.good;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.healing.SpellResurrection;

import java.util.ArrayList;
import java.util.List;

public class PriestGoodSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellDetectEvil(PriestGoodSpell.class, 1));
      spells.add(new SpellLight(PriestGoodSpell.class, 1));
      spells.add(new SpellDetectLies(PriestGoodSpell.class, 2));
      spells.add(new SpellBless(PriestGoodSpell.class, 2));
      spells.add(new SpellPacify(PriestGoodSpell.class, 3));
      spells.add(new SpellForceTruth(PriestGoodSpell.class, 4));
      spells.add(new SpellCharmPerson(PriestGoodSpell.class, 4));
      spells.add(new SpellTurnUndead(PriestGoodSpell.class, 5));
      spells.add(new SpellCharmMonster(PriestGoodSpell.class, 6));
      spells.add(new SpellBanishEvil(PriestGoodSpell.class, 7));
      spells.add(new SpellDestroyEvil(PriestGoodSpell.class, 8));
      spells.add(new SpellResurrection(PriestGoodSpell.class, 9));
   }
}
