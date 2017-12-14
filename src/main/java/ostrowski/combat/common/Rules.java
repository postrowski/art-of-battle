package ostrowski.combat.common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.orientations.OrientationDoubleCentaur;
import ostrowski.combat.common.orientations.OrientationDoubleQuadraped;
import ostrowski.combat.common.orientations.OrientationReptilian;
import ostrowski.combat.common.orientations.OrientationSingleHumaniod;
import ostrowski.combat.common.orientations.OrientationSingleQuadraped;
import ostrowski.combat.common.orientations.OrientationSingleWinged;
import ostrowski.combat.common.spells.mage.MageCollege;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.common.things.Door;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Head;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.things.Potion;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Tail;
import ostrowski.combat.common.things.Tool;
import ostrowski.combat.common.things.Wall;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.things.Wing;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.BeginBattle;
import ostrowski.combat.protocol.EnterArena;
import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.protocol.MessageText;
import ostrowski.combat.protocol.ServerStatus;
import ostrowski.combat.protocol.TargetPriorities;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionOption;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.protocol.request.RequestDefenseOfCounterAttack;
import ostrowski.combat.protocol.request.RequestEquipment;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.protocol.request.RequestPosition;
import ostrowski.combat.protocol.request.RequestSingleTargetSelection;
import ostrowski.combat.protocol.request.RequestSpellSelection;
import ostrowski.combat.protocol.request.RequestSpellTypeSelection;
import ostrowski.combat.protocol.request.RequestTarget;
import ostrowski.combat.protocol.request.RequestUseOfHeroPoint;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.Configuration;
import ostrowski.protocol.SerializableFactory;

/**
 * @author Paul
 *
 */
public class Rules extends DebugBreak implements Enums
{
   static {
      SerializableFactory.registerClass("String", String.class);
      SerializableFactory.registerClass("Advant", Advantage.class);
      SerializableFactory.registerClass("AreCoo", ArenaCoordinates.class);
      SerializableFactory.registerClass("AreLoc", ArenaLocation.class);
      SerializableFactory.registerClass("BegBat", BeginBattle.class);
      // Individual spells don't need to be listed here, because only the base class contains elements
      // that need to be serialized. Members of a child class are always the same for different instances
      // of the same class (baseRange, DamageType, etc.)
      SerializableFactory.registerClass("SpellM", MageSpell.class);
      SerializableFactory.registerClass("SpellP", PriestSpell.class);
      SerializableFactory.registerClass("Skill.", Skill.class);
      SerializableFactory.registerClass("ObjChr", Character.class);
      SerializableFactory.registerClass("CmbMap", CombatMap.class);
      SerializableFactory.registerClass("Condit", Condition.class);
      SerializableFactory.registerClass("DieSet", DiceSet.class);
      SerializableFactory.registerClass("EntAre", EnterArena.class);
      SerializableFactory.registerClass("MageCo", MageCollege.class);
      SerializableFactory.registerClass("MapVis", MapVisibility.class);
      SerializableFactory.registerClass("MesTxt", MessageText.class);
      SerializableFactory.registerClass("MLCMap", MultiLevelCombatMap.class);
      SerializableFactory.registerClass("Ori2Cn", OrientationDoubleCentaur.class);
      SerializableFactory.registerClass("Ori2Qu", OrientationDoubleQuadraped.class);
      SerializableFactory.registerClass("OriRep", OrientationReptilian.class);
      SerializableFactory.registerClass("OriHum", OrientationSingleHumaniod.class);
      SerializableFactory.registerClass("OriQua", OrientationSingleQuadraped.class);
      SerializableFactory.registerClass("OriWng", OrientationSingleWinged.class);
      SerializableFactory.registerClass("SrvSts", ServerStatus.class);
      SerializableFactory.registerClass("ReqAct", RequestAction.class);
      SerializableFactory.registerClass("ReqAcO", RequestActionOption.class);
      SerializableFactory.registerClass("ReqSty", RequestAttackStyle.class);
      SerializableFactory.registerClass("ReqDef", RequestDefense.class);
      SerializableFactory.registerClass("ReqDCA", RequestDefenseOfCounterAttack.class);
      SerializableFactory.registerClass("ReqEqu", RequestEquipment.class);
      SerializableFactory.registerClass("ReqGrp", RequestGrapplingHoldMaintain.class);
      SerializableFactory.registerClass("ReqLoc", RequestLocation.class);
      SerializableFactory.registerClass("ReqMov", RequestMovement.class);
      SerializableFactory.registerClass("ReqPos", RequestPosition.class);
      SerializableFactory.registerClass("ReqSgT", RequestSingleTargetSelection.class);
      SerializableFactory.registerClass("ReqSpl", RequestSpellSelection.class);
      SerializableFactory.registerClass("ReqSpT", RequestSpellTypeSelection.class);
      SerializableFactory.registerClass("ReqTrg", RequestTarget.class);
      SerializableFactory.registerClass("ReqHro", RequestUseOfHeroPoint.class);
      SerializableFactory.registerClass("TrgPri", TargetPriorities.class);
      SerializableFactory.registerClass("Armor.", Armor.class);
      SerializableFactory.registerClass("Hand..", Hand.class);
      SerializableFactory.registerClass("Head..", Head.class);
      SerializableFactory.registerClass("Leg...", Leg.class);
      SerializableFactory.registerClass("Tail..", Tail.class);
      SerializableFactory.registerClass("Wing..", Wing.class);
      SerializableFactory.registerClass("Potion", Potion.class);
      SerializableFactory.registerClass("Shield", Shield.class);
      SerializableFactory.registerClass("Tool..", Tool.class);
      SerializableFactory.registerClass("Wall..", Wall.class);
      SerializableFactory.registerClass("Door..", Door.class);
      SerializableFactory.registerClass("Weapon", Weapon.class);
      SerializableFactory.registerClass("MisWea", MissileWeapon.class);
      SerializableFactory.registerClass("Wound.", Wound.class);
   }

