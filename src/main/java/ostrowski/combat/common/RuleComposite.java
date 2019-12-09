package ostrowski.combat.common;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.wounds.WoundChart;
import ostrowski.combat.server.Configuration;
import ostrowski.ui.Helper;

public class RuleComposite extends Composite
{
   private static final int BROWSER_WEAPONS         = 0;
   private static final int BROWSER_ARMOR_SHIELDS   = 1;
   private static final int BROWSER_WOUNDS          = 2;
   private static final int BROWSER_RACES           = 3;
   private static final int BROWSER_SKILLS          = 4;
   private static final int BROWSER_ATTRIBUTES      = 5;
   private static final int BROWSER_ADVANTAGES      = 6;
   private static final int BROWSER_SPELLS_MAGE     = 7;
   private static final int BROWSER_SPELLS_PRIEST   = 8;
   private static final int BROWSER_MISC            = 9;
   private static final int BROWSER_TAB_COUNT       = 10;

   private final Browser[]                        _rulesBrowser       = new Browser[BROWSER_TAB_COUNT];
   private final HashMap<DamageType, Browser>     _rulesWoundsBrowser = new HashMap<>();

   public RuleComposite(Composite parent, int hSpan, int gridDataStyle, Configuration configuration,
                        int windowWidth, Color backgroundColor) {
      super(parent, 0);
      setLayout(new GridLayout(1, false));
      GridData data = new GridData(gridDataStyle);
      data.horizontalSpan = hSpan;
      data.grabExcessVerticalSpace = true;
      setLayoutData(data);
      if (configuration != null) {
         configuration.buildDisplay(this, true/*isServer*/);
      }
      TabFolder tabFolderRules = new TabFolder(this, SWT.NONE);
      GridData gdata = new GridData(GridData.FILL_BOTH);
      gdata.horizontalSpan = 1;
      gdata.grabExcessVerticalSpace = true;
      gdata.grabExcessHorizontalSpace = true;
      tabFolderRules.setLayoutData(gdata);
      for (int tab=0 ; tab<BROWSER_TAB_COUNT ; tab++) {

         // create a TabItem
         TabItem item = new TabItem( tabFolderRules, SWT.NULL);
         switch (tab) {
            case BROWSER_ARMOR_SHIELDS: item.setText("Armor && Shields");break;
            case BROWSER_RACES:         item.setText("Race Data");       break;
            case BROWSER_SKILLS:        item.setText("Skills");          break;
            case BROWSER_ATTRIBUTES:    item.setText("Attributes");      break;
            case BROWSER_ADVANTAGES:    item.setText("Advantages");      break;
            case BROWSER_SPELLS_MAGE:   item.setText("Mage Spells");     break;
            case BROWSER_SPELLS_PRIEST: item.setText("Priest Spells");   break;
            case BROWSER_WEAPONS:       item.setText("Weapons");         break;
            case BROWSER_WOUNDS:        item.setText("Wounds");          break;
            case BROWSER_MISC:          item.setText("Misc.");           break;
         }
         // create a control
         Composite subComposite = Helper.createComposite(tabFolderRules, 1, GridData.FILL_BOTH);
         // add the control to the TabItem
         item.setControl( subComposite );
         if (tab == BROWSER_WOUNDS) {
            TabFolder woundsSubFolder = new TabFolder(subComposite, SWT.NONE);
            for (DamageType damType : DamageType.values()) {
               if (damType == DamageType.NONE) {
                  continue;
               }
               // create a TabItem
               item = new TabItem( woundsSubFolder, SWT.NULL);
               if (damType == DamageType.GENERAL) {
                  item.setText("Combined damage");
               }
               else {
                  item.setText(damType.fullname);
               }
               // create a control
               subComposite = Helper.createComposite(woundsSubFolder, 1, GridData.FILL_BOTH);
               // add the control to the TabItem
               item.setControl( subComposite );
               _rulesWoundsBrowser.put(damType, new Browser(subComposite, SWT.NONE | SWT.BORDER));
               data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
               data.minimumHeight = 550;
               data.minimumWidth  = windowWidth-30;
               data.horizontalSpan = 3;
               _rulesWoundsBrowser.get(damType).setLayoutData(data);
               _rulesWoundsBrowser.get(damType).setBackground(backgroundColor);
            }
         }
         else
         {
            _rulesBrowser[tab] = new Browser(subComposite, SWT.NONE | SWT.BORDER);
            data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
            data.minimumHeight = 600;
            data.minimumWidth  = windowWidth;
            data.horizontalSpan = 3;
            _rulesBrowser[tab].setLayoutData(data);
            _rulesBrowser[tab].setBackground(backgroundColor);
         }
      }
      updateRulesSection();
   }

   /**
   *
   */
  public void updateRulesSection()
  {
     if (_rulesBrowser == null) {
        return;
     }
     StringBuilder sb = new StringBuilder();
     sb.append(HtmlBuilder.getHTMLHeader());
     sb.append("<body>");
     sb.append("<table class='Hidden'><tr><td>");
     sb.append(Armor.generateHtmlTable());
     sb.append("</td><td valign='Top'>");
     sb.append(Shield.generateHtmlTable());
     sb.append("</td></tr></table>");
     sb.append("</body>");
     _rulesBrowser[BROWSER_ARMOR_SHIELDS].setText(sb.toString());
     _rulesBrowser[BROWSER_RACES].setText(Race.generateHtmlTable());
     _rulesBrowser[BROWSER_SKILLS].setText(Rules.generateSkillsHtmlTable());
     _rulesBrowser[BROWSER_ATTRIBUTES].setText(Rules.generateAttributesHtmlTable());
     _rulesBrowser[BROWSER_ADVANTAGES].setText(Advantage.generateHtmlTable());
     _rulesBrowser[BROWSER_SPELLS_MAGE].setText(MageSpell.generateHtmlTableMageSpells());
     _rulesBrowser[BROWSER_SPELLS_PRIEST].setText(PriestSpell.generateHtmlTablePriestSpells());
     _rulesBrowser[BROWSER_WEAPONS].setText(Weapon.generateHtmlTable());

     for (DamageType damType : DamageType.values()) {
        if (damType != DamageType.NONE) {
           _rulesWoundsBrowser.get(damType).setText(WoundChart.generateHtmlTable(damType));
        }
     }
     _rulesWoundsBrowser.get(DamageType.GENERAL).setText(WoundChart.generateCombinedHtmlTable());
     _rulesBrowser[BROWSER_MISC].setText(Rules.generateMiscHtmlTable());
  }
}
