package ostrowski.combat.common.html;

public class HtmlBuilder
{
   public static final int MAX_HTML_ROWS = 4;
   public static String getCSSHeader() {
      StringBuilder sb = new StringBuilder();
      sb.append("<head>\n");
      sb.append("<style>\n");
      sb.append("a:link {color:#000000;}    /* unvisited link */\n");
      sb.append("a:visited {color:#000000;} /* visited link */\n");
      sb.append("a:hover {color:#0000FF;}   /* mouse over link */\n");
      sb.append("a:active {color:#000000;}  /* selected link */\n");
      sb.append("a { text-decoration:none }\n");
      sb.append("hr {color:sienna;}\n");
      sb.append("p {margin-left:20px;}\n");
      sb.append("table, th, td {\n");
      sb.append(" border-spacing: 0px;\n");
      sb.append(" border-style: outset;\n");
      sb.append(" border-collapse: collapse;\n");
      sb.append(" border-color: #808080;\n");
      sb.append("}\n");
      sb.append("table {\n");
      sb.append(" border-width: 2px;\n");
      sb.append("}\n");
      sb.append("th, td {\n");
      sb.append(" text-align: center;\n");
      sb.append(" border-width: 1px;\n");
      sb.append(" padding: 0px 2px;\n");
      sb.append("}\n");
      sb.append("th.alignLeft, td.alignLeft {\n");
      sb.append(" text-align: left;\n");
      sb.append("}\n");
      sb.append("tr.header-row {\n");
      sb.append(" background-color:#B8CCE4;\n");
      sb.append("}\n");
      sb.append("table.Hidden {\n");
      sb.append(" border-color: #FFFFFF;\n");
      sb.append(" border-width: 0px;\n");
      sb.append("}\n");
      sb.append("th.points {height: 200px}\n");
      sb.append(".points {max-width: 30px;}\n");
      sb.append(".race_gender {}\n");
      sb.append(".attr_STR { max-width: 15px; background-color: cornflowerblue;}\n");
      sb.append(".attr_HT  { max-width: 15px; background-color: lightskyblue;}\n");
      sb.append(".attr_TOU { max-width: 15px; background-color: cornflowerblue;}\n");
      sb.append(".attr_IQ  { max-width: 15px; background-color: lightskyblue;}\n");
      sb.append(".attr_NIM { max-width: 15px; background-color: cornflowerblue;}\n");
      sb.append(".attr_DEX { max-width: 15px; background-color: lightskyblue;}\n");
      sb.append(".attr_SOC { max-width: 15px; background-color: cornflowerblue;}\n");
      sb.append(".actions  { max-width: 15px; background-color: aliceblue;}\n");
      sb.append(".encumbrance {max-width: 15px;}\n");
      sb.append(".movement {max-width: 15px;}\n");
      sb.append(".pain_recovery {}\n");
      sb.append(".profession1 {}\n");
      sb.append(".profession2 {}\n");
      sb.append(".profession3 {}\n");
      sb.append(".professions {}\n");
      sb.append(".equipment {}\n");
      sb.append(".def_pd {}\n");
      sb.append(".def_Retreat {}\n");
      sb.append(".def_Dodge {}\n");
      sb.append(".def_Block {}\n");
      sb.append(".def_Parry {}\n");
      sb.append(".def_Blunt {}\n");
      sb.append(".def_Cut {}\n");
      sb.append(".def_Impale {}\n");
      sb.append(".def_pd      { max-width: 20px; background-color: yellowgreen}\n");
      sb.append(".def_Retreat { max-width: 15px; background-color: lightgray}\n");
      sb.append(".def_Dodge   { max-width: 15px; background-color: darkgrey}\n");
      sb.append(".def_Block   { max-width: 15px; background-color: lightgray}\n");
      sb.append(".def_Parry   { max-width: 15px; background-color: darkgrey}\n");
      sb.append(".def_Blunt   { max-width: 15px; background-color: bisque}\n");
      sb.append(".def_Cut     { max-width: 15px; background-color: burlywood}\n");
      sb.append(".def_Impale  { max-width: 15px; background-color: bisque}\n");
      sb.append(".weapon {}\n");
      sb.append(".tightTable td, .tightTable th {\n");
      sb.append("    padding: 0px;\n");
      sb.append("}\n");
      sb.append(".vertical-text {\n");
      sb.append("    writing-mode: vertical-lr;\n");
      sb.append("    -ms-writing-mode: tb-rl;\n");
      sb.append("    transform: rotate(180deg);\n");
      sb.append("    font-size: smaller;\n");
      sb.append("}");
      sb.append("tr.row0");
      for (int i = 2; i < MAX_HTML_ROWS; i++) {
         sb.append(", tr.row").append(i++);
      }
      sb.append("{ background-color:#FFFFFF;}\n");
      sb.append("tr.row1");
      for (int i = 3; i < MAX_HTML_ROWS; i++) {
         sb.append(", tr.row").append(i++);
      }
      sb.append("{ background-color:#F0F0F0;}\n");
      sb.append("table.doubleRow tr.row0, table.doubleRow tr.row1");
      for (int i=4 ; i<MAX_HTML_ROWS; i+=2) {
         sb.append(", table.doubleRow tr.row").append(i++);
         sb.append(", table.doubleRow tr.row").append(i++);
      }
      sb.append("{ background-color:#FFFFFF;}\n");
      sb.append("table.doubleRow tr.row2, table.doubleRow tr.row3");
      for (int i=6; i<MAX_HTML_ROWS; i+=2) {
         sb.append(", table.doubleRow tr.row").append(i++);
         sb.append(", table.doubleRow tr.row").append(i++);
      }
      sb.append("{ background-color:#F0F0F0;}\n");
      sb.append("\n");
      sb.append("tr.header-row th {\n");
      sb.append("  border-width: 1px 1px 3px 1px;\n");
      sb.append("}\n");
      sb.append("table.Hidden table td {");
      sb.append("  border-width:1px;");
      sb.append("}");
      sb.append("table.Hidden table.Hidden td {");
      sb.append("  border-width:0px;");
      sb.append("}");

      sb.append("\n");
      sb.append("</style>\n");
      sb.append("</head>\n");
      return sb.toString();
   }

