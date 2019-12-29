package ostrowski.combat.common.spells.priest.nature;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.nature.animal.*;
import ostrowski.combat.common.spells.priest.nature.plant.SpellEntangle;
import ostrowski.combat.common.spells.priest.nature.plant.SpellGrowPlant;
import ostrowski.combat.common.spells.priest.nature.weather.*;

import java.util.ArrayList;

public class PriestNatureSpell implements IPriestGroup
{
   static public final ArrayList<PriestSpell> _spells = new ArrayList<>();
   static {
      _spells.add(new SpellSeekElement(PriestNatureSpell.class, 1));
      _spells.add(new SpellDetectLife(PriestNatureSpell.class, 1));
      _spells.add(new SpellGrowPlant(PriestNatureSpell.class, 2));
      _spells.add(new SpellEntangle(PriestNatureSpell.class, 3));
      _spells.add(new SpellSpeakWithSpirits(PriestNatureSpell.class, 4));
      _spells.add(new SpellAnimalForm(PriestNatureSpell.class, 5));
      _spells.add(new SpellGaleWinds(PriestNatureSpell.class, 5));
      _spells.add(new SpellSummonStorm(PriestNatureSpell.class, 6));
      _spells.add(new SpellSummonSwarm(PriestNatureSpell.class, 6));
      _spells.add(new SpellFireStorm(PriestNatureSpell.class, 7));
      _spells.add(new SpellIceStorm(PriestNatureSpell.class, 8));
      _spells.add(new SpellHurricane(PriestNatureSpell.class, 9));
      _spells.add(new SpellDragonForm(PriestNatureSpell.class, 9));
      _spells.add(new SpellReverseTime(PriestNatureSpell.class, 10));
      _spells.add(new SpellPlague(PriestNatureSpell.class, 10));
   }
}
