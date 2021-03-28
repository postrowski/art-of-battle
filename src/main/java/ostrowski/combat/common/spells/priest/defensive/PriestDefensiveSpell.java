package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

import java.util.ArrayList;
import java.util.List;

public class PriestDefensiveSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellArmor(PriestDefensiveSpell.class, 1));
      spells.add(new SpellBlockAttack(PriestDefensiveSpell.class, 2));
      spells.add(new SpellPush(PriestDefensiveSpell.class, 2));
      spells.add(new SpellMagicShield(PriestDefensiveSpell.class, 3));
      spells.add(new SpellBlockSpell(PriestDefensiveSpell.class, 4));
      spells.add(new SpellImprovedArmor(PriestDefensiveSpell.class, 4));
      spells.add(new SpellMissileShield(PriestDefensiveSpell.class, 5));
      spells.add(new SpellReverseMissile(PriestDefensiveSpell.class, 6));
      spells.add(new SpellSphereOfImpenetrability(PriestDefensiveSpell.class, 7));
   }
}
