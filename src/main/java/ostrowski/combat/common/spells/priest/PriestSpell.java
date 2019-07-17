package ostrowski.combat.common.spells.priest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.defensive.PriestDefensiveSpell;
import ostrowski.combat.common.spells.priest.demonic.PriestDemonicSpell;
import ostrowski.combat.common.spells.priest.elemental.PriestElementalSpell;
import ostrowski.combat.common.spells.priest.evil.PriestEvilSpell;
import ostrowski.combat.common.spells.priest.good.PriestGoodSpell;
import ostrowski.combat.common.spells.priest.healing.PriestHealingSpell;
import ostrowski.combat.common.spells.priest.information.PriestInformationSpell;
import ostrowski.combat.common.spells.priest.nature.PriestNatureSpell;
import ostrowski.combat.common.spells.priest.offensive.PriestOffensiveSpell;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.Configuration;

public class PriestSpell extends Spell
{
   private byte                           _affinityLevel;
   protected byte                         _effectivePower;
   private Class< ? extends IPriestGroup> _group;
   private String                         _deity = null;

   public PriestSpell() {
   }

   public PriestSpell(String name, Class< ? extends IPriestGroup> group, int affinityLevel) {
      super(name);
      _group = group;
      _affinityLevel = (byte) affinityLevel;
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   public byte getEffectivePower() {
      return _effectivePower;
   }

   @Override
   public String describeSpell() {
      return null;
   }

   @Override
   public byte getIncantationTime() {
      return _affinityLevel;
   }

   @Override
   public byte getTN(Character caster) {
      return 0;
   }

   @SuppressWarnings("unchecked")
   public static String generateHtmlTablePriestSpells() {
      ArrayList<PriestSpell>[] spellsInGroup = new ArrayList[GROUP_NAMES.size()];
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader());
      sb.append("<body>");
      sb.append("<H2>Priest Spells:</H2>");

      sb.append("<table style='border-width: 0px'><tr><td style='border-width: 0px; padding:20px'>");
      sb.append("<H4>Power Points Per Day:</H4>");
      Table table = new Table();
      table.addRow(new TableRow(-1, new TableHeader("Divine<br/>Affinity").setAttribute("rowspan", "2"), new TableHeader("Divine Power").setAttribute("colspan", "5")));
      table.addRow(new TableRow(-1, "1", "2", "3", "4", "5"));
      for (byte affinity=1 ; affinity<=5 ; affinity++) {
         table.addRow(new TableRow(affinity-1, ""+affinity, affinity * 5, affinity *10,  affinity * 15, affinity * 20, affinity * 25));
      }
      sb.append(table.toString());

      sb.append("</td><td style='border-width: 0px; padding:40px'>");

      sb.append("<H4>Dice for power:</H4>");
      PriestSpell anySpell = new PriestSpell();
      table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Dice used"));
      for (byte power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power, anySpell.describeDiceForPower(power)));
      }
      sb.append(table.toString());

      sb.append("</td><td style='border-width: 0px; padding:40px'>");

      sb.append("<H4>Deity groups:</H4>");
      table = new Table();
      TableRow headerRow = new TableRow(-1, "Deity");
      table.addRow(headerRow);
      for (int col = 0; col < GROUP_NAMES.size(); col++) {
         headerRow.addHeader(GROUP_NAMES.get(col));
         spellsInGroup[col] = getSpellsInGroup(GROUP_NAMES.get(col));
      }

      for (int row = 0; row < _deities.size(); row++) {
         TableRow tableRow = new TableRow(row);

         String deity = _deities.get(row);
         tableRow.addHeader(deity);
         ArrayList<String> groupsForDeity = getSpellGroups(deity);
         for (int col = 0; col < GROUP_NAMES.size(); col++) {
            if (groupsForDeity.contains(GROUP_NAMES.get(col))) {
               tableRow.addTD("Yes");
            }
            else {
               tableRow.addTD("&nbsp;");
            }
         }
         table.addRow(tableRow);
      }
      sb.append(table.toString());
      sb.append("</td></tr></table>");

      sb.append("<H4>Spells in groups:</H4>");
      table = new Table();
      headerRow = new TableRow(-1, "Affinity");
      table.addRow(headerRow);
      for (int col = 0; col < GROUP_NAMES.size(); col++) {
         headerRow.addHeader(GROUP_NAMES.get(col));
      }
      for (int affinity = 1; affinity <= 10; affinity++) {
         TableRow tableRow = new TableRow(affinity-1);
         tableRow.addHeader(new TableHeader(""+affinity).setAttribute("valign", "center"));
         for (int col = 0; col < GROUP_NAMES.size(); col++) {
            StringBuilder content = new StringBuilder();
            for (PriestSpell spell : spellsInGroup[col]) {
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
      sb.append(table.toString());

      sb.append("<H4>Priest Spell Grimiore:</H4>");
      sb.append(getSpellGrimioreForHTML());
      sb.append("</body>");
      return sb.toString();
   }

   protected static String getSpellGrimioreForHTML() {
      SortedSet<PriestSpell> spells = new TreeSet<>();
      for (int col = 0; col < GROUPS.length; col++) {
         spells.addAll(getSpellsInGroup(GROUP_NAMES.get(col)));
      }
      return getSpellGrimioreForHTML(spells);
   }

   @SuppressWarnings("unchecked")
   static Class< ? extends IPriestGroup>[] GROUPS            = new Class[] { PriestDefensiveSpell.class, PriestEvilSpell.class, PriestOffensiveSpell.class, PriestGoodSpell.class, PriestHealingSpell.class, PriestInformationSpell.class, PriestNatureSpell.class, PriestDemonicSpell.class,};

   static private final String             THOR              = "Thor";
   static private final String             APHRODITE         = "Aphrodite";
   static private final String             DRUID             = "Druid";
   static private final String             HADES             = "Hades";
   static private final String             TAO               = "Tao";
   static private final String             DEMONIC           = "Demonic";
   static public final ArrayList<String>   _deities          = new ArrayList<>();
   static {
      _deities.add(THOR);
      _deities.add(APHRODITE);
      _deities.add(DRUID);
      _deities.add(HADES);
      _deities.add(TAO);
      _deities.add(DEMONIC);
   }

   static public final String              GROUP_DEFENSIVE   = "Defensive";
   static public final String              GROUP_HEALING     = "Healing";
   static public final String              GROUP_INFORMATION = "Information";
   static public final String              GROUP_NATURE      = "Nature";
   static public final String              GROUP_ELEMENTAL   = "Elemental";
   static public final String              GROUP_EVIL        = "Evil";
   static public final String              GROUP_GOOD        = "Good";
   static public final String              GROUP_OFFENSIVE   = "Offensive";
   static public final String              GROUP_DEMON       = "Demon";
   static public final ArrayList<String>   GROUP_NAMES       = new ArrayList<>();
   static {
      GROUP_NAMES.add(GROUP_DEFENSIVE);
      GROUP_NAMES.add(GROUP_HEALING);
      GROUP_NAMES.add(GROUP_INFORMATION);
      GROUP_NAMES.add(GROUP_NATURE);
      GROUP_NAMES.add(GROUP_ELEMENTAL);
      GROUP_NAMES.add(GROUP_EVIL);
      GROUP_NAMES.add(GROUP_GOOD);
      GROUP_NAMES.add(GROUP_OFFENSIVE);
      GROUP_NAMES.add(GROUP_DEMON);
   }
   static {
      StringBuilder problems = new StringBuilder();
      ArrayList<PriestSpell> allSpells = getSpellsInGroup(null);
      for (PriestSpell spell : allSpells) {
         if (spell.describeSpell() == null) {
            problems.append("Spell ").append(spell.getName()).append(" needs to return a string from describeSpell()\n");
         }
      }
      if (problems.length() != 0) {
         Rules.diag("Problems found:\n" + problems.toString());
         DebugBreak.debugBreak(problems.toString());
      }
   }

   static public ArrayList<String> getSpellGroups(String deity) {
      ArrayList<String> spells = new ArrayList<>();
      if (deity.equalsIgnoreCase(THOR)) {
         spells.add(GROUP_DEFENSIVE);
         spells.add(GROUP_HEALING);
         spells.add(GROUP_GOOD);
         spells.add(GROUP_OFFENSIVE);
      }
      else if (deity.equalsIgnoreCase(APHRODITE)) {
         spells.add(GROUP_DEFENSIVE);
         spells.add(GROUP_INFORMATION);
         spells.add(GROUP_HEALING);
         spells.add(GROUP_GOOD);
      }
      else if (deity.equalsIgnoreCase(DRUID)) {
         spells.add(GROUP_NATURE);
         spells.add(GROUP_ELEMENTAL);
         spells.add(GROUP_HEALING);
      }
      else if (deity.equalsIgnoreCase(HADES)) {
         spells.add(GROUP_DEFENSIVE);
         spells.add(GROUP_EVIL);
         spells.add(GROUP_INFORMATION);
         spells.add(GROUP_OFFENSIVE);
      }
      else if (deity.equalsIgnoreCase(TAO)) {
         spells.add(GROUP_EVIL);
         spells.add(GROUP_GOOD);
         spells.add(GROUP_HEALING);
         spells.add(GROUP_NATURE);
      }
      else if (deity.equalsIgnoreCase(DEMONIC)) {
         spells.add(GROUP_EVIL);
         spells.add(GROUP_DEMON);
      }
      spells.add(deity);
      return spells;
   }

   static public ArrayList<PriestSpell> getSpellsInGroup(String group) {
      ArrayList<PriestSpell> spells = new ArrayList<>();
      if ((group == null) || (group.equals(GROUP_OFFENSIVE))) {
         spells.addAll(PriestOffensiveSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_DEFENSIVE))) {
         spells.addAll(PriestDefensiveSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_HEALING))) {
         spells.addAll(PriestHealingSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_GOOD))) {
         spells.addAll(PriestGoodSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_EVIL))) {
         spells.addAll(PriestEvilSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_INFORMATION))) {
         spells.addAll(PriestInformationSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_NATURE))) {
         spells.addAll(PriestNatureSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_ELEMENTAL))) {
         spells.addAll(PriestElementalSpell._spells);
      }
      if ((group == null) || (group.equals(GROUP_DEMON))) {
         spells.addAll(PriestDemonicSpell._spells);
      }
      // add the deity-specific spells
      if ((group == null) || (group.equalsIgnoreCase(THOR))) {
      }
      else if ((group == null) || (group.equalsIgnoreCase(APHRODITE))) {
      }
      else if ((group == null) || (group.equalsIgnoreCase(DRUID))) {
      }
      else if ((group == null) || (group.equalsIgnoreCase(HADES))) {
      }
      else if ((group == null) || (group.equalsIgnoreCase(TAO))) {
      }
      else if ((group == null) || (group.equalsIgnoreCase(DEMONIC))) {
      }
      return spells;
   }

   static public List<PriestSpell> getSpellsForDeity(String deity, int maximumAffinity, boolean addNullBetweenGroups) {
      List<PriestSpell> spells = new ArrayList<>();
      List<String> groups = getSpellGroups(deity);
      for (String group : groups) {
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
      return _affinityLevel;
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
         DiceSet dice = Rules.getDice(_caster.getAttributeLevel(castingAttr), actionDice, castingAttr/*attribute*/);
         if (bonusToDice != 0) {
            return dice.addBonus(bonusToDice);
         }
         return dice;
      }
      DiceSet dice = Rules.getDice(_caster.getAttributeLevel(castingAttr), (byte) 1/*actions*/, castingAttr/*attribute*/);
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

   @Override
   public byte getCastingLevel() {
      return _caster.getAffinity(_deity);
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
         writeToStream(_affinityLevel, out);
         writeToStream(_group.getName(), out);
         writeToStream(_deity, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _affinityLevel = readByte(in);
         String groupClassName = readString(in);
         _deity = readString(in);
         for (Class< ? extends IPriestGroup> element : GROUPS) {
            if (groupClassName.equalsIgnoreCase(element.getName())) {
               _group = element;
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
         _affinityLevel = ((PriestSpell) source)._affinityLevel;
         _group = ((PriestSpell) source)._group;
         _deity = ((PriestSpell) source)._deity;
      }
   }

   public void setDeity(String deity) {
      _deity = deity;
   }

   public String getDeity() {
      return _deity;
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
      _effectivePower = (byte) (getPower() - attackerPowerPainPenalty - attackerPowerRangePenalty - magicResLevel);
      if (_effectivePower < 0 ) {
         _effectivePower = 0;
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
         tr.addTD(_effectivePower);
         tr.addTD("Effective power");
         table.addRow(tr);
         sbDescription.append(table.toString());
         sbDescription.append("<br/>");
      }
      return _effectivePower;
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
      node.setAttribute("affinityLevel", String.valueOf(_affinityLevel));
      node.setAttribute("effectivePower", String.valueOf(_effectivePower));
      node.setAttribute("deity", String.valueOf(_deity));
      return node;
   }

   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("affinityLevel");
      if (node != null) {
         _affinityLevel = Byte.parseByte(node.getNodeValue());
      }
      node = namedNodeMap.getNamedItem("effectivePower");
      if (node != null) {
         _effectivePower = Byte.parseByte(node.getNodeValue());
      }
      node = namedNodeMap.getNamedItem("deity");
      if (node != null) {
         _deity = node.getNodeValue();
      }
   }

}
