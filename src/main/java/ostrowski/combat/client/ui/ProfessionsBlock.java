package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.ui.Helper;

import java.util.*;
import java.util.List;

public class ProfessionsBlock extends Helper implements Enums, ModifyListener, IUIBlock, SelectionListener {
   final         CharacterWidget display;
   static final  int             SKILL_COUNT         = 12;
   private final Combo[]         professionType      = new Combo[SKILL_COUNT];
   private final Combo[] professionLevelRank = new Combo[SKILL_COUNT];
   private final Combo[] skillTypeCombo      = new Combo[SKILL_COUNT];
   private final Text[]  skillLevelAdj       = new Text[SKILL_COUNT];
   private final Text[]          skillCost           = new Text[SKILL_COUNT];
   private       Text            racialSize          = null;
   private static final String   NO_PROF_SELECTED    = "--";
   private static final String   NO_SKILL_SELECTED   = "---";

   public ProfessionsBlock(CharacterWidget display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group skillGroup = createGroup(parent, "Professions / Skills", 5/*columns*/, false, 3/*hSpacing*/, 1/*vSpacing*/);
      createLabel(skillGroup, "Racial adj. for size:", SWT.RIGHT, 3, null);
      racialSize = createText(skillGroup, "0", false, 1);
      createLabel(skillGroup, "", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Profession", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Skill",      SWT.CENTER, 1, null);
      createLabel(skillGroup, "Level/Rank", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Adj.",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Cost",       SWT.CENTER, 1, null);

      List<String> professionNames = new ArrayList<>();
      professionNames.add(NO_PROF_SELECTED);
      for (ProfessionType element : ProfessionType.values()) {
         professionNames.add(element.getName());
      }

      List<String> profLevels = new ArrayList<>();
      for (int i=0 ; i<=Rules.getMaxSkillLevel() ; i++) {
         profLevels.add(String.valueOf(i));
      }

      List<String> skillNames = new ArrayList<>();
      skillNames.add(NO_SKILL_SELECTED);

      List<String> ranks = new ArrayList<>();
      ranks.add(SkillRank.PROFICIENT.getName());
      ranks.add(SkillRank.FAMILIAR.getName());

      for (int i = 0; i < SKILL_COUNT; i++) {
         professionType[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, professionNames);
         professionType[i].select(0);
         skillTypeCombo[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, skillNames);
         skillTypeCombo[i].select(0);
         professionLevelRank[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, null);
         professionLevelRank[i].setEnabled(false);
         skillLevelAdj[i] = createText(skillGroup, "[0]", false/*editable*/, 1);
         skillCost[i] = createText(skillGroup, "(0)", false/*editable*/, 1);
         // register watchers on all the editable elements we've created
         professionType[i].addSelectionListener(this);
         professionLevelRank[i].addSelectionListener(this);
         skillTypeCombo[i].addSelectionListener(this);
      }

      // setup a tab ordering on the editable controls, row by row:
      Control[] tabList = new Control[SKILL_COUNT * 3];
      int index = 0;
      for (int i = 0; i < SKILL_COUNT; i++) {
         tabList[index++] = professionType[i];
         tabList[index++] = skillTypeCombo[i];
         tabList[index++] = professionLevelRank[i];
      }
      skillGroup.setTabList(tabList);
   }
   public static boolean inModify = false;
   @Override
   public void modifyText(ModifyEvent e) {
      if (!inModify) {
         inModify = true;
         try {
            display.modifyText(e, this);
         }
         finally {
            inModify = false;
         }
      }
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (!inModify) {
         inModify = true;
         Map<String, ArrayList<Integer>> existingProfessions = new HashMap<>();
         for (int i = 0; i < SKILL_COUNT; i++) {
            if (e.widget == professionType[i]) {
               onSetProfession(existingProfessions, i, skillTypeCombo[i].getText());
            }
            existingProfessions.computeIfAbsent(professionType[i].getText(), o -> new ArrayList<>()).add(i);
         }

         for (int i = 0; i < SKILL_COUNT; i++) {
            if (e.widget == skillTypeCombo[i]) {
               if (skillTypeCombo[i].getText().equals(NO_SKILL_SELECTED)){
                  if (professionType[i].getText().equals(NO_PROF_SELECTED)) {
                     professionLevelRank[i].removeAll();
                     professionLevelRank[i].setEnabled(false);
                  }
               }
               else {
                  professionLevelRank[i].setEnabled(true);
                  if (professionLevelRank[i].getText().isEmpty()) {
                     professionLevelRank[i].select(1); // familiar
                  }
               }
               break;
            }
         }

         for (int i = 0; i < SKILL_COUNT; i++) {
            if (e.widget == professionLevelRank[i]) {
               ArrayList<Integer> existingProfs = existingProfessions.get(professionType[i].getText());
               if (existingProfs.get(0) == i) {
                  // They changed a profession level of the primary profession,
                  // make sure all the levels of the same profession are set to match
                  int newLevel = professionLevelRank[i].getSelectionIndex() + 1;
                  for (Integer existingProf : existingProfs) {
                     if (i != existingProf) {
                        SkillRank rank = SkillRank.FAMILIAR;
                        if (professionLevelRank[existingProf].getSelectionIndex() == 0) {
                           rank = SkillRank.PROFICIENT;
                        }
                        recomputeLevelRank(professionLevelRank[existingProf], newLevel, rank);
                     }
                  }
               }
               break;
            }
         }
         inModify = false;
      }
      display.modifyText(e, this);
   }

