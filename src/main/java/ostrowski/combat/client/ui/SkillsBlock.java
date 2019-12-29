package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class SkillsBlock extends Helper implements Enums, ModifyListener, IUIBlock
{
   final         CharacterWidget _display;
   private final Combo[]         _skillType     = new Combo[6];
   private final Spinner[] _skillLevel    = new Spinner[_skillType.length];
   private final Text[]    _skillLevelAdj = new Text[_skillType.length];
   private final Text[]    _skillCost     = new Text[_skillType.length];
   private Text      _racialSize    = null;

   public SkillsBlock(CharacterWidget display) {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group skillGroup = createGroup(parent, "Skills", 4/*columns*/, false, 3/*hSpacing*/, 1/*vSpacing*/);
      createLabel(skillGroup, "Racial adj. for size:", SWT.RIGHT, 2, null);
      _racialSize = createText(skillGroup, "0", false, 1);
      createLabel(skillGroup, "",           SWT.CENTER, 1, null);
      createLabel(skillGroup, "Name",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Base Level", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Adj.",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Cost",       SWT.CENTER, 1, null);
      List<String> skillNames = new ArrayList<>();
      skillNames.add("---");
      Attribute priorAttribute = null;
      for (SkillType element : SkillType.values()) {
         if ((priorAttribute != element.getAttributeBase()) && (element.getAttributeBase() != Attribute.Dexterity)) {
            skillNames.add("---");
         }
         skillNames.add(element.getName());
         priorAttribute = element.getAttributeBase();
      }
      for (int i = 0; i < _skillType.length; i++) {
         _skillType[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, skillNames);
         _skillType[i].select(0);
         _skillLevel[i] = createSpinner(skillGroup, 0/*min*/, Rules.getMaxSkillLevel()/*max*/, 0/*value*/, 1);
         _skillLevelAdj[i] = createText(skillGroup, "[0]", false/*editable*/, 1);
         _skillCost[i] = createText(skillGroup, "(0)", false/*editable*/, 1);
         _skillType[i].addModifyListener(this);
         _skillLevel[i].addModifyListener(this);
      }
      Control[] tabList = new Control[_skillType.length * 2];
      int index = 0;
      for (int i = 0; i < _skillType.length; i++) {
         tabList[index++] = _skillType[i];
         tabList[index++] = _skillLevel[i];
      }
      skillGroup.setTabList(tabList);
   }

   @Override
   public void modifyText(ModifyEvent e) {
      _display.modifyText(e, this);
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      List<Skill> newSkills = new ArrayList<>();
      for (int i = 0; i < _skillType.length; i++) {
         String skillName = _skillType[i].getText();
         if (!skillName.equals("---")) {
            SkillType skillType = SkillType.getSkillTypeByName(skillName);
            if (skillType != null) {
               boolean dupFound = false;
               for (Skill dupSkill : newSkills) {
                  if (dupSkill.getType() == skillType) {
                     dupSkill.setLevel((byte)Math.max(dupSkill.getLevel(), _skillLevel[i].getSelection()));
                     dupFound = true;
                  }
               }
               if (!dupFound) {
                  newSkills.add(new Skill(skillType, getValidSkillRange((byte) _skillLevel[i].getSelection())));
               }
            }
         }
      }
      if (character != null) {
         character.setSkillsList(newSkills);
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      refreshDisplay(character);
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      byte bonusToHit = (character == null) ? 0 : character.getRace().getBonusToHit();
      List<Skill> skills = (character == null) ? new ArrayList<>() : character.getSkillsList();
      _racialSize.setText((bonusToHit >= 0) ? ("+" + bonusToHit) : String.valueOf(bonusToHit));
      int nextInsertIndex = 0;
      for (int i=0; i < _skillType.length; i++) {
         if (_skillType[i].getText().equals("---")) {
            continue;
         }
         Skill skillFound = null;
         for (Skill skill : skills) {
            if (skill.getName().equals(_skillType[i].getText())) {
               _skillLevel[i].setSelection(skill.getLevel());
               int adjustedLevel = Rules.getAdjustedSkillLevel(skill, character);

               _skillLevelAdj[i].setText("[" + adjustedLevel + "]");
               _skillCost[i].setText("(" + Rules.getSkillCost(skill.getLevel()) + ")");
               _skillType[i].setEnabled(true);
               _skillLevel[i].setEnabled(true);
               skillFound = skill;
               nextInsertIndex++;
               break;
            }
         }
         if (skillFound != null) {
            skills.remove(skillFound);
         }
         else {
            removeSkillRow(i);
            //i--;
         }
      }
      for (Skill skill : skills) {
         _skillType[nextInsertIndex].setText(skill.getName());
         _skillLevel[nextInsertIndex].setSelection(skill.getLevel());
         int adjustedLevel = Rules.getAdjustedSkillLevel(skill, character);

         _skillLevelAdj[nextInsertIndex].setText("[" + adjustedLevel + "]");
         _skillCost[nextInsertIndex].setText("(" + Rules.getSkillCost(skill.getLevel()) + ")");
         _skillType[nextInsertIndex].setEnabled(character != null);
         _skillLevel[nextInsertIndex].setEnabled(character != null);
         nextInsertIndex++;
      }
      for (int i=0; i < (_skillType.length-1); i++) {
         if ((_skillType[i].getText().equals("---")) && (!_skillType[i+1].getText().equals("---")))
         {
            if (removeSkillRow(i)) {
               --i;
            }
         }
      }
      for (int i=0; i < _skillType.length; i++) {
         _skillLevel[i].setEnabled(!_skillType[i].getText().equals("---"));
         _skillType[i].setEnabled((i==0) || (!_skillType[i-1].getText().equals("---")));
      }
   }

   private boolean removeSkillRow(int rowIndex) {
      boolean nextItemAvailable = (rowIndex+1)<_skillType.length;
      _skillLevel[rowIndex]   .setSelection(nextItemAvailable ? _skillLevel[rowIndex+1]   .getSelection() : 0);
      _skillLevelAdj[rowIndex].setText(     nextItemAvailable ? _skillLevelAdj[rowIndex+1].getText()      : "[0]");
      _skillCost[rowIndex]    .setText(     nextItemAvailable ? _skillCost[rowIndex+1]    .getText()      : "(0)");
      _skillType[rowIndex]    .select(      nextItemAvailable ? _skillType[rowIndex+1]    .getSelectionIndex() : 0);
      _skillType[rowIndex]    .setEnabled(!nextItemAvailable || _skillType[rowIndex + 1].getEnabled());
      _skillLevel[rowIndex]   .setEnabled(!nextItemAvailable || _skillLevel[rowIndex + 1].getEnabled());
      if (nextItemAvailable) {
         removeSkillRow(rowIndex+1);
      }
      return nextItemAvailable;
   }

   private static byte getValidSkillRange(byte skillVal) {
      if ((skillVal >= 0) && (skillVal <= Rules.getMaxSkillLevel())) {
         return skillVal;
      }
      return 0;
   }
}