   /*
   The crevase running east-west splits a marble hall here. To the south, the ceiling has collapsed, blocking the passage.\nPausing for a moment, you hear the sound of digging. Listening more closely, you hear voices:\nvoice 1: Dig faster Malcon, or I'll cast you back to the pit!\nvoice 2: (laughing in a dark, sinister, monsterous tone) I think not, human.\n           You know you have no chance of recovering the orb without my help.\n           You would be destroyed... HAHA.\nvoice 1: Perhaps not demon, but I don't have to share its power with you once we defeat him. Now dig you foul beast!\nThe digging resumes...
    *
   You enter a throne room. A magic glow eminates from the ceiling, lighting the entire room.\n Decades of dust and decay have reduced this once-opulent room to a fraction of it former glory.\n Along the walls of the room are dusty painting of ancient royalty, and rotten fabric were once\n hung rich tapetries. In the center of the room, raised on a small platform, sits a king's throne.\n The King, long since dead, remains perched on his throne. His skull, still visible inside the helm\n of his Mithril plate mail, appear to look right at you. Scattered about the room, the royal gaurds\n lay fallen, each showing bare bones from years of decay.
    */
   private static String getTimeSinceTopOfHour() {
      long time = System.currentTimeMillis();
      long mins = (time / 60000) % 60;
      long secs = (time / 1000) % 60;
      long millis = time % 1000;
      return valueOf(mins, 2) + ":" + valueOf(secs, 2) + ":" + valueOf(millis, 3);
   }

   static public String valueOf(long num, int places) {
      String value = String.valueOf(num);
      while (value.length() < places) {
         value = "0" + value;
      }
      return value;
   }

