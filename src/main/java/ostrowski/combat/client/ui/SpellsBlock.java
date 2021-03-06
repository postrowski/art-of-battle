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
import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.mage.MageCollege;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.mage.MageSpells;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.ui.Helper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SpellsBlock extends Helper implements IUIBlock, ModifyListener
{
   private static final int MAGE_PAGES             = 4;
   private static final int PRIEST_PAGES           = 5;
   private static final int MAGE_SPELLS_PER_PAGE   = 9;
   private static final int PRIEST_SPELLS_PER_PAGE = 16;
   private final CharacterWidget  _display;

   private final Combo[]          _collegeCombo        = new Combo[MAGE_SPELLS_PER_PAGE];
   private final Spinner[]        _collegeLevel        = new Spinner[_collegeCombo.length];
   private final Text[]           _collegeAdjLvl       = new Text[_collegeCombo.length];
   private final Text[]           _collegeCost         = new Text[_collegeCombo.length];

   // These values cache the content of the Combos, spinners, and text fields,
   // to make refresh faster:
   private final String[]  _collegeComboData   = new String[_collegeCombo.length];
   private final byte[]    _collegeLevelData   = new byte[_collegeCombo.length];
   private final byte[]    _collegeAdjLvlData  = new byte[_collegeCombo.length];
   private final String[]  _collegeCostData    = new String[_collegeCombo.length];
   private final boolean[] _itemEnabledData    = new boolean[_collegeCombo.length];

   private final Combo[]          _spellCombo          = new Combo[MAGE_SPELLS_PER_PAGE * MAGE_PAGES];
   //private Spinner[]        _spellLevel          = new Spinner[_spellCombo.length];
   private final Combo[]          _spellFamiliarity    = new Combo[_spellCombo.length];
   private final Text[]           _spellEffectiveSkill = new Text[_spellCombo.length];
   private final Text[]           _spellTime           = new Text[_spellCombo.length];
   private final Text[]           _spellRange          = new Text[_spellCombo.length];
   private final Text[]           _spellCost           = new Text[_spellCombo.length];

   // These values cache the content of the Combos, spinners, and text fields,
   // to make refresh faster:
   private final String[]         _spellComboData          = new String[_spellCombo.length];
   //private byte[]           _spellLevelData          = new byte[_spellCombo.length];
   private final String[]         _spellFamiliarityData    = new String[_spellCombo.length];
   private final String[]         _spellEffectiveSkillData = new String[_spellCombo.length];
   private final String[]         _spellTimeData           = new String[_spellCombo.length];
   private final String[]         _spellRangeData          = new String[_spellCombo.length];
   private final String[]         _spellCostData           = new String[_spellCombo.length];
   private final boolean[]        _spellEnabledData        = new boolean[_spellCombo.length];

   private TabFolder              _mainFolder;
   private final Text[][]         _priestSpellName       = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private final Text[][]         _priestSpellAffinity   = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private final Text[][]         _priestSpellRange      = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private final Text[][]         _priestSpellResistance = new Text[PRIEST_PAGES][PRIEST_SPELLS_PER_PAGE];
   private TabItem[]              _priestPage;


   public SpellsBlock(CharacterWidget widget)
   {
      _display = widget;
   }

   @Override
   public void buildBlock(Composite parent)
   {
      Group spellGroup = createGroup(parent, "Spells", 1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 1/*vSpacing*/);
      _mainFolder = new TabFolder(spellGroup, SWT.NONE);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      _mainFolder.setLayoutData(data);

      {
         // Create the Wizardry page
         TabFolder mageFolder;
         {
            TabItem item = new TabItem(_mainFolder, SWT.NULL);
            item.setText( "Wizardry");
            Composite page = Helper.createComposite(_mainFolder, 1, GridData.FILL_BOTH);
            item.setControl( page );
            page.setLayout(new GridLayout(1 /*columns*/,false/*sameWidth*/));

            mageFolder = new TabFolder(page, SWT.NONE);
            data = new GridData(GridData.FILL_HORIZONTAL);
            mageFolder.setLayoutData(data);
         }

         {
            TabItem item = new TabItem(mageFolder, SWT.NULL);
            item.setText( "Colleges");
            Composite page = Helper.createComposite(mageFolder, 1, GridData.FILL_BOTH);
            item.setControl( page );
            page.setLayout(new GridLayout(4 /*columns*/,false/*sameWidth*/));

            createLabel(page, "Name", SWT.CENTER, 1, null);
            createLabel(page, "Base Level", SWT.CENTER, 1, null);
            createLabel(page, "Adj. Level", SWT.CENTER, 1, null);
            createLabel(page, "Cost", SWT.CENTER, 1, null);
            List<String> collegeNames = MageCollege.getCollegeNames();
            collegeNames.add(0, "---");
            for (int itemIndex=0 ; itemIndex<MAGE_SPELLS_PER_PAGE ; itemIndex++) {
               _collegeComboData [itemIndex] = collegeNames.get(0);
               _collegeLevelData [itemIndex] = 0;
               _collegeAdjLvlData[itemIndex] = 0;
               _collegeCostData  [itemIndex] = "(0)";
               _itemEnabledData  [itemIndex] = true;

               _collegeCombo [itemIndex] = createCombo(page, SWT.READ_ONLY, 1, collegeNames);
               _collegeLevel [itemIndex] = createSpinner(page, 0/*min*/, Rules.getMaxCollegeLevel()/*max*/, _collegeLevelData[itemIndex]/*value*/, 1);
               _collegeAdjLvl[itemIndex] = createText(page, "["+_collegeAdjLvlData[itemIndex]+"]", false/*editable*/, 1);
               _collegeCost  [itemIndex] = createText(page, _collegeCostData[itemIndex], false/*editable*/, 1);
               _collegeCombo [itemIndex].addModifyListener(this);
               _collegeLevel [itemIndex].addModifyListener(this);
            }
            Control[] tabList = new Control[MAGE_SPELLS_PER_PAGE * 2];
            int index = 0;
            for (int itemIndex=0 ; itemIndex<MAGE_SPELLS_PER_PAGE ; itemIndex++) {
               tabList[index++] = _collegeCombo[itemIndex];
               tabList[index++] = _collegeLevel[itemIndex];
            }
            page.setTabList(tabList);
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
            List<String> familiarities = new ArrayList<>();
            familiarities.add(MageSpell.FAM_UNFAMILIAR);
            familiarities.add(MageSpell.FAM_KNOWN);
            familiarities.add(MageSpell.FAM_MEMORIZED);
            for (int i=0 ; i<MAGE_SPELLS_PER_PAGE ; i++) {
               int itemIndex = i + (pageIndex * MAGE_SPELLS_PER_PAGE);
               _spellComboData[itemIndex] = spellNames.get(0);
               _spellFamiliarityData[itemIndex] = familiarities.get(0);
               _spellEffectiveSkillData[itemIndex] = " ";
               _spellTimeData [itemIndex] = " ";
               _spellRangeData[itemIndex] = " ";
               _spellCostData [itemIndex] = "(0)";

               _spellCombo[itemIndex] = createCombo(page, SWT.READ_ONLY, 1, spellNames);
//               _spellLevel[itemIndex] = createSpinner(page, 0/*min*/, Rules.getMaxSpellLevel()/*max*/, 0/*value*/, 1);
               _spellFamiliarity   [itemIndex] = createCombo(page, SWT.NONE, 1/*hSpan*/, familiarities);
               _spellEffectiveSkill[itemIndex] = createText(page, _spellEffectiveSkillData[itemIndex], false/*editable*/, 1);
               _spellTime [itemIndex] = createText(page, _spellTimeData [itemIndex], false/*editable*/, 1);
               _spellRange[itemIndex] = createText(page, _spellRangeData[itemIndex], false/*editable*/, 1);
               _spellCost [itemIndex] = createText(page, _spellCostData [itemIndex], false/*editable*/, 1);
               _spellCombo[itemIndex].addModifyListener(this);
//               _spellLevel[itemIndex].addModifyListener(this);
               _spellFamiliarity[itemIndex].addModifyListener(this);
            }
            Control[] tabList = new Control[MAGE_SPELLS_PER_PAGE * 2];
            int index = 0;
            for (int i=0 ; i<MAGE_SPELLS_PER_PAGE ; i++) {
               int itemIndex = i + (pageIndex * MAGE_SPELLS_PER_PAGE);
               tabList[index++] = _spellCombo[itemIndex];
//               tabList[index++] = _spellLevel[itemIndex];
               tabList[index++] = _spellFamiliarity[itemIndex];
            }
            page.setTabList(tabList);
         }
      }
      {
         // create the Priest magic page
         TabFolder priestFolder;
         {
            TabItem item = new TabItem(_mainFolder, SWT.NULL);
            item.setText("Priest");
            Composite page = Helper.createComposite(_mainFolder, 1, GridData.FILL_BOTH);
            item.setControl( page );
            page.setLayout(new GridLayout(1 /*columns*/,false/*sameWidth*/));

            priestFolder = new TabFolder(page, SWT.NONE);
            data = new GridData(GridData.FILL_HORIZONTAL | SWT.BORDER);
            priestFolder.setLayoutData(data);
         }

         _priestPage = new TabItem[PRIEST_PAGES];
         for (int pageIndex=0 ; pageIndex<PRIEST_PAGES ; pageIndex++) {
            _priestPage[pageIndex] = new TabItem(priestFolder, SWT.NULL);
            _priestPage[pageIndex].setText( "Page " + (pageIndex+1));
            Composite page = Helper.createComposite(priestFolder, 1, GridData.FILL_BOTH);
            _priestPage[pageIndex].setControl( page );
            page.setLayout(new GridLayout(4 /*columns*/,false/*sameWidth*/));

            createLabel(page, "Name", SWT.CENTER, 1, null);
            createLabel(page, "Affinity", SWT.CENTER, 1, null);
            createLabel(page, "Range base", SWT.CENTER, 1, null);
            createLabel(page, "Resistance", SWT.CENTER, 1, null);

            for (int itemIndex=0 ; itemIndex<PRIEST_SPELLS_PER_PAGE ; itemIndex++) {
               _priestSpellName[pageIndex][itemIndex]       = createText(page, "", false/*editable*/, 1/*hSpan*/);
               _priestSpellAffinity[pageIndex][itemIndex]   = createText(page, "", false/*editable*/, 1/*hSpan*/);
               _priestSpellRange[pageIndex][itemIndex]      = createText(page, "", false/*editable*/, 1/*hSpan*/);
               _priestSpellResistance[pageIndex][itemIndex] = createText(page, "", false/*editable*/, 1/*hSpan*/);
            }
         }
      }

      spellGroup.setTabList(new Control[] {_mainFolder});

   }

   @Override
   @SuppressWarnings("unchecked")
   public void updateCharacterFromDisplay(Character character)
   {
      List<MageCollege> newColleges = new ArrayList<>();
      for (int i=0 ; i<_collegeCombo.length ; i++) {
         String collegeName = _collegeCombo[i].getText();
         _collegeComboData[i] = collegeName;
         if (!collegeName.equals("---")) {
            boolean collegeFound = false;
            for (MageCollege existingCollege : newColleges) {
               if (existingCollege.getName().equals(collegeName)) {
                  collegeFound = true;
                  break;
               }
            }
            if (!collegeFound) {
               MageCollege college = MageCollege.getCollege(collegeName);
               if (college != null) {
                  college.setLevel(getValidCollegeRange((byte) _collegeLevel[i].getSelection()));
                  newColleges.add(college);
               }
            }
         }
      }
      List<MageSpell> newSpells = new ArrayList<>();
      List<Class<MageSpell>> newSpellClasses = new ArrayList<>();
      for (int i=0 ; i<_spellCombo.length ; i++) {
         String spellName = _spellCombo[i].getText();
         _spellComboData[i] = spellName;
         if (!spellName.equals("---")) {
            MageSpell spell = MageSpells.getSpell(spellName);
            if (spell != null) {
               spell.setFamiliarity(_spellFamiliarity[i].getText());
//               spell.setLevel(getValidSpellRange((byte) _spellLevel[i].getSelection()));
               newSpells.add(spell);
               newSpellClasses.add((Class<MageSpell>) spell.getClass());
               if (spell._prerequisiteColleges != null) {
                  for (MageCollege college : spell._prerequisiteColleges) {
                     boolean collegeFound = false;
                     for (MageCollege existingCollege : newColleges) {
                        if (existingCollege.getName().equals(college.getName())) {
                           collegeFound = true;
                           break;
                        }
                     }
                     if (!collegeFound) {
                        newColleges.add(college);
                     }
                  }
               }
            }
         }
      }
      boolean checkSpells = true;
      while (checkSpells) {
         checkSpells = false;
         HashSet<Class<MageSpell>> spellClassesToAdd = new HashSet<> ();
         for (MageSpell spell : newSpells) {
            for (Class<MageSpell> prerequisiteSpellClass : spell._prerequisiteSpells) {
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
      if (setPrequesitiesLevel ) {
         boolean checkSpellLevels = true;
         while (checkSpellLevels) {
            checkSpellLevels = false;
            for (int mainIndex = 0 ; mainIndex<newSpells.size() ; mainIndex++) {
               MageSpell spell = newSpells.get(mainIndex);
               byte minLevel = spell.getLevel();
               for (Class<MageSpell> prerequisiteSpellClass : spell._prerequisiteSpells) {
                  for (int secondaryIndex = 0 ; secondaryIndex<newSpells.size() ; secondaryIndex++) {
                     if (secondaryIndex != mainIndex) {
                        MageSpell secondarySpell = newSpells.get(secondaryIndex);
                        if (secondarySpell.getClass() == prerequisiteSpellClass) {
                           if (secondarySpell.getLevel() < minLevel) {
                              secondarySpell.setLevel(minLevel);
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
      character.setCollegesList(newColleges);
   }
   public boolean isRequiredByOtherSpells(List<MageSpell> knownSpells, MageSpell source) {
      for (MageSpell spell : knownSpells) {
         for (Class<MageSpell> prerequisiteSpellClass : spell._prerequisiteSpells) {
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
      List<MageCollege> colleges = (character != null) ? character.getCollegesList() : new ArrayList<>();

      boolean showMagePage = (character != null) && character.hasAdvantage(Advantage.MAGICAL_APTITUDE);
      boolean showPriestPage = false;

      // build the desired college data:
      String[]  collegeComboData = new String[_collegeCombo.length];
      byte[]    collegeLevelData = new byte[_collegeCombo.length];
      byte[]    collegeAdjLvlData= new byte[_collegeCombo.length];
      String[]  collegeCostData  = new String[_collegeCombo.length];
      boolean[] itemEnabledData  = new boolean[_collegeCombo.length];
      int i=0;
      for (MageCollege college : colleges) {
         collegeComboData[i]   = college.getName();
         itemEnabledData[i]    = true;
         collegeLevelData[i]   = college.getLevel();
         collegeAdjLvlData[i]  = Rules.getAdjustedCollegeLevel(college, character);
         collegeCostData[i]    = "(" + Rules.getCollegeCost(college.getLevel()) + ")";

         i++;
         if (i >= _collegeCombo.length) {
            break;
         }
      }
      // clear out any remaining spell combo boxes
      for ( ; i<_collegeCombo.length ; i++) {
         collegeComboData[i]   = "---";
         itemEnabledData[i] = enabled;
         collegeLevelData[i]   = 0;
         collegeAdjLvlData[i]  = 0;
         collegeCostData[i]    = "(0)";
         enabled = false;
      }
      // Now update anything that needs to change in the UI
      for (int j=0 ; j<_collegeCombo.length ; j++) {
         if (!_collegeComboData[j].equals(collegeComboData[j])) {
            _collegeCombo[j].setText(collegeComboData[j]);
         }
         if (!_collegeCostData[j].equals(collegeCostData[j])) {
            _collegeCost[j].setText(collegeCostData[j]);
         }
         if (_collegeLevelData[j]   != collegeLevelData[j]) {
            _collegeLevel[j].setSelection(collegeLevelData[j]);
         }
         if (_collegeAdjLvlData[j]  != collegeAdjLvlData[j]) {
            _collegeAdjLvl[j].setText("[" + collegeAdjLvlData[j] + "]");
         }
         if (_itemEnabledData[j] != (itemEnabledData[j])) {
            _collegeCombo[j].setEnabled(itemEnabledData[j]);
            _collegeLevel[j].setEnabled(itemEnabledData[j]);
            _collegeAdjLvl[j].setEnabled(itemEnabledData[j]);
            _spellCombo[j].setEnabled(itemEnabledData[j]);
//            _spellLevel[j].setEnabled(enabledList[j]);
            _spellFamiliarity[j].setEnabled(itemEnabledData[j]);
         }
         // keep track of the changes we've made:
         _collegeComboData[j]  = collegeComboData[j];
         _collegeLevelData[j]  = collegeLevelData[j];
         _collegeAdjLvlData[j] = collegeAdjLvlData[j];
         _collegeCostData[j]   = collegeCostData[j];
         _itemEnabledData[j]   = itemEnabledData[j];
      }

      // now build the desired list of spell data:
      List<MageSpell> mageSpells = (character != null) ? character.getMageSpellsList() : new ArrayList<>();
      String rangeName;
      String[] spellComboData          = new String[_spellCombo.length];
      String[] spellFamiliarityData    = new String[_spellCombo.length];
      String[] spellCostData           = new String[_spellCombo.length];
      String[] spellEffectiveSkillData = new String[_spellCombo.length];
      String[] spellTimeData           = new String[_spellCombo.length];
      String[] spellRangeData          = new String[_spellCombo.length];
      boolean[] spellEnabledData       = new boolean[_spellCombo.length];

      int j=0;
      for (MageSpell spell : mageSpells) {
         spellComboData[j] = spell.getName();
         spellFamiliarityData[j] = spell.getFamiliarity();
         spellCostData[j] = "(" + Rules.getSpellCost(spell.getLevel()) + ")";
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
         spellEnabledData[j] = false;
         spellEnabledData[j] = !isRequiredByOtherSpells(mageSpells, spell);
         j++;
         if (j >= _spellCombo.length) {
            break;
         }
      }

      enabled = (character != null);
      // clear out any remaining spell combo boxes
      for ( ; j<_spellCombo.length ; j++) {
         spellComboData[j] = "---";
//         spellLevelData[j] = 0;
         spellFamiliarityData[j] = MageSpell.FAM_UNFAMILIAR;
         spellEffectiveSkillData[j] = " ";
         spellTimeData[j]    = " ";
         spellRangeData[j]   = " ";
         spellCostData[j]    = "(0)";
         spellEnabledData[j] = enabled;
         enabled = false;
      }
      // Now update anything that needs to change in the UI

      for (int k=0 ; k<_spellCombo.length ; k++) {
         if (!_spellComboData[k].equals(spellComboData[k])) {
            _spellCombo[k].setText(spellComboData[k]);
         }
//       if (!_spellLevelData[k].equals(spellLevelData[k])) _spellLevel[k].setSelection(spellLevelData[k]);
         if (!_spellFamiliarityData[k].equals(spellFamiliarityData[k])) {
            _spellFamiliarity[k].setText(spellFamiliarityData[k]);
         }
         if (!_spellCostData[k].equals(spellCostData[k])) {
            _spellCost[k].setText(spellCostData[k]);
         }
         if (!_spellEffectiveSkillData[k].equals(spellEffectiveSkillData[k])) {
            _spellEffectiveSkill[k].setText(spellEffectiveSkillData[k]);
         }
         if (!_spellTimeData[k].equals(spellTimeData[k])) {
            _spellTime[k].setText(spellTimeData[k]);
         }
         if (!_spellRangeData[k].equals(spellRangeData[k])) {
            _spellRange[k].setText(spellRangeData[k]);
         }
         if (_spellEnabledData[k] != spellEnabledData[k]) {
            _spellCombo[k].setEnabled(spellEnabledData[k]);
//            _spellLevel[k].setEnabled(spellEnabledData[k]);
            _spellFamiliarity[k].setEnabled(spellEnabledData[k]);
         }
         // keep track of the changes we've made:
         _spellComboData[k]   = spellComboData[k];
//       _spellLevelData[k]   = spellLevelData[k];
         _spellFamiliarityData[k]    = spellFamiliarityData[k];
         _spellCostData[k]    = spellCostData[k];
         _spellEffectiveSkillData[k] = spellEffectiveSkillData[k];
         _spellTimeData[k]    = spellTimeData[k];
         _spellRangeData[k]   = spellRangeData[k];
         _spellEnabledData[k] = spellEnabledData[k];
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

            if (pageIndex >= _priestPage.length) {
               break;
            }
            _priestPage[pageIndex].setText(group);
            int spellIndex = 0;
            for (PriestSpell spell : PriestSpell.getSpellsInGroup(group)) {
               spell.setDeity(deity);
               if (_priestSpellName[pageIndex].length <= spellIndex) {
                  continue;
               }
               _priestSpellName[pageIndex][spellIndex].setText(spell.getName());
               _priestSpellAffinity[pageIndex][spellIndex].setText(String.valueOf(spell.getAffinity()));
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
               _priestSpellRange[pageIndex][spellIndex].setText(rangeName);
               _priestSpellResistance[pageIndex][spellIndex].setText(resistance);
               enabled = (affinityLevel >= spell.getAffinity());
               _priestSpellName[pageIndex][spellIndex].setEnabled(enabled);
               _priestSpellAffinity[pageIndex][spellIndex].setEnabled(enabled);
               _priestSpellRange[pageIndex][spellIndex].setEnabled(enabled);
               _priestSpellResistance[pageIndex][spellIndex].setEnabled(enabled);
               spellIndex++;
            }
            // clear out any remaining priest spells
            for ( ; spellIndex<PRIEST_SPELLS_PER_PAGE ; spellIndex++) {
               _priestSpellName[pageIndex][spellIndex].setText("---");
               //         _spellLevel[pageIndex][spellIndex].setSelection(0);
               _priestSpellAffinity[pageIndex][spellIndex].setText("---");
               _priestSpellRange[pageIndex][spellIndex].setText(" ");
               _priestSpellResistance[pageIndex][spellIndex].setText(" ");

               _priestSpellName[pageIndex][spellIndex].setEnabled(false);
               _priestSpellAffinity[pageIndex][spellIndex].setEnabled(false);
               _priestSpellRange[pageIndex][spellIndex].setEnabled(false);
               _priestSpellResistance[pageIndex][spellIndex].setEnabled(false);
            }
            pageIndex++;
         }
      }
      for ( ; pageIndex<PRIEST_PAGES ; pageIndex++) {
         _priestPage[pageIndex].setText("---");
         // clear out any remaining priest spells
         for (int spellIndex = 0 ; spellIndex<PRIEST_SPELLS_PER_PAGE ; spellIndex++) {
            _priestSpellName[pageIndex][spellIndex].setText("---");
            //         _spellLevel[pageIndex][spellIndex].setSelection(0);
            _priestSpellAffinity[pageIndex][spellIndex].setText("---");
            _priestSpellRange[pageIndex][spellIndex].setText(" ");
            _priestSpellResistance[pageIndex][spellIndex].setText(" ");

            _priestSpellName[pageIndex][spellIndex].setEnabled(false);
            _priestSpellAffinity[pageIndex][spellIndex].setEnabled(false);
            _priestSpellRange[pageIndex][spellIndex].setEnabled(false);
            _priestSpellResistance[pageIndex][spellIndex].setEnabled(false);
         }
      }
      // If the character has only one type of spells, go to that page:
      if (showPriestPage != showMagePage) {
         _mainFolder.setSelection(showPriestPage ? 1 : 0);
      }
      // make sure it all looks good.
//      _spellFamiliarity[0].getParent().pack();
   }
   @Override
   public void modifyText(ModifyEvent e)
   {
      _display.modifyText(e, this);
   }

   @SuppressWarnings("unused")
   private static byte getValidSpellRange(byte spellValue)
   {
      if ((spellValue >= 0) && (spellValue <= Rules.getMaxSpellLevel())) {
         return spellValue;
      }
      return 0;
   }
   private static byte getValidCollegeRange(byte collegeValue)
   {
      if ((collegeValue >= 0) && (collegeValue <= Rules.getMaxCollegeLevel())) {
         return collegeValue;
      }
      return 0;
   }
}