   private void onSetProfession(Map<String, ArrayList<Integer>> existingProfessions, int rowIndex, String preferredSkillName) {
      if (professionType[rowIndex].getText().equals(NO_PROF_SELECTED)) {
         skillTypeCombo[rowIndex].setText(NO_SKILL_SELECTED);
         skillTypeCombo[rowIndex].setEnabled(false);
         professionLevelRank[rowIndex].removeAll();
         professionLevelRank[rowIndex].setEnabled(false);
         return;
      }
      ArrayList<Integer> existingProfs = existingProfessions.get(professionType[rowIndex].getText());
      if (existingProfs != null && !existingProfs.isEmpty() && (existingProfs.get(0) != rowIndex)) {
         // This entry is not the first entry of this profession type
         Combo firstProfessionCombo = professionLevelRank[existingProfs.get(0)];
         String professionLevelTextSelection = firstProfessionCombo.getText();
         String professionLevelText = professionLevelTextSelection.split(" ")[0];
         recomputeLevelRank(professionLevelRank[rowIndex], Integer.parseInt(professionLevelText), SkillRank.FAMILIAR);
      } else {
         // This is the first entry of this profession type
         String professionLevelTextSelection = professionLevelRank[rowIndex].getText();
         professionLevelRank[rowIndex].removeAll();
         for (int i=1 ; i<=10 ; i++) {
            professionLevelRank[rowIndex].add(i + " (" + SkillRank.PROFICIENT.getName() + ")");
         }
         if (professionLevelTextSelection == null) {
            professionLevelRank[rowIndex].select(3); // level 4 by default?
         }
         else {
            String professionLevelText = professionLevelTextSelection.split(" ")[0];
            if (!professionLevelText.isEmpty()) {
               Integer professionLevel = Integer.parseInt(professionLevelText);
               professionLevelRank[rowIndex].select(professionLevel - 1);
            }
         }
         professionLevelRank[rowIndex].setEnabled(true);
      }
      skillTypeCombo[rowIndex].setEnabled(true);
      for (ProfessionType type : ProfessionType.values()) {
         if (professionType[rowIndex].getText().equals(type.getName())) {
            skillTypeCombo[rowIndex].removeAll();
            skillTypeCombo[rowIndex].add(NO_SKILL_SELECTED);
            boolean skillFound = false;
            for (SkillType skType : type.skillList) {
               skillTypeCombo[rowIndex].add(skType.getName());
               if (skType.name().equals(preferredSkillName)) {
                  skillTypeCombo[rowIndex].select(skillTypeCombo[rowIndex].getItemCount() - 1);
                  skillFound = true;
               }
            }
            if (!skillFound) {
               skillTypeCombo[rowIndex].select(0);
            }
            break;
         }
      }
   }

