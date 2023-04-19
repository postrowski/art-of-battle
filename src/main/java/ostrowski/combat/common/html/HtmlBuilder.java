package ostrowski.combat.common.html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Stream;

public class HtmlBuilder
{
   public static final int MAX_HTML_ROWS = 4;
   public static String getCSSHeader() {
      StringBuilder sb = new StringBuilder();
      sb.append("<head>\n");
      sb.append("<style>\n");
      try (InputStream inputStream = HtmlBuilder.class.getResourceAsStream("/CannonFodder.css");
           InputStreamReader isr = new InputStreamReader(inputStream);
           BufferedReader br = new BufferedReader(isr);
           Stream<String> lines = br.lines()) {
         lines.forEach(sb::append);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
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
         sb.append("<head>\n");
         sb.append("<style>\n");
         try (InputStream inputStream = HtmlBuilder.class.getResourceAsStream("/CannonFodder.css");
              InputStreamReader isr = new InputStreamReader(inputStream);
              BufferedReader br = new BufferedReader(isr);
              Stream<String> lines = br.lines()) {
            lines.forEach(sb::append);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
         sb.append("</style>\n");
         sb.append("</head>\n");
         sb.append("New page!\n");
      }
      sb.append(getCSSHeader());
      return sb.toString();
   }

   public static String buildRow(int rowIndex) {
      return  "<tr class=\"row" + (rowIndex%MAX_HTML_ROWS) + "\">";
   }
}