   public static String getHTMLHeader() {
      return getHTMLHeader("");
   }
   public static String getHTMLHeader(String tableName, int height, int topRowHeight) {
      return getHTMLHeader("MakeStaticHeader('" + tableName + "', " + height + ", " + topRowHeight + ", true)");
   }
   private static String getHTMLHeader(String onLoadString) {
      StringBuilder sb = new StringBuilder();
      sb.append("<!DOCTYPE html>");
      if (onLoadString.length() > 0 ) {
         sb.append("<script language=\"javascript\" type=\"text/javascript\">\n");
         sb.append("  function MakeStaticHeader(gridId, height, headerHeight, isFooter) {\n");
         sb.append("    var tbl = document.getElementById(gridId);\n");
         sb.append("    if (tbl) {\n");
         sb.append("      var DivHR = document.getElementById('DivHeaderRow');\n");
         sb.append("      var DivMC = document.getElementById('DivMainContent');\n");
         sb.append("  \n");
         //*** Set divheaderRow Properties ****
         sb.append("      //*** Set divheaderRow Properties ****\n");
         sb.append("      DivHR.style.padding = '0px 17px 0px 0px';\n");
         sb.append("      DivHR.style.height = headerHeight + 'px';\n");
         sb.append("      DivHR.style.position = 'relative';\n");
         sb.append("      DivHR.style.top = '0px';\n");
         sb.append("      DivHR.style.zIndex = '10';\n");
         sb.append("      DivHR.style.verticalAlign = 'top';\n");
         sb.append("  \n");
         //*** Set divMainContent Properties ****
         sb.append("      //*** Set divMainContent Properties ****\n");
         sb.append("      DivMC.style.padding = '0px 0px 0px 0px';\n");
         sb.append("      DivMC.style.height = height + 'px';\n");
         sb.append("      DivMC.style.position = 'relative';\n");
         sb.append("      DivMC.style.top = -headerHeight + 'px';\n");
         sb.append("      DivMC.style.zIndex = '1';\n");
         sb.append("  \n");
         sb.append("      if (isFooter) {\n");
         sb.append("        var tblfr = tbl.cloneNode(true);\n");
         sb.append("        tblfr.removeChild(tblfr.getElementsByTagName('tbody')[0]);\n");
         sb.append("        var tblBody = document.createElement('tbody');\n");
         sb.append("        tblfr.style.width = '100%';\n");
         sb.append("        tblfr.cellSpacing = \"0\";\n");
         //*****In the case of Footer Row *******
         sb.append("        //*****In the case of Footer Row *******\n");
         sb.append("        tblBody.appendChild(tbl.rows[tbl.rows.length - 1]);\n");
         sb.append("        tblfr.appendChild(tblBody);\n");
         sb.append("      }\n");
         //****Copy Header in divHeaderRow****
         sb.append("      //****Copy Header in divHeaderRow****\n");
         sb.append("      DivHR.appendChild(tbl.cloneNode(true));\n");
         sb.append("    }\n");
         sb.append("  }\n");
         sb.append("\n");
         sb.append("  function OnScrollDiv(Scrollablediv) {\n");
         sb.append("    document.getElementById('DivHeaderRow').scrollLeft = Scrollablediv.scrollLeft;\n");
         sb.append("  }\n");
         sb.append("window.onload = function() {\n");
         sb.append(onLoadString);
         sb.append("\n}\n");
         sb.append("</script>\n");
      }
      sb.append(getCSSHeader());
      return sb.toString();
   }

   public static String buildRow(int rowIndex) {
      return  "<tr class=\"row" + (rowIndex%MAX_HTML_ROWS) + "\">";
   }
}