   static public void diag(String output) {
      StringBuilder sb = new StringBuilder();
      sb.append(getTimeSinceTopOfHour()).append(" - ");
      sb.append(Thread.currentThread().getName()).append(": ");
      sb.append(diagCompName).append("-").append(output);
      sb.append("\n");
      System.out.print(sb.toString());

      File file = new File("combat.log");
      long fileLength = file.exists() ? file.length() : 0;
      try (RandomAccessFile raf = new RandomAccessFile(file, "rw"))
      {
         // If the the file exists, delete it or set our file length equal to its length.
         raf.seek(fileLength);
         raf.writeBytes(sb.toString());
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   static String diagCompName = "";

   static public void setDiagComponentName(String name) {
      diagCompName = name;
      if (name.equalsIgnoreCase("server")) {
         File file = new File("combat.log");
         file.delete();
      }
   }

   static public byte getMaxSpellLevel()   {  return 10;   };
   static public byte getMaxCollegeLevel() {  return 10;   };
   static public byte getMaxSkillLevel()   {  return 10;   };
   static public byte getMaxAttribute()    {  return 10;   };
   static public byte getMinAttribute()    {  return -10;  };

   static public int getAttCost(byte attLevel) {
      switch (attLevel) {
         case -10:     return -40;
         case -9:      return -39;
         case -8:      return -37;
         case -7:      return -35;
         case -6:      return -32;
         case -5:      return -29;
         case -4:      return -25;
         case -3:      return -20;
         case -2:      return -14;
         case -1:      return -8;
         case 0:       return 0;
         case 1:       return 10;
         case 2:       return 20;
         case 3:       return 35;
         case 4:       return 50;
         case 5:       return 70;
         case 6:       return 90;
         case 7:       return 115;
         case 8:       return 140;
         case 9:       return 170;
         case 10:      return 200;
      }
      if (attLevel > 10) {
         return 50 * (attLevel - 6);
      }
      DebugBreak.debugBreak();
      throw new IllegalArgumentException();
   }

   static public int getSpellCost(byte spellLevel) {
      switch (spellLevel) {
         case 0:    return 0;
         case 1:    return 1;
         case 2:    return 3;
      }
      return getSkillCost(spellLevel);
   }

   static public int getCollegeCost(byte collegeLevel) {
      return getSkillCost(collegeLevel);
   }

   static public int getSkillCost(byte skillLevel) {
      switch (skillLevel) {
         case 0:    return 0;
         case 1:    return 1;
         case 2:    return 3;
         case 3:    return 7;
         case 4:    return 13;
         case 5:    return 20;
         case 6:    return 30;
         case 7:    return 45;
         case 8:    return 65;
         case 9:    return 90;
         case 10:   return 120;
      }
      DebugBreak.debugBreak();
      throw new IllegalArgumentException();
   }

   static public byte getDamageBase(byte strengthBase) {
      return strengthBase;
   }

   static HashMap<String, DiceSet> _diceTable = new HashMap<>();

   static public DiceSet getDice(byte attributeLevel, byte actions, Attribute attribute) {
      boolean useComplexDice = Configuration.useExtendedDice();
      if (!useComplexDice) {
         if ((attribute == Attribute.Toughness) && (Configuration.useComplexTOUDice())) {
            useComplexDice = true;
         }
      }
      if (!useComplexDice) {
         if (Configuration.useSimpleDice()) {
            // return a d10 for 2-actions, d10-5 for 1-action, d10+5 for 3-actions:
            int d1 = Rules.getTNBonusForActions(actions) + attributeLevel;
            return new DiceSet(d1, 0/*d4*/, 0/*d6*/, 0/*d8*/, 1/*d10*/, 0/*d12*/, 0/*d20*/, 0/*dBell*/, 1.0/*multiplier*/);
         }
         if (Configuration.useBellCurveDice()) {
            // return a d10 for 2-actions, d10-5 for 1-action, d10+5 for 3-actions:
            int d1 = Rules.getTNBonusForActions(actions) + attributeLevel;
            return new DiceSet(d1, 0/*d4*/, 0/*d6*/, 0/*d8*/, 0/*d10*/, 0/*d12*/, 0/*d20*/, 1/*dBell*/, 1.0/*multiplier*/);
         }
      }
      // two ways to get here:
      //    1) using complex dice.
      //    2) looking for dice for Toughness roll (pain roll), with useComplexTOUDice true
      String key = attributeLevel + ":" + actions;
      DiceSet set = _diceTable.get(key);
      if (set != null) {
         return set;
      }
      if ((actions < 1) || (actions > 3)) {
         DebugBreak.debugBreak();
         throw new IllegalArgumentException("actions (" + actions + ") may only be 1, 2 or 3.");
      }
      double[] expectedValues = null;
      switch (attributeLevel) {
         // These numbers are not completely linear, or even in ascending order, because the expected value
         // doesn't account for negative numbers being truncated to zero, which is very significant for the
         // most common use of these values, the pain recovery roll.
         case -10:           expectedValues = new double[] { -1.67,  0.14,  2.33};        break;
         case -9:            expectedValues = new double[] { -0.80,  1.33,  3.33};        break;
         case -8:            expectedValues = new double[] { -0.67,  1.19,  3.20};        break;
         case -7:            expectedValues = new double[] {  0.33,  1.14,  4.20};        break;
         case -6:            expectedValues = new double[] {  0.20,  2.20,  5.14};        break;
         case -5:            expectedValues = new double[] {  0.14,  2.33,  6.11};        break;
         case -4:            expectedValues = new double[] {  1.33,  3.20,  6.67};        break;
         case -3:            expectedValues = new double[] {  1.19,  3.33,  7.53};        break;
         case -2:            expectedValues = new double[] {  1.14,  4.20,  8.40};        break;
         case -1:            expectedValues = new double[] {  2.20,  5.14,  9.34};        break;
         case 0:             expectedValues = new double[] {  2.33,  6.11, 10.29};        break;
         case 1:             expectedValues = new double[] {  3.20,  6.67, 11.25};        break;
         case 2:             expectedValues = new double[] {  3.33,  7.53, 12.22};        break;
         case 3:             expectedValues = new double[] {  4.20,  8.40, 13.54};        break;
         case 4:             expectedValues = new double[] {  5.14, 10.29, 14.49};        break;
         case 5:             expectedValues = new double[] {  6.11, 11.25, 15.43};        break;
         case 6:             expectedValues = new double[] {  6.67, 12.22, 16.40};        break;
         case 7:             expectedValues = new double[] {  7.53, 13.54, 18.33};        break;
         case 8:             expectedValues = new double[] {  8.40, 14.49, 20.57};        break;
         case 9:             expectedValues = new double[] {  9.34, 15.43, 21.54};        break;
         case 10:            expectedValues = new double[] { 10.29, 16.40, 22.51};        break;
         case 11:            expectedValues = new double[] { 11.25, 17.37, 24.44};        break;
         case 12:            expectedValues = new double[] { 12.22, 18.33, 25.42};        break;
         case 13:            expectedValues = new double[] { 13.54, 19.63, 27.38};        break;
         case 14:            expectedValues = new double[] { 14.49, 20.57, 28.36};        break;
         case 15:            expectedValues = new double[] { 15.43, 21.54, 29.59};        break;
         case 16:            expectedValues = new double[] { 16.40, 22.51, 31.54};        break;
         case 17:            expectedValues = new double[] { 17.37, 24.44, 33.49};        break;
         case 18:            expectedValues = new double[] { 18.33, 25.42, 35.45};        break;
         case 19:            expectedValues = new double[] { 19.63, 26.40, 36.67};        break;
         default:
            DebugBreak.debugBreak();
            throw new IllegalArgumentException("attribute (" + attributeLevel + ") must be between -10 and +19.");
      }
      set = DiceSet.getDieClosestToExpectedRoll(expectedValues[actions - 1]);
      _diceTable.put(key, set);
      return set;
   }

   public static DiceSet getPainReductionDice(byte toughness) {
      return getDice(toughness, (byte) 1, Attribute.Toughness);
   }

   public static byte getDodgeLevel(byte nimbleness) {
      return (byte) ((nimbleness + 10) / 2); // round down
   }

   public static byte getRetreatLevel(byte nimbleness) {
      return (byte) (nimbleness + 10);
   }

   public static byte getBlockLevel(byte shieldSkill) {
      return shieldSkill;
   }

   public static byte getParryLevel(byte weaponSkill, double effectiveness) {
      if (effectiveness == 1.0) {
         return weaponSkill;
      }
      if (effectiveness < 1.0) {
         // round UP
         return (byte) Math.floor((weaponSkill * effectiveness) + .5);
      }
      // round DOWN
      return (byte) Math.floor(weaponSkill * effectiveness);
   }

   public static byte getDamageBonusForSkillExcess(byte skillExcess) {
      if (skillExcess <= 0) {
         return 0;
      }
      return (byte) (skillExcess / 2); // round down (truncate)
   }

   public static DiceSet getInitiativeDieType() {
      return new DiceSet(0, 1, 0, 0, 0, 0, 0, 0/*dBell*/, 1.0);
   }

   public static int rollInitiativeDie() {
      return getInitiativeDieType().roll(false/*allowExplodes*/);
   }

   public static byte getParryTime(byte thrustTime, byte swingTime) {
      return (byte) ((Math.max(thrustTime, swingTime) + 1) / 2);
   }

   public static byte getCollapsePainLevel() {
      return 10;
   }

   public static byte getUnconsciousWoundLevel(byte toughnessLevel) {
      return (byte) (10 + (toughnessLevel/2));
   }

   public static short getMaxMageSpellPoint(short magicalAptitude) {
      return (short) (magicalAptitude * 20);
   }

   public static short getMaxPriestSpellPoints(byte divineAffinity, byte divinePower) {
      return (short) (divineAffinity * 5 * divinePower);
   }

   public static byte getEncumbranceLevel(Character character) {
      double weightCarried = character.getWeightCarried();
      byte adjStrength = character.getAdjustedStrength();
      byte nimbleness = character.getAttributeLevel(Attribute.Nimbleness);
      for (byte enc = 0; enc <= 5; enc++) {
         if (weightCarried <= getMaxWeightForEncLevel(adjStrength, nimbleness, enc)) {
            return enc;
         }
      }
      return -1;
   }

   public static double getMaxWeightForEncLevel(byte adjStrength, byte nimbleness, byte encLevel) {
      double[] max = new double[6];
      max[5] = getMaxCarryWeight(adjStrength);
      for (byte i = 4; i > 0; i--) {
         max[i] = max[i + 1] / 2;
      }
      max[0] = 0;
      double nimAdj = roundWeight((max[1] * nimbleness) / 10);
      //// don't adjust the max weight carried because of nimbleness.
      //if (encLevel == 5) {
      //   return max[encLevel];
      //}
      return roundWeight(nimAdj + roundWeight(max[encLevel]));
   }

   private static double getMaxCarryWeight(byte strength) {
      return roundWeight(Math.pow(1.122018454, strength) * 320);
   }

   private static double getMaxBenchPress(byte strength) {
      return roundWeight(Math.pow(1.122018454, strength) * 150);
   }

   private static double getIQScoreLevel(byte iq) {
      return Math.round(Math.pow(1.08, iq) * 100);
   }

   public static byte getStartingActions(Character character) {
      return getStartingActions(getEncumbranceLevel(character));
   }

   public static byte getStartingActions(byte enc) {
      switch (enc) {
         case 0:
         case 1:
            return 5;
         case 2:
         case 3:
            return 4;
         case 4:
         case 5:
            return 3;
      }
      return 0;
   }

   public static double roundWeight(double weight) {
      if (weight == 0) {
         return 0;
      }
      int factor = 1;
      if (weight < 0) {
         factor = -1;
         weight = Math.abs(weight);
      }
      while (true) {
         if (weight > 10) {
            return (double) (Math.round(weight)) / (factor);
         }
         if (weight > 5) {
            return (Math.round(weight * 2.0)) / (factor * 2.0);
         }
         if (weight > 1) {
            return (Math.round(weight * 4.0)) / (factor * 4.0);
         }
         weight *= 10.0;
         factor *= 10;
      }
   }

   static public short quantizeNumber(short number, double factor) {
      short absQuantizedNumber = (short) (Math.round((Math.abs(number) / factor) - .01) * factor);
      return (short) (absQuantizedNumber * Math.signum(number));
   }

   static public short adjustMeleeRangeForSize(short originalRange, byte buildModifier) {
      short quantizedNumber = quantizeNumber(buildModifier, 6);
      double scaleFactor = Math.pow(Math.exp((quantizedNumber * 3.0) / 40.0), 1.0 / 3.0);
      return (short) Math.round(originalRange * scaleFactor);
   }

   public static String generateAttributesHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader());
      Table attrTable = generateAttributesTable();
      Table rangeTable = generateCommonRangeAdjustmentsForAttributes();

      sb.append("<body>");
      sb.append("<H3>Attributes:</H3>");
      sb.append(attrTable.toString());
      sb.append("<H3>Common Ranges adjusted for attibute levels:</H3>");
      sb.append(rangeTable.toString());
      sb.append("For levels above 20: subtract 15, and double the range (i.e. ASTR=26 for a base range of 20 yields twice that of an ASTR or 11. range of 20 adjusted for a ASTR of 11 is 33, so an ASTR of 26 would have an attribute adjust range of 66.) ");
      sb.append(" For levels below -10: add 15, and halve the range.");
      sb.append("</body>");
      return sb.toString();
   }

