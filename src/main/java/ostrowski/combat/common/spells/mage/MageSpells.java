package ostrowski.combat.common.spells.mage;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.spells.Spell;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class MageSpells {
   static final Collection<MageSpell> _spellsList = new TreeSet<>();
   static {
      _spellsList.add(new SpellAffectArea());
      _spellsList.add(new SpellArmor());
      _spellsList.add(new SpellBlockAttack());
      _spellsList.add(new SpellBlockSpell());
      _spellsList.add(new SpellBlockThought());
      _spellsList.add(new SpellBlur());
      _spellsList.add(new SpellCausePain());
      _spellsList.add(new SpellCauseWound());
      _spellsList.add(new SpellControlCharge());
      _spellsList.add(new SpellControlFire());
      _spellsList.add(new SpellControlLight());
      _spellsList.add(new SpellControlMind());
      _spellsList.add(new SpellControlTemperature());
      _spellsList.add(new SpellControlTime());
      _spellsList.add(new SpellCreateAir());
      _spellsList.add(new SpellCreateDarkness());
      _spellsList.add(new SpellCreateEarth());
      _spellsList.add(new SpellCreateElectricity());
      _spellsList.add(new SpellCreateFire());
      _spellsList.add(new SpellCreateForce());
      _spellsList.add(new SpellCreateLight());
      _spellsList.add(new SpellCreateRope());
      _spellsList.add(new SpellCreateWater());
      _spellsList.add(new SpellDetectDisturbance());
      _spellsList.add(new SpellDetectObject());
      _spellsList.add(new SpellFireball());
      _spellsList.add(new SpellFlameJet());
      _spellsList.add(new SpellFlamingWeapon());
      _spellsList.add(new SpellFlamingMissileWeapon());
      _spellsList.add(new SpellFlight());
      _spellsList.add(new SpellFreezeArea());
      _spellsList.add(new SpellGlue());
      _spellsList.add(new SpellHarden());
      _spellsList.add(new SpellIceDaggers());
      _spellsList.add(new SpellImmobilize());
      _spellsList.add(new SpellLevitate());
      _spellsList.add(new SpellLightning());
      _spellsList.add(new SpellMagicMissile());
      _spellsList.add(new SpellMagicShield());
      _spellsList.add(new SpellMendObjects());
      _spellsList.add(new SpellMissileShield());
      _spellsList.add(new SpellPush());
      _spellsList.add(new SpellResistPain());
      _spellsList.add(new SpellReverseMissile());
      _spellsList.add(new SpellShockingGrasp());
      _spellsList.add(new SpellSlow());
      _spellsList.add(new SpellSpeed());
      _spellsList.add(new SpellSpiderWeb());
      _spellsList.add(new SpellStrength());
      _spellsList.add(new SpellSuggestion());
      _spellsList.add(new SpellThrowSpell());
      _spellsList.add(new SpellTrip());
      _spellsList.add(new SpellWaterJet());
      _spellsList.add(new SpellWallOfFire());
      _spellsList.add(new SpellWeaken());
      _spellsList.add(new SpellWind());
   }

   static {
      // This static initializer is used to verify various things about spells, including:
      //   1) defendableSpell are missileSpells and the SpiderWebSpell
      //   2) Each spell that requires another spell, also requires the colleges used by the other spell(s)
      //   3) Each spell that requires another spell, also requires the spells required by the first spell
      StringBuilder problems = new StringBuilder();
      for (MageSpell spell : _spellsList) {
         // If this assumption isn't true, then the code in Spell.getEffectiveness needs to change:
         if (spell.isDefendable() != ((spell instanceof MissileMageSpell) || (spell instanceof SpellSpiderWeb))) {
            problems.append(spell.getName()).append(".isDefendable != (Missile || SpiderWeb)");
         }

         StringBuilder misssingCollegeProblems = new StringBuilder();
         StringBuilder misssingSpellProblems = new StringBuilder();
         for (Class<MageSpell> prerequisiteSpellClass : spell._prerequisiteSpells) {
            try {
               MageSpell prerequisiteSpell = prerequisiteSpellClass.getDeclaredConstructor().newInstance();
               //  1) Each spell that requires another spell, also requires the colleges used by the other spell(s)
               for (MageCollege prerequisiteSpellsColleges : prerequisiteSpell._prerequisiteColleges) {
                  boolean collegeFound = false;
                  for (MageCollege myColleges : spell._prerequisiteColleges) {
                     if (myColleges.getName().equals(prerequisiteSpellsColleges.getName())) {
                        collegeFound = true;
                        break;
                     }
                  }
                  if (!collegeFound) {
                     if (misssingCollegeProblems.length() == 0) {
                        misssingCollegeProblems.append("spell ").append(spell.getName()).append(" requires these Colleges: ");
                     }
                     else {
                        misssingCollegeProblems.append(", ");
                     }
                     misssingCollegeProblems.append(prerequisiteSpellsColleges.getName());
                  }
               }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
               e.printStackTrace();
               DebugBreak.debugBreak();
            }
         }
         // 2) Each spell that requires another spell, also requires the spells required by the first spell
         for (Class<MageSpell> prerequisiteSpellClassI : spell._prerequisiteSpells) {
            try {
               MageSpell prerequisiteSpell = prerequisiteSpellClassI.getDeclaredConstructor().newInstance();
               for (Class<MageSpell> prereqPrereqSpellClass : prerequisiteSpell._prerequisiteSpells) {
                  boolean spellFound = false;
                  for (Class<MageSpell> prerequisiteSpellClassJ : spell._prerequisiteSpells) {
                     if (prerequisiteSpellClassJ == prereqPrereqSpellClass) {
                        spellFound = true;
                        break;
                     }
                  }
                  if (!spellFound) {
                     if (misssingSpellProblems.length() == 0) {
                        misssingSpellProblems.append("spell ").append(spell.getName()).append(" requires these Spells: ");
                     }
                     else {
                        misssingSpellProblems.append(", ");
                     }
                     misssingSpellProblems.append(prerequisiteSpell.getName());
                  }
               }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
               e.printStackTrace();
            }
         }
         if (misssingCollegeProblems.length() != 0) {
            problems.append(misssingCollegeProblems).append("\n");
         }
         if (misssingSpellProblems.length() != 0) {
            problems.append(misssingSpellProblems).append("\n");
         }
         if (spell.describeSpell() == null) {
            problems.append("Spell ").append(spell.getName()).append(" needs to return a string from describeSpell()\n");
         }
      }
      if (problems.length() != 0) {
         Rules.diag("Problems found:\n" + problems);
         DebugBreak.debugBreak();
      }
      //printSkillInGroups();
   }
   static public MageSpell getSpell(String name) {
      if (name != null) {
         for (MageSpell element : _spellsList) {
            if (element.getName().equalsIgnoreCase(name)) {
               return (MageSpell) element.clone();
            }
         }
      }
      return null;
   }

   public static List<String> getSpellNames() {
      List<String> list = new ArrayList<>();
      for (MageSpell element : _spellsList) {
         list.add(element.getName());
      }
      return list;
   }

   protected static String getSpellGrimioreForHTML() {
      return Spell.getSpellGrimioreForHTML(_spellsList);
   }

}
