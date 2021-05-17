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
   public Profession(ProfessionType type, byte level) {
      this.type = type;
      this.level = level;
   }

   public Profession(Profession other) {
      this.type = other.type;
      this.level = other.level;
      this.familiarSkills = new ArrayList<>(other.familiarSkills);
      this.proficientSkills = new ArrayList<>(other.proficientSkills);
   }

   public ProfessionType getType() {
      return type;
   }

   public byte getLevel() {
      return level;
   }

   public void setLevel(byte level) {
      this.level = (byte) Math.min(10, Math.max(level, (this.proficientSkills.size() + this.familiarSkills.size())));
   }

   public void setRank(SkillType skillType, SkillRank rank) {
      if (!type.skillList.contains(skillType)) {
         throw new IllegalArgumentException("Skill type " + skillType.getName() + " is not in profession" + type.getName());
      }
      proficientSkills.remove(skillType);
      familiarSkills.remove(skillType);
      if (rank == SkillRank.FAMILIAR) {
         familiarSkills.add(skillType);
      }
      else if (rank == SkillRank.PROFICIENT) {
         proficientSkills.add(skillType);
      }
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
      return Collections.unmodifiableList(new ArrayList<>(familiarSkills));
   }
   public void setFamiliarSkills(List<SkillType> familiarSkills) {
      if (!type.skillList.containsAll(familiarSkills)) {
         throw new IllegalArgumentException("a SkillType in (" + familiarSkills.stream()
                                                                               .map(o -> o.getName())
                                                                               .collect(Collectors.joining(",")) +
                                            ") is not in profession " + type.getName());
      }
      this.familiarSkills.clear();
      this.familiarSkills.addAll(familiarSkills);
      this.level = (byte) Math.min(10, Math.max(this.level, (this.proficientSkills.size() + this.familiarSkills.size())));
   }
   public String getFamiliarSkillsAsString() {
      return familiarSkills.stream().map(o->o.getName()).collect(Collectors.joining(","));
   }
   public void setFamiliarSkills(String familiarSkillsAsCommaDelimitedString) {
      this.familiarSkills.clear();
      if (familiarSkillsAsCommaDelimitedString != null) {
         setFamiliarSkills(Arrays.asList(familiarSkillsAsCommaDelimitedString.split(","))
                                 .stream()
                                 .map(o -> SkillType.getSkillTypeByName(o))
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList()));
      }
   }

   public List<SkillType> getProficientSkills() {
      return Collections.unmodifiableList(new ArrayList<>(proficientSkills));
   }
   public void setProficientSkills(List<SkillType> proficientSkills) {
      if (!type.skillList.containsAll(proficientSkills)) {
         throw new IllegalArgumentException("a SkillType in (" + proficientSkills.stream()
                                                                                 .map(o -> o.getName())
                                                                                 .collect(Collectors.joining(",")) +
                                            ") is not in profession " + type.getName());
      }
      this.proficientSkills.clear();
      this.proficientSkills.addAll(proficientSkills);
      this.level = (byte) Math.min(10, Math.max(this.level, (this.proficientSkills.size() + this.familiarSkills.size())));
   }
   public String getProficientSkillsAsString() {
      return proficientSkills.stream().map(o->o.getName()).collect(Collectors.joining(","));
   }
   public void setProficientSkills(String proficientSkillsAsCommaDelimitedString) {
      this.proficientSkills.clear();
      if (proficientSkillsAsCommaDelimitedString != null) {
         setProficientSkills(Arrays.asList(proficientSkillsAsCommaDelimitedString.split(", *"))
                                   .stream()
                                   .map(o -> SkillType.getSkillTypeByName(o))
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList()));
      }
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
   }

   @Override
   public String toString() {
      return "type: " + type.getName() +
             ", level:" + level +
             ", prof:{" + getProficientSkillsAsString() + "} " +
             ", fam:{" + getFamiliarSkillsAsString() + "}";
   }
}
