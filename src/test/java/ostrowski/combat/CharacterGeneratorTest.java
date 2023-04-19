package ostrowski.combat;

import org.junit.Assert;
import org.junit.Test;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.evil.SpellFear;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackMissile;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class CharacterGeneratorTest implements Enums {

   // make a reference to the Rules object before we get into the static initializer, because that
   // makes a reference to the PriestSpells, which need the Rules object to already have been loaded.
   static final Attribute  at     = Attribute.Strength;// force load of this enum
   static final DamageType dummy1 = DamageType.NONE;
   static final SpellFear  dummy  = new SpellFear();
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
   static class SampleRaceData {
      final String  raceName;
      final int     avePoints;
      final int     pointsStep;
      final int     rowsToCover;
      final boolean ranged;

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
      System.out.println(CannonFodder.HtmlCharWriter.convertCharacterToRow(character, false, 8));
      for (int rnd = 100 ; rnd < 1000 ; rnd++){
         charStr = rnd + " rnd:" + rnd + " Minotaur";
         character = CharacterGenerator.generateRandomCharacter(charStr, null, false);
         byte brawlingLevel = character.getSkillLevel(SkillType.Brawling, null, false, false, false);
         byte karateLevel = character.getSkillLevel(SkillType.Karate, null, false, false, false);
         if ((brawlingLevel <= 1) && (karateLevel <= 1)) {
            Assert.assertTrue("Minotaurs must have brawling", false);
         }
         if ((brawlingLevel > 1) && (karateLevel > 1)) {
            Assert.assertTrue("Minotaurs may not have brawling and karate", false);
         }

      }
   }
   @Test
   public void createRandomCharacters() {
      // This isn't an actual Unit test, just a utility function used to
      // generate a table of random cannon-fodder for the game book

      CombatServer.generateNewPseudoRandomNumberSeed();
      System.out.println("Cannon fodder race: " + Race.NAME_Kobold + "\n<table border=1>");
      System.out.println(CannonFodder.HtmlCharWriter.convertCharacterToRow(new Character(), true, 6));
      for (SampleRaceData raceData : Arrays.asList(
//                                    new SampleRaceData(Race.NAME_Kobold,    40,   4, 75, false),
//              new SampleRaceData(Race.NAME_Kobold, 40, 20, 15, true),
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
//              new SampleRaceData(Race.NAME_Elf, 175, 35, 10, true),

//                                    new SampleRaceData(Race.NAME_Wolf,       0,   0,  0, false),
//                                    new SampleRaceData(Race.NAME_Wolf,      75,   5, 20, false),
                                    new SampleRaceData(Race.NAME_Warg,     100,   7, 20, false),
              new SampleRaceData(Race.NAME_Minotaur, 235, 20, 25, false)
                                    )) {
         if (raceData.avePoints == 0) {
            System.out.println("\n</table>");
            System.out.println("Cannon fodder of race: " + raceData.raceName + "\n<table border=1>");
            System.out.println(CannonFodder.HtmlCharWriter.convertCharacterToRow(new Character(), true, 6));
         } else {
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
                  // figure out how many lines this character would take up.
                  try {
                     CannonFodder.HtmlCharWriter.convertCharacterToRow(character, false, 8);
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
            chars.sort((o1, o2) -> Integer.compare(o1.getPointTotal(), o2.getPointTotal()));
            for (Character character : chars) {
               System.out.println(CannonFodder.HtmlCharWriter.convertCharacterToRow(character, false, maxRowsToUse));
            }
         }
         System.out.println();
      }
      System.out.println("\n</table>");
   }



   @Test
   public void specificFailure() {
      int seed = 288610733;
      String raceName = "Troll";
      int maxPoints = 550;
      int wealth = 0;
      CombatServer.setPseudoRandomNumberSeed(seed);
      String chrSourceStr = "? " + raceName + " " + maxPoints + " Wealth:$" + wealth +
                            " \"name:Troll Prisoner\" sword:10";
      Character character = CharacterGenerator.generateRandomCharacter(chrSourceStr, null/*arena*/, false/*printCharacter*/);
      assertTrue("name incorrect", (character.getName().equals("Troll Prisoner")));
      assertTrue("skills incorrect", (character.getSkillLevel(SkillType.Sword, null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) == 10));
      assertTrue("size adjustment incorrect", (character.getSkillLevel(SkillType.Sword, null, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) == 8));
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
            seed = (int) ((CombatServer.random() + CombatServer.random() + CombatServer.random()) * 10_000_000);
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
               assertTrue(key + " animal has multiple or 0 professions: " + character.getProfessionsList(),
                          (character.getProfessionsList().size() == 1));
               Profession profession = character.getProfessionsList().get(0);
               assertTrue(key + " animal has non-common profession: " + character.getProfessionsList(),
                          (profession.getType() == ProfessionType.Common));
               assertTrue(key + " animal has multiple or 0 common proficient skills: " + profession.getProficientSkills(),
                          (profession.getProficientSkills().size() == 1));
               assertTrue(key + " animal has familiar skills: " + profession.getFamiliarSkills(),
                          (profession.getFamiliarSkills().isEmpty()));
               assertTrue(key + " animal has non-brawling skill: " + profession.getProficientSkills(),
                          (profession.getProficientSkills().get(0) == SkillType.Brawling));
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
      Weapon weapon = Weapons.getWeapon(weaponName, character.getRace());
      if (weapon != null) {
         character.setSkillLevel(ProfessionType.Fighter, weapon.getAttackStyle(0).getSkillType(), weaponSkillLevel);
      }
      if (shield != null) {
         character.setSkillLevel(ProfessionType.Fighter, shield.getDefenseSkillTypes().get(0), shieldSkillLevel);
      }
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
         for (boolean offHand = false; offHand = !offHand; offHand = true) {
            int offHandPenalty = 0;
            character.getLimb(LimbType.HAND_LEFT).dropThing();
            character.getLimb(LimbType.HAND_RIGHT).dropThing();
            if (offHand) {
               if (!isAmbidexterous) {
                  offHandPenalty = 3;
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
               int effectiveBlock = (shieldSkillLevel/* - offHandPenalty*/) + rangeAdjPerAction;
               effectiveBlock = Math.max(0, effectiveBlock);
               int effectiveParry = (weaponSkillLevel - offHandPenalty) + rangeAdjPerAction;
               effectiveParry = Math.max(0, effectiveParry);
               if (range != RANGE.OUT_OF_RANGE) {
                  effectiveParry = 0;
               }

               int effectiveLeft = offHand ? effectiveParry : effectiveBlock;
               int effectiveRight = offHand ? effectiveBlock : effectiveParry;

               // check PD
               assertTrue("Wrong PD at range: " + range.getName(), pdAtRange == rangedPd);

               boolean isGrapple = false;
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRetreat));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_DODGE),attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_DODGE),attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_RIGHT),attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft + effectiveRight));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_RIGHT, DefenseOption.DEF_DODGE), attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft + effectiveRight + effectiveDodge));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT,  DefenseOption.DEF_RETREAT),           attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft + effectiveRetreat));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_RETREAT),           attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight + effectiveRetreat));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_LEFT, DefenseOption.DEF_DODGE), attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight + effectiveLeft + effectiveDodge));
               character.getCondition().setPosition(Position.PRONE_FRONT, map, character);
               // Can't block or parry when on stomach:
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 6)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 6)));
               // TODO check attack skill is 0
               character.getCondition().setPosition(Position.PRONE_BACK, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveLeft   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRight   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is at -4
               character.getCondition().setPosition(Position.SITTING, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is at -4
               character.getCondition().setPosition(Position.KNEELING, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_DODGE),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveDodge   - 4)));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RETREAT),                       attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + Math.max(0, effectiveRetreat - 4)));
               // TODO check attack skill is normal
               character.getCondition().setPosition(Position.CROUCHING, map, character);
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_LEFT),                          attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveLeft));
               assertDefense(character, new DefenseOptions(DefenseOption.DEF_RIGHT),                         attackingWeaponsParryPen, distance, range, isGrapple, (rangedPd + effectiveRight));
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
         String message = "Wrong " + defOpts.getDefenseName(isCharge, character, null) + " TN (" + tn + ") while " +
                          character.getPositionName() + " at range " + range.getName() + " (expected to be " + expectedTN + ")";
         assertTrue(message, false);
      }
   }

}
