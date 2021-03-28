package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

import java.util.ArrayList;
import java.util.List;

public class PriestHealingSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellDiagnose(PriestHealingSpell.class, 1));
      spells.add(new SpellDetectPoison(PriestHealingSpell.class, 1));
      spells.add(new SpellReducePain(PriestHealingSpell.class, 2));
      spells.add(new SpellCureDisease(PriestHealingSpell.class, 2));
      spells.add(new SpellCureWound(PriestHealingSpell.class, 3));
      spells.add(new SpellRevive(PriestHealingSpell.class, 4));
      spells.add(new SpellCureSeriousWound(PriestHealingSpell.class, 5));
      spells.add(new SpellStasis(PriestHealingSpell.class, 6));
      spells.add(new SpellHeal(PriestHealingSpell.class, 7));
      spells.add(new SpellRegeneration(PriestHealingSpell.class, 8));
      spells.add(new SpellResurrection(PriestHealingSpell.class, 9));
      spells.add(new SpellReverseAging(PriestHealingSpell.class, 10));
   }
}
