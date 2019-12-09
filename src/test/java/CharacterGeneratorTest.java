
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterGenerator;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.DefenseOptions;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.evil.SpellFear;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Potion;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackMissile;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;

public class CharacterGeneratorTest implements Enums {

   // make a reference to the Rules object before we get into the static initializer, because that
   // makes a reference to the PriestSpells, which need the Rules object to already have been loaded.
   static final Attribute at = Attribute.Strength;// force load of this enum
   static final DamageType _dummy = DamageType.NONE;
   static final SpellFear dummy = new SpellFear();
   static final Rules dummy_ = new Rules();

   @Test
   public void testPointCosts() {
      List<String> raceNames = Race.getRaceNames(true);
      for (String raceName : raceNames) {
         List<Gender> genders = Race.getGendersForRace(raceName);
         for (Gender gender : genders) {
            Race race = Race.getRace(raceName, gender);
            race.getPointEstimate();
         }
      }
   }
   @Test
   public void testRacialWeights() {
      List<String> raceNames = Race.getRaceNames(true);
      for (String raceName : raceNames) {
         List<Gender> genders = Race.getGendersForRace(raceName);
         for (Gender gender : genders) {
            Race race = Race.getRace(raceName, gender);
            race.testRacialSize();
         }
      }
   }
   class SampleRaceData {
      String raceName;
      int avePoints;
      int pointsStep;
      int rowsToCover;
      boolean ranged;

      public SampleRaceData(String raceName, int avePoints, int pointsStep, int rowsToCover, boolean ranged) {
         this.raceName   = raceName;
         this.avePoints  = avePoints;
         this.pointsStep = pointsStep;
         this.rowsToCover= rowsToCover;
         this.ranged     = ranged;
      }
   }
   @Test
   public void testSpecificCharacter() {
      String charStr = "40 rnd:794 Kobold missile";
      Character character = CharacterGenerator.generateRandomCharacter(charStr, null, false);
      System.out.println(convertCharacterToRow(character, false, 8));
   }
   @Test
   public void createRandomCharacters() {
      // This isn't an actual Unit test, just a utility function used to
      // generate a table of random cannon-fodder for the game book

      CombatServer.generateNewPseudoRandomNumberSeed();
      System.out.println("Cannon fodder race: " + Race.NAME_Kobold + "\n<table border=1>");
      System.out.println(convertCharacterToRow(new Character(), true, 6));
      for (SampleRaceData raceData : Arrays.asList(
//                                    new SampleRaceData(Race.NAME_Kobold,    40,   4, 75, false),
                                    new SampleRaceData(Race.NAME_Kobold,    40,  20, 15,  true),
//
//                                    new SampleRaceData(Race.NAME_Goblin,     0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Goblin,    75,   5, 75, false),
//                                    new SampleRaceData(Race.NAME_Goblin,    75,  20, 15,  true),
//
//                                    new SampleRaceData(Race.NAME_Human,      0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Human,    125,  10, 75, false),
//                                    new SampleRaceData(Race.NAME_Human,    125,  30, 15,  true),
//
//                                    new SampleRaceData(Race.NAME_Orc,        0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Orc,      125,  10, 75, false),
//                                    new SampleRaceData(Race.NAME_Orc,      125,  30, 15,  true),
//
//                                    new SampleRaceData(Race.NAME_Dwarf,      0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Dwarf,    150,  15, 45, false),
//
//                                    new SampleRaceData(Race.NAME_Elf,        0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Elf,      175,  15, 35, false),
                                    new SampleRaceData(Race.NAME_Elf,      175,  35, 10,  true),

//                                    new SampleRaceData(Race.NAME_Wolf,       0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Wolf,      75,   5, 20, false),
//                                    new SampleRaceData(Race.NAME_Warg,     100,   7, 20, false),
                                    new SampleRaceData(Race.NAME_Minotaur, 235,  20, 25, false)
                                    )) {
         if (raceData.avePoints == 0) {
            System.out.println("\n</table>");
            System.out.println("Cannon fodder of race: " + raceData.raceName + "\n<table border=1>");
            System.out.println(convertCharacterToRow(new Character(), true, 6));
         }
         else {
            List<Character> chars = new ArrayList<>();
            int pointBase = raceData.avePoints;
            int delta = 0;
            int maxRowsToUse = 2;
            int rowsToCover = raceData.rowsToCover;
            boolean isAnimal = raceData.raceName.equals(Race.NAME_Wolf) || raceData.raceName.equals(Race.NAME_Warg);
            List<Integer> deltaDirs = Arrays.asList(1, -1);
            while ((rowsToCover > 2) || (isAnimal && (rowsToCover > 1))) {
               boolean rowAdded = false;
               for (int deltaDir : deltaDirs) {
                  int curPoints = pointBase + (deltaDir * delta);
                  int seed = (int) (CombatServer.random() * 1000);
                  String charStr = "" + curPoints + " rnd:" + seed + " " + raceData.raceName;
                  if (raceData.ranged) {
                     charStr += " missile";
                  }
                  Character character = CharacterGenerator.generateRandomCharacter(charStr, null, false);
                  // figure out how many lines this character would takes up.
                  try {
                     convertCharacterToRow(character, false, 8);
                  }
                  catch (Exception e) {
                     DebugBreak.debugBreak();
                     System.out.println(e);
                  }
                  int rowsUsed = Integer.parseInt(character.getName());
                  maxRowsToUse = Math.max(rowsUsed, maxRowsToUse);
                  if (!character.getRace().isAnimal() && (rowsUsed < 3)) {
                     rowsUsed = 3;
                  }
                  character.setName(charStr);
                  if (rowsToCover >= rowsUsed) {
                     chars.add(character);
                     rowsToCover -= rowsUsed;
                     rowAdded = true;
                  }
               }
               if (rowAdded) {
                  delta += raceData.pointsStep;
               }
            }
            Collections.sort(chars, new Comparator<Character>() {
               @Override
               public int compare(Character o1, Character o2) {
                  return Integer.compare(o1.getPointTotal(), o2.getPointTotal());
               }});
            for (Character character : chars) {
               System.out.println(convertCharacterToRow(character, false, maxRowsToUse));
            }
         }
         System.out.println();
      }
      System.out.println("\n</table>");
   }

