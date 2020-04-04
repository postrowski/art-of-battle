package junit.ostrowski.combat.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterGenerator;
import ostrowski.combat.common.Enums;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.Skill.SkillType;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.server.CombatServer;

public class CharacterGeneratorTest implements Enums {

   @Test
   public void specificFailure() {
      int seed = 288610733;
      String raceName = "Wolf";
      int maxPoints = 220;
      CombatServer.setPseudoRandomNumberSeed(seed);
      Character character = CharacterGenerator.generateRandomCharacter("? " + maxPoints + " "
                                                                       + raceName);
      assertTrue("generated " + character.getPointTotal()
                 + "-point character that should have only had " + maxPoints + " points from seed "
                 + seed, character.getPointTotal() <= maxPoints);
      assertTrue("generated " + character.getRace().getName()
                 + " character that should have been a " + raceName + ".",
                 character.getRace().getName().equals(raceName));
   }

   @Test
   public void testGenerateRandomCharacterString() {
      int seed = (int) System.currentTimeMillis();
      for (String raceName : Race.getRaceNames(true/* includeNPC */)) {
         Race race = Race.getRace(raceName);
         for (int pointAdj = -50; pointAdj < 300; pointAdj += (Math.random() * 100)) {
            int maxPoints = race.getCost() + pointAdj;
            String key = seed + " generated " + maxPoints + " point " + raceName;
            CombatServer.setPseudoRandomNumberSeed(seed);
            Character character = CharacterGenerator.generateRandomCharacter("? " + maxPoints + " "
                                                                             + raceName);
            assertTrue(key + " has too many points: " + character.getPointTotal(),
                       character.getPointTotal() <= maxPoints);
            assertTrue(key + " actually was a " + character.getRace().getName(),
                       character.getRace().getName().equals(raceName));
            if (race.hasProperty(Race.PROPERTIES_ANIMAL)) {
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
      Armor armor = Armor.getArmor(armorName);
      Shield shield = Shield.getShield(shieldName);
      Weapon weapon = Weapons.getWeapon(weaponName);
      byte nim = 4;
      byte str = 5;
      Character character = CharacterGenerator.generateRandomCharacter("? 200 Human STR:" + str
                                                                       + " NIM:" + nim + " "
                                                                       + armorName + " "
                                                                       + shieldName + " "
                                                                       + weaponName);
      character.setSkillLevel(weapon.getAttackStyle(0).getSkillName(), weaponSkillLevel);
      character.setSkillLevel(shield.getDefenseSkillTypes().get(0), shieldSkillLevel);
      assertTrue("Wrong strength", character.getAttribute(ATT_STR) == 5);
      assertTrue("Wrong armor", character.getArmor().getName().equals(armorName));
      assertTrue("Wrong shield", character.getLimb(APPENDAGE_HAND_LEFT).getHeldThing().getName().equals(shieldName));
      assertTrue("Wrong weapon", character.getLimb(APPENDAGE_HAND_RIGHT).getHeldThing().getName().equals(weaponName));
      byte pd = (byte) (armor.getPassiveDefense() + shield.getPassiveDefense());
      assertTrue("Wrong PD", character.getPassiveDefense(RANGE.OUT_OF_RANGE) == pd);
      // try off-hand parrys and blocks
      for (boolean isAmbidexterous = false; !isAmbidexterous; isAmbidexterous = true) {
         if (isAmbidexterous) {
            character.addAdvantage(Advantage.getAdvantage(Advantage.AMBIDEXTROUS));
         }
         for (boolean offHand = false; !offHand; offHand = true) {
            int offHandPenalty = 0;
            character.getLimb(APPENDAGE_HAND_LEFT).dropThing();
            character.getLimb(APPENDAGE_HAND_RIGHT).dropThing();
            if (offHand) {
               if (!isAmbidexterous)
                  offHandPenalty = -3;
               character.getLimb(APPENDAGE_HAND_LEFT).setHeldThing(weapon, character);
               character.getLimb(APPENDAGE_HAND_RIGHT).setHeldThing(shield, character);
            } else {
               character.getLimb(APPENDAGE_HAND_LEFT).setHeldThing(shield, character);
               character.getLimb(APPENDAGE_HAND_RIGHT).setHeldThing(weapon, character);
            }
            byte attackingWeaponsParryPen = 0;
            for (RANGE range : RANGE.values()) {
               boolean isRanged = (range != RANGE.OUT_OF_RANGE);
               byte rangeAdj = Rules.getRangeDefenseAdjustment(range);
               if (rangeAdj == -100) rangeAdj = 0;
               byte rangedPd = (byte) (pd + rangeAdj);
               byte pdAtRange = character.getPassiveDefense(range);

               byte effectiveDodge = (byte) (Rules.getDodgeLevel(nim) + rangeAdj);
               if (effectiveDodge < 0) {
                  effectiveDodge = 0;
               }
               byte effectiveRetreat = (byte) (Rules.getRetreatLevel(nim) + rangeAdj * 2);
               if (range != RANGE.OUT_OF_RANGE) {
                  effectiveRetreat = 0;
               }
               int effectiveBlock = shieldSkillLevel - offHandPenalty + rangeAdj;
               if (effectiveBlock < 0) {
                  effectiveBlock = 0;
               }
               int effectiveParry = weaponSkillLevel - offHandPenalty + rangeAdj;
               if ((range != RANGE.OUT_OF_RANGE) || (effectiveParry < 0)) {
                  effectiveParry = 0;
               }

               // check PD
               assertTrue("Wrong PD at range: " + RANGE_NAMES[range.ordinal()], pdAtRange == rangedPd);

               // check Dodge
               byte tn = character.getDefenseTN(DEF_DODGE, (byte) 0/* minDamage */,
                                                false/* includeWoundPenalty */,
                                                false/* includePosition */,
                                                false/* includeMassiveDamPen */,
                                                attackingWeaponsParryPen, isRanged,
                                                DAM_GENERAL, false/* defAppliedAlready */,
                                                range);
               assertTrue("Wrong Dodge TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveDodge));

               // check retreat
               tn = character.getDefenseTN(DEF_RETREAT, (byte) 0/* minDamage */,
                                           false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong Retreat TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveRetreat));

               // check block
               tn = character.getDefenseTN(DEF_LEFT, (byte) 0/* minDamage */,
                                           false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveBlock));

               // check block & dodge
               tn = character.getDefenseTN(DEF_LEFT | DEF_DODGE,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block/dodge TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveBlock + effectiveDodge));
               // check parry
               tn = character.getDefenseTN(DEF_RIGHT, (byte) 0/* minDamage */,
                                           false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong parry TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveParry));

               // check block & dodge
               tn = character.getDefenseTN(DEF_RIGHT | DEF_DODGE,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block/dodge TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveParry + effectiveDodge));

               // check block & parry
               tn = character.getDefenseTN(DEF_RIGHT | DEF_LEFT,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block/parry TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveParry + effectiveBlock));

               // check block, parry & dodge
               tn = character.getDefenseTN(DEF_RIGHT | DEF_LEFT | DEF_DODGE,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block/parry/dodge TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveParry + effectiveBlock + effectiveDodge));

               // check block & retreat
               tn = character.getDefenseTN(DEF_LEFT | DEF_RETREAT,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block/retreat TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveBlock + effectiveRetreat));

               // check parry & retreat
               tn = character.getDefenseTN(DEF_RIGHT | DEF_RETREAT,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong parry/retreat TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveParry + effectiveRetreat));

               // check block, parry & dodge
               tn = character.getDefenseTN(DEF_RIGHT | DEF_LEFT | DEF_DODGE,
                                           (byte) 0/* minDamage */, false/* includeWoundPenalty */,
                                           false/* includePosition */,
                                           false/* includeMassiveDamPen */,
                                           attackingWeaponsParryPen, isRanged, DAM_GENERAL,
                                           false/* defAppliedAlready */, range);
               assertTrue("Wrong block/parry/dodge TN at range: " + RANGE_NAMES[range.ordinal()],
                          tn == (rangedPd + effectiveParry + effectiveBlock + effectiveDodge));
            }
         }
      }
   }

}
