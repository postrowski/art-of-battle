package ostrowski.combat.common.spells.priest;


import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.Profession;
import ostrowski.combat.common.ProfessionType;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.SkillType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum Deity {

   APHRODITE("Aphrodite", Arrays.asList(SpellGroup.DEFENSIVE, SpellGroup.INFORMATION, SpellGroup.HEALING, SpellGroup.GOOD),
             (chr) -> {return minSoc(chr, 1, (chr.getGender() == Race.Gender.FEMALE) ? 0 : 2, "Aphrodite");}),
   DRUID("Druid", Arrays.asList(SpellGroup.NATURE, SpellGroup.ELEMENTAL, SpellGroup.HEALING),
             (chr) -> {return minSoc(chr, 2, 0, "Druid");}),
   HADES("Hades", Arrays.asList(SpellGroup.DEFENSIVE, SpellGroup.EVIL, SpellGroup.INFORMATION, SpellGroup.OFFENSIVE),
             (chr) -> {return minSoc(chr, 2, 0, "Hades");}),
   TAO("Tao", Arrays.asList(SpellGroup.EVIL, SpellGroup.GOOD, SpellGroup.HEALING, SpellGroup.NATURE),
             (chr) -> {
      Advantage divAff = chr.getAdvantage(Advantage.DIVINE_AFFINITY_ + "Tao");
      if (divAff == null) {
         return true;
      }
      byte affinityLevel = (byte) (divAff.getLevel() + 1);
      Profession martialArts = chr.getProfession(ProfessionType.MartialArtist);
      if ((martialArts == null) || (martialArts.getLevel() < affinityLevel)) {
         chr.setProfessionLevel(ProfessionType.MartialArtist, SkillType.Aikido, affinityLevel);
         martialArts = chr.getProfession(ProfessionType.MartialArtist);
      }

      setSkillRankToProficient(martialArts, Arrays.asList(SkillType.Aikido,
                                                          SkillType.Karate));
      int[] minSkillsForDA = new int[]{0, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7};
      while (martialArts.getProficientSkills().size() < minSkillsForDA[affinityLevel]) {
         // convert familar skills into proficient skills:
         boolean foundFamSkill = false;
         for (SkillType skill : martialArts.getFamiliarSkills()) {
            if (ProfessionType.MartialArtist.skillList.contains(skill)) {
               List<SkillType> reqSkills = new ArrayList<>();
               reqSkills.add(skill);
               if (setSkillRankToProficient(martialArts, reqSkills)) {
                  foundFamSkill = true;
                  break;
               }
            }
         }
         if (!foundFamSkill) {
            for (SkillType skill : ProfessionType.MartialArtist.skillList) {
               if (!martialArts.getProficientSkills().contains(skill)) {
                  List<SkillType> reqSkills = new ArrayList<>();
                  reqSkills.add(skill);
                  if (setSkillRankToProficient(martialArts, reqSkills)) {
                     break;
                  }
               }
            }
         }
      }
      return true;
   }),
   THOR("Thor", Arrays.asList(SpellGroup.DEFENSIVE, SpellGroup.HEALING, SpellGroup.GOOD, SpellGroup.OFFENSIVE),
        (chr) -> {
      Advantage divAff = chr.getAdvantage(Advantage.DIVINE_AFFINITY_ + "Thor");
      if (divAff == null) {
         return true;
      }
      byte affinityLevel = (byte)(divAff.getLevel() + 1);
      if (!minSoc(chr, 2, 0, "Thor")) {
         return false;
      }
      Profession fighter = chr.getProfession(ProfessionType.Fighter);
      if (fighter == null) {
         chr.setProfessionLevel(ProfessionType.Fighter, SkillType.AxeMace, affinityLevel);
         fighter = chr.getProfession(ProfessionType.Fighter);
      }
      if (fighter.getLevel() < affinityLevel) {
         fighter.setLevel(affinityLevel);
      }
      setSkillRankToProficient(fighter, Arrays.asList(SkillType.AxeMace,
                                                      SkillType.TwoHanded_AxeMace,
                                                      SkillType.Quarterstaff));
      return true;
   }),
   DEMONIC("Demonic", Arrays.asList(SpellGroup.DEMON, SpellGroup.EVIL), (chr) -> true);

   private final String                       name;
   private final List<SpellGroup>             spellGroups;
   private final Function<Character, Boolean> validator;

   Deity(String name, List<SpellGroup> spellGroups, Function<Character, Boolean> validator) {
      this.name = name;
      this.spellGroups = spellGroups;
      this.validator = validator;
   }

   public String getName() {
      return this.name;
   }
   public boolean validateCharacter(Character chr) {
      return this.validator.apply(chr);
   }
   public List<SpellGroup> getSpellGroups() {
      return this.spellGroups;
   }

   public static Deity getByName(String name) {
      for (Deity deity : values()) {
         if (deity.getName().equalsIgnoreCase(name)) {
            return deity;
         }
      }
      return null;
   }

   private static boolean minSoc(Character chr, int divisor, int penalty, String deityName) {
      Advantage divAff = chr.getAdvantage(Advantage.DIVINE_AFFINITY_ + deityName);
      if (divAff == null) {
         return true;
      }
      byte affinityLevel = (byte) (divAff.getLevel() + 1);
      float minSoc = ((float)affinityLevel) / divisor;
      if (minSoc > ((int)minSoc)) {
         minSoc = ((int)minSoc) + 1;
      }
      minSoc += penalty;
      if (chr.getAttributeLevel(Attribute.Social) >= minSoc) {
         return true;
      }
      chr.setAttribute(Attribute.Social, (byte)minSoc, true);
      if (chr.getAttributeLevel(Attribute.Social) >= minSoc) {
         return true;
      }
      if (affinityLevel <= 0) {
         return false;
      }
      divAff.setLevel((byte) (divAff.getLevel() - 1));
      return minSoc(chr, divisor, penalty, deityName);
   }

   private static boolean setSkillRankToProficient(Profession prof, List<SkillType> requiredSet) {
      List<SkillType> proficientSkills = prof.getProficientSkills();
      for (SkillType skill : requiredSet) {
         if (proficientSkills.contains(skill)) {
            // They already had one of the required skill at the proficient rank
            return false;
         }
      }
      List<SkillType> newProficientSkills = new ArrayList<>(proficientSkills);
      List<SkillType> familiarSkills = prof.getFamiliarSkills();
      boolean found = false;
      for (SkillType skill : requiredSet) {
         if (familiarSkills.contains(skill)) {
            ArrayList<SkillType> newFamiliarSkills = new ArrayList<>(familiarSkills);
            newFamiliarSkills.remove(skill);
            prof.setFamiliarSkills(newFamiliarSkills);
            // They already had one of the required skill at the familiar rank, make it proficient:
            newProficientSkills.add(skill);
            found = true;
            break;
         }
      }
      if (!found) {
         // They didn't have any of the required skills, add it at the proficient rank:
         newProficientSkills.add(requiredSet.get(0));
      }
      prof.setProficientSkills(newProficientSkills);
      return true;
   }
}
