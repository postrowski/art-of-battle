package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.nature.SpellSeekElement;
import ostrowski.combat.common.spells.priest.nature.weather.SpellHurricane;

import java.util.ArrayList;
import java.util.List;

public class PriestElementalSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellSeekElement(PriestElementalSpell.class, 1)); // find nearest water, air, earth or fire. power determines range & sensitivity.
      spells.add(new SpellCreateElement(PriestElementalSpell.class, 2)); // create water, air, earth or fire, volume determined by power
      spells.add(new SpellControlElement(PriestElementalSpell.class, 3)); // move water, air, earth or fire. Speed & volume determined by power
      spells.add(new SpellSilence(PriestElementalSpell.class, 3));    // stop all sounds from passing a barrier
      spells.add(new SpellDig(PriestElementalSpell.class, 4)); // dig a hole through dirt, volume depends on power & dirt density.
      spells.add(new SpellSwim(PriestElementalSpell.class, 4)); // move quickly through water
      spells.add(new SpellSonicBlast(PriestElementalSpell.class, 4)); // deafens and pushes back anyone nearby
      spells.add(new SpellWaterJet(PriestElementalSpell.class, 5));  // similar to the mage spell
      spells.add(new SpellBreatheWater(PriestElementalSpell.class, 6)); // no need for air while swimming.
      spells.add(new SpellWallOfWind(PriestElementalSpell.class, 6)); // increase PD against missile weapons
      spells.add(new SpellBreatheFire(PriestElementalSpell.class, 6));  // similar to the mage spell FlameJet
      spells.add(new SpellWallOfFire(PriestElementalSpell.class, 7)); // obscures vision, and causes damage to anyone that crosses through
      spells.add(new SpellWalkThroughWalls(PriestElementalSpell.class, 7));
      spells.add(new SpellEarthquake(PriestElementalSpell.class, 9)); // topples buildings or rock formations
      spells.add(new SpellHurricane(PriestElementalSpell.class, 10));
   }
}
