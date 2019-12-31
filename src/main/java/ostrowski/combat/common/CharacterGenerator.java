package ostrowski.combat.common;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.mage.MageCollege;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.CombatServer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;

public class CharacterGenerator implements Enums
{

   private static List<String> convertSourceIntoTokens(String fullSource) {
      List<String> tokens = new ArrayList<>();
      StringTokenizer st = new StringTokenizer(fullSource, " ");
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         if (token.startsWith("\"")) {
            StringBuilder sb = new StringBuilder(token.substring(1));
            while ((sb.charAt(sb.length() - 1) != '"') && st.hasMoreTokens()) {
               sb.append(" ").append(st.nextToken());
            }
            // remove the trailing '"'
            if (sb.charAt(sb.length() - 1) == '"') {
               sb.setLength(sb.length() - 1);
            }
            token = sb.toString();
         }
         if ((token.length() > 0) && (!token.equals("?")) && (!token.equals("-"))) { // ignore these
            tokens.add(token);
         }
      }
      return tokens;
   }

   // make a reference to the Rules object before we get into the static initializer, because that
   // makes a reference to the PriestSpells, which need the Rules object to already have been loaded.
   static final Attribute at = Attribute.Strength;// force load of this enum
   static final String _dummy = Rules.diagCompName;

   static public Character generateRandomCharacter(String fullSource, Arena arena, boolean printCharacter) {
      List<String> tokens = convertSourceIntoTokens(fullSource);
      int points = 200;
      String raceName = Race.NAME_Human;
      List<String> raceNames = Race.getRaceNames(true);
      boolean raceNameFound = false;

      for (String token : tokens) {
         for (String realRaceName : raceNames) {
            if (realRaceName.equalsIgnoreCase(token)) {
               raceName = realRaceName;
               tokens.remove(token);
               raceNameFound = true;
               break;
            }
         }
         if (raceNameFound) {
            break;
         }
      }
      if (!raceNameFound) {
         for (String race : raceNames) {
            // races without spaces would have already been found as a single token.
            if (race.indexOf(' ') != -1) {
               int index = fullSource.indexOf(race);
               if (index != -1) {
                  String sourceWithoutRaceName = fullSource.substring(0, index).trim() + " " + fullSource.substring(index + race.length()).trim();
                  tokens = convertSourceIntoTokens(sourceWithoutRaceName);
                  raceName = race;
                  raceNameFound = true;
                  break;
               }
            }
         }
      }

      for (String token : tokens) {
         try {
            points = Integer.parseInt(token);
            if (token.equals(String.valueOf(points))) {
               tokens.remove(token);
               break;
            }
         } catch (NumberFormatException e) {
         }
      }
      return CharacterGenerator.generateRandomCharacter(points, raceName, tokens, false/*genNewPseudoRndNumber*/, arena, printCharacter);
   }

   static public Character generateRandomCharacter(int points, String raceName, String extraData, boolean genNewPseudoRndNumber, boolean printCharacter) {
      return generateRandomCharacter(points, raceName, convertSourceIntoTokens(extraData), genNewPseudoRndNumber, null/*arena*/, printCharacter);
   }


   static final HashMap<String, List<SkillType>> DISSALLOWED_SKILLS_FOR_RACE = new HashMap<>();
   static {
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Kobold,   Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Fairy,    Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Goblin,   Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Fawn,     Arrays.asList(SkillType.Aikido));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Ellyon,   Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Orc,      Arrays.asList(SkillType.Aikido));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Gargoyle, Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_InsectMan,Arrays.asList(SkillType.Aikido));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Centaur,  Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_HalfOgre, Arrays.asList(SkillType.Aikido, SkillType.Karate));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Ogre,     Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Troll,    Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Minotaur, Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Cyclops,  Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Giant,    Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Skeleton, Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Zombie,   Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
   }

   static public Character generateRandomCharacter(int points, String raceName, List<String> tokens,
                                                   boolean genNewPseudoRndNumber, Arena arena, boolean printCharacter) {
      int pointsLeft = points;
      Character character = new Character();

      Integer pseudoRndNumberToUse = null;
      String seedKey = "rnd:";
      for (String token : tokens) {
         try {
            int seed = -1;
            if (token.toLowerCase().startsWith(seedKey)) {
               seed = Integer.parseInt(token.substring(seedKey.length()));
            }
            if (token.equals("rnd:" + seed)) {
               tokens.remove(token);
               pseudoRndNumberToUse = seed;
               break;
            }
         } catch (NumberFormatException e) {
         }
      }

      if (genNewPseudoRndNumber && (pseudoRndNumberToUse == null)) {
         CombatServer.generateNewPseudoRandomNumberSeed();
         pseudoRndNumberToUse = CombatServer.getPseudoRandomNumberSeed();
      }
      if (pseudoRndNumberToUse!= null) {
         //Rules.diag("generating character with pseudoRandom seed " + pseudoRndNumberToUse);
         //         seed = 767;
         if (pseudoRndNumberToUse != CombatServer.getPseudoRandomNumberSeed()) {
            CombatServer.setPseudoRandomNumberSeed(pseudoRndNumberToUse);
         }
      }
      if (raceName == null) {
         raceName = Race.NAME_Human;
      }
      Gender gender = null;
      // See is the gender was specified as a token
      List<Gender> genders = Race.getGendersForRace(raceName);
      for (Gender g : genders) {
         for (String token : tokens) {
            if (token.equalsIgnoreCase(g._name)) {
               gender = g;
               tokens.remove(token);
               break;
            }
         }
         if (gender != null) {
            break;
         }
      }
      // Check for random missile weapon
      boolean missileWeapon = false;
      for (String token : tokens) {
         if (token.equalsIgnoreCase("missile")) {
            tokens.remove(token);
            missileWeapon = true;
            break;
         }
      }
      if (gender == null) {
         // if not specified, default to 'Male'
         gender = Gender.MALE;
         // 7% percent of the time, randomly assign the gender, excluding Male.
         double rnd = CombatServer.random();
         if (rnd > .93) {
            // exclude MALE from the remaining set to choose from
            genders.remove(gender);
            if (genders.size() > 0) {
               // if this race has non-males, pick any random non-male gender:
               gender = genders.get((int) (CombatServer.random() * genders.size()));
            }
         }
      }
      character.setRace(raceName, gender);

      boolean noEquip = false;
      boolean weaponSheathed = false;
      List<Weapon> weapons = new ArrayList<>();
      Armor requiredArmor = null;
      Shield requiredShield = null;
      int maxExpenditure = -1;
      HashMap<String, Integer> requiredAttributes = new HashMap<>();
      HashMap<SkillType, Byte> requiredSkills = new HashMap<>();
      String requiredName = null;

      while (tokens.size() > 0) {
         String token = tokens.remove(0);
         String twoTokens = (tokens.size() < 1) ? null : token + " " + tokens.get(0);
         String threeTokens = (tokens.size() < 2) ? null : twoTokens + " " + tokens.get(1);
         Thing thing = Thing.getThing(token, false/*allowTool*/, character.getRace());
         if (thing == null) {
            if (twoTokens != null) {
               thing = Thing.getThing(twoTokens, false/*allowTool*/, character.getRace());
               if (thing != null) {
                  // If we used the second token to make a thing, consume the token
                  tokens.remove(0);
               }
               else {
                  if (threeTokens != null) {
                     thing = Thing.getThing(threeTokens, false/*allowTool*/, character.getRace());
                     if (thing != null) {
                        // If we used the second and third tokens to make a thing, consume the tokens
                        tokens.remove(0);
                        tokens.remove(0);
                     }
                  }
               }
            }
         }
         if (thing == null) {
            // What else could this be?
            Advantage adv = Advantage.getAdvantage(token);
            if (adv == null) {
               adv = Advantage.getAdvantage(twoTokens);
               if (adv != null) {
                  // If we used the second token to make an advantage, consume the token
                  tokens.remove(0);
               }
               if (adv == null) {
                  adv = Advantage.getAdvantage(threeTokens);
                  if (adv != null) {
                     // If we used the second and third tokens to make an advantage, consume the tokens
                     tokens.remove(0);
                     tokens.remove(0);
                  }
               }
            }
            if (adv != null) {
               character.addAdvantage(adv);
               if (adv.getName().startsWith(Advantage.WEALTH)) {
                  String wealthLevel = adv.getLevelNames().get(adv.getLevel());
                  wealthLevel = wealthLevel.substring(1).replaceAll(",", "");// remove the '$' and all commas
                  maxExpenditure = Integer.parseInt(wealthLevel);
                  if (maxExpenditure == 0) {
                     noEquip = true;
                  }
                  maxExpenditure *= character.getRace().getWealthMultiplier();
               }
            }
            else {
               // Allow specific attribute levels
               int indexOfColon = token.indexOf(':');
               if (indexOfColon != -1) {
                  String name = token.substring(0, indexOfColon);
                  String value = token.substring(indexOfColon + 1);
                  if (name.equalsIgnoreCase("team")) {
                     for (byte teamID = 0; teamID < TEAM_NAMES.length; teamID++) {
                        if (value.equalsIgnoreCase(TEAM_NAMES[teamID])) {
                           character._teamID = teamID;
                           break;
                        }
                     }
                  }
                  else if (name.equalsIgnoreCase("name")) {
                     requiredName = value;
                  }
                  else {
                     boolean attrFound = false;
                     for (Attribute att : Attribute.values()) {
                        if (name.equalsIgnoreCase(att.shortName)) {
                           Integer level = Integer.parseInt(value);
                           requiredAttributes.put(att.shortName, level);
                           character.setAttribute(att, level.byteValue(), true/*containInLimits*/);
                           attrFound = true;
                           break;
                        }
                     }
                     if (!attrFound) {
                        SkillType skillType = SkillType.getSkillTypeByName(name);
                        if (skillType != null) {
                           Byte level = Byte.parseByte(value);
                           requiredSkills.put(skillType, level);
                        }
                        else {
                           MageSpell mageSpell = MageSpell.getSpell(name);
                           if (mageSpell != null) {
                              byte level = Byte.parseByte(value);
                              mageSpell.setFamiliarity(MageSpell.FAM_KNOWN);
                              character.setSpellLevel(mageSpell.getName(), mageSpell.getLevel());
                              Class<? extends MageSpell>[] requiredSpells = mageSpell._prerequisiteSpells;
                              for (Class<? extends MageSpell> preReq : requiredSpells) {
                                 try {
                                    MageSpell preReqSpell = preReq.getDeclaredConstructor().newInstance();
                                    character.setSpellLevel(preReqSpell.getName(), mageSpell.getLevel());
                                 } catch (InstantiationException |
                                          IllegalAccessException |
                                          IllegalArgumentException |
                                          InvocationTargetException |
                                          NoSuchMethodException |
                                          SecurityException e) {
                                    e.printStackTrace();
                                 }
                              }
                              for (MageCollege college : mageSpell._prerequisiteColleges) {
                                 if (level > character.getCollegeLevel(college.getName())) {
                                    character.setCollegeLevel(college.getName(), level);
                                 }
                              }
                           }
                           else {
                              // What else could this be? It has a colon (':') in it
                              // It could be a key ("key:red door")
                              character.addEquipment(Thing.getThing(token, character.getRace()));
                           }
                        }
                     }
                  }
               }
               else {
                  if (token.equalsIgnoreCase("sheathed")) {
                     weaponSheathed = true;
                  }
                  else {
                     // TODO: allow classes? (priest, mage, archer, tank)
                     character.addEquipment(Thing.getThing(token, character.getRace()));
                  }
               }
            }
         }
         if (thing != null) {
            if (thing instanceof Armor) {
               requiredArmor = (Armor) thing;
            }
            else if (thing instanceof Shield) {
               requiredShield = (Shield) thing;
            }
            else if (thing instanceof Weapon) {
               weapons.add((Weapon) thing);
            }
            else {
               if (thing instanceof Potion) {
                  // If we are carrying a beneficial potion (but not a healing potion)
                  // then we should start without a weapon readied, so we can apply the
                  // potion first, and then ready the weapon afterwards.
                  Potion potion = (Potion) thing;
                  if (potion.isBeneficial() && !potion.isHealing()) {
                     weaponSheathed = true;
                  }
               }
               // Must be a non-weapon things like a potion or key
               character.addEquipment(thing);
            }
         }
      }
      if (maxExpenditure == -1) {
         // allow the character to go up a max of 3 levels
         maxExpenditure = (int)(5000 * character.getRace().getWealthMultiplier());
      }

      if ((requiredArmor != null) || (requiredShield != null)) {
         // Set the armor & shield now, so the points required to pay for this are figured in.
         if (requiredArmor != null) {
            character.setArmor(requiredArmor.getName());
         }
         if (requiredShield != null) {
            character.getLimb(LimbType.HAND_LEFT).setHeldThing(requiredShield, character);
         }
         character.computeWealth();
      }
      if (character.isAnimal()) {
         noEquip = true;
      }

      pointsLeft = pointsLeft - character.getPointTotal();

      Weapon primaryWeapon = null;
      // override with specified weapon:
      if (weapons.size() > 0) {
         primaryWeapon = weapons.remove(0);
      }

      if (requiredAttributes.get(Attribute.Strength.shortName) == null) {
         byte attStr;
         if (primaryWeapon != null) {
            attStr = getBestStrengthForWeapon(primaryWeapon, points, pointsLeft);
         }
         else {
            attStr = getAttributeLevel(points, pointsLeft);
         }
         character.setAttribute(Attribute.Strength, attStr, true/*containInLimits*/);
         pointsLeft = points - character.getPointTotal();
      }
      // set all the required skill levels:
      for (SkillType skillType : requiredSkills.keySet()) {
         character.setSkillLevel(skillType, requiredSkills.get(skillType));
      }
      pointsLeft = points - character.getPointTotal();

      if (noEquip) {
         primaryWeapon = Weapon.getWeapon(Weapon.NAME_Punch, character.getRace());
         requiredArmor = Armor.getArmor(Armor.NAME_NoArmor, character.getRace());
         requiredShield = Shield.getShield(Shield.NAME_None, character.getRace());
      }
      else {
         if (primaryWeapon == null) {
            primaryWeapon = getBestWeaponForStrength(character.getAttributeLevel(Attribute.Strength), missileWeapon, (requiredShield == null)/*allowTwoHanded*/,
                                                     maxExpenditure, character.getRace());
         }
      }

      Hand rightHand = (Hand) character.getLimb(LimbType.HAND_RIGHT);
      if (rightHand != null) {
         if (character.isAnimal()) {
            primaryWeapon = rightHand.getWeapon(character);
         }
         else {
            if (!primaryWeapon.getName().equals(Weapon.NAME_KarateKick)) {
               if (weaponSheathed) {
                  character.addEquipment(primaryWeapon);
               }
               else {
                  rightHand.setHeldThing(primaryWeapon, character);
               }
            }
         }
      }
      else {
         Head head = (Head) character.getLimb(LimbType.HEAD);
         primaryWeapon = head.getWeapon(character);
      }
      if (primaryWeapon == null) {
         primaryWeapon = Weapon.getWeapon(Weapon.NAME_Punch, character.getRace());
      }

      Skill weaponSkill = new Skill(primaryWeapon.getAttackStyle(0).getSkillType(), (byte)0);
      byte shieldLevel = 0;
      boolean usingAsOneHanded = (primaryWeapon.getAttackStyle(0)._handsRequired == 1);
      if (usingAsOneHanded) {
         // Bastard swords and Katanas should only be used one-handed if we have a STR greater than the fast STR for 1-handed
         WeaponStyleAttack oneHandedSwing = null;
         WeaponStyleAttack twoHandedSwing = null;
         for (WeaponStyleAttack style : primaryWeapon.getAttackStyles()) {
            if (style.getAttackType() == AttackType.SWING) {
               if (style._handsRequired == 1) {
                  oneHandedSwing = style;
               }
               else {
                  twoHandedSwing = style;
               }
            }
         }
         if ((oneHandedSwing != null) && (twoHandedSwing != null)) {
            byte str = character.getAttributeLevel(Attribute.Strength);
            // If we are strong enough to use this one handed, do it
            // If we are not strong enough to use this quickly one handed, but are not strong enough to use
            // it two handed quickly either, then use it one handed, as long as we are strong enough to not be slow:
            if (str >= oneHandedSwing.getFastStr()) {
               usingAsOneHanded = true;
            }
            else usingAsOneHanded = (str < twoHandedSwing.getFastStr()) && (str > oneHandedSwing.getSlowStr());
            if (!usingAsOneHanded) {
               weaponSkill = new Skill(twoHandedSwing.getSkillType(), (byte)0);
            }
         }
      }
      boolean fourHands = (character.getRace().getArmCount() > 2);
      if (fourHands) {
         if (usingAsOneHanded) {
            // If we have four (or more) arms, put the same kind of weapon in our second pair:
            if (!primaryWeapon.isOnlyTwoHanded()) {
               Hand leftHand = (Hand) character.getLimb(LimbType.HAND_LEFT);
               if (leftHand != null) {
                  leftHand.setHeldThing(Weapon.getWeapon(primaryWeapon.getName(), character.getRace()), character);
               }
            }
         }
      }

      Byte reqSkillLevel = requiredSkills.get(weaponSkill.getType());
      byte weaponLevel = getSkillLevel(points, pointsLeft);
      if (reqSkillLevel != null) {
         weaponLevel = reqSkillLevel;
      }
      weaponSkill.setLevel(weaponLevel);
      character.setSkillLevel(weaponSkill.getType(), weaponLevel);
      pointsLeft = points - character.getPointTotal();

      if ((usingAsOneHanded || fourHands) && !noEquip) {
         shieldLevel = getSkillLevel(points, pointsLeft);
         Byte reqShieldLevel = requiredSkills.get(SkillType.Shield);
         if (reqShieldLevel != null) {
            shieldLevel = reqShieldLevel;
         }
         character.setSkillLevel(SkillType.Shield, shieldLevel);
         pointsLeft = points - character.getPointTotal();
      }
      // If we are using a two-handed weapon, and it has a 1-handed skill, we should learn that too:
      if (primaryWeapon.isReal() && !usingAsOneHanded) {
         // We should be able to use this weapon in all of its secondary attack styles (at a lower skill level):
         for (WeaponStyleAttack attackStyle : primaryWeapon.getAttackStyles()) {
            SkillType attackStyleSkillType = attackStyle.getSkillType();
            if (character.getSkillLevel(attackStyleSkillType, null/*useHand*/, false/*sizeAdjust*/, false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0) {
               byte desiredSecondaryLevel = getSkillLevel(points / 4, pointsLeft / 4);
               Byte requiredSecondaryLevel = requiredSkills.get(attackStyleSkillType);
               if (requiredSecondaryLevel != null) {
                  desiredSecondaryLevel = requiredSecondaryLevel;
               }
               else {
                  // Don't let the secondary weapon level be higher than the primary weapon level.
                  desiredSecondaryLevel = (byte) Math.min(desiredSecondaryLevel, weaponLevel);
               }
               character.setSkillLevel(attackStyleSkillType, desiredSecondaryLevel);
               pointsLeft = points - character.getPointTotal();
            }
         }
      }
      byte desiredBrawlingLevel = getSkillLevel(points / 3, pointsLeft / 3);
      SkillType unarmedCombatType = null;
      if (requiredSkills.get(SkillType.Karate) != null) {
         unarmedCombatType = SkillType.Karate;
         desiredBrawlingLevel = requiredSkills.get(SkillType.Karate);
      }
      else if (requiredSkills.get(SkillType.Boxing) != null) {
         unarmedCombatType = SkillType.Boxing;
         desiredBrawlingLevel = requiredSkills.get(SkillType.Boxing);
      }
      else if (requiredSkills.get(SkillType.Aikido) != null) {
         unarmedCombatType = SkillType.Aikido;
         desiredBrawlingLevel = requiredSkills.get(SkillType.Aikido);
      }
      else if (requiredSkills.get(SkillType.Brawling) != null) {
         unarmedCombatType = SkillType.Brawling;
         desiredBrawlingLevel = requiredSkills.get(SkillType.Brawling);
      }
      else if (requiredSkills.get(SkillType.Wrestling) != null) {
         unarmedCombatType = SkillType.Wrestling;
         desiredBrawlingLevel = requiredSkills.get(SkillType.Wrestling);
      }
      else if (character.getRace().hasProperty(Race.PROPERTIES_HORNS)) {
         unarmedCombatType = SkillType.Brawling;
      }
      double unarmedCombatChance = Math.max(0d, Math.min(95d, points));
      if (primaryWeapon.isUnarmedStyle()) {
         unarmedCombatChance = unarmedCombatChance/5;
      }
      if (!character.isAnimal() && (unarmedCombatChance > (CombatServer.random() * 100))) {
         boolean forced = false;
         while (!forced && (unarmedCombatType == null)) {

            // It's possible that we chose Karate (or another offensive combat style) as our primary weapon.
            // If so, get a secondary unarmed combat skill, since this character is an unarmed combat practitioner.
            if (weaponSkill.getName().equals(SkillType.Karate.getName())) {
               unarmedCombatType = SkillType.Aikido;
               forced = true;
            }
            else if (weaponSkill.getName().equals(SkillType.Boxing.getName())) {
               unarmedCombatType = SkillType.Wrestling;
               forced = true;
            }
            else {
               double rnd = CombatServer.random();
               if (rnd < .10) {
                  unarmedCombatType = SkillType.Aikido; // 10%
               }
               else if (rnd < .25) {
                  unarmedCombatType = SkillType.Karate; // 15%
               }
               else if (rnd < .45) {
                  unarmedCombatType = SkillType.Boxing; // 20%
               }
               else if (rnd < .70) {
                  unarmedCombatType = SkillType.Wrestling;// 25%
               }
               else {
                  unarmedCombatType = SkillType.Brawling; // 30%
               }
            }
            List<SkillType> dissallowedSkillsForRace = DISSALLOWED_SKILLS_FOR_RACE.get(character.getRace().getName());
            if ((dissallowedSkillsForRace != null) && (dissallowedSkillsForRace.contains(unarmedCombatType))) {
               // try again until we find a skill we're allowed
               unarmedCombatType = null;
               // If our primary weapon was an unarmed style, forced will be true, and
               // we will exit the loop with no unarmed Combat Type.
            }
         }
         if ((unarmedCombatType != null) && (desiredBrawlingLevel > 0)) {
            Byte reqUnarmedCombatLevel = requiredSkills.get(unarmedCombatType);
            if (reqUnarmedCombatLevel != null) {
               desiredBrawlingLevel = reqUnarmedCombatLevel;
            }
            character.setSkillLevel(unarmedCombatType, desiredBrawlingLevel);
            pointsLeft = points - character.getPointTotal();
         }
      }

      boolean throwWeapon = false;
      if (primaryWeapon.isThrowable()) {
         double throwSkillChance = Math.max(0d, Math.min(95d, points / 2d));
         throwWeapon = (throwSkillChance > (CombatServer.random() * 100));
         if (missileWeapon) {
            throwWeapon = true;
            // Have at least one additional throwable weapon
            character.addEquipment(Weapon.getWeapon(primaryWeapon.getName(), character.getRace()));
         }
         if (throwWeapon) {
            byte desiredThrowLevel = getSkillLevel(points / 5, pointsLeft / 5);
            character.setSkillLevel(SkillType.Throwing, desiredThrowLevel);
         }
      }

      Attribute[] atts = new Attribute[] { Attribute.Dexterity, Attribute.Nimbleness, Attribute.Health, Attribute.Toughness};
      for (Attribute attr : atts) {
         if (requiredAttributes.get(attr.shortName) == null) {
            byte attLevel = getAttributeLevel(points, pointsLeft);
            attLevel += character.getRace().getAttributeMods(attr);
            character.setAttribute(attr, attLevel, true/*containInLimits*/);

            // Nimbleness should try to be an even number, to maximize the dodge score.
            if ((attr == Attribute.Nimbleness) && ((attLevel % 2) == 1)) {
               int diffCost = Rules.getAttCost((byte) (attLevel + 1)) - Rules.getAttCost(attLevel);
               if (pointsLeft > diffCost) {
                  character.setAttribute(Attribute.Nimbleness, ++attLevel, true/*containInLimits*/);
               }
            }
            pointsLeft = points - character.getPointTotal();
         }
      }
      // Half the time, make sure that DEX is at least as high as NIM
      // The other half of the time, make sure its at least as high as HT
      if (requiredAttributes.get(Attribute.Dexterity.shortName) == null) {
         boolean checkNim = false;
         boolean checkHt = false;
         // if we can adjust both NIM and HT, randomly consider either one:
         if ((requiredAttributes.get(Attribute.Nimbleness.shortName) == null) && (requiredAttributes.get(Attribute.Health.shortName) == null)) {
            checkNim = (CombatServer.random() > .5);
            checkHt = !checkNim;
         }
         else if (requiredAttributes.get(Attribute.Nimbleness.shortName) == null) {
            checkNim = true;
            checkHt = false;
         }
         else if (requiredAttributes.get(Attribute.Health.shortName) == null) {
            checkNim = false;
            checkHt = true;
         }

         if (checkNim) {
            byte dex = (byte) (character.getAttributeLevel(Attribute.Dexterity) - character.getRace().getAttributeMods(Attribute.Dexterity));
            byte nim = (byte) (character.getAttributeLevel(Attribute.Nimbleness) - character.getRace().getAttributeMods(Attribute.Nimbleness));
            if (dex < nim) {
               // switch DEX and NIM
               byte newDex = (byte) (nim + character.getRace().getAttributeMods(Attribute.Dexterity));
               byte newNim = (byte) (dex + character.getRace().getAttributeMods(Attribute.Nimbleness));
               // keep the new NIM even:
               if ((newNim % 2) == 1) {
                  newNim++; // now newNim should be even
                  newDex--; // offset the points we had to spend to increase our the NIM
               }
               character.setAttribute(Attribute.Dexterity, newDex, false/*limitRange*/);
               character.setAttribute(Attribute.Nimbleness, newNim, false/*limitRange*/);
            }
         }
         if (checkHt) {
            byte dex = (byte) (character.getAttributeLevel(Attribute.Dexterity) - character.getRace().getAttributeMods(Attribute.Dexterity));
            byte ht = (byte) (character.getAttributeLevel(Attribute.Health) - character.getRace().getAttributeMods(Attribute.Health));
            if (dex < ht) {
               // switch DEX and HT
               byte newDex = (byte) (ht + character.getRace().getAttributeMods(Attribute.Dexterity));
               byte newHt = (byte) (dex + character.getRace().getAttributeMods(Attribute.Health));
               character.setAttribute(Attribute.Dexterity, newDex, false/*limitRange*/);
               character.setAttribute(Attribute.Health, newHt, false/*limitRange*/);
            }
         }
      }

      byte desiredEncLevel = Rules.getEncumbranceLevel(character);
      // determine best armor.
      if (!noEquip && ((requiredArmor == null) || (requiredShield == null))) {
         double rnd = CombatServer.random();
         if (missileWeapon) {
            // ranged attackers should not be heavily encumbered.
            rnd = rnd / 2.0;
         }
         if (rnd > .10) {
            desiredEncLevel++;
            if (rnd > .65) {
               desiredEncLevel++;
               if (rnd > .85) {
                  desiredEncLevel++;
               }
            }
         }
         // if we are using a weapon (like karate or boxing) that is adversely affected by encumbrance,
         // keep our encumbrance below 2.
         Skill primarySkill = character.getBestSkill(primaryWeapon);
         if (primarySkill.isAdjustedForEncumbrance()) {
            if (desiredEncLevel > 1) {
               desiredEncLevel = 1;
            }
         }
         computeBestArmorAndShield(character, shieldLevel, desiredEncLevel, requiredArmor, requiredShield, maxExpenditure);
      }

      double weightCarried = character.getWeightCarried();
      byte adjStrength = character.getAdjustedStrength();
      byte nimbleness = character.getAttributeLevel(Attribute.Nimbleness);
      double maxWeightCarried = Rules.getMaxWeightForEncLevel(adjStrength, nimbleness, desiredEncLevel);
      double weightAvailable = maxWeightCarried - weightCarried;
      double haveKnifeChance;
      if (character.hasAdvantage("Wealth: $0") || character.hasAdvantage(Race.PROPERTIES_CLAWS) || character.isAnimal()) {
         haveKnifeChance = 0d;
      }
      else {
         haveKnifeChance = Math.max(0d, Math.min(95d, points));
      }

      boolean hasKnife = (primaryWeapon.getName() == Weapon.NAME_Knife);
      if (haveKnifeChance > (CombatServer.random() * 100)) {
         Weapon knife = Weapon.getWeapon(Weapon.NAME_Knife, character.getRace());
         if (!hasKnife) {
            if (weightAvailable > knife.getAdjustedWeight()) {
               if ((unarmedCombatType == null) || (pointsLeft > 6)) {
                  weightCarried += knife.getWeight();
                  weightAvailable = maxWeightCarried - weightCarried;
                  character.addEquipment(knife);
                  byte desiredKnifeLevel = getSkillLevel(points / 5, pointsLeft / 5);
                  // Don't let the knife level be higher than the primary weapon level.
                  desiredKnifeLevel = (byte) Math.min(desiredKnifeLevel, weaponLevel);
                  Byte reqKnifeLevel = requiredSkills.get(knife.getAttackStyle(0).getSkillType());
                  if (reqKnifeLevel != null) {
                     desiredKnifeLevel = reqKnifeLevel;
                  }
                  character.setSkillLevel(knife.getAttackStyle(0).getSkillType(), desiredKnifeLevel);

                  if (!throwWeapon) {
                     double throwSkillChance = Math.max(0d, Math.min(95d, points / 3d));
                     throwWeapon = (throwSkillChance > (CombatServer.random() * 100));
                     if (throwWeapon) {
                        byte desiredThrowLevel = getSkillLevel(points / 5, pointsLeft / 5);
                        character.setSkillLevel(SkillType.Throwing, desiredThrowLevel);
                     }
                  }

               }
            }
         }
      }
      // Consider a second throwable weapon if they know how to throw it.
      if (throwWeapon) {
         Weapon throwableWeapon = null;
         character.getEquipment();
         if (primaryWeapon.isThrowable()) {
            if (weightAvailable > primaryWeapon.getAdjustedWeight()) {
               throwableWeapon = Weapon.getWeapon(primaryWeapon.getName(), character.getRace());
            }
         }
         if (throwableWeapon == null) {
            // Consider a knife, if they know how to use it
            if (character.getSkill(SkillType.Knife) != null) {
               Weapon knife = Weapon.getWeapon(Weapon.NAME_Knife, character.getRace());
               if (weightAvailable > knife.getAdjustedWeight()) {
                  throwableWeapon = knife;
               }
            }
         }
         if (throwableWeapon != null) {
            weightCarried += throwableWeapon.getWeight();
            weightAvailable = maxWeightCarried - weightCarried;
            character.addEquipment(throwableWeapon);
         }
      }
      character.computeWealth();
      checkSkillLevels(character, primaryWeapon, points);

      pointsLeft = points - character.getPointTotal();

      while (pointsLeft != 0) {
         Attribute attChanged = null;
         if (pointsLeft < 0) {
            double rnd = CombatServer.random();
            String[] order;
            if (rnd < .04) {
               order = new String[] { "adv", "att", "skl", "at+"}; // 8% of the time, try to reduce the advantages first
            }
            else if (rnd < .08) {
               order = new String[] { "adv", "skl", "att", "at+"};
            }
            else if (rnd < .12) {
               order = new String[] { "skl", "adv", "att", "at+"}; // 8% of the time, try to reduce the skills first
            }
            else if (rnd < .16) {
               order = new String[] { "skl", "att", "adv", "at+"};
            }
            else if (rnd < .58) {
               order = new String[] { "att", "skl", "adv", "at+"}; // 84% of the time, try to reduce the attributes.
            }
            else {
               order = new String[] { "att", "adv", "skl", "at+"};
            }
            boolean reduced = false;
            for (String element : order) {
               if (element.equals("att")) {
                  attChanged = reduceCharacterAttributes(character, false, requiredAttributes);
                  reduced = (attChanged != null);
               }
               if (element.equals("at+")) {
                  attChanged = reduceCharacterAttributes(character, true, requiredAttributes);
                  reduced = (attChanged != null);
               }
               if (element.equals("adv") && !character.isAnimal()) {
                  reduced = reduceCharacterAdvantages(character);
               }
               if (element.equals("skl")) {
                  reduced = reduceCharacterSkillPoints(character, pointsLeft, requiredSkills);
               }
               if (reduced) {
                  break;
               }
            }
            if (!reduced) {
               DebugBreak.debugBreak("unable to reduce points enough to meet requirements");
               break;
            }
         }
         else {
            attChanged = increaseCharacterPoints(character, pointsLeft, (byte) -1, (byte) -1, requiredAttributes);
            // If we can't increase any attribute, try increasing a skill level.
            if (attChanged == null) {
               if (!increaseCharacterSkillPoints(character, pointsLeft, requiredSkills)) {
                  // If we can't even increase a skill level, then we are done.
                  break;
               }
            }
         }
         if ((attChanged == Attribute.Nimbleness) || (attChanged == Attribute.Strength)) {
            // recompute our best armor
            computeBestArmorAndShield(character, shieldLevel, desiredEncLevel, requiredArmor, requiredShield, maxExpenditure);
            // If our armor changed, recompute our required wealth level
            character.computeWealth();
            // This might cause us to go back negative again.
         }
         checkSkillLevels(character, primaryWeapon, points);
         pointsLeft = points - character.getPointTotal();
      }

      character.computeWealth();
      pointsLeft = points - character.getPointTotal();
      // if we have any weight and money left over, buy some potions:
      {
         adjStrength = character.getAdjustedStrength();
         nimbleness = character.getAttributeLevel(Attribute.Nimbleness);
         maxWeightCarried = Rules.getMaxWeightForEncLevel(adjStrength, nimbleness, desiredEncLevel);
         character.computeWealth();
         Advantage wealth = character.getAdvantage(Advantage.WEALTH);
         // adjust for the wealth multiplier for the character's race:
         float wealthMultiplier = character.getRace().getWealthMultiplier();

         // allow the character to go up a max of 3 levels
         String baseWealth = wealth.getLevelName();
         int racialWealthValue = Integer.parseInt(baseWealth.substring(1).replaceAll(",", ""));// remove the '$' and all commas
         maxExpenditure = Math.round(racialWealthValue * wealthMultiplier);

         weightAvailable = maxWeightCarried - weightCarried;
         int allowableExpenditure = maxExpenditure - character.getTotalCost();
         List<Potion> desiredPotions = new ArrayList<>();
         desiredPotions.add(Potion.getPotion(Potion.POTION_FULL_HEALING, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_SPEED, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_MAJOR_HEALING, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_INCR_STRENGTH, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_HEALING, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_INCR_DEXTERITY, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_MAJOR_HEALING, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_INCR_NIMBLENESS, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_HEALING, character.getRace()));
         desiredPotions.add(Potion.getPotion(Potion.POTION_MINOR_HEALING, character.getRace()));

         while ((weightAvailable > 1) && (allowableExpenditure > 250) && (desiredPotions.size() > 0)) {
            Potion potion = desiredPotions.remove(0);
            if ((potion._weight > weightAvailable) || (potion._cost > allowableExpenditure)) {
               continue;
            }
            character.addEquipment(potion);
            weightAvailable -= potion._weight;
            allowableExpenditure -= potion._cost;
         }
      }

      if (requiredName != null) {
         character.setName(requiredName);
      }
      else {
         if (character.isAnimal() || character.hasAdvantage(Advantage.UNDEAD)) {
            Integer count = generatedAnimalsCount.get(raceName);
            if (count == null) {
               count = 0;
            }
            character.setName(raceName + "-" + count);
            generatedAnimalsCount.put(raceName, count + 1);
         }
         else {
            character.setName(getName(arena, character.getGender() == Gender.MALE));
         }
      }
      if (printCharacter) {
         Rules.diag("Character generated: " + character);
      }
      return character;
   }

   private static void checkSkillLevels(Character character, Weapon primaryWeapon, int maxCharPoints) {
      Skill primarySkill = character.getBestSkill(primaryWeapon);
      Skill shieldSkill  = character.getSkill(SkillType.Shield);
      List<Skill> skills = character.getSkillsList();
      for (Skill skill : skills) {
         boolean isPrimary = (skill.getName().equals(primarySkill.getName()));
         if (skill.isAdjustedForEncumbrance()) {
            byte encLevel = Rules.getEncumbranceLevel(character);

            String skillName = skill.getType().getName();
            int minSkillPenalty = 0;
            // Wrestling can't attack without a -2 penalty,
            // and Karate needs a penalty of -2 to kick, so to make it worth more than boxing, use 2
            if (skillName.equals(SkillType.Wrestling.getName()) ||
                skillName.equals(SkillType.Karate.getName())) {
               minSkillPenalty = 2;
            }
            byte requiredLevel  = (byte) (encLevel + minSkillPenalty + 1);

            if (requiredLevel > skill.getLevel()) {
               // Can we increase the skill level to make this work?
               int curCost = Rules.getSkillCost(skill.getLevel());
               int reqCost = Rules.getSkillCost(requiredLevel);
               // If this is our primary skill, we absolutely MUST increase its level to make it work.
               if (isPrimary || ((reqCost - curCost) < (maxCharPoints - character.getPointTotal()))) {
                  skill.setLevel(requiredLevel);
                  // recurse to look for more
                  checkSkillLevels(character, primaryWeapon, maxCharPoints);
                  return;
               }
               // Get rid of this skill, we can't afford to have it
               character.setSkillLevel(skill.getType(), (byte)0);
               // recurse to look for more
               checkSkillLevels(character, primaryWeapon, maxCharPoints);
               return;
            }
         }
         // Make sure no skill is higher than the primary skill
         if (!isPrimary) {
            if (skill.getLevel() > primarySkill.getLevel()) {
               skill.setLevel((byte) (skill.getLevel() - 1));
               primarySkill.setLevel((byte) (primarySkill.getLevel() + 1));
               // recurse to look for more
               checkSkillLevels(character, primaryWeapon, maxCharPoints);
               return;
            }
         }
         // Make sure no skill other than the primary is higher than the shield
         if ((shieldSkill != null) && (shieldSkill.getLevel() > 0)) {
            if (!skill.getName().equals(shieldSkill.getName())) {
               if (!skill.getName().equals(primarySkill.getName())) {
                  if (skill.getLevel() > shieldSkill.getLevel()) {
                     skill.setLevel((byte) (skill.getLevel() - 1));
                     shieldSkill.setLevel((byte) (shieldSkill.getLevel() + 1));
                     // recurse to look for more
                     checkSkillLevels(character, primaryWeapon, maxCharPoints);
                     return;
                  }
               }
            }
         }
      }
   }

   private static byte getBestStrengthForWeapon(Weapon primaryWeapon, int points, int pointsLeft) {
      // generate a random number between 0.5 and 1.0, with a bell curve averaging around .75:
      double maxPercentageOfTotalPoint = 0.5 + ((CombatServer.random() + CombatServer.random() + CombatServer.random() + CombatServer.random()) / 8.0);
      int maxPointsForStrength = (int) (pointsLeft * maxPercentageOfTotalPoint);
      // randomize the list of attack styles:
      List<WeaponStyleAttack> styleList = new ArrayList<>(Arrays.asList(primaryWeapon.getAttackStyles()));
      List<WeaponStyleAttack> randomizedStyleListThrust = new ArrayList<>();
      List<WeaponStyleAttack> randomizedStyleListSwing = new ArrayList<>();
      List<WeaponStyleAttack> randomizedStyleListOther = new ArrayList<>();
      while (!styleList.isEmpty()) {
         WeaponStyleAttack style = styleList.remove((int) (styleList.size() * CombatServer.random()));
         if (style.getAttackType() == AttackType.THRUST) {
            randomizedStyleListThrust.add(style);
         }
         else if (style.getAttackType() == AttackType.SWING) {
            randomizedStyleListSwing.add(style);
         }
         else {
            randomizedStyleListOther.add(style); // thrown, missile or grappling attacks
         }
      }
      // check swing types first, 80% of the time
      List<WeaponStyleAttack> randomizedStyleList = new ArrayList<>();
      if (CombatServer.random() < .8) {
         randomizedStyleList.addAll(randomizedStyleListSwing);
         randomizedStyleList.addAll(randomizedStyleListThrust);
      }
      else {
         randomizedStyleList.addAll(randomizedStyleListThrust);
         randomizedStyleList.addAll(randomizedStyleListSwing);
      }
      randomizedStyleList.addAll(randomizedStyleListOther);

      for (WeaponStyleAttack style : randomizedStyleList) {
         byte fastStr = style.getFastStr();
         if ((fastStr > -99) && (fastStr < 99)) {
            if (Rules.getAttCost(fastStr) < maxPointsForStrength) {
               return fastStr;
            }
         }
      }
      return getAttributeLevel(points, pointsLeft);
   }

   static final HashMap<String, Integer> generatedAnimalsCount = new HashMap<>();

   static final String[] NAMES_MALE   = new String[] {"Arron", "Barley", "Charlie", "David", "Eric", "Freddy", "Guido", "Harold", "Indy", "Jarmok", "Kerri", "Liam", "Max", "Niro", "Othello", "Paul", "Quincy", "Ralph", "Samuel", "Thomas", "Unvar", "Vaughn", "William", "Xork", "Yorge", "Zazzon"};
   static final String[] NAMES_FEMALE = new String[] {"Angelina", "Betty", "Cindy", "Dominique", "Elana", "Fran", "Gena", "Hellen", "Iris", "Jeri", "Kendra", "Luanne", "Margo", "Nelly", "Ophellia", "Peggy", "Quilemina", "Rachel", "Samantha", "Tina", "Ulga", "Vemma", "Wendy", "Xena", "Yanna", "Zella"};

   public static final List<String> NAMES_LIST_MALE   = new ArrayList<>();
   public static final List<String> NAMES_LIST_FEMALE = new ArrayList<>();

   private static String getName(Arena arena, boolean maleName) {
      double rnd = CombatServer.random();
      if ((NAMES_LIST_MALE.size() == 0) || (arena != null)) {
         NAMES_LIST_MALE.clear();
         NAMES_LIST_MALE.addAll(Arrays.asList(NAMES_MALE));
      }
      if ((NAMES_LIST_FEMALE.size() == 0) || (arena != null)) {
         NAMES_LIST_FEMALE.clear();
         NAMES_LIST_FEMALE.addAll(Arrays.asList(NAMES_FEMALE));
      }
      // make sure that we exclude all names that are already used in the arena.
      if (arena != null) {
         for (Character character : arena.getCombatants()) {
            NAMES_LIST_MALE.remove(character.getName());
            NAMES_LIST_FEMALE.remove(character.getName());
         }
      }
      if (maleName && NAMES_LIST_MALE.isEmpty()) {
         // If there are no unused names, just grab one, and don't worry about it
         // (don't look at the names in use by the arena, so arena=null)
         return getName(null, maleName);
      }
      if (!maleName && NAMES_LIST_FEMALE.isEmpty()) {
         // If there are no unused names, just grab one, and don't worry about it
         // (don't look at the names in use by the arena, so arena=null)
         return getName(null, maleName);
      }
      if (maleName) {
         int index = (int) (rnd * NAMES_LIST_MALE.size());
         return NAMES_LIST_MALE.remove(index);
      }
      int index = (int) (rnd * NAMES_LIST_FEMALE.size());
      return NAMES_LIST_FEMALE.remove(index);
   }

   private static void computeBestArmorAndShield(Character character, byte shieldLevel, byte desiredEncLevel, Armor requiredArmor, Shield requiredShield,
                                                 int maxExpenditure) {
      // These lists must be sorted by weight so we can find the best armor for our carrying limit:
      List<Armor> armors = Armor.getArmorListForRace(character.getRace());
      List<Shield> shields = Shield.getShieldListForRace(character.getRace());
      double elvenArmorOdds = 0;
      if (character.getRace().getName() == Race.NAME_Elf) {
         elvenArmorOdds = 0.90;
      }
      else if (character.getRace().getName() == Race.NAME_HalfElf) {
         elvenArmorOdds = 0.45;
      }

      List<String> armorNamesToRemove = new ArrayList<>();
      if (CombatServer.random() < elvenArmorOdds) {
         armorNamesToRemove.add(Armor.NAME_ChainMail);
         armorNamesToRemove.add(Armor.NAME_HeavyChain);
         armorNamesToRemove.add(Armor.NAME_ScaleMail);
         armorNamesToRemove.add(Armor.NAME_BandedMail);
      }
      if (elvenArmorOdds == 0) {
         // only elves & 1/2 elves should wear elven chainmail
         armorNamesToRemove.add(Armor.NAME_ElvenChain);
      }
      if ((character.getRace().getName() != Race.NAME_Dwarf) && (character.getRace().getName() != Race.NAME_Gnome)) {
         // only dwarves and gnomes wear mithril:
         armorNamesToRemove.add(Armor.NAME_Mithril);
         armorNamesToRemove.add(Armor.NAME_DwarvenScale);
      }

      if (!armorNamesToRemove.isEmpty()) {
         List<Armor> armorsToRemove = new ArrayList<>();
         for (Armor armor : armors) {
            if (armorNamesToRemove.contains(armor.getName())) {
               armorsToRemove.add(armor);
            }
         }
         armors.removeAll(armorsToRemove);
      }
      armors.sort(Thing.comparatorByCostHighToLow);
      armors.sort(Thing.comparatorByWeightHighToLow);
      shields.sort(Thing.comparatorByWeightHighToLow);
      byte adjStrength = character.getAdjustedStrength();
      byte nimbleness = character.getAttributeLevel(Attribute.Nimbleness);
      double maxWeightForEnc = Rules.getMaxWeightForEncLevel(adjStrength, nimbleness, desiredEncLevel);

      if (requiredArmor == null) {
         // Remove any existing armor, so we can recompute from ground zero
         character.setArmor(Armor.NAME_NoArmor);
      }
      // If we has four arms, the shields go in the second pair
      if (requiredShield == null) {
         // Remove any existing shields, in all of our Hands
         for (Limb limb : character.getLimbs()) {
            if (limb instanceof Hand) {
               Thing currentShield = limb.getHeldThing();
               if (currentShield instanceof Shield) {
                  limb.setHeldThing(null, character);
               }
            }
         }
      }

      // A '-1' indicates that there is no limit, but a '0' means we can't spend anything
      if (maxExpenditure == 0) {
         return;
      }

      boolean hasFourArms = (character.getRace().getArmCount() > 2);
      double availableWeight = maxWeightForEnc - character.getWeightCarried();
      if (hasFourArms) {
         availableWeight /= 2.0;
      }
      double minShieldCost = 0;
      if ((shieldLevel != 0) && (requiredShield == null)) {
         // always allow for the character to carry the smallest possible shield.
         Shield smallestShield = Shield.getShield(Shield.NAME_Buckler, character.getRace());
         minShieldCost = smallestShield.getCost();
         availableWeight -= smallestShield.getAdjustedWeight();
         if (availableWeight < 0) {
            // If we can't even carry a buckler, go up a level of encumbrance
            computeBestArmorAndShield(character, shieldLevel, (byte) (desiredEncLevel + 1), requiredArmor, requiredShield, maxExpenditure);
            return;
         }
      }
      // how much do we have left to spend? (after weapon and mandatory equipment has been purchased)
      int allowableExpenditure = maxExpenditure - character.getTotalCost();
      if (hasFourArms) {
         allowableExpenditure /= 2;
      }

      if (requiredArmor == null) {
         int maxCost = 10000;
         double armorWeight;
         // Start at the heaviest armor and most expensive, and find the first armor we can wear and afford:
         for (Armor armor : armors) {
            armorWeight = armor.getAdjustedWeight();
            if (armorWeight == 0) {
               continue;
            }
            if (armorWeight < availableWeight) {
               int armorCost = armor.getCost();
               if (maxExpenditure == -1) {
                  if (armorCost < maxCost) {
                     character.setArmor(armor.getName());
                     break;
                  }
               }
               else if (armorCost <= (allowableExpenditure - minShieldCost)) {
                  character.setArmor(armor.getName());
                  allowableExpenditure -= armorCost;
                  break;
               }
            }
            armorWeight = 0;
         }
      }
      if (requiredShield == null) {
         availableWeight = maxWeightForEnc - character.getWeightCarried();
         if (shieldLevel != 0) {
            Shield shieldToUse = null;
            for (Shield shield : shields) {
               if (!shield.isReal()) {
                  continue;
               }
               if (shield.getAdjustedWeight() < availableWeight) {
                  if (maxExpenditure == -1) {
                     shieldToUse = shield;
                     break;
                  }
                  else if (shield.getCost() <= allowableExpenditure) {
                     shieldToUse = shield;
                     break;
                  }
               }
            }
            if (shieldToUse != null) {
               if (hasFourArms) {
                  character.getLimb(LimbType.HAND_RIGHT_2).setHeldThing(shieldToUse, character);
                  Shield shield2 = Shield.getShield(shieldToUse.getName(), character.getRace());
                  character.getLimb(LimbType.HAND_LEFT_2).setHeldThing(shield2, character);
               }
               else {
                  character.getLimb(LimbType.HAND_LEFT).setHeldThing(shieldToUse, character);
               }
            }
         }
      }
   }

   private static boolean increaseCharacterSkillPoints(Character character, int pointsLeft, HashMap<SkillType, Byte> requiredSkills) {
      // Look for the largest skill level increase that does not exceed the pointsLeft.
      TreeSet<Skill> skills = sortSkillsByLevel(character.getSkillsList(), false/*ascending*/);
      for (Skill skill : skills) {
         if (requiredSkills.get(skill.getType()) == null) {
            byte level = skill.getLevel();
            if (level < Rules.getMaxSkillLevel()) {
               if ((Rules.getSkillCost((byte) (level + 1)) - Rules.getSkillCost(level)) <= pointsLeft) {
                  skill.setLevel((byte) (level + 1));
                  return true;
               }
            }
         }
      }
      return false;
   }

   private static boolean reduceCharacterSkillPoints(Character character, int pointsLeft, HashMap<SkillType, Byte> requiredSkills) {
      // Look for the smallest skill level decrease that will satisfy the pointsLeft.
      TreeSet<Skill> skills = sortSkillsByLevel(character.getSkillsList(), true/*ascending*/);
      Skill lastSkillWeCanModify = null;
      for (Skill skill : skills) {
         if (requiredSkills.get(skill.getType()) == null) {
            byte level = skill.getLevel();
            if (level > 0) {
               lastSkillWeCanModify = skill;
               if ((Rules.getSkillCost((byte) (level - 1)) - Rules.getSkillCost(level)) <= pointsLeft) {
                  skill.setLevel((byte) (level - 1));
                  return true;
               }
            }
         }
      }
      // If we didn't find one that met the pointsLeft criteria, reduce the largest skill by one
      if (lastSkillWeCanModify != null) {
         lastSkillWeCanModify.setLevel((byte) (lastSkillWeCanModify.getLevel() - 1));
         return true;
      }
      return false;
   }

   private static TreeSet<Skill> sortSkillsByLevel(List<Skill> skillsList, final boolean ascending) {
      TreeSet<Skill> skills = new TreeSet<>(new Comparator<>() {
         @Override
         public int compare(Skill o1, Skill o2) {
            if (o1.getLevel() == o2.getLevel()) {
               return 0;
            }
            if (o1.getLevel() < o2.getLevel()) {
               return ascending ? -1 : 1;
            }
            return ascending ? 1 : -1;
         }
      });
      skills.addAll(skillsList);
      return skills;
   }

   private static Attribute increaseCharacterPoints(Character character, int pointsLeft, byte startIndex, byte attIndex, HashMap<String, Integer> requiredAttributes) {
      Attribute[] atts = new Attribute[] { Attribute.Intelligence, Attribute.Social, Attribute.Toughness, Attribute.Health, Attribute.Nimbleness, Attribute.Dexterity};
      if (attIndex == -1) {
         attIndex = (byte) (CombatServer.random() * atts.length);
         startIndex = attIndex;
      }
      else {
         if (attIndex == atts.length) {
            attIndex = 0;
         }
         if (startIndex == attIndex) {
            return null;
         }
      }
      // don't mess with pre-defined attributes:
      if (requiredAttributes.get(atts[attIndex].shortName) == null) {
         byte racialAttrLevel = character.getRace().getAttributeMods(atts[attIndex]);
         byte attrLevel = character.getAttributeLevel(atts[attIndex]);
         int curCost = Rules.getAttCost((byte) (attrLevel - racialAttrLevel));
         attrLevel++;
         if ((attrLevel - racialAttrLevel) <= Rules.getMaxAttribute()) {
            int upCost = Rules.getAttCost((byte) (attrLevel - racialAttrLevel));
            if ((upCost - curCost) <= pointsLeft) {
               character.setAttribute(atts[attIndex], attrLevel, true/*containInLimits*/);
               return atts[attIndex];
            }
         }
      }
      // If we can't raise that attribute, try the next one
      return increaseCharacterPoints(character, pointsLeft, startIndex, ++attIndex, requiredAttributes);
   }

   static final Attribute[] REDUCE_ATTRIBUTES = new Attribute[] {Attribute.Intelligence, Attribute.Social, Attribute.Toughness, Attribute.Health, Attribute.Nimbleness, Attribute.Intelligence, Attribute.Social, Attribute.Toughness, Attribute.Intelligence, Attribute.Social};

   private static Attribute reduceCharacterAttributes(Character character, boolean allowDexAndStr, HashMap<String, Integer> requiredAttributes) {
      if (allowDexAndStr) {
         return reduceCharacterAttributes(character, new Attribute[] { Attribute.Dexterity, Attribute.Strength}, requiredAttributes);
      }
      return reduceCharacterAttributes(character, REDUCE_ATTRIBUTES, requiredAttributes);
   }

   private static Attribute reduceCharacterAttributes(Character character, Attribute[] reducibleAttributes, HashMap<String, Integer> requiredAttributes) {
      byte redAttIndex = (byte) (CombatServer.random() * reducibleAttributes.length);
      return reduceCharacterAttributes(character, redAttIndex, redAttIndex, reducibleAttributes, requiredAttributes);
   }

   private static Attribute reduceCharacterAttributes(Character character, byte startIndex, byte redAttIndex, Attribute[] reducibleAttributes,
                                                 HashMap<String, Integer> requiredAttributes) {
      Attribute attr = reducibleAttributes[redAttIndex];
      // don't mess with pre-defined attributes:
      if (requiredAttributes.get(attr.shortName) == null) {
         byte attrLevel = character.getAttributeLevel(attr);
         if ((attrLevel - character.getRace().getAttributeMods(attr)) > -3) {
            character.setAttribute(attr, --attrLevel, true/*containInLimits*/);
            return attr;
         }
      }
      if (++redAttIndex == reducibleAttributes.length) {
         redAttIndex = 0;
      }
      if (redAttIndex == startIndex) {
         return null;
      }
      return reduceCharacterAttributes(character, startIndex, redAttIndex, reducibleAttributes, requiredAttributes);
   }

   static final String[] REDUCE_ADVANTAGES = new String[] {Advantage.BAD_TEMPER, Advantage.COMPULSIVE_LIAR, Advantage.GREEDY, Advantage.HONEST, Advantage.BAD_TEMPER, Advantage.COMPULSIVE_LIAR, Advantage.GREEDY, Advantage.HONEST, Advantage.LECHEROUS, Advantage.SADISTIC, Advantage.BERSERKER, Advantage.MUTE};

   private static boolean reduceCharacterAdvantages(Character character) {
      byte redAdvIndex = (byte) (CombatServer.random() * REDUCE_ADVANTAGES.length);
      return reduceCharacterAdvantages(character, redAdvIndex, redAdvIndex);
   }

   private static boolean reduceCharacterAdvantages(Character character, byte startIndex, byte redAdvIndex) {
      String disadvantage = REDUCE_ADVANTAGES[redAdvIndex];
      Advantage adv = Advantage.getAdvantage(disadvantage);
      if (character.addAdvantage(adv)) {
         return true;
      }
      if (++redAdvIndex >= REDUCE_ADVANTAGES.length) {
         redAdvIndex = 0;
      }
      if (redAdvIndex == startIndex) {
         return false;
      }
      return reduceCharacterAdvantages(character, startIndex, redAdvIndex);
   }

   private static byte getAttributeLevel(int points, int pointsLeft) {
      int avePoint = (points + (2 * pointsLeft)) / 3;
      int attrMaxPoints = (int) (avePoint * .4);
      if (points > 50) {
         attrMaxPoints = (int) (avePoint * .5);
      }
      if (points > 100) {
         attrMaxPoints = (int) (avePoint * .6);
      }
      if (points > 150) {
         attrMaxPoints = (int) (avePoint * .6);
      }
      if (points > 200) {
         attrMaxPoints = (int) (avePoint * .6);
      }
      if (points > 300) {
         attrMaxPoints = (int) (avePoint * .4);
      }
      if (points > 400) {
         attrMaxPoints = (int) (avePoint * .35);
      }
      if (points > 500) {
         attrMaxPoints = (int) (avePoint * .3);
      }
      if (points > 750) {
         attrMaxPoints = (int) (avePoint * .2);
      }
      attrMaxPoints = Math.min(attrMaxPoints, 200);
      int attrPoints = (int) ((CombatServer.random() * attrMaxPoints) + (CombatServer.random() * attrMaxPoints)) / 2;

      attrPoints = Math.min(attrPoints, pointsLeft + 10);
      for (byte attLevel = Rules.getMaxAttribute(); attLevel > Rules.getMinAttribute(); attLevel--) {
         if (Rules.getAttCost(attLevel) <= attrPoints) {
            return attLevel;
         }
      }
      return 0;
   }

   private static byte getSkillLevel(int totalPoints, int pointsLeft) {
      byte skillLevel = 2;
      if (pointsLeft > 0) {
         skillLevel = 3;
      }
      if (pointsLeft > 40) {
         skillLevel = 4;
      }
      if (pointsLeft > 100) {
         skillLevel = 5;
      }
      if (pointsLeft > 150) {
         skillLevel = 6;
      }
      if (pointsLeft > 200) {
         skillLevel = 7;
      }
      if (pointsLeft > 300) {
         skillLevel = 8;
      }
      if (pointsLeft > 400) {
         skillLevel = 9;
      }
      if (pointsLeft > 500) {
         skillLevel = 10;
      }

      if (totalPoints >= 250) {
         skillLevel++;
         if (totalPoints >= 400) {
            skillLevel++;
            if (totalPoints >= 600) {
               skillLevel++;
            }
         }
      }
      double rnd = CombatServer.random();
      if ((rnd < .2) && (pointsLeft > 40)) {
         skillLevel--;
      }
      if (rnd > .7) {
         skillLevel++;
         if (rnd > .9) {
            skillLevel++;
         }
      }
      if (skillLevel > 10) {
         skillLevel = 10;
      }
      return skillLevel;
   }

   private static Weapon getBestWeaponForStrength(byte str, boolean missileWeapon, boolean allowTwoHanded, int maxExpenditure, Race racialBase) {
      List<String> appropriateWeapons = new ArrayList<>();
      getBestWeaponForStrength(str, missileWeapon, appropriateWeapons, allowTwoHanded, maxExpenditure, racialBase);
      getBestWeaponForStrength(str, missileWeapon, appropriateWeapons, allowTwoHanded, maxExpenditure, racialBase);
      if (!missileWeapon) {
         getBestWeaponForStrength((byte) (str - 1), missileWeapon, appropriateWeapons, allowTwoHanded, maxExpenditure, racialBase);
      }
      String weaponName = appropriateWeapons.get((int) (appropriateWeapons.size() * CombatServer.random()));
      return Weapon.getWeapon(weaponName, racialBase);
   }

   private static void getBestWeaponForStrength(byte str, boolean missileWeapon, List<String> appropriateWeapons, boolean allowTwoHanded,
                                                int maxExpenditure, Race racialBase) {
      int wealthMultiplierTimes100 = (int)(racialBase.getWealthMultiplier() * 100);
      if (missileWeapon) {
         HashMap<String, List<Integer>> counts = new HashMap<>();
         // possible values                                [.20, .25, .33, .5,  1,  2,  3 ]
         counts.put(Weapon.NAME_BlowGun,       Arrays.asList( 2,   2,   1,  0,  0,  0,  0 ));
         counts.put(Weapon.NAME_Sling,         Arrays.asList( 2,   2,   1,  1,  0,  0,  0 ));
         counts.put(Weapon.NAME_Knife,         Arrays.asList( 1,   2,   1,  1,  1,  0,  0 ));
         counts.put(Weapon.NAME_Spear,         Arrays.asList( 1,   2,   2,  1,  1,  0,  0 ));
         counts.put(Weapon.NAME_ThrowingAxe,   Arrays.asList( 0,   1,   2,  2,  1,  1,  0 ));
         counts.put(Weapon.NAME_Javelin,       Arrays.asList( 0,   1,   1,  1,  1,  1,  1 ));
         counts.put(Weapon.NAME_StaffSling,    Arrays.asList( 0,   1,   1,  1,  1,  1,  1 ));
         counts.put(Weapon.NAME_BowShortbow,   Arrays.asList( 0,   0,   0,  2,  2,  2,  1 ));
         counts.put(Weapon.NAME_BowLongbow,    Arrays.asList( 0,   0,   0,  1,  3,  3,  4 ));
       //counts.put(Weapon.NAME_BowComposite,  Arrays.asList( 0,   0,   0,  0,  0,  1,  4 ));
         counts.put(Weapon.NAME_CrossbowLight, Arrays.asList( 0,   0,   0,  1,  1,  1,  1 ));
         counts.put(Weapon.NAME_Crossbow,      Arrays.asList( 0,   0,   0,  0,  1,  1,  1 ));
         counts.put(Weapon.NAME_CrossbowHeavy, Arrays.asList( 0,   0,   0,  0,  1,  1,  1 ));
         int index = -1;
         switch (wealthMultiplierTimes100) {
            case 300: index++;
            case 200: index++;
            case 100: index++;
            case  50: index++;
            case  33: index++;
            case  25: index++;
            case  20: index++;
         }
         for (Entry<String, List<Integer>> countsPerWeapon : counts.entrySet()) {
            String weaponName = countsPerWeapon.getKey();
            Integer weaponCount = countsPerWeapon.getValue().get(index);
            for (int i=0 ; i<weaponCount ; i++) {
               appropriateWeapons.add(weaponName);
            }
         }
      }
      else {
         switch (str) {
            case -1:
               appropriateWeapons.add(Weapon.NAME_Club);
               appropriateWeapons.add(Weapon.NAME_Spear);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               /*appropriateWeapons.add(Weapon.NAME_Knife); *//*not optimal*/
               break;
            case 0:
               appropriateWeapons.add(Weapon.NAME_Quarterstaff);
               appropriateWeapons.add(Weapon.NAME_MorningStar);
               appropriateWeapons.add(Weapon.NAME_MorningStar);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               /*appropriateWeapons.add(Weapon.NAME_Knife); *//*not optimal*/
               break;
            case 1:
               appropriateWeapons.add(Weapon.NAME_Katana);
               appropriateWeapons.add(Weapon.NAME_Quarterstaff);
               /*appropriateWeapons.add(Weapon.NAME_Knife);*//*not optimal*/
               appropriateWeapons.add(Weapon.NAME_Rapier);
               appropriateWeapons.add(Weapon.NAME_Rapier);
               break;
            case 2:
               appropriateWeapons.add(Weapon.NAME_Longsword);
               appropriateWeapons.add(Weapon.NAME_Mace);
               appropriateWeapons.add(Weapon.NAME_Sabre);
               appropriateWeapons.add(Weapon.NAME_TwoHandedSword);
               break;
            case 3:
               appropriateWeapons.add(Weapon.NAME_Katana);
               appropriateWeapons.add(Weapon.NAME_Quarterstaff);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               break;
            case 4:
               appropriateWeapons.add(Weapon.NAME_KarateKick);
               appropriateWeapons.add(Weapon.NAME_BastardSword);
               appropriateWeapons.add(Weapon.NAME_Sabre);
               appropriateWeapons.add(Weapon.NAME_WarHammer);
               break;
            case 5:
               appropriateWeapons.add(Weapon.NAME_Broadsword);
               appropriateWeapons.add(Weapon.NAME_GreatAxe);
               appropriateWeapons.add(Weapon.NAME_GreatAxe);
               break;
            case 6:
               appropriateWeapons.add(Weapon.NAME_Axe);
               appropriateWeapons.add(Weapon.NAME_Maul);
               appropriateWeapons.add(Weapon.NAME_Spear);
               break;
            case 7:
               appropriateWeapons.add(Weapon.NAME_Club);
               appropriateWeapons.add(Weapon.NAME_Flail);
               appropriateWeapons.add(Weapon.NAME_Halberd);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               appropriateWeapons.add(Weapon.NAME_TwoHandedSword);
               break;
            case 8:
               appropriateWeapons.add(Weapon.NAME_BastardSword);
               appropriateWeapons.add(Weapon.NAME_PickAxe);
               break;
            case 9:
            case 10:/*not optimal*/
            case 11:/*not optimal*/
            case 12:/*not optimal*/
               appropriateWeapons.add(Weapon.NAME_MorningStar);
               break;
            default:
               appropriateWeapons.add(Weapon.NAME_Punch);
               appropriateWeapons.add(Weapon.NAME_Dagger);
               appropriateWeapons.add(Weapon.NAME_Knife);
               appropriateWeapons.add(Weapon.NAME_Shortsword);
               appropriateWeapons.add(Weapon.NAME_Nunchucks);
               appropriateWeapons.add(Weapon.NAME_Club);
               appropriateWeapons.add(Weapon.NAME_Quarterstaff);
               appropriateWeapons.add(Weapon.NAME_ThreePartStaff);
         }
      }
      List<String> removeWeapons = new ArrayList<>();
      if (!allowTwoHanded) {
         for (String weaponName : appropriateWeapons) {
            Weapon weap = Weapon.getWeapon(weaponName, racialBase);
            if (weap.isOnlyTwoHanded() || weap.getName().equals(Weapon.NAME_Katana)) {
               removeWeapons.add(weaponName);
            }
            else if ((weaponName == Weapon.NAME_BastardSword) && (str < 8)) {
               // Bastard swords should only be used one-handed if we have a STR of 8 of greater
               removeWeapons.add(weaponName);
            }
            else if ((weaponName == Weapon.NAME_Katana) && (str < 1)) {
               // Katanas should only be used one-handed if we have a STR of 1 of greater
               removeWeapons.add(weaponName);
            }
         }
      }
      if (maxExpenditure >= 0) {
         for (String weaponName : appropriateWeapons) {
            Weapon weap = Weapon.getWeapon(weaponName, racialBase);
            if (weap.getCost() > maxExpenditure) {
               removeWeapons.add(weaponName);
            }
         }
      }
      appropriateWeapons.removeAll(removeWeapons);
      if (appropriateWeapons.isEmpty()) {
         appropriateWeapons.add(Weapon.NAME_Punch);
         if (maxExpenditure > Weapon.getWeapon(Weapon.NAME_Club, racialBase).getCost()) {
            appropriateWeapons.add(Weapon.NAME_Club);
         }
      }
   }
}
