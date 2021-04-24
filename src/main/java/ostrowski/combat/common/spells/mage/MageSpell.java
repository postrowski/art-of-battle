/*
 * Created on Jul 12, 2006
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.*;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.IResistedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class MageSpell extends Spell implements Enums
{
   public enum Familiarity {
      UNFAMILIAR("unfamiliar", 0, 5),
      KNOWN("known", 1, 3),
      MEMORIZED("memorized", 3, 1);

      private String name;
      private byte cost;
      private byte incantationTime;
      Familiarity(String name, int cost, int incantationTime) {
         this.name = name;
         this.cost = (byte) cost;
         this.incantationTime = (byte) incantationTime;
      }

      public String getName() {
         return this.name;
      }
      public byte getCost() {
         return this.cost;
      }
      public byte getIncantationTime() {
         return this.incantationTime;
      }
      public static Familiarity getFamiliarityByName(String name) {
         for (Familiarity fam : values()) {
            if (fam.name.equalsIgnoreCase(name)) {
               return fam;
            }
         }
         return null;
      }
   };

   public       Class<MageSpell>[]       prerequisiteSpells;
   public final SkillType[]              prerequisiteSkillTypes;
   protected    HashMap<Attribute, Byte> attributeMod = new HashMap<>();
   private      Familiarity              familiarity = Familiarity.UNFAMILIAR;

   public MageSpell() {
      this("", null, null);
   }

   @SuppressWarnings({ "unchecked", "rawtypes"})
   protected MageSpell(String name, Class[] prerequisiteSpellClasses, SkillType[] skillTypes) {
      super(name);
      prerequisiteSpells = prerequisiteSpellClasses;
      prerequisiteSkillTypes = skillTypes;
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell() {
      return null;
   }

   public void setFamiliarity(Familiarity familiarity) {
      this.familiarity = familiarity;
   }
   public void setFamiliarity(String familiarityName) {
      setFamiliarity(Familiarity.getFamiliarityByName(familiarityName));
   }

   public Familiarity getFamiliarity() {
      return this.familiarity;
   }

   @Override
   public byte getIncantationTime() {
      return this.familiarity.getIncantationTime();
   }

   //   static public byte getIncantationTime(byte level)
   //   {
   //      switch (level) {
   //         case 1:   return 4;
   //         case 2:   return 3;
   //         case 3:   return 2;
   //         case 4:   return 2;
   //         case 5:   return 1;
   //         case 6:   return 1;
   //         case 7:   return 1;
   //         case 8:   return 0;
   //         case 9:   return 0;
   //         case 10:  return 0;
   //      }
   //      return 100;
   //   }

   @Override
   public Wound channelEnergy(byte additionalPower) {
      byte currentBurnLevel = getBurnLevel();
      super.channelEnergy(additionalPower);
      byte newBurnLevel = getBurnLevel();
      byte burnLevelGain = (byte) (newBurnLevel - currentBurnLevel);
      if (burnLevelGain > 0) {
         return new Wound(burnLevelGain, Enums.Location.BODY, "Magic Burn", burnLevelGain/*painLevel*/, 0/*wounds*/, 0/*bleedRate*/, 0/*armPenalty*/,
                          0/*movePenalty*/, 0/*knockedDownDist*/, DamageType.ELECTRIC, 0/*effectMask*/, getCaster()/*Target*/);
      }
      return null;
   }

   //   private static void printSkillInGroups() {
