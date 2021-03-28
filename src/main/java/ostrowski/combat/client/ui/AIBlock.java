package ostrowski.combat.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.ui.Helper;

public class AIBlock extends Helper implements IUIBlock, ModifyListener {

   final   CharacterDisplay display;
   private Combo            aiSelection;

   public AIBlock(CharacterDisplay display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group group = createGroup(parent, "AI Engine", 1, true/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "AI:", SWT.LEFT, 1, null);
      List<String> AIs = new ArrayList<>();
      AIs.add("Off");
      for (AI_Type aiType : AI_Type.values()) {
         AIs.add("AI - " + aiType.name);
      }

      aiSelection = createCombo(group, SWT.READ_ONLY, 1, AIs);
      aiSelection.select(0);
      aiSelection.addModifyListener(this);
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
   }

   @Override
   public void refreshDisplay(Character character) {
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
   }

   public void setAI(boolean aiOn) {
       int newSelIndex = aiOn ? 1 : 0;
       if (newSelIndex != aiSelection.getSelectionIndex()) {
         aiSelection.select(newSelIndex);
      }
   }
   @Override
   public void modifyText(ModifyEvent e) {
      if (!CharacterWidget.inModify) {
         CharacterWidget.inModify = true;
         if (e.widget == aiSelection) {
            int selectedIndex = aiSelection.getSelectionIndex();
            String aiEngine = aiSelection.getItem(selectedIndex);
            if (aiEngine.startsWith("AI - ")) {
               aiEngine = aiEngine.replace("AI - ", "");
            }
            display.setAIEngine(aiEngine);
         }
         CharacterWidget.inModify = false;
      }
   }

}
