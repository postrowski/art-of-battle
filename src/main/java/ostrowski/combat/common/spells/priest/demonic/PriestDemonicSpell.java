package ostrowski.combat.common.spells.priest.demonic;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.defensive.SpellArmor;
import ostrowski.combat.common.spells.priest.defensive.SpellBlockAttack;
import ostrowski.combat.common.spells.priest.defensive.SpellBlockSpell;
import ostrowski.combat.common.spells.priest.defensive.SpellImprovedArmor;
import ostrowski.combat.common.spells.priest.evil.PriestEvilSpell;
import ostrowski.combat.common.spells.priest.evil.SpellOpenGateToHell;
import ostrowski.combat.common.spells.priest.offensive.SpellSpeed;

import java.util.ArrayList;
import java.util.List;

public class PriestDemonicSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      // All Demons can have these:
      spells.add(new SpellArmor(PriestDemonicSpell.class, 2));            // defensive
      spells.add(new SpellSummonHellHounds(PriestDemonicSpell.class, 3)); // evil(6)
      
      // Demons & Major Demons can have these:
      spells.add(new SpellBlockSpell(PriestDemonicSpell.class, 4));       // defensive
      spells.add(new SpellBlockAttack(PriestDemonicSpell.class, 4));      // defensive
      spells.add(new SpellSummonMinorDemon(PriestDemonicSpell.class, 5));
      spells.add(new SpellImprovedArmor(PriestDemonicSpell.class, 6));    // defensive
      
      // Only Major Demons can have these:
      spells.add(new SpellSpeed(PriestDemonicSpell.class, 7));            // offensive
      spells.add(new SpellSummonDemon(PriestDemonicSpell.class, 8));
      spells.add(new SpellOpenGateToHell(PriestEvilSpell.class, 9));      // evil(10)

      // Not even Major Demons get these:
      spells.add(new SpellSummonMajorDemon(PriestDemonicSpell.class, 10));
   }
}