//      List<ArrayList<MageSpell>> listOfGroupedSpells = new ArrayList<>();
//      List<MageSpell> spellsToGroup = new ArrayList<>();
//      spellsToGroup.addAll(spellsList);
//      while (spellsToGroup.size() > 0) {
//         MageSpell spellToGroup = spellsToGroup.remove(0);
//         List<MageSpell> preProcessedGroup = new ArrayList<>();
//         List<MageSpell> postProcessedGroup = new ArrayList<>();
//         preProcessedGroup.add(spellToGroup);
//         while (preProcessedGroup.size() > 0) {
//            MageSpell spellToProcess = preProcessedGroup.remove(0);
//            postProcessedGroup.add(spellToProcess);
//            for (int i=0 ; i<spellsToGroup.size(); i++) {
//               boolean inGroup = false;
//               MageSpell spellToGroupRemaing = spellsToGroup.get(i);
//               for (Class<MageSpell> relatedSpellClass : spellToProcess.prerequisiteSpells) {
//                  if (spellToGroupRemaing.getClass() == relatedSpellClass) {
//                     inGroup = true;
//                     break;
//                  }
//               }
//               if (!inGroup) {
//                  for (Class<MageSpell> relatedSpellClass : spellToGroupRemaing.prerequisiteSpells) {
//                     if (spellToProcess.getClass() == relatedSpellClass) {
//                        inGroup = true;
//                        break;
//                     }
//                  }
//               }
//               if (inGroup) {
//                  spellsToGroup.remove(spellToGroupRemaing);
//                  preProcessedGroup.add(spellToGroupRemaing);
//                  i--;
//               }
//            }
//         }
//         listOfGroupedSpells.add(postProcessedGroup);
//      }
//
//      int n=0;
//      for (List<MageSpell> groupedSpells : listOfGroupedSpells) {
//         System.out.println("Spell Group " + n++ + ":");
//         for (int level=0 ; level<10 ; level++) {
//            for (MageSpell groupedSpell : groupedSpells) {
//               if (groupedSpell.getPrerequisiteLevel() == level) {
//                  System.out.println(level + ": " + groupedSpell.getName());
//               }
//            }
//         }
//      }
//   }

   public byte getEffectiveSkill(Character character, boolean includeKnowledgePenalty) {
      // check for the required skills
      if (prerequisiteSkillTypes == null) {
         return 0;
      }
      byte minSkill = -1;
      Profession spellcasting = character.getProfession(ProfessionType.Spellcasting);
      if (spellcasting != null) {
         for (SkillType skillType : prerequisiteSkillTypes) {
            byte skillLevel = spellcasting.getLevel(skillType);
            if ((minSkill == -1) || (skillLevel < minSkill)) {
               minSkill = skillLevel;
            }
         }
         // adjust for knowledge of spell
         if (includeKnowledgePenalty) {
            minSkill -= getKnowledgePenalty(character);
         }
      }
      return minSkill;
   }

   public byte getKnowledgePenalty(Character character) {
      if (this.familiarity != Familiarity.UNFAMILIAR) {
         // If this spell is known (level >0), then all prerequisite spells must also already be known.
         return 0;
      }
      // Spell is not known (level 0 means 'familiar'), so penalty is 4.
      byte penalty = 4;
      // check for other required spells that may also not be known
      for (Class<MageSpell> prereqSpellClass : prerequisiteSpells) {
         try {
            MageSpell prereqSpell = prereqSpellClass.getDeclaredConstructor().newInstance();
            byte knownSpellLevel = character.getSpellLevel(prereqSpell.getName());
            if (knownSpellLevel == 0) {
               // For every unknown prerequisite spell, assess another 2 point penalty
               penalty += 2;
            }
         } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
         }
      }
      return penalty;
   }

   @Override
   public byte getTN(Character caster) {
      if (isInnate()) {
         return 0;
      }

      byte tn = getTN(getPower(), caster.getMagicalAptitude());
      // adjust for knowledge of spell
      return (byte) (tn + getKnowledgePenalty(caster));
   }

   public byte getTN(Character caster, byte power) {
      byte tn = getTN(power, caster.getMagicalAptitude());
      // adjust for knowledge of spell
      return (byte) (tn + getKnowledgePenalty(caster));
   }

   public static byte getTN(byte power, int magicalAptitude) {
      byte[] TN = new byte[] { 0, 1, 2, 4, 7, 11, 16, 22, 29, 37, 46, 56, 67, 79, 92};
      if (Configuration.isSpellTnAffectedByMa()) {
         return TN[(4 + power) - magicalAptitude];
      }
      return TN[2 + power];
   }

   private int getMaxAttackActions() {
      return 3;
   }

   protected void copyDataFrom(MageSpell source) {
      super.copyDataFrom(source);
      attributeMod = source.attributeMod;
      prerequisiteSpells = source.prerequisiteSpells;
   }

   public byte getBurnLevel() {
      if (isInnate()) {
         return 0;
      }
      return (byte) Math.max(0, (getPower() - getCaster().getMagicalAptitude()));
   }

   @Override
   public Wound setResults(int excessSuccess, boolean success, boolean effective, int castRoll, byte skill, Arena arena) {
      if (!success) {
         // Spell burn possible, is over-powered
         Wound wound = getFailureBurnWound();
         if (wound != null) {
            caster.applyWound(wound, arena);
            String message = getCasterName() + "'s failed spell causes " + wound.getWounds() + " wounds " + "and " + wound.getPain() + "points of pain";
            arena.sendMessageTextToAllClients(message, false/*popUp*/);
            return wound;
         }
      }
      return null;
   }

   @Override
   public Wound getFailureBurnWound() {
      byte burnLevel = getBurnLevel();
      if (burnLevel > 0) {
         return new Wound((byte) (burnLevel * 3), Enums.Location.BODY, "Magical burn", burnLevel /*painLevel*/, burnLevel/*wound*/, 0 /*bleedRate*/,
                          0 /*armPenalty*/, 0 /*movePenalty*/, 0 /*knockedDownDist*/, DamageType.ELECTRIC, 0 /*effectMask*/, caster);
      }
      return null;
   }

   @Override
   public Wound getNewTurnBurnWound() {
      byte burnLevel = getBurnLevel();
      if (burnLevel > 0) {
         return new Wound(burnLevel, Enums.Location.BODY, "Magical burn", burnLevel /*painLevel*/, 0/*wound*/, 0 /*bleedRate*/, 0 /*armPenalty*/,
                          0 /*movePenalty*/, 0 /*knockedDownDist*/, DamageType.ELECTRIC, 0 /*effectMask*/, caster);
      }
      return null;
   }

   public static String generateHtmlTableMageSpells() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader());
      sb.append("<body>");
      sb.append("<H4>Mage Spells:</H4>");
      sb.append("<table><tr><td>");
      sb.append("<H4>Maximum spell points per day:</H4>");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Magical<br/>Aptitude", "Max. power<br/>per day"));
      for (byte ma = 1; ma <= 5; ma++) {
         table.addRow(new TableRow(ma-1, ""+ma, ""+Rules.getMaxMageSpellPoint(ma)));
      }
      sb.append(table);

      sb.append("</td><td>");
      sb.append("<H4>Spell TNs:</H4>");
      int maxPower = 10;
      table = new Table();
      TableRow header1 = new TableRow(-1);
      table.addRow(header1);
      header1.addHeader(new TableHeader("Magical<br/>Aptitude<br/>(MA)").setRowSpan(2));
      header1.addHeader(new TableHeader("Spell power").setColSpan(maxPower));
      TableRow header2 = new TableRow(-1);
      table.addRow(header2);
      for (int power = 1; power <= maxPower; power++) {
         header2.addHeader(new TableHeader(""+power));
      }

      //table.addRow(new TableRow(-1).addHeader(new TableHeader("Magical<br/>Aptitude<br/>(MA)").setRowSpan(6)));
      for (int ma = 1; ma <= 5; ma++) {
         TableRow row = new TableRow(ma-1);
         row.addHeader(""+ma);
         for (byte power = 1; power <= maxPower; power++) {
            row.addHeader(""+MageSpell.getTN(power, ma));
         }
         table.addRow(row);
      }
      sb.append(table);
      sb.append("</td></tr></table>");

      sb.append("<H4>Mage Spell Grimiore:</H4>");
      sb.append(MageSpells.getSpellGrimioreForHTML());
      sb.append("</body>");
      return sb.toString();
   }

   @Override
   protected void describeGrimioreDescription(StringBuilder descriptionBuffer) {
      super.describeGrimioreDescription(descriptionBuffer);
      descriptionBuffer.append("<br/><b>Skills:</b>");
      descriptionBuffer.append(getSkillTypeNames());
      descriptionBuffer.append("<br/><b>Prerequisites:</b>");
      //String prereqs = getPrerequisiteNames();
      String prereqs = getImmediatePrerequisiteNames();
      if (prereqs.trim().length() == 0) {
         prereqs = "<i>None.</i>";
      }
      descriptionBuffer.append(prereqs);

      descriptionBuffer.append("<br/><i>Required by:</i>");
      String reqBy = getRequiredByNames();
      if (reqBy.length() == 0) {
         descriptionBuffer.append("none");
      }
      else {
         descriptionBuffer.append(reqBy);
      }
   }

   private String getSkillTypeNames() {
      return Arrays.stream(prerequisiteSkillTypes)
                   .map(o->o.getName())
                   .collect(Collectors.joining(", "));
   }