   private void recomputeLevelRank(Combo combo, int professionLevel, SkillRank rank) {
      combo.removeAll();
      combo.add(professionLevel + " (" + SkillRank.PROFICIENT.getName() + ")");
      combo.add((professionLevel - 2) + " (" + SkillRank.FAMILIAR.getName() + ")");
      if (rank == SkillRank.PROFICIENT) {
         combo.select(0);
      }
      else { // (rank == SkillRank.FAMILIAR)
         combo.select(1);
      }
      combo.setEnabled(true);
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
      widgetSelected(e);
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      Map<ProfessionType, Profession> charProfs = new HashMap<>();//character.getProfessionsList();
      for (int i = 0; i < SKILL_COUNT; i++) {
         String profName = professionType[i].getText();
         if (!profName.equals(NO_PROF_SELECTED)) {
            ProfessionType profType = ProfessionType.getByName(profName);
            String levelRankStr = professionLevelRank[i].getText();
            if (!levelRankStr.isEmpty()) {
               String[] levelAndRank = levelRankStr.split(" ");
               byte profLevel = Byte.parseByte(levelAndRank[0]);
               SkillRank skillRank = SkillRank.PROFICIENT;
               if (levelAndRank[1].contains(SkillRank.FAMILIAR.getName())) {
                  skillRank = SkillRank.FAMILIAR;
                  profLevel += 2;
               }
               byte finalProfLevel = profLevel;
               SkillType skillType = null;
               String skillName = skillTypeCombo[i].getText();
               if (!skillName.equals(NO_SKILL_SELECTED)) {
                  skillType = SkillType.getSkillTypeByName(skillName);
               }
               SkillType finalSkillType = skillType;
               Profession charProfession = charProfs.computeIfAbsent(profType, o -> new Profession(profType, finalSkillType, finalProfLevel));
               charProfession.setLevel(profLevel);
               if (skillType != null && skillRank != null && skillRank != SkillRank.UNKNOWN) {
                  charProfession.setRank(skillType, skillRank);
               }
            }
         }
      }
      if (character != null) {
         character.setProfessionsList(new ArrayList<>(charProfs.values()));
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      refreshDisplay(character);
   }

   private static class ProfessionDisplay {
      public ProfessionType profType;
      public SkillType skillType;
      public SkillRank rank;
      public byte      level;
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      byte bonusToHit = (character == null) ? 0 : character.getRace().getBonusToHit();
      List<Profession> professions = (character == null) ? new ArrayList<>() : character.getProfessionsList();
      List<ProfessionDisplay> charProfs = new ArrayList<>();
      for (Profession profession : professions) {
         for (SkillType skill : profession.getProficientSkills()) {
            ProfessionDisplay charProf = new ProfessionDisplay();
            charProf.profType = profession.getType();
            charProf.skillType = SkillType.getSkillTypeByName(skill.name);
            charProf.rank = SkillRank.PROFICIENT;
            charProf.level = profession.getLevel();
            charProfs.add(charProf);
         }
         for (SkillType skill : profession.getFamiliarSkills()) {
            ProfessionDisplay charProf = new ProfessionDisplay();
            charProf.profType = profession.getType();
            charProf.skillType = SkillType.getSkillTypeByName(skill.name);
            charProf.rank = SkillRank.FAMILIAR;
            charProf.level = profession.getLevel();
            charProfs.add(charProf);
         }
      }
      charProfs.sort((o1, o2) -> {
         if (o1.profType != o2.profType) {
            int compare = Byte.compare(o1.level, o2.level);
            if (compare != 0) {
               // different professions, of different levels
               return compare * -1; // descending
            }
            // different professions, same level, order by profession name
            return o1.profType.getName().compareTo(o2.profType.getName());
         }
         // same profession, order by rank first
         int compare = Byte.compare(o1.rank.getCost(), o2.rank.getCost());
         if (compare != 0) {
            // same profession, different ranks
            return compare * -1; // descending
         }
         if (o1.skillType == o2.skillType) {
            return 0;
         }
         // same profession, same rank, order by skill name - but always put shield below other fighting skills
         if (o1.skillType == SkillType.Shield) {
            return 1;
         }
         if (o2.skillType == SkillType.Shield) {
            return -1;
         }
         return o1.skillType.getName().compareTo(o2.skillType.getName());
      });

      racialSize.setText((bonusToHit >= 0) ? ("+" + bonusToHit) : String.valueOf(bonusToHit));
      int nextInsertIndex = 0;
      Map<String, ArrayList<Integer>> existingProfessions = new HashMap<>();
      for (int i = 0; i < SKILL_COUNT; i++) {
         if (professionType[i].getText().equals(NO_PROF_SELECTED)) {
            continue;
         }
         ArrayList<Integer> existingProfRows = existingProfessions.computeIfAbsent(professionType[i].getText(), o -> new ArrayList<>());
         existingProfRows.add(i);
         boolean isFirstProf = existingProfRows.size() == 1;
         ProfessionDisplay professionFound = null;
         ProfessionType profType = ProfessionType.getByName(professionType[i].getText());
         SkillType skillType = SkillType.getSkillTypeByName(this.skillTypeCombo[i].getText());
         for (ProfessionDisplay charProf : charProfs) {
            if (charProf.profType == profType &&
                ((charProf.skillType == skillType) || this.skillTypeCombo[i].getText().equals(NO_SKILL_SELECTED))) {
               //professionType[i] already matches
               onSetProfession(existingProfessions, i, charProf.skillType.getName());
               this.skillTypeCombo[i].setText(charProf.skillType.getName());
               if (isFirstProf) {
                  charProf.rank = SkillRank.PROFICIENT;
                  professionLevelRank[i].select(charProf.level - 1);
               }
               else {
                  if (charProf.rank == SkillRank.PROFICIENT) {
                     professionLevelRank[i].select(0); // proficient
                  }
                  else {
                     professionLevelRank[i].select(1); // familiar
                  }
               }

               int adjustedLevel = Rules.getAdjustedSkillLevel(profType, charProf.skillType, character);
               skillLevelAdj[i].setText("[" + adjustedLevel + "]" + (charProf.skillType.isAdjustedForEncumbrance ? "*" : ""));
               if (isFirstProf) {
                  skillCost[i].setText("(" + Rules.getProfessionCost(charProf.level) + ")");
               }
               else {
                  skillCost[i].setText("(" + charProf.rank.getCost() + ")");
               }
               this.skillTypeCombo[i].setEnabled(true);
               professionLevelRank[i].setEnabled(true);
               professionFound = charProf;
               nextInsertIndex++;
               break;
            }
         }
         if (professionFound != null) {
            charProfs.remove(professionFound);
         }
         else {
            int cost;
            if (isFirstProf) {
               cost = Rules.getProfessionCost((byte) (professionLevelRank[i].getSelectionIndex() + 1));
            }
            else {
               SkillRank rank = SkillRank.FAMILIAR;
               if (professionLevelRank[i].getSelectionIndex() == 0) {
                  rank = SkillRank.PROFICIENT;
               }
               cost = rank.getCost();
            }
            skillCost[i].setText("(" + cost + ")");

            if (this.skillTypeCombo[i].getText().equals(NO_SKILL_SELECTED) && isFirstProf) {
               skillLevelAdj[i].setText("[0]");
            }
            else if (this.skillTypeCombo[i].getText().equals(NO_SKILL_SELECTED)) {
               skillLevelAdj[i].setText("?");
            }
            else {
               removeSkillRow(i);
            }
            //i--;
         }
      }
      // Add any profession from the character that we didn't already find.
      for (ProfessionDisplay charProf : charProfs) {
         String profTypeText = professionType[nextInsertIndex].getText();
         ArrayList<Integer> existingProfRows = existingProfessions.computeIfAbsent(profTypeText,
                                                                                   o -> new ArrayList<>());
         existingProfRows.add(nextInsertIndex);
         boolean isFirstProf = existingProfRows.size() == 1;
         professionType[nextInsertIndex].setText(charProf.profType.getName());
         onSetProfession(existingProfessions, nextInsertIndex, charProf.skillType.getName());
         skillTypeCombo[nextInsertIndex].setText(charProf.skillType.getName());
         if (isFirstProf) {
            professionLevelRank[nextInsertIndex].select(charProf.level - 1);
         }
         else {
            int index = (charProf.rank == SkillRank.FAMILIAR) ? 1 : 0;
            professionLevelRank[nextInsertIndex].select(index);
         }
         int adjustedLevel = Rules.getAdjustedSkillLevel(charProf.profType, charProf.skillType, character);

         skillLevelAdj[nextInsertIndex].setText("[" + adjustedLevel + "]");
         if (isFirstProf) {
            skillCost[nextInsertIndex].setText("(" + Rules.getProfessionCost(charProf.level) + ")");
         }
         else {
            skillCost[nextInsertIndex].setText("(" + charProf.rank.getCost() + ")");
         }
         professionLevelRank[nextInsertIndex].setEnabled(true);
         nextInsertIndex++;
      }
      // Now remove any remaining rows
      for (int i = 0; i < (SKILL_COUNT - 1); i++) {
         if ((professionType[i].getText().equals(NO_PROF_SELECTED)) && (!professionType[i + 1].getText().equals(NO_PROF_SELECTED))) {
            if (removeSkillRow(i)) {
               --i;
            }
         }
      }
      existingProfessions.clear();
      for (int i = 0; i < SKILL_COUNT; i++) {
         boolean isFirstProf = (null == existingProfessions.put(professionType[i].getText(), new ArrayList<>()));
         if (professionType[i].getText().equals(NO_PROF_SELECTED)) {
            professionLevelRank[i].removeAll();
            professionLevelRank[i].setEnabled(false);
            skillTypeCombo[i].setText(NO_SKILL_SELECTED);
            skillTypeCombo[i].setEnabled(false);
         }
         if (skillTypeCombo[i].getText().equals(NO_SKILL_SELECTED) && !isFirstProf) {
            skillLevelAdj[i].setText("[0]");
            skillCost[i].setText("(0)");
         }
      }
   }

   private boolean removeSkillRow(int rowIndex) {
      boolean nextItemAvailable = (rowIndex+1) < SKILL_COUNT;
      professionLevelRank[rowIndex].removeAll();
      if (nextItemAvailable) {
         for (String item : professionType[rowIndex + 1].getItems()) {
            professionLevelRank[rowIndex].add(item);
         }
      }
      professionType[rowIndex].select(nextItemAvailable ? professionType[rowIndex + 1].getSelectionIndex() : 0);
      professionType[rowIndex].setEnabled(!nextItemAvailable || professionType[rowIndex + 1].getEnabled());
      Map<String, ArrayList<Integer>> existingProfessions = new HashMap<>();
      for (int row=0 ; row<rowIndex ; row++) {
         existingProfessions.computeIfAbsent(professionType[row].getText(), o -> new ArrayList<>()).add(row);
      }
      onSetProfession(existingProfessions, rowIndex, nextItemAvailable ? skillTypeCombo[rowIndex + 1].getText() : "");
      professionLevelRank[rowIndex].select(nextItemAvailable ? professionLevelRank[rowIndex + 1].getSelectionIndex() : 0);
      skillLevelAdj[rowIndex].setText(nextItemAvailable ? skillLevelAdj[rowIndex + 1].getText() : "[0]");
      skillCost[rowIndex].setText(nextItemAvailable ? skillCost[rowIndex + 1].getText() : "(0)");
      skillTypeCombo[rowIndex].select(nextItemAvailable ? skillTypeCombo[rowIndex + 1].getSelectionIndex() : 0);
      skillTypeCombo[rowIndex].setEnabled(!nextItemAvailable || skillTypeCombo[rowIndex + 1].getEnabled());
      professionLevelRank[rowIndex].setEnabled(!nextItemAvailable || professionLevelRank[rowIndex + 1].getEnabled());
      if (nextItemAvailable) {
         removeSkillRow(rowIndex+1);
      }
      return nextItemAvailable;
   }

}
