package ostrowski.combat.common.spells.priest.nature;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.nature.animal.*;
import ostrowski.combat.common.spells.priest.nature.plant.SpellEntangle;
import ostrowski.combat.common.spells.priest.nature.plant.SpellGrowPlant;
import ostrowski.combat.common.spells.priest.nature.weather.*;

import java.util.ArrayList;
import java.util.List;

public class PriestNatureSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellSeekElement(PriestNatureSpell.class, 1));
      spells.add(new SpellDetectLife(PriestNatureSpell.class, 1));
      spells.add(new SpellGrowPlant(PriestNatureSpell.class, 2));
      spells.add(new SpellEntangle(PriestNatureSpell.class, 3));
      spells.add(new SpellSpeakWithSpirits(PriestNatureSpell.class, 4));
      spells.add(new SpellAnimalForm(PriestNatureSpell.class, 5));
      spells.add(new SpellGaleWinds(PriestNatureSpell.class, 5));
      spells.add(new SpellSummonStorm(PriestNatureSpell.class, 6));
      spells.add(new SpellSummonSwarm(PriestNatureSpell.class, 6));
      spells.add(new SpellFireStorm(PriestNatureSpell.class, 7));
      spells.add(new SpellIceStorm(PriestNatureSpell.class, 8));
      spells.add(new SpellHurricane(PriestNatureSpell.class, 9));
      spells.add(new SpellDragonForm(PriestNatureSpell.class, 9));
      spells.add(new SpellReverseTime(PriestNatureSpell.class, 10));
      spells.add(new SpellPlague(PriestNatureSpell.class, 10));
   }
}