   private static String convertCharacterToRow(Character character, boolean returnHeaderNames, int rowsToUse) {
      StringBuilder advantages = new StringBuilder();
      for (Advantage adv : character.getAdvantagesList()) {
         if (adv.getName().equals(Advantage.WEALTH)) {
            continue;
         }
         if (advantages.length() > 0) {
            advantages.append(", ");
         }
         advantages.append(adv.getName());
         if (adv.hasLevels()) {
            advantages.append(":").append(adv.getLevelName());
         }
      }
      byte dex = character.getAttributeLevel(Attribute.Dexterity);
      WeaponDesc weaponDescPrime = null;
      WeaponDesc weaponDescAlt = null;
      StringBuilder equipment = new StringBuilder();
      Shield shield = null;
      Weapon weaponPrime = null;
      Weapon weaponAlt = null;
      for (Limb limb : character.getLimbs()) {
         Thing thing = limb.getHeldThing();
         if (thing != null) {
            if (thing instanceof Shield) {
               shield = (Shield) thing;
            }
            else if (thing instanceof Weapon) {
               weaponPrime = (Weapon) thing;
               weaponDescPrime = new WeaponDesc(character, weaponPrime, limb._limbType);
            }
            else if (thing.isReal()) {
               if (equipment.length() > 0) {
                  equipment.append(", ");
               }
               equipment.append(thing.getName());
            }
         }
      }
      if ((weaponPrime == null) || (weaponAlt == null)) {
         for (LimbType limbType : Arrays.asList(LimbType.HAND_RIGHT, LimbType.LEG_RIGHT, LimbType.HEAD, LimbType.TAIL)) {
            Limb limb = character.getLimb(limbType);
            if (limb == null) {
               continue;
            }
            Weapon weapon = limb.getWeapon(character);
            if (weapon != null) {
               if (weaponPrime == null) {
                  weaponPrime = weapon;
                  weaponDescPrime = new WeaponDesc(character, weapon, limb._limbType);
                  break;
               }
               else if ((weaponAlt == null) && (weapon != weaponPrime)) {
                  if (weaponPrime.isReal()) {
                     // Show the 'punch' option for the alt weapon.
                     limb = character.getLimb(LimbType.HAND_RIGHT);
                     Thing oldWeap = limb.dropThing();
                     weapon = limb.getWeapon(character);
                     limb.setHeldThing(oldWeap, character);
                  }
                  weaponDescAlt = new WeaponDesc(character, weapon, limb._limbType);
//                  if ((weaponStrAlt != null) && (!weaponStrAlt.isEmpty()) && (!weaponStrPrime.equals(weaponStrAlt))) {
//                     weaponAlt = weapon;
//                  }
//                  else {
//                     weaponStrAlt = null;
//                  }
                  break;
               }
            }
         }
      }

      for (Thing thing : character.getEquipment()) {
         if ((thing == weaponPrime) || (thing == shield)) {
            continue;
         }
         if (thing instanceof Weapon) {
            if (weaponPrime == null) {
               weaponPrime = (Weapon) thing;
               weaponDescPrime = new WeaponDesc(character, weaponPrime, LimbType.HAND_RIGHT);
               continue;
            }
            else if ((weaponAlt == null) && (thing != weaponPrime)) {
               weaponAlt = (Weapon) thing;
               weaponDescAlt = new WeaponDesc(character, (Weapon) thing, LimbType.HAND_RIGHT);
               continue;
            }
         }
         if (thing instanceof Potion) {
            continue;
         }
         if (equipment.length() > 0) {
            equipment.append(", ");
         }
         equipment.append(thing.getName());
      }

      List<Skill> skillsList = character.getSkillsList();
      int totalSkillCount = skillsList.size();
      // Sort the skills by highest skill level.
      Collections.sort(skillsList, new Comparator<Skill>() {
         @Override
         public int compare(Skill o1, Skill o2) {
            return Byte.compare(o2.getLevel(), o1.getLevel());
         }});

      Skill primaryWeaponSkill   = (weaponPrime == null) ? null : character.getBestSkill(weaponPrime);
      Skill secondaryWeaponSkill =   (weaponAlt == null) ? null : character.getBestSkill(weaponAlt);
      Skill shieldSkill = character.getSkill(SkillType.Shield);
      List<Skill> unarmedSkillsList = new ArrayList<>();

      List<Skill> skillsListWeapons = new ArrayList<>();
      List<Skill> skillsListShieldAndUnarmedSkills = new ArrayList<>();
      if (primaryWeaponSkill != null) {
         skillsList.remove(primaryWeaponSkill);
         skillsListWeapons.add(primaryWeaponSkill);
      }
      if (secondaryWeaponSkill != null) {
         // If primarySkill == secondaryWeaponSkill, then it will already be removed from skillsList, so remove will return false;
         if (skillsList.remove(secondaryWeaponSkill)) {
            skillsListWeapons.add(secondaryWeaponSkill);
         }
      }
      if (shieldSkill != null) {
         skillsList.remove(shieldSkill);
         skillsListShieldAndUnarmedSkills.add(shieldSkill);
      }
      for (Skill skill : skillsList) {
         if (skill.getType().isUnarmed()) {
            unarmedSkillsList.add(skill);
         }
      }
      if (secondaryWeaponSkill == null) {
         if (unarmedSkillsList.size() == 1) {
            Skill unarmedSkill = unarmedSkillsList.remove(0);
            skillsListWeapons.add(unarmedSkill);
            skillsList.remove(unarmedSkill);
         }
      }
      for (Skill skill : unarmedSkillsList) {
         skillsList.remove(skill);
         skillsListShieldAndUnarmedSkills.add(skill);
      }

      List<List<Skill>> sortedSkillsByLine = new ArrayList<>();
      sortedSkillsByLine.add(skillsListWeapons);
      if (!skillsListShieldAndUnarmedSkills.isEmpty()) {
         sortedSkillsByLine.add(skillsListShieldAndUnarmedSkills);
      }
      if (!skillsList.isEmpty()) {
         sortedSkillsByLine.add(skillsList);
      }
      StringBuilder skills = new StringBuilder();

      String previousLine = "";
      for (List<Skill> skillsLine : sortedSkillsByLine) {
         if (skills.length() > 0) {
            skills.append("<br/>");
         }
         boolean firstOnLine = true;
         for (Skill skill : skillsLine) {
            String thisSkillDesc = skill.getName() + ": " + skill.getLevel();
            thisSkillDesc = thisSkillDesc.replace("2-Handed ", "2-Hand ");
            byte skillLevel = character.getSkillLevel(skill.getType(), null, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
            skillLevel += dex;
            if (skillLevel != skill.getLevel()) {
               thisSkillDesc += "[" + skillLevel + "]";
            }

            if (!firstOnLine) {
               boolean forceSplit = ((totalSkillCount + ((advantages.length() > 0) ? 1 : 0)) < 4);
               if ((previousLine.length() + skill.getName().length()) > 24) {
                  forceSplit = true;
               }
               if (forceSplit) {
                  skills.append("<br/>");
               }
               else {
                  skills.append(", ");
               }
            }
            firstOnLine = false;
            skills.append(thisSkillDesc);
            previousLine = thisSkillDesc;
         }
      }
      Armor armor = character.getArmor();
      String strStr = String.valueOf(character.getAttributeLevel(Attribute.Strength));
      String htStr = String.valueOf(character.getAttributeLevel(Attribute.Health));
      if (character.getRace().getBuildModifier() != 0) {
         strStr += "(" + character.getAdjustedStrength() + ")";
         htStr += "(" + character.getBuildBase() + ")";
      }
      short distance = 1;
      HashMap<RANGE, HashMap<DefenseOption, Byte>> defMap =
               character.getDefenseOptionsBase(DamageType.GENERAL,
                                               false/*isGrappleAttack*/,
                                               false /*includeWoundPenalty*/,
                                               false /*includePosition*/,
                                               false /*computePdOnly*/, distance);

      DiceSet painDice = Rules.getPainReductionDice(character.getAttributeLevel(Attribute.Toughness));

      StringBuilder attributes = new StringBuilder();
      for (Attribute attr : Attribute.values()) {
         String attrStr = String.valueOf(character.getAttributeLevel(attr));
         int maxLen = 4;
         if (attr == Attribute.Strength) {
            attrStr = strStr;
            maxLen = 6;
         }
         else if (attr == Attribute.Health) {
            attrStr = htStr;
            maxLen = 6;
         }
         while (attrStr.length() < maxLen) {
            attrStr = " " + attrStr;
         }
         attributes.append(attrStr);
      }
      byte enc = Rules.getEncumbranceLevel(character);
      byte move = character.getMovementRate();
      byte actions = character.getActionsPerTurn();
         weaponDescPrime.describeWeapon();
         if (weaponDescAlt != null) {
            weaponDescAlt.describeWeapon();
         }
      TableRow[] rows = new TableRow[rowsToUse];
      for (int r = 0 ; r < rowsToUse ; r++) {
         rows[r] = new TableRow();
      }
      if (returnHeaderNames) {
         //rows[0].addTD(new TableData("gen").setRowSpan(rowsUsed));
         rows[0].addTD(new TableData("Points").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Race<br/>Gender").setRowSpan(rowsToUse));
         for (Attribute attr : Attribute.values()) {
            rows[0].addTD(new TableData(attr.name()).setRowSpan(rowsToUse));
         }
         rows[0].addTD(new TableData("Encumberance").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Move / Action").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Actions / Turn").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Dice for Pain Recovery").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Skills<br/>Name: base level [adj. level]<br/>Advantages").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Armor<br/>Shield").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("PD").setRowSpan(rowsToUse));
         rows[0].addTD(new TableData("Base Defenses").setColSpan(4));
         rows[1].addTD(new TableData("Retreat"));
         rows[1].addTD(new TableData("Dodge"));
         rows[1].addTD(new TableData("Block"));
         rows[1].addTD(new TableData("Parry"));
         rows[0].addTD(new TableData("Build vs.").setColSpan(3));
         rows[1].addTD(new TableData("Blunt"));
         rows[1].addTD(new TableData("Cut"));
         rows[1].addTD(new TableData("Impale"));
         rows[0].addTD(new TableData("Primary Weapon").setColSpan(3));
         rows[1].addTD(new TableData("Style Name"));
         rows[1].addTD(new TableData("Re-ready Actions"));
         rows[1].addTD(new TableData("Damage"));
         rows[0].addTD(new TableData("Alternate Weapon").setColSpan(3));
         rows[1].addTD(new TableData("Style Name"));
         rows[1].addTD(new TableData("Re-ready Actions"));
         rows[1].addTD(new TableData("Damage"));
      }
      else {
         //rows[0].addTD(new TableData(character.getName()).setRowSpan(rowsUsed));
         rows[0].addTD(new TableData(String.valueOf(character.getPointTotal())).setRowSpan(rowsToUse));
         String gender = character.getRace().getGender().toString();
         gender = gender.charAt(0) + gender.substring(1).toLowerCase();
         rows[0].addTD(new TableData(character.getRace().getName() + "<br/>" + gender).setRowSpan(rowsToUse));
         for (Attribute attr : Attribute.values()) {
            String attrVal = String.valueOf(character.getAttributeLevel(attr));
            if (attr == Attribute.Strength) {
               if (character.getAttributeLevel(attr) != character.getAdjustedStrength()) {
                  attrVal += "<br/><b>" + character.getAdjustedStrength() + "</b>";
               }
               else {
                  attrVal = "<b>" + attrVal + "</b>";
               }
            }
            else if (attr == Attribute.Health) {
               if (character.getAttributeLevel(attr) != character.getBuildBase()) {
                  attrVal += "<br/><b>" + character.getBuildBase() + "</b>";
               }
               else {
                  attrVal = "<b>" + attrVal + "</b>";
               }
            }
            rows[0].addTD(new TableData(attrVal).setRowSpan(rowsToUse));
         }
         rows[0].addTD(new TableData(enc).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(move).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(actions).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(painDice.toString()).setRowSpan(rowsToUse));
         if (advantages.length() > 0) {
            rows[0].addTD(new TableData(skills.toString() + "<br/>" + advantages.toString()).setRowSpan(rowsToUse));
         }
         else {
            rows[0].addTD(new TableData(skills.toString()).setRowSpan(rowsToUse));
         }
         String equip = armor.getName() + ((shield == null) ? "" : "<br/>" + shield.getName()) + "<br/>";
         List<Thing> things = new ArrayList<>();
         Limb rightHand = character.getLimb(LimbType.HAND_RIGHT);
         Limb leftHand = character.getLimb(LimbType.HAND_LEFT);
         Thing thingR = (rightHand == null) ? null : rightHand.getHeldThing();
         Thing thingL = ( leftHand == null) ? null :  leftHand.getHeldThing();
         if ((thingR != null) && (thingR.isReal())) {
            things.add(thingR);
         }
         if ((thingL != null) && (thingL.isReal())) {
            if (!(thingL instanceof Shield)) {
               // We've already listed our shield above, don't print it twice.
               things.add(thingL);
            }
         }
         things.addAll(character.getEquipment());
         boolean first = true;
         for (Thing thing : things) {
            if (thing instanceof Potion) {
               // Don't list potions in the cannon fodder list.
               // When created, they did not impact the characters enc. level or money spent.
               continue;
            }
            if (!first) {
               equip += ", ";
            }
            first = false;
            equip += thing._name;
         }
         HashMap<DefenseOption, Byte> defOptMap = defMap.get(RANGE.OUT_OF_RANGE);
         rows[0].addTD(new TableData(equip).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(character.getPassiveDefense(RANGE.OUT_OF_RANGE, false, distance))).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(defOptMap.get(DefenseOption.DEF_RETREAT))).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(defOptMap.get(DefenseOption.DEF_DODGE))).setRowSpan(rowsToUse));
         Byte block = defOptMap.get(DefenseOption.DEF_LEFT);
         if ((shield == null) || (block == null)) {
            block = 0;
         }
         rows[0].addTD(new TableData(block < 1 ? "-" : String.valueOf(block)).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(defMap.get(RANGE.OUT_OF_RANGE).get(DefenseOption.DEF_RIGHT))).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(character.getBuild(DamageType.BLUNT))).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(character.getBuild(DamageType.CUT))).setRowSpan(rowsToUse));
         rows[0].addTD(new TableData(String.valueOf(character.getBuild(DamageType.IMP))).setRowSpan(rowsToUse));
         if (weaponDescPrime.weapon.isUnarmedStyle()) {
            Skill primeSkill = character.getBestSkill(weaponDescPrime.weapon);
            rows[0].addTD(new TableData(primeSkill.getName()).setBold());
         } else {
            rows[0].addTD(new TableData(weaponDescPrime.weapon.getName()).setBold());
         }
         rows[0].addTD(new TableData("Adjusted skill: " + weaponDescPrime.adjustedSkill).setColSpan(1));
         //rows[0].addTD(new TableData("Adjusted skill: " + weaponDescPrime.adjustedSkill).setColSpan(2));
         int s = 1;
         for (WeaponStyleDesc styleDesc : weaponDescPrime.styleList) {
            rows[s++].addTD(new TableData(styleDesc.getAlteredName(weaponDescPrime)))
                     .addTD(new TableData(styleDesc.actionsRequired))
                     .addTD(new TableData(styleDesc.damageStr));
         }
         while (s < rowsToUse) {
            rows[s++].addTD(new TableData("&nbsp;")).addTD(new TableData("&nbsp;")).addTD(new TableData("&nbsp;"));
         }
         s = 1;
         boolean altWeaponAvailable = false;
         if ((weaponDescAlt != null) && (!weaponDescAlt.weapon.getName().equals(weaponDescPrime.weapon.getName()))) {
            altWeaponAvailable = true;
            if (weaponDescAlt.weapon.isUnarmedStyle()) {
               Skill altSkill = character.getBestSkill(weaponDescAlt.weapon);
               if (altSkill == null) {
                  altWeaponAvailable = false;
               }
               else {
                  rows[0].addTD(new TableData(altSkill.getName()).setBold());
               }
            } else {
               rows[0].addTD(new TableData(String.valueOf(weaponDescAlt.weapon.getName())).setBold());
            }
            if (altWeaponAvailable) {
               rows[0].addTD(new TableData("Adjusted skill: " + weaponDescAlt.adjustedSkill).setColSpan(1));
               //rows[0].addTD(new TableData("Adjusted skill: " + weaponDescAlt.adjustedSkill).setColSpan(2));
               for (WeaponStyleDesc styleDesc : weaponDescAlt.styleList) {
                  rows[s++].addTD(new TableData(styleDesc.getAlteredName(weaponDescAlt)))
                           .addTD(new TableData(styleDesc.actionsRequired))
                           .addTD(new TableData(styleDesc.damageStr));
               }
            }
         }
         if (!altWeaponAvailable) {
            rows[0].addTD(new TableData("&nbsp;"));
            rows[0].addTD(new TableData("&nbsp;"));
            //rows[0].addTD(new TableData("&nbsp;").setColSpan(2));
         }
         while (s < rowsToUse) {
            rows[s++].addTD(new TableData("&nbsp;")).addTD(new TableData("&nbsp;")).addTD(new TableData("&nbsp;"));
         }
         rows[0].addTD(new TableData("&nbsp;"));
         rows[0].addTD(new TableData("&nbsp;"));
      }
      int maxStyles = 1;
      if (weaponDescPrime != null) {
         maxStyles = Math.max(maxStyles, weaponDescPrime.styleList.size());
      }
      if (weaponDescAlt != null) {
         maxStyles = Math.max(maxStyles, weaponDescAlt.styleList.size());
      }
      maxStyles++;
      character.setName("" + maxStyles);
      StringBuilder sb = new StringBuilder();
      for (TableRow row : rows) {
         sb.append(row.toString());
      }
      return sb.toString().replace("<br/></", "</").replaceAll("<br/>", "%");
   }

   static class WeaponStyleDesc {
      private final WeaponStyleAttack style;
      private final Skill skill;
      public byte baseSkill;
      public byte adjustedSkill = (byte)0;
      public byte actionsRequired;
      public String damageStr;
      public String styleName;
      public boolean isShowable = false;
      public WeaponStyleDesc(WeaponStyleAttack style, Character character, LimbType limbType) {
         this.style           = style;
         this.styleName       = style.getName();
         if (this.style.isRanged() && (this.style instanceof WeaponStyleAttackMissile)) {
            this.actionsRequired = ((WeaponStyleAttackMissile)this.style).getNumberOfPreparationSteps();
         }
         else {
            this.actionsRequired = style.getSpeed(character.getAttributeLevel(Attribute.Strength));
         }
         this.damageStr       = style.getDamageString(character.getPhysicalDamageBase());
         this.skill           = character.getSkill(style.getSkillType());
         if (this.skill != null) {
            this.adjustedSkill = character.getSkillLevel(this.skill.getType(), limbType,
                                                         true/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
            this.adjustedSkill += character.getAttributeLevel(Attribute.Dexterity);
            this.adjustedSkill -= this.style.getSkillPenalty();
            byte baseSkillLevel = character.getSkillLevel(this.skill.getType(), limbType, false, true, false);
            this.isShowable = (baseSkillLevel > this.style.getSkillPenalty());
         }
      }

      public String getAlteredName(WeaponDesc weaponDesc) {
         String alteredName = this.styleName;
         boolean diffSkill = weaponDesc.singleSkillType != skill.getType();
         if (weaponDesc.weapon.getName().equals(Weapon.NAME_BastardSword) ||
             weaponDesc.weapon.getName().equals(Weapon.NAME_BastardSword_Fine)) {
            diffSkill = false;
         }
         if (diffSkill) {
            alteredName += " (" + skill.getType()._name;
         }
         if (weaponDesc.adjustedSkill != adjustedSkill) {
            alteredName += " [" + adjustedSkill + "]";
         }
         if (diffSkill) {
            alteredName += ")";
         }
         if (this.style.isRanged()) {
            short maxDistance = ((WeaponStyleAttackRanged)this.style).getMaxDistance(weaponDesc.character.getAdjustedStrength());
            if (alteredName.equalsIgnoreCase("shoot")) {
               alteredName = "Base Range:" + (maxDistance / 4.0);
            }
            else {
               alteredName += "<br/>Base Range:" + (maxDistance / 4.0);
            }
         }
         return alteredName;
      }

      private void describeStyle(boolean isOnlySkill, Byte singleSkillBaseLevel, StringBuilder description) {
         if (description.length() > 0) {
            description.append("\n");
            if (isOnlySkill) {
               description.append("    ");
            }
         }
         description.append(this.styleName).append(", ");
         this.baseSkill = this.skill.getLevel();
         if (isOnlySkill) {
            description.append(" skill ").append(this.skill.getName()).append(": ").append(this.baseSkill);
         }
         if (((singleSkillBaseLevel != null) ? singleSkillBaseLevel.byteValue() : this.baseSkill) != this.adjustedSkill) {
            description.append(" (adj. ").append(this.adjustedSkill).append(")");
         }
         description.append(" ").append(this.actionsRequired).append(" actions: ");
         description.append(this.damageStr);
      }
   }

   static class WeaponDesc {
      private final Character character;
      private final Weapon weapon;
      private final LimbType limbType;
      private SkillType singleSkillType = null;
      public List<WeaponStyleDesc> styleList = new ArrayList<>();
      public int baseSkill;
      public int adjustedSkill;
      public Byte singleSkillBaseLevel = null;

      public WeaponDesc(Character character, Weapon weapon, LimbType limbType) {
         this.character = character;
         this.weapon = weapon;
         this.limbType = limbType;
      }

      public String describeWeapon() {
         // See if there is a single skill that governs all the possible attack styles:
         for (WeaponStyleAttack style : this.weapon._attackStyles) {
            Skill skill = this.character.getSkill(style.getSkillType());
            if ((skill == null) || (skill.getLevel() <1 )) {
               continue;
            }

            if (this.singleSkillType == null) {
               this.singleSkillType = style.getSkillType();
            }
            else if (this.singleSkillType != style.getSkillType()) {
               if (this.singleSkillType.getName().equals(SkillType.Knife.getName()) && style.getSkillType().getName().equals(SkillType.Throwing.getName())) {
                  continue;
               }
               if (this.singleSkillType.getName().equals(SkillType.Throwing.getName()) && style.getSkillType().getName().equals(SkillType.Knife.getName())) {
                  this.singleSkillType = style.getSkillType();
                  continue;
               }
               this.singleSkillType = null;
               break;
            }
         }
         StringBuilder description = new StringBuilder();
         if (this.weapon.isReal()) {
            description.append(this.weapon.getName()).append(": ");
         }
         if (this.singleSkillType != null) {
            Skill skill = this.character.getSkill(this.singleSkillType);
            this.singleSkillBaseLevel = skill.getLevel();
            byte skillLevel = this.character.getSkillLevel(this.singleSkillType, this.limbType, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
            skillLevel += this.character.getAttributeLevel(Attribute.Dexterity);
            if (this.weapon.isReal()) {
               description.append(" skill ");
            }
            this.baseSkill = this.singleSkillBaseLevel;
            this.adjustedSkill = skillLevel;
            description.append(skill.getName()).append(": ").append(this.singleSkillBaseLevel);
            if (this.singleSkillBaseLevel.byteValue() != skillLevel) {
               description.append(" (adj. ").append(skillLevel).append(")");
            }
            this.singleSkillBaseLevel = skillLevel;
         }

         for (WeaponStyleAttack style : this.weapon._attackStyles) {
            WeaponStyleDesc styleDesc = new WeaponStyleDesc(style, this.character, this.limbType);
            if (styleDesc.isShowable) {
               styleDesc.describeStyle((this.singleSkillType != null), this.singleSkillBaseLevel, description);
               this.styleList.add(styleDesc);
            }
         }
         // list the attack types corresponding to the higher skill level first:
         Collections.sort(this.styleList, new Comparator<WeaponStyleDesc>() {
            @Override
            public int compare(WeaponStyleDesc o1, WeaponStyleDesc o2) {
               if (o1.style._skillType.getName().equals(o2.style._skillType.getName())) {
                  return 0;
               }
               byte l1 = character.getSkillLevel(o1.style._skillType, LimbType.HAND_RIGHT, false, false, false);
               byte l2 = character.getSkillLevel(o2.style._skillType, LimbType.HAND_RIGHT, false, false, false);
               if (l1 == l2) {
                  return 0;
               }
               return l1 > l2 ? -1 : 1;
            }});
         // unarmed skills need to also list the damage from kicks:
         if (!(this.weapon.isReal() || (this.limbType != LimbType.HAND_RIGHT))) {
            Limb leg = this.character.getLimb(LimbType.LEG_RIGHT);
            if (leg != null) {
               Weapon legWeapon = leg.getWeapon(this.character);
               for (WeaponStyleAttack style : legWeapon._attackStyles) {
                  WeaponStyleDesc styleDesc = new WeaponStyleDesc(style, this.character, LimbType.LEG_RIGHT);
                  if (styleDesc.isShowable) {
                     styleDesc.describeStyle((this.singleSkillType != null), this.singleSkillBaseLevel, description);
                     this.styleList.add(styleDesc);
                  }
               }
            }
         }
         return description.toString();
      }
   }

   @Test
   public void specificFailure() {
      int seed = 288610733;
      String raceName = "Troll";
      int maxPoints = 550;
      int wealth = 0;
      CombatServer.setPseudoRandomNumberSeed(seed);
      Character character = CharacterGenerator.generateRandomCharacter("? "+ raceName +" "+maxPoints+" Wealth:$"+wealth+" \"name:Troll Prisoner\" sword:10 karate:0 brawling:0 aikido:0", null/*arena*/, false/*printCharacter*/);
      assertTrue("name incorrect", (character.getName().equals("Troll Prisoner")));
      assertTrue("skills incorrect", (character.getSkillLevel(SkillType.Sword, LimbType.HAND_RIGHT, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) == 10));
      assertTrue("size adjustment incorrect", (character.getSkillLevel(SkillType.Sword, LimbType.HAND_RIGHT, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) == 8));
      assertTrue("Troll should have spent less than or equal to $" + wealth, (character.getTotalCost() <= wealth));
      assertTrue("generated " + character.getPointTotal()
                 + "-point character that should have only had " + maxPoints + " points from seed "
                 + seed, character.getPointTotal() <= maxPoints);
      assertTrue("generated " + character.getRace().getName()
                 + " character that should have been a " + raceName + ".",
                 character.getRace().getName().equals(raceName));

      raceName = "Elf";
      String name = "Fred Flintstone";
      maxPoints = 350;
      CombatServer.setPseudoRandomNumberSeed(seed);
      character = CharacterGenerator.generateRandomCharacter("? "+ raceName +" "+maxPoints+" \"name:"+ name +"\" \"Magic Missile:3\" IQ:3 \"Magical Aptitude:+3\"", null/*arena*/, false/*printCharacter*/);
      assertTrue("name incorrect", (character.getName().equals(name)));
      assertTrue("generated " + character.getPointTotal()
                 + "-point character that should have only had " + maxPoints + " points from seed "
                 + seed, character.getPointTotal() <= maxPoints);
      assertTrue("generated " + character.getRace().getName()
                 + " character that should have been a " + raceName + ".",
                 character.getRace().getName().equals(raceName));

      character = CharacterGenerator.generateRandomCharacter("? 225 Orc key:cell3 key:cell4 Potion:Complete Healing Potion:Speed Potion:Major Healing", null/*arena*/, false/*printCharacter*/);
      assertTrue("should have keys", character.hasKey("cell3"));
   }

   public void testGenerateRandomCharacterString() {
      int seed = (int) System.currentTimeMillis();
      CombatServer.setPseudoRandomNumberSeed(seed);
      for (String raceName : Race.getRaceNames(true/* includeNPC */)) {
         Race race = Race.getRace(raceName, Gender.MALE);
         for (int pointAdj = -50; pointAdj < 300; pointAdj += (Math.random() * 100)) {
            int maxPoints = race.getCost() + pointAdj;
            seed = (int) ((CombatServer.random() + CombatServer.random() + CombatServer.random()) * 10000000);
            String key = seed + " generated " + maxPoints + " point " + raceName;
            CombatServer.setPseudoRandomNumberSeed(seed);
            Character character = CharacterGenerator.generateRandomCharacter("? " + maxPoints + " "
                                                                             + raceName, null/*arena*/, false/*printCharacter*/);
            assertTrue(key + " has too many points: " + character.getPointTotal(),
                       character.getPointTotal() <= maxPoints);
            assertTrue(key + " actually was a " + character.getRace().getName(),
                       character.getRace().getName().equals(raceName));
            if (race.isAnimal()) {
               assertTrue(key + " animal has equipment", character.getWeightCarried() == 0);
               assertTrue(key + " animal has multiple or 0 skills: " + character.getSkillsList(),
                          (character.getSkillsList().size() == 1));
               assertTrue(key + " animal has non-brawling skill: " + character.getSkillsList(),
                          (character.getSkillsList().get(0).getType() == SkillType.Brawling));
            }
            seed++;
         }
      }
   }

   @Test
   public void testDefenses() {
      String armorName = Armor.NAME_ScaleMail;
      String shieldName = Shield.NAME_Large;
      String weaponName = Weapon.NAME_Broadsword;
      byte weaponSkillLevel = 5;
      byte shieldSkillLevel = 5;
      byte nim = 4;
      byte str = 5;
      short distance = 1;
      Character character = CharacterGenerator.generateRandomCharacter("? 200 Human STR:" + str
                                                                       + " NIM:" + nim +
                                                                       " \"" + armorName + "\"" +
                                                                       " \"" + shieldName + "\"" +
                                                                       " " + weaponName,
                                                                       null/*arena*/, false/*printCharacter*/);
      Armor armor = Armor.getArmor(armorName, character.getRace());
      Shield shield = Shield.getShield(shieldName, character.getRace());
      Weapon weapon = Weapon.getWeapon(weaponName, character.getRace());
      character.setSkillLevel(weapon.getAttackStyle(0).getSkillType(), weaponSkillLevel);
      character.setSkillLevel(shield.getDefenseSkillTypes().get(0), shieldSkillLevel);
      assertTrue("Wrong strength", character.getAttributeLevel(Attribute.Strength) == str);
      assertTrue("Wrong nimbleness", character.getAttributeLevel(Attribute.Nimbleness) == nim);
      assertTrue("Wrong armor", character.getArmor().getName().equals(armorName));
      assertTrue("Wrong shield", character.getLimb(LimbType.HAND_LEFT).getHeldThing().getName().equals(shieldName));
      assertTrue("Wrong weapon", character.getLimb(LimbType.HAND_RIGHT).getHeldThing().getName().equals(weaponName));
      byte pd = (byte) (armor.getPassiveDefense() + shield.getPassiveDefense());
      byte testPd = character.getPassiveDefense(RANGE.OUT_OF_RANGE, false/*isGrappleAttack*/, distance);
      assertTrue("Wrong PD", testPd == pd);
      // try off-hand parrys and blocks
      CombatMap map = new CombatMap((short)10, (short)10, null);

      ArenaLocation startLoc = map.getLocation((short)5, (short)5);
      map.addCharacter(character, startLoc, null);
      for (boolean isAmbidexterous = false; !isAmbidexterous; isAmbidexterous = true) {
         if (isAmbidexterous) {
            character.addAdvantage(Advantage.getAdvantage(Advantage.AMBIDEXTROUS));
         }
         for (boolean offHand = false; !offHand; offHand = true) {
            int offHandPenalty = 0;
            character.getLimb(LimbType.HAND_LEFT).dropThing();
            character.getLimb(LimbType.HAND_RIGHT).dropThing();
            if (offHand) {
               if (!isAmbidexterous) {
                  offHandPenalty = -3;
               }
               character.getLimb(LimbType.HAND_LEFT).setHeldThing(weapon, character);
               character.getLimb(LimbType.HAND_RIGHT).setHeldThing(shield, character);
            } else {
               character.getLimb(LimbType.HAND_LEFT).setHeldThing(shield, character);
               character.getLimb(LimbType.HAND_RIGHT).setHeldThing(weapon, character);
            }
            byte attackingWeaponsParryPen = 0;
            for (RANGE range : RANGE.values()) {
               byte rangeAdjToPD = Rules.getRangeDefenseAdjustmentToPD(range);
               byte rangeAdjPerAction = Rules.getRangeDefenseAdjustmentPerAction(range);
               if (rangeAdjToPD == -100) {
                  rangeAdjToPD = 0;
               }
               if (rangeAdjPerAction == -100) {
                  rangeAdjPerAction = 0;
               }
               byte rangedPd = (byte) (pd + rangeAdjToPD);
               byte pdAtRange = character.getPassiveDefense(range, false/*isGrappleAttack*/, distance);

               byte effectiveDodge = (byte) (Rules.getDodgeLevel(nim) + rangeAdjPerAction);
               if (effectiveDodge < 0) {
                  effectiveDodge = 0;
               }
               byte effectiveRetreat = (byte) (Rules.getRetreatLevel(nim) + (rangeAdjPerAction * 2));
               effectiveRetreat = (byte) Math.max(0, effectiveRetreat);
               int effectiveBlock = (shieldSkillLevel - offHandPenalty) + rangeAdjPerAction;
               effectiveBlock = Math.max(0, effectiveBlock);
               int effectiveParry = (weaponSkillLevel - offHandPenalty) + rangeAdjPerAction;
               effectiveParry = Math.max(0, effectiveParry);
               if (range != RANGE.OUT_OF_RANGE) {
                  effectiveParry = 0;
               }

               // check PD
               assertTrue("Wrong PD at range: " + range.getName(), pdAtRange == rangedPd);

               boolean isGrapple = false;
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRetreat));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_DODGE),attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_DODGE),attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_RIGHT),attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock + effectiveParry));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_RIGHT, DefenseOption.DEF_DODGE), attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock + effectiveParry + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_RETREAT),           attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock + effectiveRetreat));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_RETREAT),           attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry + effectiveRetreat));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_LEFT, DefenseOption.DEF_DODGE), attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry + effectiveBlock + effectiveDodge));
               character.getCondition().setPosition(Position.PRONE_FRONT, map, character);
               // Can't block or parry when on stomach:
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 6)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 6)));
               // TODO check attack skill is 0
               character.getCondition().setPosition(Position.PRONE_BACK, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveBlock   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveParry   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is at -4
               character.getCondition().setPosition(Position.SITTING, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is at -4
               character.getCondition().setPosition(Position.KNEELING, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is normal
               character.getCondition().setPosition(Position.CROUCHING, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveBlock));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveParry));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is normal
               character.getCondition().setPosition(Position.STANDING, map, character);
            }
         }
      }
   }
   public void assertDefense(Character character, DefenseOptions defOpts, byte attackingWeaponsParryPen,
                             short distance, RANGE range, boolean isGrapple, int expectedTN) {
      boolean isRanged = (range != RANGE.OUT_OF_RANGE);
      boolean isCharge = false;
      byte tn = character.getDefenseOptionTN(defOpts, (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                  false/*includeHolds*/, true/* includePosition */,
                                  false/* includeMassiveDamPen */, attackingWeaponsParryPen, isRanged, distance,
                                  isGrapple, isCharge, DamageType.GENERAL, false/* defAppliedAlready */, range);
      if (tn != expectedTN) {
         tn = character.getDefenseOptionTN(defOpts, (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                     false/*includeHolds*/, true/* includePosition */,
                                     false/* includeMassiveDamPen */, attackingWeaponsParryPen, isRanged, distance,
                                     isGrapple, isCharge, DamageType.GENERAL, false/* defAppliedAlready */, range);
         assertTrue("Wrong " + defOpts.getDefenseName(isCharge, character, null) + " TN (" + tn + ") while " +
                    character.getPositionName() + " at range " + range.getName() +" (expected to be " + expectedTN + ")", false);
      }
   }

}
