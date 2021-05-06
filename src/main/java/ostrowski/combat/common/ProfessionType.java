package ostrowski.combat.common;

import ostrowski.combat.common.enums.SkillType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ProfessionType {
   Fighter("Fighter", new SkillType[]{
           SkillType.AxeMace,
           SkillType.Brawling,
           SkillType.Fencing,
           SkillType.Flail,
           SkillType.Knife,
           SkillType.Polearm,
           SkillType.Quarterstaff,
           SkillType.Shield,
           SkillType.Spear,
           SkillType.Sword,
           SkillType.TwoHanded_AxeMace,
           SkillType.TwoHanded_Sword
   }),
   Shooting("Shooting", new SkillType[]{
           SkillType.BlowGun,
           SkillType.Bow,
           SkillType.Crossbow,
           SkillType.Sling
   }),
   MartialArtist("Martial Arts", new SkillType[]{
           SkillType.Aikido,
           SkillType.Boxing,
           SkillType.Karate,
           SkillType.Knife,
           SkillType.NunChucks,
           SkillType.Quarterstaff,
           SkillType.Spear,
           SkillType.Sword,
           SkillType.Wrestling
   }),
   Knight("Knight", new SkillType[]{
           SkillType.Riding,
           SkillType.Jousting,
           SkillType.Tactics,
           SkillType.Leadership,
           SkillType.Heraldry,
           SkillType.Etiquette
   }),
   Thief("Thief", new SkillType[]{
           SkillType.Stealth,
           SkillType.Climbing,
           SkillType.PickPocket,
           SkillType.LockPicking,
           SkillType.Disguise,
           SkillType.Lying
   }),
   Assassin("Assassin", new SkillType[]{
           SkillType.Stealth,
           SkillType.Climbing,
           SkillType.Disguise,
           SkillType.Backstab,
           SkillType.Poisons
   }),
   Spellcasting("Spellcasting", new SkillType[]{
           SkillType.Spellcasting_Fire,
           SkillType.Spellcasting_Water,
           SkillType.Spellcasting_Earth,
           SkillType.Spellcasting_Air,
           SkillType.Spellcasting_Energy,
           SkillType.Spellcasting_Evocation,
           SkillType.Spellcasting_Conjuration,
           SkillType.Spellcasting_Illusion,
           SkillType.Spellcasting_Divination,
           SkillType.Spellcasting_Necromancy,
           SkillType.Spellcasting_Protection,
           SkillType.Spellcasting_Enchantment,
           SkillType.Arcane_History,
           SkillType.Ancient_Language,
           }),
   Common("Common", new SkillType[]{
           SkillType.AreaKnowledge,
           SkillType.Brawling,
           SkillType.CommonKnowledge,
           SkillType.Cooking,
           SkillType.Language2,
           SkillType.Observation,
           SkillType.Riding,
           SkillType.Swimming,
           SkillType.Throwing
   }),
   Athlete("Athlete", new SkillType[]{
           SkillType.Acrobatics,
           SkillType.Climbing,
           SkillType.Sport,
           SkillType.Riding,
           SkillType.Running,
           SkillType.Swimming,
           SkillType.Throwing
   }),
   Linguist("Linguist", new SkillType[]{
           SkillType.Language2,
           SkillType.Language3,
           SkillType.Language4,
           SkillType.Language5,
           SkillType.Language6
   }),
   Doctor("Doctor", new SkillType[]{
           SkillType.FirstAid,
           SkillType.Surgery,
           SkillType.Dentistry,
           SkillType.Veterinarian,
           SkillType.Poisons,
           SkillType.Herbalist
   }),
   Entertainer("Entertainer", new SkillType[]{
           SkillType.Singing,
           SkillType.Dancing,
           SkillType.GuitarPlaying,
           SkillType.Piano,
           SkillType.Flute,
           SkillType.Violin
   }),
   AnimalHandler("Animal Handler", new SkillType[]{
           SkillType.Riding,
           SkillType.AnimalTraining,
           SkillType.AnimalKnowledge
   }),
   Merchant("Merchant", new SkillType[]{
           SkillType.Bartering,
           SkillType.SexAppeal,
           SkillType.Persuasion,
           SkillType.Lying,
           SkillType.DetectLies,
           SkillType.Appraising
   }),
   Detective("Detective", new SkillType[]{
           SkillType.Investigation,
           SkillType.DetectLies,
           SkillType.Lying,
           SkillType.Persuasion,
           SkillType.Tracking
   }),
   Tradesman("Tradesman", new SkillType[]{
           SkillType.LeatherWorking,
           SkillType.Blacksmith,
           SkillType.StoneWorking,
           SkillType.Carpentry,
           SkillType.Repair,
           SkillType.Cooking,
           SkillType.Painting
   }),
   Survivalist("Survivalist", new SkillType[]{
           SkillType.AreaKnowledge,
           SkillType.Tracking,
           SkillType.Trapping,
           SkillType.Meteorology,
           SkillType.Stealth,
           SkillType.PlantKnowledge,
           SkillType.AnimalKnowledge
   }),
   Farmer("Farmer", new SkillType[]{
           SkillType.PlantKnowledge,
           SkillType.AnimalHusbandry,
           SkillType.Meteorology,
           SkillType.Geology,
           SkillType.Riding
   });

   public String name;
   public List<SkillType> skillList = new ArrayList<>();
   private ProfessionType(String name, SkillType[] skills) {
      this.name = name;
      skillList.addAll(Arrays.asList(skills));
   }

   public String getName() {
      return this.name;
   }

   public static ProfessionType getByName(String name) {
      for (ProfessionType type : values()) {
         if (type.getName().equalsIgnoreCase(name)) {
            return type;
         }
      }
      return null;
   }
}
