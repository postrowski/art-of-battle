package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

import java.util.ArrayList;
import java.util.List;

public class PriestHealingSpell implements IPriestGroup
{
   static public final List<PriestSpell> _spells = new ArrayList<>();
   static {
      _spells.add(new SpellDiagnose(PriestHealingSpell.class, 1));
      _spells.add(new SpellDetectPoison(PriestHealingSpell.class, 1));
      _spells.add(new SpellReducePain(PriestHealingSpell.class, 2));
      _spells.add(new SpellCureDisease(PriestHealingSpell.class, 2));
      _spells.add(new SpellCureWound(PriestHealingSpell.class, 3));
      _spells.add(new SpellRevive(PriestHealingSpell.class, 4));
      _spells.add(new SpellCureSeriousWound(PriestHealingSpell.class, 5));
      _spells.add(new SpellStasis(PriestHealingSpell.class, 6));
      _spells.add(new SpellHeal(PriestHealingSpell.class, 7));
      _spells.add(new SpellRegeneration(PriestHealingSpell.class, 8));
      _spells.add(new SpellResurrection(PriestHealingSpell.class, 9));
      _spells.add(new SpellReverseAging(PriestHealingSpell.class, 10));
   }
}
