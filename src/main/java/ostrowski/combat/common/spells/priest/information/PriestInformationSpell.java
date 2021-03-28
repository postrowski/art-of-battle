package ostrowski.combat.common.spells.priest.information;

import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.evil.SpellDetectGood;
import ostrowski.combat.common.spells.priest.good.SpellDetectEvil;
import ostrowski.combat.common.spells.priest.good.SpellDetectLies;
import ostrowski.combat.common.spells.priest.healing.SpellDetectPoison;
import ostrowski.combat.common.spells.priest.nature.SpellSeekElement;
import ostrowski.combat.common.spells.priest.nature.animal.SpellDetectLife;

import java.util.ArrayList;
import java.util.List;

public class PriestInformationSpell implements IPriestGroup
{
   static public final List<PriestSpell> spells = new ArrayList<>();
   static {
      spells.add(new SpellDetectGood(PriestInformationSpell.class, 1));
      spells.add(new SpellDetectEvil(PriestInformationSpell.class, 1));
      spells.add(new SpellDetectLife(PriestInformationSpell.class, 1));
      spells.add(new SpellSeekElement(PriestInformationSpell.class, 1));
      spells.add(new SpellDetectPoison(PriestInformationSpell.class, 2));
      spells.add(new SpellDetectLies(PriestInformationSpell.class, 2));
      spells.add(new SpellDetectDanger(PriestInformationSpell.class, 3));
      spells.add(new SpellDeterminePath(PriestInformationSpell.class, 4));
      spells.add(new SpellLocatePerson(PriestInformationSpell.class, 5));
      spells.add(new SpellLocateObject(PriestInformationSpell.class, 6));
      spells.add(new SpellRemoteConversation(PriestInformationSpell.class, 7));
      spells.add(new SpellRemoteVision(PriestInformationSpell.class, 8));
      spells.add(new SpellTeleportation(PriestInformationSpell.class, 9));
      spells.add(new SpellOpenPortal(PriestInformationSpell.class, 10));
   }
}
