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
import ostrowski.combat.common.things.Weapons;
import ostrowski.combat.common.wounds.WoundCharts;
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

   private final Browser[]                    rulesBrowser       = new Browser[BROWSER_TAB_COUNT];
   private final HashMap<DamageType, Browser> rulesWoundsBrowser = new HashMap<>();

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
               rulesWoundsBrowser.put(damType, new Browser(subComposite, SWT.NONE | SWT.BORDER));
               data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
               data.minimumHeight = 550;
               data.minimumWidth  = windowWidth-30;
               data.horizontalSpan = 3;
               rulesWoundsBrowser.get(damType).setLayoutData(data);
               rulesWoundsBrowser.get(damType).setBackground(backgroundColor);
            }
         }
         else
         {
            rulesBrowser[tab] = new Browser(subComposite, SWT.NONE | SWT.BORDER);
            data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
            data.minimumHeight = 600;
            data.minimumWidth  = windowWidth;
            data.horizontalSpan = 3;
            rulesBrowser[tab].setLayoutData(data);
            rulesBrowser[tab].setBackground(backgroundColor);
         }
      }
      updateRulesSection();
   }

   /**
   *
   */
  public void updateRulesSection()
  {
     if (rulesBrowser == null) {
        return;
     }
     String sb = HtmlBuilder.getHTMLHeader() +
                 "<body>" +
                 "<table class='Hidden'><tr><td>" +
                 Armor.generateHtmlTable() +
                 "</td><td valign='Top'>" +
                 Shield.generateHtmlTable() +
                 "</td></tr></table>" +
                 "</body>";
     rulesBrowser[BROWSER_ARMOR_SHIELDS].setText(sb);
     rulesBrowser[BROWSER_RACES].setText(Race.generateHtmlTable());
     rulesBrowser[BROWSER_SKILLS].setText(Rules.generateSkillsHtmlTable());
     rulesBrowser[BROWSER_ATTRIBUTES].setText(Rules.generateAttributesHtmlTable());
     rulesBrowser[BROWSER_ADVANTAGES].setText(Advantage.generateHtmlTable());
     rulesBrowser[BROWSER_SPELLS_MAGE].setText(MageSpell.generateHtmlTableMageSpells());
     rulesBrowser[BROWSER_SPELLS_PRIEST].setText(PriestSpell.generateHtmlTablePriestSpells());
     rulesBrowser[BROWSER_WEAPONS].setText(Weapons.generateHtmlTable());

     for (DamageType damType : DamageType.values()) {
        if (damType != DamageType.NONE) {
           rulesWoundsBrowser.get(damType).setText(WoundCharts.generateHtmlTable(damType));
        }
     }
     rulesWoundsBrowser.get(DamageType.GENERAL).setText(WoundCharts.generateCombinedHtmlTable());
     rulesBrowser[BROWSER_MISC].setText(Rules.generateMiscHtmlTable());
  }
}
