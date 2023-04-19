package ostrowski.combat.common;

import org.eclipse.swt.custom.CCombo;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.mage.MageSpells;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.CombatServer;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   static final Attribute at    = Attribute.Strength;// force load of this enum
   static final String    dummy = Rules.diagCompName;

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
      List<SkillType> aikido                = Collections.unmodifiableList(Arrays.asList(SkillType.Aikido));
      List<SkillType> aikidoAndKarate       = Collections.unmodifiableList(Arrays.asList(SkillType.Aikido, SkillType.Karate));
      List<SkillType> aikidoKarateAndBoxing = Collections.unmodifiableList(Arrays.asList(SkillType.Aikido, SkillType.Karate, SkillType.Boxing));
      List<SkillType> boxing                = Collections.unmodifiableList(Arrays.asList(SkillType.Boxing));
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Kobold,    aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Fairy,     aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Goblin,    aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Fawn,      aikido);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Ellyon,    aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Orc,       aikido);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Gargoyle,  aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_LizardMan, boxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_InsectMan, aikido);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Centaur,   aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_HalfOgre,  aikidoAndKarate);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Ogre,      aikidoKarateAndBoxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Troll,     aikidoKarateAndBoxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Minotaur,  aikidoKarateAndBoxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Cyclops,   aikidoKarateAndBoxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Giant,     aikidoKarateAndBoxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Skeleton,  aikidoKarateAndBoxing);
      DISSALLOWED_SKILLS_FOR_RACE.put(Race.NAME_Zombie,    aikidoKarateAndBoxing);
   }

   static final List<String> DUAL_WIELDABLE_WEAPON_NAMES = new ArrayList<>() {{
      add(Weapon.NAME_Axe);
      add(Weapon.NAME_Club);
      add(Weapon.NAME_Dagger);
      add(Weapon.NAME_Knife);
      add(Weapon.NAME_Shortsword);
      //add(Weapon.NAME_Broadsword);
      //add(Weapon.NAME_Longsword);
      //add(Weapon.NAME_PickAxe);
      //add(Weapon.NAME_Rapier);
      //add(Weapon.NAME_Sabre);
   }};

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
            if (token.equalsIgnoreCase(g.name)) {
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
         // 6% percent of the time, randomly assign the gender, excluding Male.
         double rnd = CombatServer.random();
         if (rnd > .94) {
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
      HashMap<ProfessionType, Byte> requiredProfs = new HashMap<>();
      String requiredName = null;

      Race race = character.getRace();
      while (tokens.size() > 0) {
         String token = tokens.remove(0);
         String twoTokens = (tokens.size(

                                        ) < 1) ? null : token + " " + tokens.get(0);
         String threeTokens = (tokens.size() < 2) ? null : twoTokens + " " + tokens.get(1);
         Thing thing = Thing.getThing(token, false/*allowTool*/, race);
         if (thing == null) {
            if (twoTokens != null) {
               thing = Thing.getThing(twoTokens, false/*allowTool*/, race);
               if (thing != null) {
                  // If we used the second token to make a thing, consume the token
                  tokens.remove(0);
               }
               else {
                  if (threeTokens != null) {
                     thing = Thing.getThing(threeTokens, false/*allowTool*/, race);
                     if (thing != null) {
                        // If we used the second and third tokens to make a thing, consume the tokens
                        tokens.remove(0);
                        tokens.remove(0);
                     }
                  }
               }
            }
            if (thing == null) {
               // What else could this be?
               if (token.startsWith("$")) {
                  token = Advantage.WEALTH + ": " + token;
               }
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
                     maxExpenditure *= race.getWealthMultiplier();
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
                              character.teamID = teamID;
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
                              List<ProfessionType> profsList = requiredProfs.keySet()
                                                                            .stream()
                                                                            .filter(p -> p.skillList.contains(skillType))
                                                                            .collect(Collectors.toList());
                              if (!profsList.isEmpty()) {
                                 profsList.forEach(p -> {
                                    Byte priorLevel = requiredProfs.get(p);
                                    if (level > priorLevel) {
                                       requiredProfs.put(p, level);
                                    }
                                 });
                              } else {
                                 ProfessionType[] profs = new ProfessionType[]{
                                         ProfessionType.Fighter,
                                         ProfessionType.Shooting,
                                         ProfessionType.MartialArtist,
                                         ProfessionType.Athlete,
                                         ProfessionType.Common,
                                         ProfessionType.Spellcasting,
                                         ProfessionType.Thief,
                                         ProfessionType.Survivalist,
                                         ProfessionType.Knight,
                                         ProfessionType.Assassin,
                                         ProfessionType.AnimalHandler,
                                         ProfessionType.Farmer,
                                         ProfessionType.Linguist,
                                         ProfessionType.Tradesman,
                                         ProfessionType.Merchant,
                                         ProfessionType.Entertainer,
                                         ProfessionType.Doctor,
                                         ProfessionType.Detective
                                 };
                                 for (ProfessionType profType : profs) {
                                    if (profType.skillList.contains(skillType)) {
                                       requiredProfs.put(profType, level);
                                       break;
                                    }
                                 }
                              }
                           }
                           else {
                              ProfessionType professionType = ProfessionType.getByName(name);
                              if (professionType != null) {
                                 Byte level = Byte.parseByte(value);
                                 requiredProfs.put(professionType, level);
                              } else {
                                 MageSpell mageSpell = MageSpells.getSpell(name);
                                 if (mageSpell != null) {
                                    byte level = Byte.parseByte(value);
                                    character.setSpellLevel(mageSpell.getName(), level, MageSpell.Familiarity.KNOWN);
                                    Class<? extends MageSpell>[] requiredSpells = mageSpell.prerequisiteSpells;
                                    for (Class<? extends MageSpell> preReq : requiredSpells) {
                                       character.setSpellLevel(preReq.getName(), level, MageSpell.Familiarity.KNOWN);
                                    }
                                    List<Profession> profs = character.getProfessionsList();
                                    Profession spellcastingProf = null;
                                    for (Profession prof : profs) {
                                       if (prof.getType() == ProfessionType.Spellcasting) {
                                          spellcastingProf = prof;
                                       }
                                    }
                                    if (spellcastingProf == null) {
                                       spellcastingProf = new Profession(ProfessionType.Spellcasting,
                                                                         Arrays.asList(mageSpell.prerequisiteSkillTypes),
                                                                         level);
                                       profs.add(spellcastingProf);
                                    }
                                    else {
                                       spellcastingProf.setLevel(level);
                                    }
                                    List<SkillType> profSkills = spellcastingProf.getProficientSkills();
                                    profSkills.addAll(Arrays.asList(mageSpell.prerequisiteSkillTypes));
                                    spellcastingProf.setProficientSkills(profSkills);
                                    character.setProfessionsList(profs);
                                 } else {
                                    // What else could this be? It has a colon (':') in it
                                    // It could be a key ("key:red door")
                                    character.addEquipment(Thing.getThing(token, race));
                                 }
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
                        character.addEquipment(Thing.getThing(token, race));
                     }
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
         maxExpenditure = (int)(5000 * race.getWealthMultiplier());
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
      if (primaryWeapon == null) {
         if (!requiredSkills.isEmpty()) {
            List<SkillType> weaponSkills = Stream.of(SkillType.values())
                                                 .filter(skillType -> skillType.isAdjustedForSize)
                                                 .filter(skillType -> !skillType.isUnarmed)
                                                 .collect(Collectors.toList());
            boolean hasRequiredWeaponSkill = weaponSkills.stream().anyMatch(s -> requiredSkills.containsKey(s));
            if (!hasRequiredWeaponSkill) {
               if (requiredSkills.containsKey(SkillType.Karate)) {
                  primaryWeapon = Weapons.getWeapon(Weapon.NAME_KarateKick, race);
               }
               if (requiredSkills.containsKey(SkillType.Boxing) ||
                   requiredSkills.containsKey(SkillType.Brawling)) {
                  primaryWeapon = Weapons.getWeapon(Weapon.NAME_Punch, race);
               }
            }
         }
         if ((primaryWeapon == null) &&
              requiredProfs.containsKey(ProfessionType.MartialArtist) &&
             !requiredProfs.containsKey(ProfessionType.Fighter)) {
            // we need a martial arts weapon, but it wasn't specified which weapon.
            List<String> martialArtsWeaponNames = new ArrayList<>() {{
               for (int i=0 ; i<14; i++ ) add(Weapon.NAME_KarateKick);
               for (int i=0 ; i<9 ; i++ ) add(Weapon.NAME_Quarterstaff);
               for (int i=0 ; i<7 ; i++ ) add(Weapon.NAME_Katana);
               for (int i=0 ; i<5 ; i++ ) add(Weapon.NAME_Nunchucks);
               for (int i=0 ; i<4 ; i++ ) add(Weapon.NAME_ThreePartStaff);
               for (int i=0 ; i<4 ; i++ ) add(Weapon.NAME_Spear);
               for (int i=0 ; i<3 ; i++ ) add(Weapon.NAME_Punch); // boxing
               for (int i=0 ; i<2 ; i++ ) add(Weapon.NAME_Knife);
               for (int i=0 ; i<2 ; i++ ) add(Weapon.NAME_Longsword);
               for (int i=0 ; i<1 ; i++ ) add(Weapon.NAME_Shortsword);
            }};
            int selectedIndex = (int) (CombatServer.random() * martialArtsWeaponNames.size());
            String selectedWeaponName = martialArtsWeaponNames.get(selectedIndex);
            primaryWeapon = Weapons.getWeapon(selectedWeaponName, race);
         }
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
         for (ProfessionType profType : ProfessionType.values()) {
            if (profType.skillList.contains(skillType)) {
               character.setSkillLevel(profType, skillType, requiredSkills.get(skillType));
            }
         }
      }
      for (Entry<ProfessionType, Byte> entry : requiredProfs.entrySet()) {
         character.setProfessionLevel(entry.getKey(), null, entry.getValue());
      }
      pointsLeft = points - character.getPointTotal();

      if (noEquip) {
         primaryWeapon = Weapons.getWeapon(Weapon.NAME_Punch, race);
         requiredArmor = Armor.getArmor(Armor.NAME_NoArmor, race);
         requiredShield = Shield.getShield(Shield.NAME_None, race);
      }
      else {
         if (primaryWeapon == null) {
            primaryWeapon = getBestWeaponForStrength(character.getAttributeLevel(Attribute.Strength), missileWeapon,
                                                     (requiredShield == null)/*allowTwoHanded*/, maxExpenditure, race);
         }
      }

      boolean isDualWieldable = DUAL_WIELDABLE_WEAPON_NAMES.contains(primaryWeapon.getName());
      boolean dualWield = false;
      if (isDualWieldable && (requiredShield == null)) {
         double dualWieldChance = 0;
              if (points > 100) dualWieldChance = .025;
         else if (points > 200) dualWieldChance = .050;
         if (race.getName().equals(Race.NAME_Orc)) dualWieldChance = .05 + 2*dualWieldChance;
         if (race.getName().equals(Race.NAME_HalfOrc)) dualWieldChance = .025 + 1.5*dualWieldChance;
         if (race.getName().equals(Race.NAME_HalfOgre)) dualWieldChance = .025 + 1.5*dualWieldChance;
         if (race.getName().equals(Race.NAME_InsectMan)) dualWieldChance = 0;
         dualWield = (CombatServer.random() < dualWieldChance);
      }

      Hand rightHand = (Hand) character.getLimb(LimbType.HAND_RIGHT);
      if (rightHand != null) {
         if (character.isAnimal()) {
            primaryWeapon = rightHand.getWeapon(character);
         }
         else {
            if ((primaryWeapon != null) && !primaryWeapon.getName().equals(Weapon.NAME_KarateKick)) {
               if (weaponSheathed) {
                  character.addEquipment(primaryWeapon);
               }
               else {
                  rightHand.setHeldThing(primaryWeapon, character);
                  if (dualWield) {
                     Hand leftHand = (Hand) character.getLimb(LimbType.HAND_LEFT);
                     leftHand.setHeldThing(primaryWeapon.clone(), character);
                     character.addAdvantage(Advantage.getAdvantage(Advantage.AMBIDEXTROUS));
                  }
               }
            }
         }
      }
      else {
         Head head = (Head) character.getLimb(LimbType.HEAD);
         primaryWeapon = head.getWeapon(character);
      }
      Skill weaponSkill = null;
      if (primaryWeapon == null) {
         primaryWeapon = Weapons.getWeapon(Weapon.NAME_Punch, race);
         Byte requireMartialArtsLevel = requiredProfs.get(ProfessionType.MartialArtist);
         if (requireMartialArtsLevel != null) {
            if (requiredSkills.containsKey(SkillType.Boxing) && !requiredSkills.containsKey(SkillType.Karate)) {
               weaponSkill = new Skill(SkillType.Boxing, requireMartialArtsLevel);
            } else {
               weaponSkill = new Skill(SkillType.Karate, requireMartialArtsLevel);
            }
         }
      }
      if (weaponSkill == null) {
         if (!requiredProfs.isEmpty()) {
            Set<SkillType> primaryProfsSkills = requiredProfs.keySet()
                                                             .stream()
                                                             .map(pt -> pt.skillList)
                                                             .flatMap(Collection::parallelStream)
                                                             .collect(Collectors.toSet());

            Set<SkillType> primaryWeaponSkills = Arrays.stream(primaryWeapon.attackStyles)
                                                       .map(wsa -> wsa.getSkillType())
                                                       .filter(st -> primaryProfsSkills.contains(st))
                                                       .collect(Collectors.toSet());
            if (!primaryWeaponSkills.isEmpty()) {
               byte level = 0;
               SkillType skillType = null;
               for (SkillType primaryWeaponSkill : primaryWeaponSkills) {
                  if (requiredSkills.containsKey(primaryWeaponSkill)) {
                     level = requiredSkills.get(primaryWeaponSkill);
                     skillType = primaryWeaponSkill;
                     break;
                  }
                  if (primaryWeaponSkill == SkillType.Wrestling) {
                     continue;
                  }
                  for (ProfessionType profType : requiredProfs.keySet()) {
                     if (profType.skillList.contains(primaryWeaponSkill)) {
                        if (skillType == null || level < requiredProfs.get(profType)) {
                           level = requiredProfs.get(profType);
                           skillType = primaryWeaponSkill;
                        }
                     }
                  }
               }
               if (skillType != null) {
                  weaponSkill = new Skill(skillType, level);
               }
            }
         }
      }
      if (weaponSkill == null) {
         weaponSkill = new Skill(primaryWeapon.getAttackStyle(0).getSkillType(), (byte)0);
      }
      boolean usingAsOneHanded = (primaryWeapon.getAttackStyle(0).handsRequired == 1);
      if (usingAsOneHanded) {
         // Bastard swords and Katanas should only be used one-handed if we have a STR greater than the fast STR for 1-handed
         WeaponStyleAttack oneHandedSwing = null;
         WeaponStyleAttack twoHandedSwing = null;
         for (WeaponStyleAttack style : primaryWeapon.getAttackStyles()) {
            if (style.getAttackType() == AttackType.SWING) {
               if (style.handsRequired == 1) {
                  oneHandedSwing = style;
               }
               else {
                  twoHandedSwing = style;
               }
            }
         }
         if ((oneHandedSwing != null) && (twoHandedSwing != null)) {
            byte str = character.getAttributeLevel(Attribute.Strength);
            // If we are strong enough to use this one-handed, do it
            // If we are not strong enough to use this quickly one-handed, but are not strong enough to use
            // it two-handed quickly either, then use it one-handed, as long as we are strong enough to not be slow:
            if (str >= oneHandedSwing.getFastStr()) {
               usingAsOneHanded = true;
            }
            else {
               usingAsOneHanded = (str < twoHandedSwing.getFastStr()) && (str > oneHandedSwing.getSlowStr());
            }
            if (!usingAsOneHanded) {
               weaponSkill = new Skill(twoHandedSwing.getSkillType(), (byte)0);
            }
         }
      }
      boolean fourHands = (race.getArmCount() > 2);
      if (fourHands) {
         if (usingAsOneHanded) {
            // If we have four (or more) arms, put the same kind of weapon in our second pair:
            if (!primaryWeapon.isOnlyTwoHanded()) {
               Hand leftHand = (Hand) character.getLimb(LimbType.HAND_LEFT);
               if (leftHand != null) {
                  leftHand.setHeldThing(Weapons.getWeapon(primaryWeapon.getName(), race), character);
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
      ProfessionType primaryProfessionType = ProfessionType.Fighter;
      for (ProfessionType pt : Arrays.asList(ProfessionType.Fighter,
                                             ProfessionType.MartialArtist,
                                             ProfessionType.Shooting)) {
         if (pt.skillList.contains(weaponSkill.getType())) {
            primaryProfessionType = pt;
            break;
         }
      }
      if (!requiredProfs.isEmpty()) {
         Set<SkillType> primaryWeaponSkills = Arrays.stream(primaryWeapon.attackStyles)
                                                    .map(WeaponStyle::getSkillType)
                                                    .collect(Collectors.toSet());
         List<ProfessionType> primaryProfs = requiredProfs.keySet()
                                                          .stream()
                                                          .filter(pt -> pt.skillList.stream()
                                                                                    .anyMatch(primaryWeaponSkills::contains))
                                                          .collect(Collectors.toList());
         if (!primaryProfs.isEmpty()) {
            primaryProfs.sort(Comparator.comparingInt(requiredProfs::get));
            primaryProfessionType = primaryProfs.get(primaryProfs.size()-1); // get the last item now that its sorted
         }
         if (requiredProfs.containsKey(primaryProfessionType)) {
            weaponLevel = requiredProfs.get(primaryProfessionType);
         }
      }
      SkillType weaponSkillType = weaponSkill.getType();
      if (weaponSkillType == SkillType.Throwing && primaryProfessionType != ProfessionType.Athlete) {
         character.setSkillLevel(ProfessionType.Athlete, weaponSkillType, weaponLevel);
      } else {
         character.setSkillLevel(primaryProfessionType, weaponSkillType, weaponLevel);
      }

      pointsLeft = points - character.getPointTotal();

      byte shieldLevel = 0;
      boolean martialArtistWithoutShield = primaryProfessionType == ProfessionType.MartialArtist &&
                                           !character.getProfessionsList().contains(ProfessionType.Fighter);
      // occasionally, a martial artist with a sword might know the shield skill:
      if (martialArtistWithoutShield && CombatServer.random() < .20) {
         martialArtistWithoutShield = false;
      }
      if (primaryWeapon.getName().equals(Weapon.NAME_Nunchucks) ||
          primaryWeapon.getName().equals(Weapon.NAME_ThreePartStaff) ) {
         martialArtistWithoutShield = true;
      }
      if ((usingAsOneHanded || fourHands) && !noEquip && !dualWield && !martialArtistWithoutShield) {
         shieldLevel = getSkillLevel(points, pointsLeft);
         Byte reqShieldLevel = requiredSkills.get(SkillType.Shield);
         if (reqShieldLevel != null) {
            shieldLevel = reqShieldLevel;
         }
         character.setSkillLevel(ProfessionType.Fighter, SkillType.Shield, shieldLevel);
         pointsLeft = points - character.getPointTotal();
      }
      boolean hasTwoHandedOption = Arrays.stream(primaryWeapon.getAttackStyles())
                                         .anyMatch(wsa -> wsa.getAttackType() == AttackType.SWING && wsa.isTwoHanded());
      boolean hasOneHandedOption = Arrays.stream(primaryWeapon.getAttackStyles())
                                         .anyMatch(wsa -> wsa.getAttackType() == AttackType.SWING && !wsa.isTwoHanded());
      // If we are using a two-handed weapon, and it has a 1-handed skill, we should learn that too:
      if (primaryWeapon.isReal() && hasTwoHandedOption && hasOneHandedOption) {
         // We should be able to use this weapon in all of its secondary attack styles (at a lower skill level):
         for (WeaponStyleAttack attackStyle : primaryWeapon.getAttackStyles()) {
            SkillType attackStyleSkillType = attackStyle.getSkillType();
            if (character.getSkillLevel(attackStyleSkillType, null/*useHand*/, false/*sizeAdjust*/,
                                        false/*adjustForEncumbrance*/, false/*adjustForHolds*/) == 0) {
               byte desiredSecondaryLevel = getSkillLevel(points / 4, pointsLeft / 4);
               if (primaryProfessionType.skillList.contains(attackStyleSkillType)) {
                  // if we already have this profession, use its level (or the familiar proficiency level of it)
                  int pointsForSecondaryLevel = (int) ((pointsLeft / 3.0) * CombatServer.random());
                  if (pointsForSecondaryLevel >= 4) {
                     desiredSecondaryLevel = weaponLevel;
                     if (pointsForSecondaryLevel < 10) {
                        desiredSecondaryLevel -= 2;
                     }
                  }
               }
               Byte requiredSecondaryLevel = requiredSkills.get(attackStyleSkillType);
               if (requiredSecondaryLevel != null) {
                  desiredSecondaryLevel = requiredSecondaryLevel;
               } else {
                  // Don't let the secondary weapon level be higher than the primary weapon level.
                  desiredSecondaryLevel = (byte) Math.min(desiredSecondaryLevel, weaponLevel);
               }
               ProfessionType secondaryProfession = ProfessionType.Fighter;
               for (ProfessionType pt : new ProfessionType[]{primaryProfessionType,
                                                             ProfessionType.Fighter,
                                                             ProfessionType.MartialArtist,
                                                             ProfessionType.Shooting}) {
                  if (pt.skillList.contains(attackStyleSkillType)) {
                     secondaryProfession = pt;
                     break;
                  }
               }
               character.setSkillLevel(secondaryProfession, attackStyleSkillType, desiredSecondaryLevel);
               pointsLeft = points - character.getPointTotal();
            }
         }
      }
      byte desiredBrawlingLevel = getSkillLevel(points / 3, pointsLeft / 3);
      SkillType unarmedCombatType = null;
      boolean naturalWeapons = race.hasProperty(Race.PROPERTIES_HORNS) ||
                               race.hasProperty(Race.PROPERTIES_CLAWS) ||
                               race.hasProperty(Race.PROPERTIES_FANGS);
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
      else if (naturalWeapons) {
         // If they have any natural weapons, be sure to get brawling:
         unarmedCombatType = SkillType.Brawling;
      }
      double unarmedCombatChance = Math.max(0d, Math.min(95d, points));
      if (naturalWeapons) {
         unarmedCombatChance = 100d;
      }
      if (primaryWeapon.isUnarmedStyle()) {
         unarmedCombatChance = unarmedCombatChance/5;
      }
      if (!character.isAnimal() && (unarmedCombatChance > (CombatServer.random() * 100))) {
         boolean forced = false;
         while (!forced && (unarmedCombatType == null)) {

            // It's possible that we chose Karate (or another offensive combat style) as our primary weapon.
            // If so, get a secondary unarmed combat skill, since this character is an unarmed combat practitioner.
            if (weaponSkill.getType() == SkillType.Karate) {
               unarmedCombatType = SkillType.Aikido;
               forced = true;
            }
            else if (weaponSkill.getType() == SkillType.Boxing) {
               unarmedCombatType = SkillType.Wrestling;
               forced = true;
            }
            else {
               double rnd = CombatServer.random();
               if (rnd < .05) {
                  unarmedCombatType = SkillType.Aikido; // 5%
               }
               else if (rnd < .15) {
                  unarmedCombatType = SkillType.Karate; // 10%
               }
               else if (rnd < .35) {
                  unarmedCombatType = SkillType.Boxing; // 20%
               }
               else if (rnd < .60) {
                  unarmedCombatType = SkillType.Wrestling;// 25%
               }
               else {
                  unarmedCombatType = SkillType.Brawling; // 40%
               }
            }
            List<SkillType> dissallowedSkillsForRace = DISSALLOWED_SKILLS_FOR_RACE.get(race.getName());
            if ((dissallowedSkillsForRace != null) && (dissallowedSkillsForRace.contains(unarmedCombatType))) {
               // try again until we find a skill we're allowed
               unarmedCombatType = null;
               // If our primary weapon was an unarmed style, forced will be true, and
               // we will exit the loop with no unarmed Combat Type.
            }
            else if (primaryProfessionType == ProfessionType.MartialArtist
                && unarmedCombatType == SkillType.Brawling) {
               // If we are a martial artist, don't learn the brawling skill
               unarmedCombatType = null;
            }
         }
         if ((unarmedCombatType != null) && (desiredBrawlingLevel > 0)) {
            SkillRank desiredRank = SkillRank.FAMILIAR;
            double excessPoints = pointsLeft - SkillRank.PROFICIENT.getCost();
            if (excessPoints > CombatServer.random() * 20 || naturalWeapons) {
               desiredRank = SkillRank.PROFICIENT;
            }

            Byte reqUnarmedCombatLevel = requiredSkills.get(unarmedCombatType);
            if (reqUnarmedCombatLevel != null) {
               desiredBrawlingLevel = reqUnarmedCombatLevel;
               desiredRank = SkillRank.PROFICIENT;
            }
            Profession primaryProfession = character.getProfession(primaryProfessionType);
            if (primaryProfessionType.skillList.contains(unarmedCombatType)) {
               primaryProfession.setRank(unarmedCombatType, desiredRank);
            }
            else {
               // don't learn 'Brawling' or 'Boxing' if we already know Karate
               if (primaryProfessionType != ProfessionType.MartialArtist ||
                   (!primaryProfession.getProficientSkills().contains(SkillType.Karate) &&
                    !primaryProfession.getFamiliarSkills().contains(SkillType.Karate)) ||
                   (unarmedCombatType != SkillType.Brawling &&
                    unarmedCombatType != SkillType.Boxing)) {
                   for (ProfessionType pt : new ProfessionType[] {primaryProfessionType,
                                                                  ProfessionType.Fighter,
                                                                  ProfessionType.MartialArtist}) {
                      if (pt.skillList.contains(unarmedCombatType)) {
                         character.setSkillLevel(pt, unarmedCombatType, desiredBrawlingLevel);
                         break;
                      }
                   }
               }
            }
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
            character.addEquipment(Weapons.getWeapon(primaryWeapon.getName(), race));
         }
         if (throwWeapon) {
            byte desiredThrowLevel = getSkillLevel(points / 6, pointsLeft / 6);
            character.setSkillLevel(ProfessionType.Athlete, SkillType.Throwing, desiredThrowLevel);
         }
      }

      Attribute[] atts = new Attribute[] { Attribute.Dexterity, Attribute.Nimbleness, Attribute.Health, Attribute.Toughness};
      for (Attribute attr : atts) {
         if (requiredAttributes.get(attr.shortName) == null) {
            byte attLevel = getAttributeLevel(points, pointsLeft);
            attLevel += race.getAttributeMods(attr);
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
            byte dex = (byte) (character.getAttributeLevel(Attribute.Dexterity) - race.getAttributeMods(Attribute.Dexterity));
            byte nim = (byte) (character.getAttributeLevel(Attribute.Nimbleness) - race.getAttributeMods(Attribute.Nimbleness));
            if (dex < nim) {
               // switch DEX and NIM
               byte newDex = (byte) (nim + race.getAttributeMods(Attribute.Dexterity));
               byte newNim = (byte) (dex + race.getAttributeMods(Attribute.Nimbleness));
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
            byte dex = (byte) (character.getAttributeLevel(Attribute.Dexterity) - race.getAttributeMods(Attribute.Dexterity));
            byte ht = (byte) (character.getAttributeLevel(Attribute.Health) - race.getAttributeMods(Attribute.Health));
            if (dex < ht) {
               // switch DEX and HT
               byte newDex = (byte) (ht + race.getAttributeMods(Attribute.Dexterity));
               byte newHt = (byte) (dex + race.getAttributeMods(Attribute.Health));
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
         SkillType primarySkill = character.getBestSkillType(primaryWeapon);
         if (primarySkill.isAdjustedForEncumbrance) {
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

      boolean hasKnife = (primaryWeapon.getName().equals(Weapon.NAME_Knife));
      if (haveKnifeChance > (CombatServer.random() * 100)) {
         Weapon knife = Weapons.getWeapon(Weapon.NAME_Knife, race);
         if (!hasKnife) {
            if (weightAvailable > knife.getAdjustedWeight()) {
               if ((unarmedCombatType == null) || (pointsLeft >= SkillRank.FAMILIAR.getCost())) {
                  weightCarried += knife.getWeight();
                  weightAvailable = maxWeightCarried - weightCarried;
                  character.addEquipment(knife);
                  SkillRank desiredRank = SkillRank.FAMILIAR;
                  double excessPoints = pointsLeft - SkillRank.PROFICIENT.getCost();
                  if (excessPoints > CombatServer.random() * 20) {
                     desiredRank = SkillRank.PROFICIENT;
                  }
                  ProfessionType knifeProfessionType = ProfessionType.Fighter;
                  SkillType knifeSkill = knife.getAttackStyle(0).getSkillType();
                  for (ProfessionType pt : new ProfessionType[] {primaryProfessionType,
                                                                 ProfessionType.Fighter,
                                                                 ProfessionType.MartialArtist}) {
                     if (pt.skillList.contains(knifeSkill)) {
                        knifeProfessionType = pt;
                        break;
                     }
                  }
                  Profession knifeProfession = character.getProfession(knifeProfessionType);
                  if (knifeProfession == null) {
                     List<Profession> profs = character.getProfessionsList();
                     byte desiredLevel = getSkillLevel(points, pointsLeft);
                     knifeProfession = new Profession(knifeProfessionType, knifeSkill, desiredLevel);
                     profs.add(knifeProfession);
                     character.setProfessionsList(profs);
                  }
                  knifeProfession.setRank(knifeSkill, desiredRank);

                  if (!throwWeapon) {
                     double throwSkillChance = Math.max(0d, Math.min(95d, points / 3d));
                     throwWeapon = (throwSkillChance > (CombatServer.random() * 100));
                     if (throwWeapon) {
                        byte desiredThrowLevel = getSkillLevel(points / 6, pointsLeft / 6);
                        character.setSkillLevel(ProfessionType.Athlete, SkillType.Throwing, desiredThrowLevel);
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
               throwableWeapon = Weapons.getWeapon(primaryWeapon.getName(), race);
            }
         }
         if (throwableWeapon == null) {
            // Consider a knife, if they know how to use it
            if (character.getSkillLevel(SkillType.Knife, LimbType.HAND_RIGHT, false, true, true) > 0) {
               Weapon knife = Weapons.getWeapon(Weapon.NAME_Knife, race);
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
         Object attChanged = null;
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
               if (element.equals("adv") && !character.isAnimal() && !character.hasAdvantage(Advantage.UNDEAD)) {
                  reduced = (reduceCharacterAdvantages(character) != null);
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
      for (Profession prof : character.getProfessionsList()) {
         if (prof.getProficientSkills().isEmpty()) {
            DebugBreak.debugBreak("Empty proficient set in profession list. " + prof.getType().getName());
         }
      }

      character.computeWealth();
      pointsLeft = points - character.getPointTotal();
      // if we have any weight and money left over, buy some potions - unless we are undead:
      if (race.getAdvantage(Advantage.UNDEAD) == null)
      {
         adjStrength = character.getAdjustedStrength();
         nimbleness = character.getAttributeLevel(Attribute.Nimbleness);
         maxWeightCarried = Rules.getMaxWeightForEncLevel(adjStrength, nimbleness, desiredEncLevel);
         character.computeWealth();
         Advantage wealth = character.getAdvantage(Advantage.WEALTH);
         // adjust for the wealth multiplier for the character's race:
         float wealthMultiplier = race.getWealthMultiplier();

         // allow the character to go up a max of 3 levels
         String baseWealth = wealth.getLevelName();
         // remove the '$' and all commas as we parse this money string:
         int racialWealthValue = Integer.parseInt(baseWealth.substring(1).replaceAll(",", ""));
         maxExpenditure = Math.round(racialWealthValue * wealthMultiplier);

         weightAvailable = maxWeightCarried - weightCarried;
         int allowableExpenditure = maxExpenditure - character.getTotalCost();
         List<Potion> desiredPotions = new ArrayList<>();
         desiredPotions.add(Potion.getPotion(Potion.POTION_FULL_HEALING, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_SPEED, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_MAJOR_HEALING, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_INCR_STRENGTH, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_HEALING, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_INCR_DEXTERITY, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_MAJOR_HEALING, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_INCR_NIMBLENESS, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_HEALING, race));
         desiredPotions.add(Potion.getPotion(Potion.POTION_MINOR_HEALING, race));

         while ((weightAvailable > 1) && (allowableExpenditure > 250) && (desiredPotions.size() > 0)) {
            Potion potion = desiredPotions.remove(0);
            if ((potion.weight > weightAvailable) || (potion.cost > allowableExpenditure)) {
               continue;
            }
            character.addEquipment(potion);
            weightAvailable -= potion.weight;
            allowableExpenditure -= potion.cost;
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
      SkillType primarySkill = character.getBestSkillType(primaryWeapon);
      Profession primaryProfession = null;
      List<Profession> professions = character.getProfessionsList();
      for (Profession profession : professions) {
         ArrayList<SkillType> proficientSkills = new ArrayList<>(profession.getProficientSkills());
         ArrayList<SkillType> familiarSkills = new ArrayList<>(profession.getFamiliarSkills());
         boolean isPrimaryProfession = (primarySkill != null) && (proficientSkills.contains(primarySkill));
         for (List<SkillType> skillList : new ArrayList[]{proficientSkills, familiarSkills}) {
            for (SkillType skill : skillList) {
               boolean isPrimarySkill = isPrimaryProfession && (skill == primarySkill);
               if (isPrimarySkill) {
                  primaryProfession = profession;
               }
               if (skill.isAdjustedForEncumbrance) {
                  byte encLevel = Rules.getEncumbranceLevel(character);

                  String skillName = skill.getName();
                  int minSkillPenalty = 0;
                  // Wrestling can't attack without a -2 penalty,
                  // and Karate needs a penalty of -2 to kick, so to make it worth more than boxing, use 2
                  if (skillName.equals(SkillType.Wrestling.getName()) ||
                      skillName.equals(SkillType.Karate.getName())) {
                     minSkillPenalty = 2;
                  }

                  byte requiredLevel = (byte) (encLevel + minSkillPenalty + 1);

                  if (requiredLevel > profession.getLevel(skill)) {

                     // Can we increase the profession level to make this work?
                     int curCost = Rules.getProfessionCost(profession.getLevel());
                     int reqCost = Rules.getProfessionCost(requiredLevel);
                     // If this is our primary profession, we absolutely MUST increase its level to make it work.
                     boolean mustIncrease = isPrimarySkill ||
                                            ((reqCost - curCost) < (maxCharPoints - character.getPointTotal())) ||
                                            (skill == SkillType.Brawling && (character.getRace().hasProperty(Race.PROPERTIES_HORNS) ||
                                                                             character.getRace().hasProperty(Race.PROPERTIES_CLAWS) ||
                                                                             character.getRace().hasProperty(Race.PROPERTIES_FANGS)));

                     if (mustIncrease) {
                        if ((requiredLevel + 2) > profession.getLevel(skill) && skillList == familiarSkills) {
                           // move this skill from familiar to proficient
                           familiarSkills.remove(skill);
                           proficientSkills.add(skill);
                           profession.setProficientSkills(proficientSkills);
                           profession.setFamiliarSkills(familiarSkills);
                           character.setProfessionsList(professions);
                           // recurse to look for more
                           checkSkillLevels(character, primaryWeapon, maxCharPoints);
                           return;
                        }
                        profession.setLevel(requiredLevel);
                        character.setProfessionsList(professions);
                        // recurse to look for more
                        checkSkillLevels(character, primaryWeapon, maxCharPoints);
                        return;
                     }
                     // Get rid of this skill, we can't afford to increase it to the effective level
                     skillList.remove(skill);
                     if (proficientSkills.isEmpty()) {
                        if (familiarSkills.isEmpty()) {
                           professions.remove(profession);
                           character.setProfessionsList(professions);
                           // recurse to look for more
                           checkSkillLevels(character, primaryWeapon, maxCharPoints);
                           return;
                        }
                        // Move something from our familiar list into the proficient list.
                        proficientSkills.add(familiarSkills.remove(0));
                     }
                     profession.setProficientSkills(proficientSkills);
                     profession.setFamiliarSkills(familiarSkills);
                     character.setProfessionsList(professions);
                     // recurse to look for more
                     checkSkillLevels(character, primaryWeapon, maxCharPoints);
                     return;
                  }
               }
            }
         }
         // Make sure no profession is higher than the primary profession
      }

      for (Profession profession : professions) {
         // Make sure no profession is higher than the primary prof
         if ((primaryProfession != null) && (profession.getLevel() > primaryProfession.getLevel())) {
            profession.setLevel((byte) (profession.getLevel() - 1));
            primaryProfession.setLevel((byte) (primaryProfession.getLevel() + 1));
            character.setProfessionsList(professions);
            // recurse to look for more
            checkSkillLevels(character, primaryWeapon, maxCharPoints);
            return;
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

   private static void computeBestArmorAndShield(Character character, byte shieldLevel, byte desiredEncLevel,
                                                 Armor requiredArmor, Shield requiredShield, int maxExpenditure) {
      // These lists must be sorted by weight so we can find the best armor for our carrying limit:
      List<Armor> armors = Armor.getArmorListForRace(character.getRace());
      List<Shield> shields = Shield.getShieldListForRace(character.getRace());
      double elvenArmorOdds = 0;
      if (character.getRace().getName().equals(Race.NAME_Elf)) {
         elvenArmorOdds = 0.90;
      }
      else if (character.getRace().getName().equals(Race.NAME_HalfElf)) {
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
      if ((!character.getRace().getName().equals(Race.NAME_Dwarf)) && (!character.getRace().getName().equals(Race.NAME_Gnome))) {
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
      // If we have four arms, the shields go in the second pair
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
      TreeSet<Profession> professions = sortProfessionsByLevel(character.getProfessionsList(), false/*ascending*/);
      for (Profession profession : professions) {
         // check if any the skills in this profession are also in the required skills list.
         // if they are, don't adjust the profession level
         List<SkillType> allSkills = new ArrayList<>(profession.getFamiliarSkills());
         allSkills.addAll(profession.getProficientSkills());
         if (allSkills.stream().noneMatch(requiredSkills::containsKey)) {
            byte level = profession.getLevel();
            if (level < Rules.getMaxSkillLevel()) {
               if ((Rules.getProfessionCost((byte) (level + 1)) - Rules.getProfessionCost(level)) <= pointsLeft) {
                  character.setProfessionLevel(profession.getType(), profession.getProficientSkills().get(0), (byte)(level+1));
                  return true;
               }
            }
         }
      }
      return false;
   }

   private static boolean reduceCharacterSkillPoints(Character character, int pointsLeft, HashMap<SkillType, Byte> requiredSkills) {
      // Look for the smallest skill level decrease that will satisfy the pointsLeft.
      TreeSet<Profession> professions = sortProfessionsByLevel(character.getProfessionsList(), true/*ascending*/);
      Profession lastSkillWeCanModify = null;
      for (Profession profession : professions) {
         // check if any the skills in this profession are also in the required skills list.
         // if they are, don't adjust the profession level
         List<SkillType> allSkills = new ArrayList<>(profession.getFamiliarSkills());
         allSkills.addAll(profession.getProficientSkills());
         if (allSkills.stream().noneMatch(requiredSkills::containsKey)) {
            byte level = profession.getLevel();
            if (level > 0) {
               lastSkillWeCanModify = profession;
               if ((Rules.getProfessionCost((byte) (level - 1)) - Rules.getProfessionCost(level)) <= pointsLeft) {
                  profession.setLevel((byte) (level - 1));
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

   public static TreeSet<Profession> sortProfessionsByLevel(List<Profession> professionList, final boolean ascending) {
      TreeSet<Profession> professions = new TreeSet<>((o1, o2) -> (ascending ? 1 : -1) * Byte.compare(o1.getLevel(), o2.getLevel()));
      professions.addAll(professionList);
      return professions;
   }

   private static Object increaseCharacterPoints(Character character, int pointsLeft, byte startIndex,
                                                    byte attIndex, HashMap<String, Integer> requiredAttributes) {
      Attribute[] atts = new Attribute[] { Attribute.Intelligence,
                                           Attribute.Social,
                                           Attribute.Toughness,
                                           Attribute.Health,
                                           Attribute.Nimbleness,
                                           Attribute.Dexterity,
                                           null // this means we should add an advantage instead
                                    // don't include Strength here, because that's determined separately by weapon type
                                          };
      if (character.getWeapon() == null || !character.getWeapon().isReal() || character.isAnimal()) {
         // Allow the bump on the Strength attribute if we are not basing their strength on a weapon
         atts = new Attribute[]{Attribute.Strength,
                                Attribute.Intelligence,
                                Attribute.Social,
                                Attribute.Toughness,
                                Attribute.Health,
                                Attribute.Nimbleness,
                                Attribute.Dexterity,
                                null // this means we should add an advantage instead
                                // don't include Strength here, because that's determined separately by weapon type
         };
      }
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
      if (atts[attIndex] == null) {
         // only add advantages 20% of the time we hit that null;
         if (CombatServer.random() > .8 && !character.isAnimal() && !character.hasAdvantage(Advantage.UNDEAD)) {
            Advantage adv = increaseCharacterAdvantages(character);
            if (adv != null) {
               return adv;
            }
         }
      }
      else if (!requiredAttributes.containsKey(atts[attIndex].shortName)) {
         // don't mess with pre-defined attributes:
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

   static final Attribute[] REDUCE_ATTRIBUTES = new Attribute[] {Attribute.Intelligence,
                                                                 Attribute.Social,
                                                                 Attribute.Toughness,
                                                                 Attribute.Health,
                                                                 Attribute.Nimbleness,
                                                                 Attribute.Intelligence,
                                                                 Attribute.Social,
                                                                 Attribute.Toughness,
                                                                 Attribute.Intelligence,
                                                                 Attribute.Social};

   private static Attribute reduceCharacterAttributes(Character character, boolean allowDexAndStr, HashMap<String, Integer> requiredAttributes) {
      if (allowDexAndStr) {
         return reduceCharacterAttributes(character, new Attribute[] { Attribute.Dexterity, Attribute.Strength},
                                          requiredAttributes);
      }
      return reduceCharacterAttributes(character, REDUCE_ATTRIBUTES, requiredAttributes);
   }

   private static Attribute reduceCharacterAttributes(Character character, Attribute[] reducibleAttributes,
                                                      HashMap<String, Integer> requiredAttributes) {
      byte redAttIndex = (byte) (CombatServer.random() * reducibleAttributes.length);
      return reduceCharacterAttributes(character, redAttIndex, redAttIndex, reducibleAttributes, requiredAttributes);
   }

   private static Attribute reduceCharacterAttributes(Character character, byte startIndex, byte redAttIndex,
                                                      Attribute[] reducibleAttributes,
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

   static final Advantage[] DISADVANTAGES = new Advantage[] {
           Advantage.getAdvantage(Advantage.ALERTNESS + ":Unaware"),
           Advantage.getAdvantage(Advantage.APPEARANCE + ":Unattractive"),
           Advantage.getAdvantage(Advantage.APPEARANCE + ":Ugly"),
           Advantage.getAdvantage(Advantage.BAD_TEMPER),
           Advantage.getAdvantage(Advantage.BAD_TEMPER),
           Advantage.getAdvantage(Advantage.BAD_TEMPER),
           Advantage.getAdvantage(Advantage.BERSERKER),
           Advantage.getAdvantage(Advantage.BERSERKER),
           Advantage.getAdvantage(Advantage.CODE_OF_CONDUCT + ":Fair Fighter"),
           Advantage.getAdvantage(Advantage.CODE_OF_CONDUCT + ":Fair Fighter"),
           Advantage.getAdvantage(Advantage.CODE_OF_CONDUCT + ":Chivalrous"),
           Advantage.getAdvantage(Advantage.CODE_OF_CONDUCT + ":Bushido"),
           Advantage.getAdvantage(Advantage.COMPULSIVE_LIAR),
           Advantage.getAdvantage(Advantage.COMPULSIVE_LIAR),
           Advantage.getAdvantage(Advantage.DISFIGURED_FACE + ":Minor scars"),
           Advantage.getAdvantage(Advantage.DISFIGURED_FACE + ":Minor scars"),
           Advantage.getAdvantage(Advantage.DISFIGURED_FACE + ":Scars"),
           Advantage.getAdvantage(Advantage.DISFIGURED_FACE + ":Severe scars"),
           Advantage.getAdvantage(Advantage.GREEDY),
           Advantage.getAdvantage(Advantage.GREEDY),
           Advantage.getAdvantage(Advantage.HEARING + ":Poor Hearing"),
           Advantage.getAdvantage(Advantage.HONEST),
           Advantage.getAdvantage(Advantage.HONEST),
           Advantage.getAdvantage(Advantage.HONEST),
           Advantage.getAdvantage(Advantage.LECHEROUS),
           Advantage.getAdvantage(Advantage.MUTE),
           Advantage.getAdvantage(Advantage.PHOBIA + ":Moderate"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_FEMALE + ":Serf"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_FEMALE + ":Peasant"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_MALE + ":Serf"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_MALE + ":Peasant"),
           Advantage.getAdvantage(Advantage.SADISTIC),
           Advantage.getAdvantage(Advantage.VISION + ":Poor Sight")
   };
   static final Advantage[] ADVANTAGES    = new Advantage [] {
           Advantage.getAdvantage(Advantage.AMBIDEXTROUS),
           Advantage.getAdvantage(Advantage.ALERTNESS + ":Alert"),
           Advantage.getAdvantage(Advantage.ALERTNESS + ":Alert"),
           Advantage.getAdvantage(Advantage.ALERTNESS + ":Very Alert"),
           Advantage.getAdvantage(Advantage.APPEARANCE + ":Attractive"),
           Advantage.getAdvantage(Advantage.HEARING + ":Acute Hearing"),
           Advantage.getAdvantage(Advantage.KNIGHTHOOD),
           Advantage.getAdvantage(Advantage.MAGIC_RESISTANCE + ":+1"),
           Advantage.getAdvantage(Advantage.MAGIC_RESISTANCE + ":+1"),
           Advantage.getAdvantage(Advantage.MAGIC_RESISTANCE + ":+2"),
           Advantage.getAdvantage(Advantage.MAGIC_RESISTANCE + ":+2"),
           Advantage.getAdvantage(Advantage.MAGIC_RESISTANCE + ":+3"),
           Advantage.getAdvantage(Advantage.RANK_MILITARY_ENLISTED + ":Sergeant"),
           Advantage.getAdvantage(Advantage.RANK_MILITARY_ENLISTED + ":Sergeant"),
           Advantage.getAdvantage(Advantage.RANK_MILITARY_ENLISTED + ":High Sergeant"),
           Advantage.getAdvantage(Advantage.RANK_MILITARY_OFFICER + ":Lieutenant"),
           Advantage.getAdvantage(Advantage.RANK_MILITARY_OFFICER + ":Captain"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_FEMALE + ":Baronness"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_FEMALE + ":Viscountess"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_MALE + ":Baron"),
           Advantage.getAdvantage(Advantage.RANK_SOCIAL_MALE + ":Viscount"),
           Advantage.getAdvantage(Advantage.VISION + ":Acute Vision"),
           };

   private static Advantage reduceCharacterAdvantages(Character character) {
      byte redAdvIndex = (byte) (CombatServer.random() * DISADVANTAGES.length);
      return applyOneAdvantageFromArray(character, redAdvIndex, redAdvIndex, DISADVANTAGES, false);
   }
   private static Advantage increaseCharacterAdvantages(Character character) {
      byte redAdvIndex = (byte) (CombatServer.random() * ADVANTAGES.length);
      return applyOneAdvantageFromArray(character, redAdvIndex, redAdvIndex, ADVANTAGES, true);
   }

   private static Advantage applyOneAdvantageFromArray(Character character, byte startIndex, byte redAdvIndex,
                                                       Advantage[] advantage, boolean mustAddPoints) {
      Advantage adv = advantage[redAdvIndex];
      Advantage existingAdv = character.getAdvantage(adv.name);
      boolean canAdd = true;
      if (existingAdv != null) {
         // If we already have the base advantage, make sure adding this advantage doesn't
         // replace an existing advantage that causes our points to move in the wrong direction.
         int existingAdvCost = existingAdv.getCost(character.getRace());
         int newAdvCost = adv.getCost(character.getRace());
         if (mustAddPoints) {
            canAdd = newAdvCost > existingAdvCost;
         } else {
            canAdd = newAdvCost < existingAdvCost;
         }
      }
      // Women can't be in the military, and can't be knighted in these times:
      boolean isOfficer = adv.getName().equals(Advantage.RANK_MILITARY_OFFICER);
      boolean isMilitaryRank = adv.getName().equals(Advantage.RANK_MILITARY_ENLISTED) ||
                               isOfficer;
      boolean isSocialRank = adv.getName().equals(Advantage.RANK_SOCIAL_FEMALE) ||
                             adv.getName().equals(Advantage.RANK_SOCIAL_MALE);
      Advantage wealth = character.getAdvantage(Advantage.WEALTH);
      // Prevent a serf or peasant from being wealthy, and prevent poor royalty:
      boolean isWealthy = wealth.getCost(character.getRace()) > 0;
      boolean isPoor = wealth.getCost(character.getRace()) < 0;

      if (isSocialRank){
         boolean isSerfOrPeasant = adv.getCost(character.getRace()) < 0;
         boolean isRoyalty = adv.getCost(character.getRace()) > 0;
         if (isSerfOrPeasant && isWealthy) {
            canAdd = false;
         }
         if (isRoyalty && isPoor) {
            canAdd = false;
         }
      }
      if (isOfficer && isPoor) {
         canAdd = false;
      }
      if (canAdd && character.getRace().getGender() == Gender.FEMALE) {
         if (isMilitaryRank || adv.getName().equals(Advantage.KNIGHTHOOD)) {
            canAdd = false;
         }
      }
      if (canAdd && character.getRace().getWealthMultiplier() < 1.0) {
         if (isSocialRank || adv.getName().equals(Advantage.KNIGHTHOOD)) {
            canAdd = false;
         }
      }
      if (canAdd && character.addAdvantage(adv)) {
         return adv;
      }
      if (++redAdvIndex >= advantage.length) {
         redAdvIndex = 0;
      }
      if (redAdvIndex == startIndex) {
         return null;
      }
      return applyOneAdvantageFromArray(character, startIndex, redAdvIndex, advantage, mustAddPoints);
   }

   private static byte getAttributeLevel(int points, int pointsLeft) {
      int avePoint = (points + (2 * pointsLeft)) / 3;
      int attrMaxPoints;
      if (points > 750) {
         attrMaxPoints = (int) (avePoint * .2);
      } else if (points > 500) {
         attrMaxPoints = (int) (avePoint * .3);
      } else if (points > 400) {
         attrMaxPoints = (int) (avePoint * .35);
      } else if (points > 300) {
         attrMaxPoints = (int) (avePoint * .4);
      } else if (points > 100) {
         attrMaxPoints = (int) (avePoint * .6);
      } else if (points > 50) {
         attrMaxPoints = (int) (avePoint * .5);
      } else {
         attrMaxPoints = (int) (avePoint * .4);
      }
      //attrMaxPoints = Math.min(attrMaxPoints, Rules.getAttCost(Rules.getMaxAttribute()));
      int attrPoints = (int) (attrMaxPoints * ((CombatServer.random() + CombatServer.random()) / 2.0));

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
      if (pointsLeft > 500) {
         skillLevel = 10;
      } else if (pointsLeft > 400) {
         skillLevel = 9;
      } else if (pointsLeft > 300) {
         skillLevel = 8;
      } else if (pointsLeft > 200) {
         skillLevel = 7;
      } else if (pointsLeft > 150) {
         skillLevel = 6;
      } else if (pointsLeft > 100) {
         skillLevel = 5;
      } else if (pointsLeft > 40) {
         skillLevel = 4;
      } else if (pointsLeft > 0) {
         skillLevel = 3;
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
      if ((rnd < .2) && (skillLevel > 3)) {
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

   private static Weapon getBestWeaponForStrength(byte str, boolean missileWeapon, boolean allowTwoHanded,
                                                  int maxExpenditure, Race racialBase) {
      List<String> appropriateWeapons = new ArrayList<>();
      getBestWeaponForStrength(str, missileWeapon, appropriateWeapons, allowTwoHanded, maxExpenditure, racialBase);
      getBestWeaponForStrength(str, missileWeapon, appropriateWeapons, allowTwoHanded, maxExpenditure, racialBase);
      if (!missileWeapon) {
         getBestWeaponForStrength((byte) (str - 1), missileWeapon, appropriateWeapons, allowTwoHanded, maxExpenditure, racialBase);
      }
      String weaponName = appropriateWeapons.get((int) (appropriateWeapons.size() * CombatServer.random()));
      return Weapons.getWeapon(weaponName, racialBase);
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
            Weapon weap = Weapons.getWeapon(weaponName, racialBase);
            if (weap.isOnlyTwoHanded() || weap.getName().equals(Weapon.NAME_Katana)) {
               removeWeapons.add(weaponName);
            }
            else if ((weaponName.equals(Weapon.NAME_BastardSword)) && (str < 8)) {
               // Bastard swords should only be used one-handed if we have a STR of 8 of greater
               removeWeapons.add(weaponName);
            }
            else if ((weaponName.equals(Weapon.NAME_Katana)) && (str < 1)) {
               // Katanas should only be used one-handed if we have a STR of 1 of greater
               removeWeapons.add(weaponName);
            }
         }
      }
      if (maxExpenditure >= 0) {
         boolean allowDelicateWeapons = true
                                        && !racialBase.getName().equals(Race.NAME_Centaur)
                                        && !racialBase.getName().equals(Race.NAME_Giant)
                                        && !racialBase.getName().equals(Race.NAME_HalfOgre)
                                        && !racialBase.getName().equals(Race.NAME_LizardMan)
                                        && !racialBase.getName().equals(Race.NAME_Ogre)
                                        && !racialBase.getName().equals(Race.NAME_Orc)
                                        && !racialBase.getName().equals(Race.NAME_Troll)
                                        && !racialBase.getName().equals(Race.NAME_Minotaur)
                                        && !racialBase.getName().equals(Race.NAME_Zombie)
                 ;
         if (racialBase.getName().equals(Race.NAME_HalfOrc)) {
            allowDelicateWeapons = CombatServer.random() < .5;
         }

         for (String weaponName : appropriateWeapons) {
            Weapon weap = Weapons.getWeapon(weaponName, racialBase);
            if ((weap != null) && (weap.getCost() > maxExpenditure)) {
               removeWeapons.add(weaponName);
            }
            else if (!allowDelicateWeapons) {
               if (weap.getName().equals(Weapon.NAME_Rapier) || weap.getName().equals(Weapon.NAME_Sabre)) {
                  removeWeapons.add(weaponName);
               }
               else if (weap.getName().equals(Weapon.NAME_Katana) && CombatServer.random() < .5) {
                  removeWeapons.add(weaponName);
               }
            }
         }
      }
      appropriateWeapons.removeAll(removeWeapons);
      if (appropriateWeapons.isEmpty()) {
         appropriateWeapons.add(Weapon.NAME_Punch);
         Weapon club = Weapons.getWeapon(Weapon.NAME_Club, racialBase);
         if ((club != null) && (maxExpenditure > club.getCost())) {
            appropriateWeapons.add(Weapon.NAME_Club);
         }
      }
   }
}
