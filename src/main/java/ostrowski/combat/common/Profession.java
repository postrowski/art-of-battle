package ostrowski.combat.common;

import ostrowski.combat.common.enums.SkillType;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Profession extends SerializableObject {
   private ProfessionType  type;
   private byte            level;
   private List<SkillType> familiarSkills = new ArrayList<>();
   private List<SkillType> proficientSkills = new ArrayList<>();
   public Profession() {
      // used for serialization
   }
   public Profession(ProfessionType type, SkillType proficientSkillType, byte level) {
      this.type = type;
      this.level = level;
      if (proficientSkillType != null) {
         if (!type.skillList.contains(proficientSkillType)) {
            throw new IllegalArgumentException("Skill type " + proficientSkillType.getName() + " is not in profession" + type.getName());
         }
         this.proficientSkills.add(proficientSkillType);
      }
      setMinLevel();
   }
   public Profession(ProfessionType type, List<SkillType> proficientSkillTypes, byte level) {
      this.type = type;
      this.level = level;
      setProficientSkills(proficientSkillTypes);
   }

   public Profession(Profession other) {
      this.type = other.type;
      this.level = other.level;
      this.familiarSkills = new ArrayList<>(other.familiarSkills);
      this.proficientSkills = new ArrayList<>(other.proficientSkills);
      setMinLevel();
   }

   public ProfessionType getType() {
      return type;
   }

   public byte getLevel() {
      return level;
   }

   public void setLevel(byte level) {
      this.level = level;
      setMinLevel();
   }

   public void setRank(SkillType skillType, SkillRank rank) {
      if (!type.skillList.contains(skillType)) {
         throw new IllegalArgumentException("Skill type " + skillType.getName() + " is not in profession" + type.getName());
      }
      proficientSkills.remove(skillType);
      if (proficientSkills.isEmpty()) {
         // Dont let them downgrade the only proficient skill to Familiar
         rank = SkillRank.PROFICIENT;
      }
      familiarSkills.remove(skillType);
      if (rank == SkillRank.FAMILIAR) {
         familiarSkills.add(skillType);
      }
      else if (rank == SkillRank.PROFICIENT) {
         proficientSkills.add(skillType);
      }
      setMinLevel();
   }

   public byte getLevel(SkillType skillType) {
      if (proficientSkills.contains(skillType)) {
         return level;
      }
      if (familiarSkills.contains(skillType)) {
         return (byte) (level - 2);
      }
      return (byte) (level - 4);
   }

   public List<SkillType> getFamiliarSkills() {
      return List.copyOf(familiarSkills);
   }
   public void setFamiliarSkills(List<SkillType> familiarSkills) {
      if (!type.skillList.containsAll(familiarSkills)) {
         throw new IllegalArgumentException("a SkillType in (" + familiarSkills.stream()
                                                                               .map(SkillType::getName)
                                                                               .collect(Collectors.joining(",")) +
                                            ") is not in profession " + type.getName());
      }
      this.familiarSkills.clear();
      // prevent duplicates
      this.familiarSkills.addAll(new HashSet<>(familiarSkills));
      setMinLevel();
   }

   private void setMinLevel() {
      this.level = (byte) Math.min(10, Math.max(this.level, (this.proficientSkills.size() + this.familiarSkills.size())));
   }

   public String getFamiliarSkillsAsString() {
      return familiarSkills.stream().map(SkillType::getName).collect(Collectors.joining(","));
   }
   public void setFamiliarSkills(String familiarSkillsAsCommaDelimitedString) {
      this.familiarSkills.clear();
      if (familiarSkillsAsCommaDelimitedString != null) {
         setFamiliarSkills(Arrays.stream(familiarSkillsAsCommaDelimitedString.split(","))
                                 .map(SkillType::getSkillTypeByName)
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList()));
      }
   }

   public List<SkillType> getProficientSkills() {
      return List.copyOf(proficientSkills);
   }
   public void setProficientSkills(List<SkillType> proficientSkills) {
      if (proficientSkills.isEmpty()) {
         throw new IllegalArgumentException("cant have 0 proficient skills");
      }
      if (!type.skillList.containsAll(proficientSkills)) {
         throw new IllegalArgumentException("a SkillType in (" + proficientSkills.stream()
                                                                                 .map(SkillType::getName)
                                                                                 .collect(Collectors.joining(",")) +
                                            ") is not in profession " + type.getName());
      }
      this.proficientSkills.clear();
      // prevent duplicates
      this.proficientSkills.addAll(new HashSet<>(proficientSkills));
      setMinLevel();
   }
   public String getProficientSkillsAsString() {
      return proficientSkills.stream().map(SkillType::getName).collect(Collectors.joining(","));
   }
   public void setProficientSkills(String proficientSkillsAsCommaDelimitedString) {
      if (proficientSkillsAsCommaDelimitedString == null) {
         throw new IllegalArgumentException("Cant have an empty proficient skill set");
      }
      setProficientSkills(Arrays.stream(proficientSkillsAsCommaDelimitedString.split(", *"))
                                .map(SkillType::getSkillTypeByName)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(type.name, out);
         writeToStream(level, out);
         writeToStream(getFamiliarSkillsAsString(), out);
         writeToStream(getProficientSkillsAsString(), out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         type = ProfessionType.getByName(readString(in));
         level = readByte(in);
         setFamiliarSkills(readString(in));
         setProficientSkills(readString(in));
      } catch (IOException e) {
         e.printStackTrace();
      }
      setMinLevel();
   }

   @Override
   public String toString() {
      return "type: " + type.getName() +
             ", level:" + level +
             ", prof:{" + getProficientSkillsAsString() + "} " +
             ", fam:{" + getFamiliarSkillsAsString() + "}";
   }
}