   private static Table generateAttributesTable() {
      Table table = new Table();
      table.setClassName("doubleRow");
      TableRow header1 = new TableRow(-1);
      TableRow header2 = new TableRow(-1);
      table.addRow(header1);
      table.addRow(header2);
      header1.addTD(new TableHeader("Attribute<br/>level").setRowSpan(2));
      header1.addTD(new TableHeader("Cost").setRowSpan(2));
      if (Configuration._useExtendedDice) {
         header1.addTD(new TableHeader("Dice").setColSpan(3));
      }
      if (!Configuration.useExtendedDice() && Configuration.useComplexTOUDice()) {
         header1.addTD(new TableHeader("TOU<br/>Roll").setRowSpan(2));
         header1.addTD(new TableHeader("Saving<br/>Throw<br/>Roll").setRowSpan(2));
      }
      header1.addTD(new TableHeader("Max.&nbsp;weight&nbsp;for&nbsp;enc.&nbsp;level<br/>(not&nbsp;accounting&nbsp;for&nbsp;nimbleness)").setColSpan(6));
      header1.addTD(new TableHeader("Maximum<br/>bench&nbsp;press<br/>(STR&nbsp;attribute)").setRowSpan(2));
      header1.addTD(new TableHeader("IQ<br/>test&nbsp;score<br/>(IQ&nbsp;attribute)").setRowSpan(2));
      header1.addTD(new TableHeader("Range<br/>adjustment").setRowSpan(2));

      if (Configuration._useExtendedDice) {
         for (int i = 1; i <= 3; i++) {
            header2.addTD(new TableHeader(i + "-actions"));
         }
      }
      for (int i = 0; i <= 5; i++) {
         header2.addTD(new TableHeader(""+i));
      }

      int htmlRow = 0;
      for (byte attLevel = getMinAttribute(); attLevel <= 19; attLevel++) {
         TableRow row = new TableRow(htmlRow++);
         row.addTD(new TableHeader(attLevel));
         if (attLevel <= getMaxAttribute()) {
            row.addTD(new TableData(getAttCost(attLevel)));
         }
         else {
            row.addTD(new TableData("---"));
         }
         if (Configuration._useExtendedDice) {
            for (byte actions = 1; actions < 4; actions++) {
               DiceSet dice = getDice(attLevel, actions, Attribute.Intelligence);
               row.addTD(new TableData(dice));
            }
         }
         if (!Configuration.useExtendedDice() && Configuration.useComplexTOUDice()) {
            DiceSet dice = getDice(attLevel, (byte) 1/*actions*/, Attribute.Toughness);
            row.addTD(new TableData(dice));
            dice = getDice(attLevel, (byte) 2/*actions*/, Attribute.Toughness);
            row.addTD(new TableData(dice));
         }
         for (byte enc = 0; enc < 6; enc++) {
            double maxWeight = getMaxWeightForEncLevel(attLevel, (byte) 0, enc);
            if (maxWeight == Math.floor(maxWeight)) {
               // print as an integer, if it's a whole number.
               row.addTD(new TableData((int) maxWeight));
            }
            else {
               row.addTD(new TableData(maxWeight));
            }
         }
         int maxBench = (int) getMaxBenchPress(attLevel);
         int iqTest = (int) getIQScoreLevel(attLevel);
         row.addTD(new TableData(maxBench));
         double rangeAdj = getRangeAdjusterForAdjustedStr(attLevel);
         rangeAdj = Math.round(rangeAdj * 1000.0) / 1000.0;
         row.addTD(new TableData(iqTest));
         row.addTD(new TableData(rangeAdj));
         table.addRow(row);
      }
      return table;
   }

