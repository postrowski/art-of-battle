package ostrowski.combat.common.spells.priest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ostrowski.DebugBreak;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.html.*;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.defensive.PriestDefensiveSpell;
import ostrowski.combat.common.spells.priest.demonic.PriestDemonicSpell;
import ostrowski.combat.common.spells.priest.elemental.PriestElementalSpell;
import ostrowski.combat.common.spells.priest.evil.PriestEvilSpell;
import ostrowski.combat.common.spells.priest.good.PriestGoodSpell;
import ostrowski.combat.common.spells.priest.good.SpellBless;
import ostrowski.combat.common.spells.priest.healing.PriestHealingSpell;
import ostrowski.combat.common.spells.priest.information.PriestInformationSpell;
import ostrowski.combat.common.spells.priest.nature.PriestNatureSpell;
import ostrowski.combat.common.spells.priest.offensive.PriestOffensiveSpell;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class PriestSpell extends Spell
{
   private byte                           affinityLevel;
   protected byte                         effectivePower;
   private   Class< ? extends IPriestGroup> group;
   private   Deity                          deity = null;
   protected byte                           level = 0;

   public PriestSpell() {
   }

   public PriestSpell(String name, Class< ? extends IPriestGroup> group, int affinityLevel) {
      super(name);
      this.group = group;
      this.affinityLevel = (byte) affinityLevel;
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   public byte getEffectivePower() {
      return effectivePower;
   }

   @Override
   public String describeSpell() {
      return null;
   }

   @Override
   public byte getIncantationTime() {
      return affinityLevel;
   }

   @Override
   public byte getTN(Character caster) {
      return 0;
   }

   static public PriestSpell getSpell(String name) {
      for (Deity deity : Deity.values()) {
         List<PriestSpell> deitySpells = PriestSpell.getSpellsForDeity(deity, 10, false/*addNullBetweenGroups*/);
         for (PriestSpell priestSpell : deitySpells) {
            if (priestSpell.getName().equalsIgnoreCase(name)) {
               return priestSpell;
            }
         }
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   public static String generateHtmlTablePriestSpells() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader());
      sb.append("<body>");
      sb.append("<H2>Priest Spells:</H2>");

      sb.append("<table style='border-width: 0px'><tr><td style='border-width: 0px; padding:20px'>");
      sb.append("<H4>Power Points Per Day:</H4>");
      Table table = new Table();
      table.addRow(new TableRow(-1, new TableHeader("Divine<br/>Affinity").setAttribute("rowspan", "2"), new TableHeader("Divine Power").setAttribute("colspan", "5")));
      table.addRow(new TableRow(-1, "1", "2", "3", "4", "5"));
      for (byte affinity=1 ; affinity<=10 ; affinity++) {
         table.addRow(new TableRow(affinity-1, ""+affinity, affinity * 5, affinity *10,  affinity * 15, affinity * 20, affinity * 25));
      }
      sb.append(table);

      sb.append("</td><td style='border-width: 0px; padding:40px'>");

      sb.append("<H4>Dice for power:</H4>");
      PriestSpell anySpell = new SpellBless();
      table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Dice used"));
      for (byte power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power, anySpell.describeDiceForPower(power)));
      }
      sb.append(table);

      sb.append("</td><td style='border-width: 0px; padding:40px'>");

      sb.append("<H4>Deity groups:</H4>");
      table = new Table();
      TableRow headerRow = new TableRow(-1, "Deity");
      table.addRow(headerRow);
      int col = 0;
      for (SpellGroup spellGroup : SpellGroup.values()) {
         headerRow.addHeader(spellGroup.getName());
      }

      int row=0;
      for (Deity deity : Deity.values()) {
         TableRow tableRow = new TableRow(row++);

         tableRow.addHeader(deity.getName());
         List<SpellGroup> groupsForDeity = getSpellGroups(deity);
         for (SpellGroup spellGroup : SpellGroup.values()) {
            if (groupsForDeity.contains(spellGroup)) {
               tableRow.addTD("Yes");
            } else {
               tableRow.addTD("&nbsp;");
            }
         }
         table.addRow(tableRow);
      }
      sb.append(table);
      sb.append("</td></tr></table>");

      sb.append("<H4>Spells in groups:</H4>");
      table = new Table();
      headerRow = new TableRow(-1, "Affinity");
      table.addRow(headerRow);
      for (SpellGroup spellGroup : SpellGroup.values()) {
         headerRow.addHeader(spellGroup.getName());
      }
      for (int affinity = 1; affinity <= 10; affinity++) {
         TableRow tableRow = new TableRow(affinity-1);
         tableRow.addHeader(new TableHeader(""+affinity).setAttribute("valign", "center"));

         for (SpellGroup spellGroup : SpellGroup.values()) {
            StringBuilder content = new StringBuilder();
            for (PriestSpell spell : getSpellsInGroup(spellGroup)) {
               if (spell.getAffinity() == affinity) {
                  String trimmedName = spell.getName().replaceAll(" ", "");
                  content.append("<a href='#").append(trimmedName).append("'>");
                  content.append(spell.getName());
                  content.append("</a>");
                  content.append("<br/>");
               }
            }
            if (content.length() == 0) {
               content.append("&nbsp;");
            }
            tableRow.addTD(new TableData(content.toString()).setAttribute("valign", "center"));
         }
         table.addRow(tableRow);
      }
      sb.append(table);

      sb.append("<H4>Priest Spell Grimiore:</H4>");
      sb.append(getSpellGrimioreForHTML());
      sb.append("</body>");
      return sb.toString();
   }

   protected static String getSpellGrimioreForHTML() {
      SortedSet<PriestSpell> spells = new TreeSet<>();
      for (SpellGroup groupName : SpellGroup.values()) {
         List<PriestSpell> groupSpells = getSpellsInGroup(groupName);
         spells.addAll(groupSpells);
      }
      return getSpellGrimioreForHTML(spells);
   }

   @SuppressWarnings("unchecked")
   static final Class< ? extends IPriestGroup>[] GROUPS = new Class[] {PriestDefensiveSpell.class,
                                                                       PriestEvilSpell.class,
                                                                       PriestOffensiveSpell.class,
                                                                       PriestGoodSpell.class,
                                                                       PriestHealingSpell.class,
                                                                       PriestElementalSpell.class,
                                                                       PriestInformationSpell.class,
                                                                       PriestNatureSpell.class,
                                                                       PriestDemonicSpell.class,
                                                                       };

   static {
      StringBuilder problems = new StringBuilder();
      List<PriestSpell> allSpells = getSpellsInGroup(null);
      for (PriestSpell spell : allSpells) {
         if (spell.describeSpell() == null) {
            problems.append("Spell ").append(spell.getName()).append(" needs to return a string from describeSpell()\n");
         }
      }
      if (problems.length() != 0) {
         Rules.diag("Problems found:\n" + problems);
         DebugBreak.debugBreak(problems.toString());
      }
   }

   static public List<SpellGroup> getSpellGroups(Deity deity) {
      List<SpellGroup> spells = new ArrayList<>();
      for (SpellGroup spellGroup : deity.getSpellGroups()) {
         spells.add(spellGroup);
      }
      //spells.add(deity.getName());
      return spells;
   }

   static public List<PriestSpell> getSpellsInGroup(SpellGroup group) {
      List<PriestSpell> spells = new ArrayList<>();
      if ((group == null) || (group == SpellGroup.OFFENSIVE)) {
         spells.addAll(PriestOffensiveSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.DEFENSIVE)) {
         spells.addAll(PriestDefensiveSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.HEALING)) {
         spells.addAll(PriestHealingSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.GOOD)) {
         spells.addAll(PriestGoodSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.EVIL)) {
         spells.addAll(PriestEvilSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.INFORMATION)) {
         spells.addAll(PriestInformationSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.NATURE)) {
         spells.addAll(PriestNatureSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.ELEMENTAL)) {
         spells.addAll(PriestElementalSpell.spells);
      }
      if ((group == null) || (group == SpellGroup.DEMON)) {
         spells.addAll(PriestDemonicSpell.spells);
      }
      // add the deity-specific spells
//      if ((group == null) || (group.equalsIgnoreCase(THOR))) {
//      }
//      else if ((group == null) || (group.equalsIgnoreCase(APHRODITE))) {
//      }
//      else if ((group == null) || (group.equalsIgnoreCase(DRUID))) {
//      }
//      else if ((group == null) || (group.equalsIgnoreCase(HADES))) {
//      }
//      else if ((group == null) || (group.equalsIgnoreCase(TAO))) {
//      }
//      else if ((group == null) || (group.equalsIgnoreCase(DEMONIC))) {
//      }
      return spells;
   }

   static public List<PriestSpell> getSpellsForDeity(Deity deity, int maximumAffinity, boolean addNullBetweenGroups) {
      List<PriestSpell> spells = new ArrayList<>();
      List<SpellGroup> groups = getSpellGroups(deity);
      for (SpellGroup group : groups) {
         List<PriestSpell> groupSpells = getSpellsInGroup(group);
         for (PriestSpell spell : groupSpells) {
            if ((maximumAffinity == 0) || (spell.getAffinity() <= maximumAffinity)) {
               spell.setDeity(deity);
               spells.add(spell);
            }
         }
         if (addNullBetweenGroups) {
            spells.add(null);
         }
      }
      return spells;
   }

   public byte getAffinity() {
      return affinityLevel;
   }

   @Override
   public Attribute getCastingAttribute() {
      //      if (isInate()) {
      //         return ATT_DEX;
      //      }
      return Attribute.Social;
   }

   protected String describeDiceForPower(byte effectivePower) {
      if (effectivePower == 0) {
         return "0";
      }
      StringBuilder sb = new StringBuilder();
      if (Configuration.useExtendedDice()) {
         sb.append(getActionsDice(effectivePower)).append("-action ");
         sb.append(getCastingAttribute().shortName);
         byte bonusDice = getBonusDice(effectivePower);
         if (bonusDice > 0) {
            sb.append(" +").append(bonusDice);
         }
      }
      else {
         sb.append(getCastingAttribute().shortName);
         sb.append(" +  Divine Affinity");
         byte bonusDice = (byte) (((effectivePower - 1) * 3) - 5);
         if (bonusDice != 0) {
            sb.append(" ");
            if (bonusDice > 0) {
               sb.append("+");
            }
            sb.append(bonusDice);
         }
      }
      return sb.toString();
   }

   private static byte getActionsDice(byte effectivePower) {
      if (effectivePower < 1) {
         return 0;
      }
      if (effectivePower < 3) {
         return 1;
      }
      if (effectivePower < 5) {
         return 2;
      }
      return 3;
   }

   private static byte getBonusDice(byte effectivePower) {
      if (effectivePower == 2) {
         return 2;
      }
      if (effectivePower == 4) {
         return 2;
      }
      if (effectivePower > 5) {
         return (byte) ((effectivePower - 5) * 2);
      }
      return 0;
   }

   public DiceSet getCastDice(byte effectivePower) {
      Attribute castingAttr = getCastingAttribute();
      if (Configuration.useExtendedDice()) {
         byte actionDice = getActionsDice(effectivePower);
         byte bonusToDice = getBonusDice(effectivePower);
         DiceSet dice = Rules.getDice(caster.getAttributeLevel(castingAttr), actionDice, castingAttr/*attribute*/, RollType.SPELL_CASTING);
         if (bonusToDice != 0) {
            return dice.addBonus(bonusToDice);
         }
         return dice;
      }
      DiceSet dice = Rules.getDice(caster.getAttributeLevel(castingAttr), (byte) 1/*actions*/, castingAttr/*attribute*/, RollType.SPELL_CASTING);
      byte powerBonus = (byte) ((effectivePower - 1) * 3);
      return dice.addBonus(powerBonus);
   }

   @Override
   public byte getRangeTNAdjustment(short distanceInHexes) {
      // Priest spells lose power over their distance, rather than modifying the resistant TN
      return 0;
   }

   public byte getPowerReductionForRange(short distanceInHexes, RANGE range) {
      if (this instanceof IRangedSpell) {
         switch (range) {
            case POINT_BLANK:
               return 0;
            case SHORT:
               return 1;
            case MEDIUM:
               return 2;
            case LONG:
               return 3;
            case OUT_OF_RANGE:
               return 0;
         }
      }
      if (distanceInHexes <= 1) {
         return 0;
      }
      return (byte) Math.min((distanceInHexes - 1), 127);
   }

   public byte getLevel() {
      return level;
   }

   public void setLevel(byte level) {
      this.level = level;
   }


   @Override
   public byte getCastingLevel() {
      return caster.getAffinity(deity);
   }

   @Override
   public byte getAdjustedCastingSkillLevel(Character caster) {
      byte skill = getCastingLevel();
      byte attrLevel = caster.getAttributeLevel(getCastingAttribute());
      return (byte) (skill + attrLevel);
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(affinityLevel, out);
         writeToStream(group.getName(), out);
         writeToStream(deity.getName(), out);
         writeToStream(level, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         affinityLevel = readByte(in);
         String groupClassName = readString(in);
         deity = Deity.getByName(readString(in));
         level = readByte(in);
         for (Class< ? extends IPriestGroup> element : GROUPS) {
            if (groupClassName.equalsIgnoreCase(element.getName())) {
               group = element;
               break;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected void copyDataFrom(Spell source) {
      super.copyDataFrom(source);
      if (source instanceof PriestSpell) {
         affinityLevel = ((PriestSpell) source).affinityLevel;
         group = ((PriestSpell) source).group;
         deity = ((PriestSpell) source).deity;
         level = ((PriestSpell) source).level;
      }
   }

   public void setDeity(Deity deity) {
      this.deity = deity;
   }

   public Deity getDeity() {
      return deity;
   }

   protected byte resolveEffectivePower(StringBuilder sbDescription, short distanceInHexes, RANGE range) {
      byte attackerPowerPainPenalty = getPowerPenaltyForWoundsAndPain();
      byte attackerPowerRangePenalty = getPowerReductionForRange(distanceInHexes, range);
      Character target = getTarget();
      byte magicResLevel = 0;
      if (target != null) {
         if (!(this instanceof PriestMissileSpell)) {
            Advantage magicResistanceAdv = target.getAdvantage(Advantage.MAGIC_RESISTANCE);
            if (magicResistanceAdv != null) {
               magicResLevel = magicResistanceAdv.getLevel();
            }
         }
      }
      effectivePower = (byte) (getPower() - attackerPowerPainPenalty - attackerPowerRangePenalty - magicResLevel);
      if (effectivePower < 0 ) {
         effectivePower = 0;
      }

      if (sbDescription != null) {
         Table table = new Table();
         TableRow tr = new TableRow();
         tr.addTD(getPower());
         tr.addTD("base power");
         table.addRow(tr);
         if (attackerPowerPainPenalty > 0) {
            tr = new TableRow();
            tr.addTD("-" + attackerPowerPainPenalty);
            tr.addTD("Caster's pain");
            table.addRow(tr);
         }
         if (attackerPowerRangePenalty != 0) {
            tr = new TableRow();
            tr.addTD("-" + attackerPowerRangePenalty);
            tr.addTD("Casting distance");
            table.addRow(tr);
         }
         if (magicResLevel > 0) {
            tr = new TableRow();
            tr.addTD("-" + magicResLevel);
            tr.addTD(target.getName() + "'s Magic Resistance level " + magicResLevel);
            table.addRow(tr);
         }
         tr = new TableRow();
         tr.addTD(effectivePower);
         tr.addTD("Effective power");
         table.addRow(tr);
         sbDescription.append(table);
         sbDescription.append("<br/>");
      }
      return effectivePower;
   }

   @Override
   protected byte getCastingTN(RequestAction attack, short distanceInHexes, RANGE range, byte spellTN, Battle battle,
                                           StringBuilder sbDescription) {
      return 0;
   }

   @Override
   protected byte getRangeAdjustedCastingTN(RequestAction attack, short distanceInHexes, RANGE range, byte castingTN, byte rangeTNAdjustment, Battle battle,
                                            StringBuilder sbDescription) {
      return 0;
   }

   @Override
   public byte getCastingPower(RequestAction attack, short distanceInHexes, RANGE range, Battle battle, StringBuilder sbDescription) {
      return resolveEffectivePower(sbDescription, distanceInHexes, range);
   }

   @Override
   protected DiceSet getCastDice(RequestAction attack, byte distanceModifiedTN, byte distanceModifiedPower, short distanceInHexes, RANGE range, Battle battle,
                                 StringBuilder sbDescription) {
      return new DiceSet(0, 0, 0, 0, 0, 0, 0, 0/*dBell*/, 1.0);
   }

   @Override
   protected int getDefenseResult(RequestDefense defense, byte distanceModifiedPower, Battle battle, StringBuilder sbDescription) {
      return 0;
   }

   @Override
   public byte getSpellPoints() {
      return (byte) (getAffinity() * getPower());
   }

   @Override
   public short getMaxRange(Character caster) {
      if (this instanceof IRangedSpell) {
         return (short) (getAdjustedRangeBase() * 4);
      }
      return super.getMaxRange(caster);
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element node = super.getXMLObject(parentDoc, newLine);
      node.setAttribute("affinityLevel", String.valueOf(affinityLevel));
      node.setAttribute("effectivePower", String.valueOf(effectivePower));
      node.setAttribute("deity", String.valueOf(deity));
      return node;
   }

   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("affinityLevel");
      if (node != null) {
         affinityLevel = Byte.parseByte(node.getNodeValue());
      }
      node = namedNodeMap.getNamedItem("effectivePower");
      if (node != null) {
         effectivePower = Byte.parseByte(node.getNodeValue());
      }
      node = namedNodeMap.getNamedItem("deity");
      if (node != null) {
         deity = Deity.getByName(node.getNodeValue());
      }
   }

}
