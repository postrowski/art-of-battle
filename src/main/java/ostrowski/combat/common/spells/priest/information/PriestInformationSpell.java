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

public class PriestInformationSpell implements IPriestGroup
{
   static public final ArrayList<PriestSpell> _spells = new ArrayList<>();
   static {
      _spells.add(new SpellDetectGood(PriestInformationSpell.class, 1));
      _spells.add(new SpellDetectEvil(PriestInformationSpell.class, 1));
      _spells.add(new SpellDetectLife(PriestInformationSpell.class, 1));
      _spells.add(new SpellSeekElement(PriestInformationSpell.class, 1));
      _spells.add(new SpellDetectPoison(PriestInformationSpell.class, 2));
      _spells.add(new SpellDetectLies(PriestInformationSpell.class, 2));
      _spells.add(new SpellDetectDanger(PriestInformationSpell.class, 3));
      _spells.add(new SpellDeterminePath(PriestInformationSpell.class, 4));
      _spells.add(new SpellLocatePerson(PriestInformationSpell.class, 5));
      _spells.add(new SpellLocateObject(PriestInformationSpell.class, 6));
      _spells.add(new SpellRemoteConversation(PriestInformationSpell.class, 7));
      _spells.add(new SpellRemoteVision(PriestInformationSpell.class, 8));
      _spells.add(new SpellTeleportation(PriestInformationSpell.class, 9));
      _spells.add(new SpellOpenPortal(PriestInformationSpell.class, 10));
   }
}
