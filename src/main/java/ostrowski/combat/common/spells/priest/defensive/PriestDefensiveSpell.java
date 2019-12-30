package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

import java.util.ArrayList;
import java.util.List;

public class PriestDefensiveSpell implements IPriestGroup
{
   static public final List<PriestSpell> _spells = new ArrayList<>();
   static {
      _spells.add(new SpellArmor(PriestDefensiveSpell.class, 1));
      _spells.add(new SpellBlockAttack(PriestDefensiveSpell.class, 2));
      _spells.add(new SpellPush(PriestDefensiveSpell.class, 2));
      _spells.add(new SpellMagicShield(PriestDefensiveSpell.class, 3));
      _spells.add(new SpellBlockSpell(PriestDefensiveSpell.class, 4));
      _spells.add(new SpellImprovedArmor(PriestDefensiveSpell.class, 4));
      _spells.add(new SpellMissileShield(PriestDefensiveSpell.class, 5));
      _spells.add(new SpellReverseMissile(PriestDefensiveSpell.class, 6));
      _spells.add(new SpellSphereOfImpenetrability(PriestDefensiveSpell.class, 7));
   }
}