   private static Table generateCommonRangeAdjustmentsForAttributes() {
      Table table = new Table();
      table.setClassName("doubleRow");
      TableRow header1 = new TableRow(-1);
      table.addRow(header1);
      header1.addTD(new TableHeader("Attribute level <br/> (ASTR or IQ)").setRowSpan(2));
      int range[] = { 8, 10, 12, 14, 16, 18, 20, 24, 28, 30, 32, 36, 40, 48, 50, 56, 60, 64, 72, 80, 100, 120, 160, 200, 240};
      header1.addTD(new TableHeader("Base range").setColSpan(range.length));

      TableRow header2 = new TableRow(-1);
      table.addRow(header2);
      for (int rangeBase : range) {
         header2.addTD(new TableHeader(rangeBase));
      }

      int htmlRow = 0;
      for (byte attrLevel = -10; attrLevel <= 20; attrLevel++) {
         TableRow row = new TableRow(htmlRow++);
         row.addTD(new TableData(attrLevel));
         for (int rangeBase : range) {
            double adjustedRange = getRangeAdjusterForAdjustedStr(attrLevel) * rangeBase;
            if (attrLevel == 0) {
               row.addTD(new TableHeader(Math.round(adjustedRange)));
            }
            else {
               row.addTD(new TableData(Math.round(adjustedRange)));
            }
         }
         table.addRow(row);
      }
      return table;
   }

