package ostrowski.combat.common;

import ostrowski.combat.common.enums.SkillType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ProfessionType {
   Fighter("Fighter", new SkillType[] {
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
   Shooting("Shooting", new SkillType[] {
              SkillType.BlowGun,
              SkillType.Bow,
              SkillType.Crossbow,
              SkillType.Sling
   }),
   MartialArtist("Martial Arts", new SkillType[] {
           SkillType.Boxing,
           SkillType.Knife,
           SkillType.Sword,
           SkillType.Spear,
           SkillType.Quarterstaff,
           SkillType.Wrestling,
           SkillType.Aikido,
           SkillType.Karate,
           SkillType.NunChucks
   }),
   Knight("Knight", new SkillType[] {
              SkillType.Riding,
              SkillType.Jousting,
              SkillType.Tactics
   }),
   Thief("Thief", new SkillType[] {
              SkillType.Stealth,
              SkillType.PickPocket,
              SkillType.LockPicking
   }),
   Spellcasting("Spellcasting", new SkillType[] {
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
              SkillType.Spellcasting_Enchantment
   }),
   Common("Common", new SkillType[] {
           SkillType.Brawling,
           SkillType.Throwing,
           SkillType.Swimming,
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
