/*
 * Created on Jul 13, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.mage.MageSpells;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.ui.Helper;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SpellsBlock extends Helper implements IUIBlock, ModifyListener
{
   private static final int             MAGE_PAGES             = 4;
   private static final int             PRIEST_PAGES           = 5;
   private static final int             MAGE_SPELLS_PER_PAGE   = 9;
   private static final int             PRIEST_SPELLS_PER_PAGE = 16;
   private final        CharacterWidget display;

   // These values cache the content of the Combos, spinners, and text fields,
   // to make refresh faster:
   private final boolean[] itemEnabledData   = new boolean[MAGE_SPELLS_PER_PAGE];

   private final Combo[] spellCombo          = new Combo[MAGE_SPELLS_PER_PAGE * MAGE_PAGES];
   //private Spinner[]
   // = new Spinner[_spellCombo.length];
   private final Combo[] spellFamiliarity    = new Combo[spellCombo.length];
   private final Text[]  spellEffectiveSkill = new Text[spellCombo.length];
   private final Text[]  spellTime           = new Text[spellCombo.length];
   private final Text[]  spellRange          = new Text[spellCombo.length];
   private final Text[]  spellCost           = new Text[spellCombo.length];

   // These values cache the content of the Combos, spinners, and text fields,
   // to make refresh faster:
   private final String[]  spellComboData          = new String[spellCombo.length];
   //private byte[]           spellLevelData          = new byte[_spellCombo.length];
   private final String[]  spellFamiliarityData    = new String[spellCombo.length];
   private final String[]  spellEffectiveSkillData = new String[spellCombo.length];
   private final String[]  spellTimeData           = new String[spellCombo.length];
   private final String[]  spellRangeData          = new String[spellCombo.length];
   private final String[]  spellCostData           = new String[spellCombo.length];
   private final boolean[] spellEnabledData        = new boolean[spellCombo.length];

   private       TabFolder mainFolder;
   private final Text[][]  priestSpellName       = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private final Text[][]  priestSpellAffinity   = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private final Text[][]  priestSpellRange      = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private final Text[][]  priestSpellResistance = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private       TabItem[] priestPage;


   public SpellsBlock(CharacterWidget widget)
   {
      display = widget;
   }

   @Override
   public void buildBlock(Composite parent)
   {
      Group spellGroup = createGroup(parent, "Spells", 1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 1/*vSpacing*/);
      mainFolder = new TabFolder(spellGroup, SWT.NONE);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      mainFolder.setLayoutData(data);

      {
         // Create the Wizardry page
         TabFolder mageFolder;
         {
            TabItem item = new TabItem(mainFolder, SWT.NULL);
            item.setText( "Wizardry");
            Composite page = Helper.createComposite(mainFolder, 1, GridData.FILL_BOTH);
            item.setControl( page );
            page.setLayout(new GridLayout(1 /*columns*/,false/*sameWidth*/));

            mageFolder = new TabFolder(page, SWT.NONE);
            data = new GridData(GridData.FILL_HORIZONTAL);
            mageFolder.setLayoutData(data);
         }

         for (int pageIndex=0 ; pageIndex<MAGE_PAGES ; pageIndex++) {
            TabItem item = new TabItem(mageFolder, SWT.NULL);
            item.setText( "Page " + (pageIndex+1));
            Composite page = Helper.createComposite(mageFolder, 1, GridData.FILL_BOTH);
            item.setControl( page );
            page.setLayout(new GridLayout(6 /*columns*/,false/*sameWidth*/));

            createLabel(page, "Name", SWT.CENTER, 1, null);
            createLabel(page, "Familiarity", SWT.CENTER, 1, null);
            createLabel(page, "Adj. Skill", SWT.CENTER, 1, null);
            createLabel(page, "Time", SWT.CENTER, 1, null);
            createLabel(page, "Range base", SWT.CENTER, 1, null);
            createLabel(page, "Cost", SWT.CENTER, 1, null);
            List<String> spellNames = MageSpells.getSpellNames();
            spellNames.add(0, "---");
            List<MageSpell.Familiarity> familiarities = new ArrayList<>();
            familiarities.add(MageSpell.Familiarity.UNFAMILIAR);
            familiarities.add(MageSpell.Familiarity.KNOWN);
            familiarities.add(MageSpell.Familiarity.MEMORIZED);
            for (int i=0 ; i<MAGE_SPELLS_PER_PAGE ; i++) {
               int itemIndex = i + (pageIndex * MAGE_SPELLS_PER_PAGE);
               spellComboData[itemIndex] = spellNames.get(0);
               spellFamiliarityData[itemIndex] = familiarities.get(0).getName();
               spellEffectiveSkillData[itemIndex] = " ";
               spellTimeData[itemIndex] = " ";
               spellRangeData[itemIndex] = " ";
               spellCostData[itemIndex] = "(0)";

               spellCombo[itemIndex] = createCombo(page, SWT.READ_ONLY, 1, spellNames);
//               spellLevel[itemIndex] = createSpinner(page, 0/*min*/, Rules.getMaxSpellLevel()/*max*/, 0/*value*/, 1);
               spellFamiliarity[itemIndex] = createCombo(page, SWT.NONE, 1/*hSpan*/, familiarities.stream().map(o -> o.getName()).collect(Collectors.toList()));
               spellEffectiveSkill[itemIndex] = createText(page, spellEffectiveSkillData[itemIndex], false/*editable*/, 1);
               spellTime[itemIndex] = createText(page, spellTimeData[itemIndex], false/*editable*/, 1);
               spellRange[itemIndex] = createText(page, spellRangeData[itemIndex], false/*editable*/, 1);
               spellCost[itemIndex] = createText(page, spellCostData[itemIndex], false/*editable*/, 1);
               spellCombo[itemIndex].addModifyListener(this);
//               spellLevel[itemIndex].addModifyListener(this);
               spellFamiliarity[itemIndex].addModifyListener(this);
            }
            Control[] tabList = new Control[MAGE_SPELLS_PER_PAGE * 2];
            int index = 0;
            for (int i=0 ; i<MAGE_SPELLS_PER_PAGE ; i++) {
               int itemIndex = i + (pageIndex * MAGE_SPELLS_PER_PAGE);
               tabList[index++] = spellCombo[itemIndex];
//               tabList[index++] = spellLevel[itemIndex];
               tabList[index++] = spellFamiliarity[itemIndex];
            }
            page.setTabList(tabList);
         }
      }
      {
         // create the Priest magic page
         TabFolder priestFolder;
         {
            TabItem item = new TabItem(mainFolder, SWT.NULL);
            item.setText("Priest");
            Composite page = Helper.createComposite(mainFolder, 1, GridData.FILL_BOTH);
            item.setControl( page );
            page.setLayout(new GridLayout(1 /*columns*/,false/*sameWidth*/));

            priestFolder = new TabFolder(page, SWT.NONE);
            data = new GridData(GridData.FILL_HORIZONTAL | SWT.BORDER);
            priestFolder.setLayoutData(data);
         }

         priestPage = new TabItem[PRIEST_PAGES];
         for (int pageIndex=0 ; pageIndex<PRIEST_PAGES ; pageIndex++) {
            priestPage[pageIndex] = new TabItem(priestFolder, SWT.NULL);
            priestPage[pageIndex].setText("Page " + (pageIndex + 1));
            Composite page = Helper.createComposite(priestFolder, 1, GridData.FILL_BOTH);
            priestPage[pageIndex].setControl(page);
            page.setLayout(new GridLayout(4 /*columns*/,false/*sameWidth*/));

            createLabel(page, "Name", SWT.CENTER, 1, null);
            createLabel(page, "Affinity", SWT.CENTER, 1, null);
            createLabel(page, "Range base", SWT.CENTER, 1, null);
            createLabel(page, "Resistance", SWT.CENTER, 1, null);

            for (int itemIndex=0 ; itemIndex<PRIEST_SPELLS_PER_PAGE ; itemIndex++) {
               priestSpellName[pageIndex][itemIndex]       = createText(page, "", false/*editable*/, 1/*hSpan*/);
               priestSpellAffinity[pageIndex][itemIndex]   = createText(page, "", false/*editable*/, 1/*hSpan*/);
               priestSpellRange[pageIndex][itemIndex]      = createText(page, "", false/*editable*/, 1/*hSpan*/);
               priestSpellResistance[pageIndex][itemIndex] = createText(page, "", false/*editable*/, 1/*hSpan*/);
            }
         }
      }

      spellGroup.setTabList(new Control[] {mainFolder});

   }

   @Override
   @SuppressWarnings("unchecked")
   public void updateCharacterFromDisplay(Character character)
   {
      List<MageSpell> newSpells = new ArrayList<>();
      List<Class<MageSpell>> newSpellClasses = new ArrayList<>();
      for (int i = 0; i < spellCombo.length ; i++) {
         String spellName = spellCombo[i].getText();
         spellComboData[i] = spellName;
         if (!spellName.equals("---")) {
            MageSpell spell = MageSpells.getSpell(spellName);
            if (spell != null) {
               spell.setFamiliarity(spellFamiliarity[i].getText());
//               spell.setLevel(getValidSpellRange((byte) spellLevel[i].getSelection()));
               newSpells.add(spell);
               newSpellClasses.add((Class<MageSpell>) spell.getClass());
            }
         }
      }
      boolean checkSpells = true;
      while (checkSpells) {
         checkSpells = false;
         HashSet<Class<MageSpell>> spellClassesToAdd = new HashSet<> ();
         for (MageSpell spell : newSpells) {
            for (Class<MageSpell> prerequisiteSpellClass : spell.prerequisiteSpells) {
               if (!newSpellClasses.contains(prerequisiteSpellClass)) {
                  spellClassesToAdd.add(prerequisiteSpellClass);
               }
            }
         }
         Class<MageSpell>[] spellClassesToBeAdded = spellClassesToAdd.toArray(new Class[] {});
         for (Class<MageSpell> spellClassToAdd : spellClassesToBeAdded) {
            checkSpells = true;
            try {
               newSpells.add(spellClassToAdd.getDeclaredConstructor().newInstance());
               newSpellClasses.add(spellClassToAdd);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
               e.printStackTrace();
            }
         }
      }
      boolean setPrequesitiesLevel = true;
      if (setPrequesitiesLevel) {
         boolean checkSpellLevels = true;
         while (checkSpellLevels) {
            checkSpellLevels = false;
            for (int mainIndex = 0 ; mainIndex<newSpells.size() ; mainIndex++) {
               MageSpell spell = newSpells.get(mainIndex);
               MageSpell.Familiarity minFamiliarity = spell.getFamiliarity();
               for (Class<MageSpell> prerequisiteSpellClass : spell.prerequisiteSpells) {
                  for (int secondaryIndex = 0 ; secondaryIndex<newSpells.size() ; secondaryIndex++) {
                     if (secondaryIndex != mainIndex) {
                        MageSpell secondarySpell = newSpells.get(secondaryIndex);
                        if (secondarySpell.getClass() == prerequisiteSpellClass) {
                           if (secondarySpell.getFamiliarity().ordinal() < minFamiliarity.ordinal()) {
                              secondarySpell.setFamiliarity(minFamiliarity);
                              checkSpellLevels = (mainIndex > secondaryIndex);
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
      character.setSpellsList(newSpells);
   }
   public boolean isRequiredByOtherSpells(List<MageSpell> knownSpells, MageSpell source) {
      for (MageSpell spell : knownSpells) {
         for (Class<MageSpell> prerequisiteSpellClass : spell.prerequisiteSpells) {
            if (source.getClass() == prerequisiteSpellClass) {
               return true;
            }
         }
      }
      return false;
   }
   @Override
   public void updateDisplayFromCharacter(Character character)
   {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      refreshDisplay(character);
   }
   @Override
   public void refreshDisplay(Character character)
   {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      boolean enabled = (character != null);

      boolean showMagePage = (character != null) && character.hasAdvantage(Advantage.MAGICAL_APTITUDE);
      boolean showPriestPage = false;

      // now build the desired list of spell data:
      List<MageSpell> mageSpells = (character != null) ? character.getMageSpellsList() : new ArrayList<>();
      String rangeName;
      String[] spellComboData          = new String[spellCombo.length];
      String[] spellFamiliarityData    = new String[spellCombo.length];
      String[] spellCostData           = new String[spellCombo.length];
      String[] spellEffectiveSkillData = new String[spellCombo.length];
      String[] spellTimeData           = new String[spellCombo.length];
      String[] spellRangeData          = new String[spellCombo.length];
      boolean[] spellEnabledData       = new boolean[spellCombo.length];

      int j=0;
      for (MageSpell spell : mageSpells) {
         spellComboData[j] = spell.getName();
         spellFamiliarityData[j] = spell.getFamiliarity().getName();
         spellCostData[j] = "(" + spell.getFamiliarity().getCost() + ")";
         byte atr = character.getAttributeLevel(spell.getCastingAttribute());
         byte adjustedSkillLevel = Rules.getAdjustedSkillLevel(character.getSpellSkill(spell.getName()), atr);
         spellEffectiveSkillData[j] = String.valueOf(adjustedSkillLevel);
         spellTimeData[j] = String.valueOf(spell.getIncantationTime());

         if (spell instanceof IRangedSpell) {
            spellRangeData[j] = String.valueOf(spell.getAdjustedRangeBase());
         }
         else {
            if ((spell.getTargetType() == Enums.TargetType.TARGET_SELF) ||
                (spell.getTargetType() == Enums.TargetType.TARGET_NONE)) {
               spellRangeData[j] = "---";
            }
            else {
               spellRangeData[j] = "-1 per hex";
            }
         }
         spellEnabledData[j] = !isRequiredByOtherSpells(mageSpells, spell);
         j++;
         if (j >= spellCombo.length) {
            break;
         }
      }

      enabled = (character != null);
      // clear out any remaining spell combo boxes
      for (; j < spellCombo.length ; j++) {
         spellComboData[j] = "---";
//         spellLevelData[j] = 0;
         spellFamiliarityData[j] = MageSpell.Familiarity.UNFAMILIAR.getName();
         spellEffectiveSkillData[j] = " ";
         spellTimeData[j]    = " ";
         spellRangeData[j]   = " ";
         spellCostData[j]    = "(0)";
         spellEnabledData[j] = enabled;
         enabled = false;
      }
      // Now update anything that needs to change in the UI

      for (int k = 0; k < spellCombo.length ; k++) {
         if (!this.spellComboData[k].equals(spellComboData[k])) {
            spellCombo[k].setText(spellComboData[k]);
         }
//       if (!spellLevelData[k].equals(spellLevelData[k])) spellLevel[k].setSelection(spellLevelData[k]);
         if (!this.spellFamiliarityData[k].equals(spellFamiliarityData[k])) {
            spellFamiliarity[k].setText(spellFamiliarityData[k]);
         }
         if (!this.spellCostData[k].equals(spellCostData[k])) {
            spellCost[k].setText(spellCostData[k]);
         }
         if (!this.spellEffectiveSkillData[k].equals(spellEffectiveSkillData[k])) {
            spellEffectiveSkill[k].setText(spellEffectiveSkillData[k]);
         }
         if (!this.spellTimeData[k].equals(spellTimeData[k])) {
            spellTime[k].setText(spellTimeData[k]);
         }
         if (!this.spellRangeData[k].equals(spellRangeData[k])) {
            spellRange[k].setText(spellRangeData[k]);
         }
         //if (this.spellEnabledData[k] != spellEnabledData[k]) {
            spellCombo[k].setEnabled(spellEnabledData[k]);
//            spellLevel[k].setEnabled(spellEnabledData[k]);
            spellFamiliarity[k].setEnabled(spellEnabledData[k]);
         //}
         // keep track of the changes we've made:
         this.spellComboData[k]          = spellComboData[k];
//       spellLevelData[k]               = spellLevelData[k];
         this.spellFamiliarityData[k]    = spellFamiliarityData[k];
         this.spellCostData[k]           = spellCostData[k];
         this.spellEffectiveSkillData[k] = spellEffectiveSkillData[k];
         this.spellTimeData[k]           = spellTimeData[k];
         this.spellRangeData[k]          = spellRangeData[k];
         this.spellEnabledData[k]        = spellEnabledData[k];
      }

      // TODO: optimize the priest spells the same way we cache the mage spell data
      List<String> priestDeities = (character != null) ? character.getPriestDeities() : new ArrayList<>();
      int pageIndex = 0;
      for (String deity : priestDeities) {
         List<String> deityGroups = PriestSpell.getSpellGroups(deity);
         Advantage advantage = character.getAdvantage(Advantage.DIVINE_AFFINITY_+ deity);
         int affinityLevel = advantage.getLevel() + 1;
         for (String group : deityGroups) {

            showPriestPage = true;

            if (pageIndex >= priestPage.length) {
               break;
            }
            priestPage[pageIndex].setText(group);
            int spellIndex = 0;
            for (PriestSpell spell : PriestSpell.getSpellsInGroup(group)) {
               spell.setDeity(deity);
               if (priestSpellName[pageIndex].length <= spellIndex) {
                  continue;
               }
               priestSpellName[pageIndex][spellIndex].setText(spell.getName());
               priestSpellAffinity[pageIndex][spellIndex].setText(String.valueOf(spell.getAffinity()));
               if (spell instanceof IRangedSpell) {
                  rangeName = String.valueOf(spell.getAdjustedRangeBase());
               }
               else {
                  if ((spell.getTargetType() == Enums.TargetType.TARGET_SELF) ||
                      (spell.getTargetType() == Enums.TargetType.TARGET_NONE)) {
                     rangeName = "---";
                  }
                  else {
                     rangeName = "-1 per hex";
                  }
               }
               String resistance = "";
               if (spell instanceof ResistedPriestSpell) {
                  ResistedPriestSpell resistedSpell = (ResistedPriestSpell) spell;
                  resistance = resistedSpell.describeResistance(character);
               }
               priestSpellRange[pageIndex][spellIndex].setText(rangeName);
               priestSpellResistance[pageIndex][spellIndex].setText(resistance);
               enabled = (affinityLevel >= spell.getAffinity());
               priestSpellName[pageIndex][spellIndex].setEnabled(enabled);
               priestSpellAffinity[pageIndex][spellIndex].setEnabled(enabled);
               priestSpellRange[pageIndex][spellIndex].setEnabled(enabled);
               priestSpellResistance[pageIndex][spellIndex].setEnabled(enabled);
               spellIndex++;
            }
            // clear out any remaining priest spells
            for ( ; spellIndex<PRIEST_SPELLS_PER_PAGE ; spellIndex++) {
               priestSpellName[pageIndex][spellIndex].setText("---");
               //         spellLevel[pageIndex][spellIndex].setSelection(0);
               priestSpellAffinity[pageIndex][spellIndex].setText("---");
               priestSpellRange[pageIndex][spellIndex].setText(" ");
               priestSpellResistance[pageIndex][spellIndex].setText(" ");

               priestSpellName[pageIndex][spellIndex].setEnabled(false);
               priestSpellAffinity[pageIndex][spellIndex].setEnabled(false);
               priestSpellRange[pageIndex][spellIndex].setEnabled(false);
               priestSpellResistance[pageIndex][spellIndex].setEnabled(false);
            }
            pageIndex++;
         }
      }
      for ( ; pageIndex<PRIEST_PAGES ; pageIndex++) {
         priestPage[pageIndex].setText("---");
         // clear out any remaining priest spells
         for (int spellIndex = 0 ; spellIndex<PRIEST_SPELLS_PER_PAGE ; spellIndex++) {
            priestSpellName[pageIndex][spellIndex].setText("---");
            //         spellLevel[pageIndex][spellIndex].setSelection(0);
            priestSpellAffinity[pageIndex][spellIndex].setText("---");
            priestSpellRange[pageIndex][spellIndex].setText(" ");
            priestSpellResistance[pageIndex][spellIndex].setText(" ");

            priestSpellName[pageIndex][spellIndex].setEnabled(false);
            priestSpellAffinity[pageIndex][spellIndex].setEnabled(false);
            priestSpellRange[pageIndex][spellIndex].setEnabled(false);
            priestSpellResistance[pageIndex][spellIndex].setEnabled(false);
         }
      }
      // If the character has only one type of spells, go to that page:
      if (showPriestPage != showMagePage) {
         mainFolder.setSelection(showPriestPage ? 1 : 0);
      }
      // make sure it all looks good.
//      spellFamiliarity[0].getParent().pack();
   }
   @Override
   public void modifyText(ModifyEvent e)
   {
      display.modifyText(e, this);
   }

   @SuppressWarnings("unused")
   private static byte getValidSpellRange(byte spellValue)
   {
      if ((spellValue >= 0) && (spellValue <= Rules.getMaxSpellLevel())) {
         return spellValue;
      }
      return 0;
   }
}