   public static String generateSkillsHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getCSSHeader());
      sb.append("<body>");
      sb.append("<H3>Skill cost:</H3>");
      Table table = new Table();
      TableRow tr = new TableRow();
      tr.addHeader("Skill level");
      tr.addHeader("Cost");
      table.addRow(tr);
      for (byte i = 0; i <= getMaxSkillLevel(); i++) {
         tr = new TableRow();
         tr.addHeader(i);
         tr.addTD(getSkillCost(i));
         table.addRow(tr);
      }
      sb.append(table.toString());
      sb.append("</body>");
      return sb.toString();
   }

   private static String generateChargeDamageTable() {
      StringBuilder sb = new StringBuilder();
      sb.append("<H4>Charge damage:</H4>");
      sb.append("<table class='doubleRow'>");
      sb.append("<tr class=\"header-row\">");
      sb.append("<th>Movement Rate</th>");
      sb.append("<th>Bonus Damage</th>");
      sb.append("</tr>");
      for (byte i = 0; i <= 16; i++) {
         sb.append(HtmlBuilder.buildRow(i));
         sb.append("<th>").append(i).append("</th>");
         sb.append("<td>+").append(getChargeDamageForSpeed(i)).append("</td>");
         sb.append("</tr>");
      }
      sb.append("</table>");
      return sb.toString();
   }

   public static String generatePositionAdjustmentTable() {
      StringBuilder sb = new StringBuilder();
      sb.append("<H4>Position adjustemnts:</H4>");
      sb.append("<table>");
      sb.append("<tr class=\"header-row\">");
      sb.append("<th>Position</th>");
      sb.append("<th>Attack adj.</th>");
      sb.append("<th>Parry adj.</th>");
      sb.append("<th>dodge adj.</th>");
      sb.append("<th>retreat adj.</th>");
      sb.append("</tr>");
      for (Position pos : Position.values()) {
         sb.append(HtmlBuilder.buildRow(pos.value));
         sb.append("<th>").append(pos.name).append("</th>");
         int adjustment = pos.adjustmentToAttack;
         sb.append("<td>").append(((adjustment <= -99) ? "N/A" : adjustment)).append("</td>");
         adjustment = pos.adjustmentToDefenseParry;
         sb.append("<td>").append(((adjustment <= -99) ? "N/A" : adjustment)).append("</td>");
         adjustment = pos.adjustmentToDefenseDodge;
         sb.append("<td>").append(((adjustment <= -99) ? "N/A" : adjustment)).append("</td>");
         adjustment = pos.adjustmentToDefenseRetreat;
         sb.append("<td>").append(((adjustment <= -99) ? "N/A" : adjustment)).append("</td>");
         sb.append("</tr>");
      }
      sb.append("</table>");
      return sb.toString();
   }

   public static String generateRangeAdjustmentTable() {
      StringBuilder sb = new StringBuilder();
      sb.append("<H4>Range adjustemnts:</H4>");
      sb.append("<table>");
      sb.append("<tr class=\"header-row\">");
      sb.append("<th>Range</th><th>PD Adjustment</th><th>Active Defense Adj.<br/>Per Def. Action</th></tr>");
      int htmlRow = 0;
      for (RANGE range : RANGE.values()) {
         if (range != RANGE.OUT_OF_RANGE) {
            sb.append(HtmlBuilder.buildRow(htmlRow++));
            sb.append("<th>").append(range.getName()).append("</th>");
            int adj = Rules.getRangeDefenseAdjustmentToPD(range);
            sb.append("<td>");
            if (adj > 0) {
               sb.append("+");
            }
            sb.append(adj).append("</td>");
            sb.append("<td>");
            adj = Rules.getRangeDefenseAdjustmentPerAction(range);
            if (adj > 0) {
               sb.append("+");
            }
            sb.append(adj).append("</td>");
            sb.append("</tr>");
         }
      }
      sb.append("</table>");
      return sb.toString();
   }

   public static String generateSizeAdjustmentTable() {
      StringBuilder sb = new StringBuilder();
      sb.append("<table>");
      sb.append("<tr class=\"header-row\"><th rowspan=2>Racial Build</th><th colspan=4>Weapon's original melee range</th><th rowspan=2>Common Races:</th></tr>");
      sb.append("<tr class=\"header-row\"><th>1</th><th>2</th><th>3</th><th>4</th></tr>");
      int htmlRow = 0;
      for (byte ave = -36; ave <= 36; ave += 6) {
         sb.append(HtmlBuilder.buildRow(htmlRow++));
         sb.append("<th>");
         int min = ave - 2;
         int max = ave +3;
         if (ave > 0) {
            sb.append("+");
         }
         else {
            min = ave - 3;
         }
         sb.append(min);
         sb.append(" to ");
         if (ave >= 0) {
            sb.append("+");
         }
         else {
            max = ave + 2;
         }
         sb.append(max);
         sb.append("</th>");
         for (short orig = 1; orig < 5; orig++) {
            sb.append("<td>").append(adjustMeleeRangeForSize(orig, ave)).append("</td>");
         }
         sb.append("<td class='alignLeft'>");
         // don't show races for size 0 beings.
         if (ave != 0) {
            ArrayList<String> raceNames = new ArrayList<>();
            for (Race race : Race._raceList) {
               if (raceNames.contains(race.getName())) {
                  // ignore duplicate races that exist to deal with genders.
                  continue;
               }
               if ((race.getBuildModifier() >= min) && (race.getBuildModifier() <= max)) {
                  if (race.getArmCount() > 0) {
                     if (race.hasProperty(Race.PROPERTIES_ANIMAL)) {
                        continue;
                     }
                     if (!raceNames.isEmpty()) {
                        sb.append(", ");
                     }
                     sb.append(race.getName().replace(", ", "-"));
                     raceNames.add(race.getName());
                  }
               }
            }
         }
         sb.append("</td>");
         sb.append("</tr>");
      }
      sb.append("</table>");
      return sb.toString();
   }

   public static String generateMiscHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader());
      sb.append("<body>");
      sb.append("<table class='Hidden'>");
      sb.append("<tr><td class='alignLeft'>");
      sb.append(generateChargeDamageTable());
      sb.append("</td><td class='alignLeft'>");
      sb.append(generatePositionAdjustmentTable());
      sb.append(generateRangeAdjustmentTable());

      sb.append("<h4>Target Movement Adjustments:</h4>");
      sb.append("<table class=\"Hidden\">");
      sb.append("<tr><td class='alignLeft'>PD Adjustment for target moving:</td><td> +").append(Rules.getTNBonusForMovement(RANGE.LONG, false)).append("</td></tr>");
      sb.append("<tr><td class='alignLeft'>PD Adjustment for target moving evasively:</td><td> +").append(Rules.getTNBonusForMovement(RANGE.LONG, true)).append("</td></tr>");
      sb.append("</table>");
      sb.append("</td><td class='alignLeft'>");
      sb.append("<h4>Weapon Size Adjustments:</h4>");
      sb.append(generateSizeAdjustmentTable());
      sb.append("</td></tr>");
      sb.append("</table>");
      sb.append("</body>");
      return sb.toString();
   }

   public static byte getTNBonusForActions(byte numberOfActionsToAttack) {
      return (byte) ((5 * numberOfActionsToAttack) - 10);
   }

   public static byte getTNBonusForMovement(RANGE range, boolean isMovingEvasively) {
      if (isMovingEvasively) {
         return 5;
      }
      return 2;
   }

   public static byte getRangeDefenseAdjustmentToPD(RANGE range) {
      switch (range) {
         case POINT_BLANK:     return 0;
         case SHORT:           return +4;
         case MEDIUM:          return +8;
         case LONG:            return +12;
         case OUT_OF_RANGE:
      }
      return 0;
   }

   public static byte getRangeDefenseAdjustmentPerAction(RANGE range) {
      switch (range) {
         case POINT_BLANK:     return -4;
         case SHORT:           return 0;
         case MEDIUM:          return +4;
         case LONG:            return +8;
         case OUT_OF_RANGE:
      }
      return 0;
   }

   public static double getRangeAdjusterForAdjustedStr(byte adjustedStrength) {
      return Math.pow(1.0473, adjustedStrength);
   }

   public static byte getHandUsePenalty(LimbType hand, int armCount) {
      if (hand == LimbType.HAND_RIGHT) {
         return 0;
      }
      if (armCount == 2) {
         if (hand == LimbType.HAND_LEFT) {
            return 3;
         }
      }
      else if (armCount == 4) {
         switch (hand) {
            case HAND_LEFT:    return 2;
            case HAND_RIGHT_2: return 2;
            case HAND_LEFT_2:  return 3;
         }
      }
      else if (armCount == 6) {
         switch (hand) {
            case HAND_LEFT:    return 1;
            case HAND_RIGHT_2: return 1;
            case HAND_LEFT_2:  return 2;
            case HAND_RIGHT_3: return 2;
            case HAND_LEFT_3:  return 3;
         }
      }
      return 0;
   }

   public static byte getPositionAdjustedDefenseOption(Position position, DefenseOption defBase, byte baseActiveDef) {
      if (position == Position.STANDING) {
         return baseActiveDef;
      }

      int adjuster = 1;
      if (defBase == DefenseOption.DEF_DODGE) {
         adjuster = position.adjustmentToDefenseDodge;
      }
      else if (defBase == DefenseOption.DEF_RETREAT) {
         adjuster = position.adjustmentToDefenseRetreat;
      }
      else if ((defBase == DefenseOption.DEF_LEFT) ||
               (defBase == DefenseOption.DEF_LEFT_2)||
               (defBase == DefenseOption.DEF_LEFT_3) ||
               (defBase == DefenseOption.DEF_RIGHT) ||
               (defBase == DefenseOption.DEF_RIGHT_2) ||
               (defBase == DefenseOption.DEF_RIGHT_3)) {
         adjuster = position.adjustmentToDefenseParry;
         // magic defenses are unaffected by your position/
      }
      return (byte) Math.max(0, baseActiveDef + adjuster);
   }

   public static byte getMagicResistanceBonus(Advantage magicResistanceAdv, byte resistanceActions) {
      if (!Configuration._useExtendedDice) {
         resistanceActions = 3;
      }
      return (byte) (magicResistanceAdv.getLevel() * resistanceActions);
   }

   public static byte getMaxAppliedDamageForTNSuccess(byte attackSuccessRollOverTN, byte baseDamage) {
      if (attackSuccessRollOverTN < 5) {
         return (byte) Math.min((attackSuccessRollOverTN + 1) * 5, baseDamage);
      }
      return baseDamage;
   }

   public static byte getChargeDamageForSpeed(byte speed) {
      switch (speed) {
         case  0: return (byte) 0;
         case  1: return (byte) 0;
         case  2: return (byte) 1;
         case  3: return (byte) 2;
         case  4: return (byte) 3;
         case  5: return (byte) 5;
         case  6: return (byte) 7;
         case  7: return (byte) 9;
         case  8: return (byte) 11;
         case  9: return (byte) 13;
         case 10: return (byte) 15;
      }
      return (byte) (15 + ((speed - 10) * 3));
   }

   public static RANGE getRangeForSpell(short distanceInHexes, short rangeBase) {
      return getRangeForWeapon(distanceInHexes, rangeBase, false);
   }

   public static RANGE getRangeForWeapon(short distanceInHexes, short rangeBase, boolean thrown) {
      //if (distanceInHexes <= (rangeBase / 4.0)) return RANGE.POINT_BLANK;
      if ((distanceInHexes <= (rangeBase / 2.0)) && !thrown) {
         return RANGE.POINT_BLANK;
      }
      if (distanceInHexes <= rangeBase) {
         return RANGE.SHORT;
      }
      if (distanceInHexes <= (rangeBase * 2)) {
         return RANGE.MEDIUM;
      }
      if (distanceInHexes <= (rangeBase * 4)) {
         return RANGE.LONG;
      }
      return RANGE.OUT_OF_RANGE;
   }

   public static short getDistanceForRange(RANGE range, double adjustedRangeBase, boolean isThrown) {
      switch (range) {
         case POINT_BLANK:
            if (isThrown) {
               return 0;
            }
            return (short) (adjustedRangeBase / 2.0);
         case SHORT:
            return (short) adjustedRangeBase;
         case MEDIUM:
            return (short) (adjustedRangeBase * 2);
         case LONG:
            return (short) (adjustedRangeBase * 4);
      }
      throw new IllegalArgumentException("range (" + range + ") is not valid.");
   }

}
