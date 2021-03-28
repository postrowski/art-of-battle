package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

import java.util.ArrayList;
import java.util.List;

public abstract class PriestOffensiveSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellStrength(PriestOffensiveSpell.class, 2));
      spells.add(new SpellIncreaseDamage(PriestOffensiveSpell.class, 4));
      spells.add(new SpellCallLightning(PriestOffensiveSpell.class, 4));
      spells.add(new SpellDexterity(PriestOffensiveSpell.class, 5));
      spells.add(new SpellSleep(PriestOffensiveSpell.class, 5));
      spells.add(new SpellSpeed(PriestOffensiveSpell.class, 6));
      spells.add(new SpellSummonWarrior(PriestOffensiveSpell.class, 6));
      spells.add(new SpellSummonChampion(PriestOffensiveSpell.class, 9));
      spells.add(new SpellSummonDeity(PriestOffensiveSpell.class, 10));
   }
}
