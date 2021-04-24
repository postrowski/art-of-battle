package ostrowski.combat.common.spells.mage;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.Spell;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class MageSpells {
   static final Collection<MageSpell> spellsList = new TreeSet<>();
   static {
      spellsList.add(new SpellAffectArea());
      spellsList.add(new SpellArmor());
      spellsList.add(new SpellBlockAttack());
      spellsList.add(new SpellBlockSpell());
      spellsList.add(new SpellBlockThought());
      spellsList.add(new SpellBlur());
      spellsList.add(new SpellCausePain());
      spellsList.add(new SpellCauseWound());
      spellsList.add(new SpellControlCharge());
      spellsList.add(new SpellControlFire());
      spellsList.add(new SpellControlLight());
      spellsList.add(new SpellControlMind());
      spellsList.add(new SpellControlTemperature());
      spellsList.add(new SpellControlTime());
      spellsList.add(new SpellCreateAir());
      spellsList.add(new SpellCreateDarkness());
      spellsList.add(new SpellCreateEarth());
      spellsList.add(new SpellCreateElectricity());
      spellsList.add(new SpellCreateFire());
      spellsList.add(new SpellCreateForce());
      spellsList.add(new SpellCreateLight());
      spellsList.add(new SpellCreateRope());
      spellsList.add(new SpellCreateWater());
      spellsList.add(new SpellDetectDisturbance());
      spellsList.add(new SpellDetectObject());
      spellsList.add(new SpellFireball());
      spellsList.add(new SpellFlameJet());
      spellsList.add(new SpellFlamingWeapon());
      spellsList.add(new SpellFlamingMissileWeapon());
      spellsList.add(new SpellFlight());
      spellsList.add(new SpellFreezeArea());
      spellsList.add(new SpellGlue());
      spellsList.add(new SpellHarden());
      spellsList.add(new SpellIceDaggers());
      spellsList.add(new SpellImmobilize());
      spellsList.add(new SpellLevitate());
      spellsList.add(new SpellLightning());
      spellsList.add(new SpellMagicMissile());
      spellsList.add(new SpellMagicShield());
      spellsList.add(new SpellMendObjects());
      spellsList.add(new SpellMissileShield());
      spellsList.add(new SpellPush());
      spellsList.add(new SpellResistPain());
      spellsList.add(new SpellReverseMissile());
      spellsList.add(new SpellShockingGrasp());
      spellsList.add(new SpellSlow());
      spellsList.add(new SpellSpeed());
      spellsList.add(new SpellSpiderWeb());
      spellsList.add(new SpellStrength());
      spellsList.add(new SpellSuggestion());
      spellsList.add(new SpellThrowSpell());
      spellsList.add(new SpellTrip());
      spellsList.add(new SpellWaterJet());
      spellsList.add(new SpellWallOfFire());
      spellsList.add(new SpellWeaken());
      spellsList.add(new SpellWind());
   }

   static {
      // This static initializer is used to verify various things about spells, including:
      //   1) defendableSpell are missileSpells and the SpiderWebSpell
      //   2) Each spell that requires another spell, also requires the skills used by the other spell(s)
      //   3) Each spell that requires another spell, also requires the spells required by the first spell
      StringBuilder problems = new StringBuilder();
      for (MageSpell spell : spellsList) {
         // If this assumption isn't true, then the code in Spell.getEffectiveness needs to change:
         if (spell.isDefendable() != ((spell instanceof MissileMageSpell) || (spell instanceof SpellSpiderWeb))) {
            problems.append(spell.getName()).append(".isDefendable != (Missile || SpiderWeb)");
         }

         StringBuilder misssingSkillTypeProblems = new StringBuilder();
         StringBuilder misssingSpellProblems = new StringBuilder();
         for (Class<MageSpell> prerequisiteSpellClass : spell.prerequisiteSpells) {
            try {
               MageSpell prerequisiteSpell = prerequisiteSpellClass.getDeclaredConstructor().newInstance();
               //  1) Each spell that requires another spell, also requires the skillTypes used by the other spell(s)
               for (SkillType prerequisiteSpellsSkillTypes : prerequisiteSpell.prerequisiteSkillTypes) {
                  boolean skillTypeFound = false;
                  for (SkillType skillType : spell.prerequisiteSkillTypes) {
                     if (skillType.getName().equals(prerequisiteSpellsSkillTypes.getName())) {
                        skillTypeFound = true;
                        break;
                     }
                  }
                  if (!skillTypeFound) {
                     if (misssingSkillTypeProblems.length() == 0) {
                        misssingSkillTypeProblems.append("spell ").append(spell.getName()).append(" requires these SkillTypes: ");
                     }
                     else {
                        misssingSkillTypeProblems.append(", ");
                     }
                     misssingSkillTypeProblems.append(prerequisiteSpellsSkillTypes.getName());
                  }
               }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
               e.printStackTrace();
               DebugBreak.debugBreak();
            }
         }
         // 2) Each spell that requires another spell, also requires the spells required by the first spell
         for (Class<MageSpell> prerequisiteSpellClassI : spell.prerequisiteSpells) {
            try {
               MageSpell prerequisiteSpell = prerequisiteSpellClassI.getDeclaredConstructor().newInstance();
               for (Class<MageSpell> prereqPrereqSpellClass : prerequisiteSpell.prerequisiteSpells) {
                  boolean spellFound = false;
                  for (Class<MageSpell> prerequisiteSpellClassJ : spell.prerequisiteSpells) {
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
         if (misssingSkillTypeProblems.length() != 0) {
            problems.append(misssingSkillTypeProblems).append("\n");
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
         for (MageSpell element : spellsList) {
            if (element.getName().equalsIgnoreCase(name)) {
               return (MageSpell) element.clone();
            }
         }
      }
      return null;
   }

   public static List<String> getSpellNames() {
      List<String> list = new ArrayList<>();
      for (MageSpell element : spellsList) {
         list.add(element.getName());
      }
      return list;
   }

   protected static String getSpellGrimioreForHTML() {
      return Spell.getSpellGrimioreForHTML(spellsList);
   }

}
