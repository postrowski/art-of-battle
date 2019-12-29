package ostrowski.combat.common.wounds;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.server.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
 * Created on May 4, 2006
 *
 */
public abstract class WoundChart implements Enums {
   static final HashMap<DamageType, WoundChart> _charts = new HashMap<>();
   static { // static initializer
      @SuppressWarnings("unused")
      WoundChart a = new WoundChartBlunt();
      a = new WoundChartCut();
      a = new WoundChartImp();
      a = new WoundChartFire();
      a = new WoundChartElectric();
      a = new WoundChartSimple();
   }

   DamageType _damageType;
   // prevent default ctor
   @SuppressWarnings("unused")
   private WoundChart() {}

   public WoundChart(DamageType damageType) {
      _damageType = damageType;
      registerWithFactory(this);
   }

   abstract public Wound getWound(byte damageLevel, Character target);

   // factory class:
   static public boolean registerWithFactory(WoundChart chart) {
      DamageType key = chart._damageType;
      if (_charts.containsKey(key)) {
         return false;
      }
      _charts.put(key, chart);
      return true;
   }
   static public Wound getWound(byte damageLevel, DamageType damageType, Character target, StringBuilder alterationExplanationBuffer) {
      if (Configuration.useSimpleDamage()) {
         damageType = DamageType.GENERAL;
      }
      WoundChart chart = _charts.get(damageType);
      if (chart != null) {
         do {
            try {
                Wound wound = chart.getWound(damageLevel, target);
                return target.alterWound(wound, alterationExplanationBuffer);
            }
            catch (WoundCantBeAppliedToTargetException e) {
               // This gets thrown if we try to apply a wound that cant fit the target,
               // such as "arm crippled" when attacking something without arms.
               // decrement the damage level, until an applicable wound is found.
               damageLevel--;
               alterationExplanationBuffer.append(" damage reduce to ").append(damageLevel).append(" because ").append(e.getMessage());
            }
         } while (damageLevel >= 0);
      }
      return null;
   }