//   private String getPrerequisiteNames() {
//      StringBuilder sb = new StringBuilder();
//      int spellCount = 0;
//      for (Class< ? extends MageSpell> prereq : prerequisiteSpells) {
//         if (spellCount++ > 0) {
//            sb.append(", ");
//         }
//         try {
//            MageSpell spell = prereq.newInstance();
//            sb.append(spell.getName());
//         } catch (InstantiationException e) {
//            e.printStackTrace();
//         } catch (IllegalAccessException e) {
//            e.printStackTrace();
//         }
//      }
//      return sb.toString();
//   }

   private String getImmediatePrerequisiteNames() {
      StringBuilder sb = new StringBuilder();
      int spellCount = 0;
      for (Class< ? extends MageSpell> prereq : prerequisiteSpells) {
         try {
            // Check if any of the other prerequisite spells list this spell as a prerequisite
            boolean skip = false;
            for (Class< ? extends MageSpell> prereq2 : prerequisiteSpells) {
               if (prereq != prereq2) {
                  try {
                     MageSpell spell2 = prereq2.getDeclaredConstructor().newInstance();
                     for (Class< ? extends MageSpell> prereq3 : spell2.prerequisiteSpells) {
                        if (prereq == prereq3) {
                           skip = true;
                           break;
                        }
                     }
                  } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                     e.printStackTrace();
                  }
               }
               if (skip) {
                  break;
               }
            }
            if (skip) {
               continue;
            }
            MageSpell spell = prereq.getDeclaredConstructor().newInstance();
            if (spellCount++ > 0) {
               sb.append(", ");
            }
            sb.append(spell.getName());
         } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
         }
      }
      return sb.toString();
   }

   private String getRequiredByNames() {
      StringBuilder sb = new StringBuilder();
      int spellCount = 0;
      for (MageSpell spell : MageSpells.spellsList) {
         for (Class<MageSpell> prereq : spell.prerequisiteSpells) {
            if (prereq == this.getClass()) {
               if (spellCount++ > 0) {
                  sb.append(", ");
               }
               sb.append(spell.getName());
               break;
            }
         }
      }
      return sb.toString();
   }

   @Override
   protected byte getCastingTN(RequestAction attack, short distanceInHexes, RANGE range, byte spellTN, Battle battle,
                               StringBuilder sbDescription) {
      TableRow tr = new TableRow();
      tr.addTD(spellTN);
      tr.addTD("spell Casting TN (power=" + getPower() +
               ", MA=" + caster.getMagicalAptitude() + ")");
      sbDescription.append(tr);
      return spellTN;
   }

   @Override
   protected byte getRangeAdjustedCastingTN(RequestAction attack, short distanceInHexes, RANGE range, byte castingTN, byte rangeTNAdjustment, Battle battle,
                                            StringBuilder sbDescription) {
      if ((rangeTNAdjustment != 0) || (range != RANGE.OUT_OF_RANGE)) {
         if (!(this instanceof IResistedSpell) && !(this instanceof IRangedSpell)) {
            TableRow tr = new TableRow();
            if (rangeTNAdjustment > 0) {
               tr.addTD("+" + rangeTNAdjustment);
            }
            else {
               tr.addTD(rangeTNAdjustment);
            }
            StringBuilder sb = new StringBuilder();
            if (rangeTNAdjustment > 0) {
               sb.append("range penalty for a distance of ");
            }
            else {
               sb.append("range bonus for a distance of ");
            }
            sb.append(distanceInHexes).append(" hexes");
            if (range != RANGE.OUT_OF_RANGE) {
               sb.append("(").append(range.getName()).append(" range)");
            }
            tr.addTD(sb.toString());
            sbDescription.append(tr);
         }
      }
      byte rangeAdjustedCastingTN = (byte) (castingTN + rangeTNAdjustment);

      if (rangeTNAdjustment != 0) {
         TableRow tr = new TableRow();
         tr.addTD(rangeAdjustedCastingTN);
         tr.addTD(new TableData("Range Adjusted Spell TN", true/*isBold*/));
         sbDescription.append(tr);
      }
      return rangeAdjustedCastingTN;
   }

   @Override
   public byte getCastingPower(RequestAction attack, short distanceInHexes, RANGE range, Battle battle, StringBuilder sbDescription) {
      return 0;
   }

   @Override
   protected DiceSet getCastDice(RequestAction attack, byte distanceModifiedTN, byte distanceModifiedPower, short distanceInHexes, RANGE range, Battle battle,
                                 StringBuilder sbDescription) {
      // attack (cast) information:
      byte spellActions = getActionsUsed(attack);
      return getCastDice(spellActions, range);
   }

   public byte getActionsUsed(RequestAction attack) {
      return (byte) Math.min(attack.getActionsUsed(), getMaxAttackActions());
   }

   @Override
   protected int getDefenseResult(RequestDefense defense, byte distanceModifiedPower, Battle battle, StringBuilder sbDescription) {
      return 0;
   }

   @Override
   public byte getSpellPoints() {
      return getPower();
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(familiarity.getName(), out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         familiarity = Familiarity.getFamiliarityByName(readString(in));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

}