   private static final String[] ATTRIBUTES_HEAD = new String[] { "<i>",
                                                                  "<font color='#347235'>", // blunt
                                                                  "<font color='#E56717'>", // cut
                                                                  "<font color='#893BFF'>", // imp
                                                                  "<font color='#E41B17'>", // fire
                                                                  "<font color='#2554C7'>"};// electric
   private static final String[] ATTRIBUTES_TAIL = new String[] { "</i>", "</font>", "</font>", "</font>", "</font>", "</font>"};
   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
      List<WoundChart> woundCharts = new ArrayList<>();
      woundCharts.add(_charts.get(DamageType.GENERAL));
      woundCharts.add(_charts.get(DamageType.BLUNT));
      woundCharts.add(_charts.get(DamageType.CUT));
      woundCharts.add(_charts.get(DamageType.IMP));
      woundCharts.add(_charts.get(DamageType.FIRE));
      woundCharts.add(_charts.get(DamageType.ELECTRIC));
      for (WoundChart element : woundCharts) {
         sb.append(generateHtmlTable(element));
      }
      sb.append(generateCombinedHtmlTable(woundCharts));
      return sb.toString();
   }
   public static String generateHtmlTable(DamageType damType) {
      return generateHtmlTable(_charts.get(damType));
   }
   public static String generateCombinedHtmlTable() {
      List<WoundChart> woundCharts = new ArrayList<>();
      woundCharts.add(_charts.get(DamageType.GENERAL));
      woundCharts.add(_charts.get(DamageType.BLUNT));
      woundCharts.add(_charts.get(DamageType.CUT));
      woundCharts.add(_charts.get(DamageType.IMP));
      woundCharts.add(_charts.get(DamageType.FIRE));
      woundCharts.add(_charts.get(DamageType.ELECTRIC));
      return generateCombinedHtmlTable(woundCharts);
   }
   public static String generateHtmlTable(WoundChart chart) {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader("TblWounds", 400, 62));
      sb.append("<body>");
      sb.append("<H4>Wound Chart for ").append(chart.getWound((byte)0, null/*target*/)._damageType.fullname).append(" damage:</H4>");
      sb.append("<div style=\"overflow: hidden;\" id=\"DivHeaderRow\">\n");
      sb.append("</div>\n");
      sb.append("<div style=\"overflow:scroll;overflow-x:hidden; border-width:0px; border-bottom:1px; border-style:solid;\" onscroll=\"OnScrollDiv(this)\" id=\"DivMainContent\">\n");
      sb.append("<table id='TblWounds' class='doubleRow' width='100%'>");
      sb.append(generateWoundChartHeaderRow());
      for (byte woundLevel=0 ; woundLevel<30 ; woundLevel++) {
         Wound wound = chart.getWound(woundLevel, null/*target*/);
         sb.append(HtmlBuilder.buildRow(woundLevel));
         sb.append("<th>").append(woundLevel).append("</th>");
         for (int column = 0 ; column<9 ; column++) {
            sb.append("<td>").append(getDataForWoundInColumn(wound, column)).append("</td>");
         }
         sb.append("</tr>");
      }
      sb.append("<tr><td>&nbsp;</td></tr>\n");
      sb.append("</table>");
      sb.append("</div>");
      sb.append("</body>");
      return sb.toString();
   }

   public static String generateCombinedHtmlTable(List<WoundChart> charts) {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader("TblWounds", 400, 62));
      sb.append("<body>");
      sb.append("<H4>Combined Wound Chart:</H4>");
      sb.append("<div style=\"overflow: hidden;\" id=\"DivHeaderRow\">\n");
      sb.append("</div>\n");
      sb.append("<div style=\"overflow:scroll;overflow-x:hidden; border-width:0px; border-bottom:1px; border-style:solid;\" onscroll=\"OnScrollDiv(this)\" id=\"DivMainContent\">\n");
      sb.append("<table id='TblWounds' width='100%'>");
      sb.append(generateWoundChartHeaderRow());
      for (byte woundLevel=0 ; woundLevel<30 ; woundLevel++) {
         List<Wound> wounds = new ArrayList<>();
         for (WoundChart chart : charts) {
            wounds.add(chart.getWound(woundLevel, null/*target*/));
         }
         sb.append(HtmlBuilder.buildRow(woundLevel));
         sb.append("<th>").append(woundLevel).append("</th>");
         for (int column=0 ; column<9 ; column++) {
            sb.append("<td>").append(getCombinedDataForWoundInColumn(wounds, column)).append("</td>");
         }
         sb.append("</tr>");
      }
      sb.append("</table>");
      sb.append("</div>");
      sb.append("</body>");
      return sb.toString();
   }
   /**
    */
   private static String generateWoundChartHeaderRow()
   {
      TableRow tr = new TableRow();
      tr.setClassName("header-row");
      tr.addHeader("Damage<br/>Level");
      tr.addHeader("Location");
      tr.addHeader("Description");
      tr.addHeader("Pain");
      tr.addHeader("Wounds");
      tr.addHeader("Bleeding");
      tr.addHeader("Arm<br/>Penalty");
      tr.addHeader("Move /<br/>Retreat<br/>Penalty");
      tr.addHeader("Knock<br/>Distance");
      tr.addHeader("Effects");
      return tr.toString();
   }
   public static String getDataForWoundInColumn(Wound wound, int column) {
      switch (column) {
         case 0: return wound.getBaseLocationName();
         case 1: return wound._description;
         case 2: return String.valueOf(wound.getPain());
         case 3: return String.valueOf(wound.getWounds());
         case 4: return String.valueOf(wound.getBleedRate());
         case 5: return wound.describeArmPenalty();
         case 6: return wound.describeMovePenalty();
         case 7: return String.valueOf(wound.getKnockedBackDist());
         case 8: return wound.describeEffects();
      }
      return null;

   }
   public static String getCombinedDataForWoundInColumn(List<Wound> wounds, int column) {
      HashSet<String> uniqueDataSet = new HashSet<>();
      String[] combinedData = new String[wounds.size()];
      for (int i=0 ; i<wounds.size() ; i++) {
         combinedData[i] = getDataForWoundInColumn(wounds.get(i), column);
         uniqueDataSet.add(combinedData[i]);
      }

      // Are all values the same?
      if (uniqueDataSet.size() == 1) {
         return "<b>"+combinedData[0] + "</b>";
      }
      if (uniqueDataSet.size() == 2) {
         // If this is the case of one being different from all others, then
         // we can list is separately from the others.
         for (int indexOfUniqueItem=0 ; indexOfUniqueItem<combinedData.length ; indexOfUniqueItem++) {
            boolean matchFound = false;
            for (int indexOfTestItem=0 ; indexOfTestItem<combinedData.length ; indexOfTestItem++) {
               if (indexOfTestItem != indexOfUniqueItem) {
                  if (combinedData[indexOfTestItem].equals(combinedData[indexOfUniqueItem])) {
                     matchFound = true;
                     break;
                  }
               }
            }
            if (!matchFound) {
               int nonUniqueIndex = indexOfUniqueItem + 1;
               if (nonUniqueIndex >= combinedData.length) {
                  nonUniqueIndex = 0;
               }
               return "<b>"+combinedData[nonUniqueIndex] + "</b><br/>" +
                        ATTRIBUTES_HEAD[indexOfUniqueItem] +
                        combinedData[indexOfUniqueItem] +
                        ATTRIBUTES_TAIL[indexOfUniqueItem];
            }
         }
      }
      StringBuilder sb = new StringBuilder();
      for (int i=0 ; i<combinedData.length ; i++) {
         sb.append(ATTRIBUTES_HEAD[i]).append(combinedData[i]).append(ATTRIBUTES_TAIL[i]).append("<br/>");
      }
      return sb.toString();
   }

}
