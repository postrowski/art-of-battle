package ostrowski.combat.common;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.client.ui.*;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

/*
 * Created on May 5, 2006
 *
 */

/**
 * @author Paul
 *
 */
public class CharacterWidget implements Enums, ModifyListener {

   // these object all implement the IUIBlock Interface:
   private final AdvantagesBlock  advantagesBlock    = new AdvantagesBlock(this);
   private final ArmorBlock       armorBlock         = new ArmorBlock(this);
   private final AttributesBlock  attributesBlock    = new AttributesBlock(this);
   private final DefenseBlock     defenseBlock       = new DefenseBlock(this);
   private final DiceBlock        diceBlock          = new DiceBlock(this);
   private final SkillsBlock      skillsBlock        = new SkillsBlock(this);
   private final MainBlock        topBlock;
   private final WeaponBlock      weaponBlock        = new WeaponBlock(this);
   private final EncumbranceBlock encumbranceBlock   = new EncumbranceBlock(this);
   private final SpellsBlock      spellsBlock        = new SpellsBlock(this);
   // these don't:
   public        Character        character          = null;
   public final  List<IUIBlock>   uiBlocks           = new ArrayList<>();
   private       int              uniqueConnectionID = -1;
   public        AI               ai;
   private       boolean          blocksInitialized  = false;

   public static boolean inModify = false;
   boolean inRefreshDisplay = false;

   public CharacterWidget(String preferedCharName, CharacterFile charFile) {
       this(preferedCharName, charFile, null);
   }
   public CharacterWidget(String preferedCharName, CharacterFile charFile, CharacterDisplay display) {
      if (charFile.getCharacterNames().size() == 0) {
         setCharacter(new Character());
      }
      else {
         if (preferedCharName != null) {
            // Is this name the name of an character in our data file?
            if (charFile.getCharacter(preferedCharName) == null) {
               // if not, ignore the parameter.
               preferedCharName = null;
            }
         }
         if (preferedCharName == null) {
            // grab the first name in the list
            List<String> names = charFile.getCharacterNames();
            if (names.size() > 0) {
               preferedCharName = names.get(0);
            }
         }
         if (preferedCharName != null) {
            setCharacter(charFile.getCharacter(preferedCharName));
         }
      }
      topBlock = new MainBlock(this, charFile, display);

      if (advantagesBlock != null) {
         uiBlocks.add(advantagesBlock);
      }
      if (armorBlock != null) {
         uiBlocks.add(armorBlock);
      }
      if (attributesBlock != null) {
         uiBlocks.add(attributesBlock);
      }
      if (defenseBlock != null) {
         uiBlocks.add(defenseBlock);
      }
      if (diceBlock != null) {
         uiBlocks.add(diceBlock);
      }
      if (skillsBlock != null) {
         uiBlocks.add(skillsBlock);
      }
      if (weaponBlock != null) {
         uiBlocks.add(weaponBlock);
      }
      if (advantagesBlock != null) {
         uiBlocks.add(advantagesBlock); // weaponBlock updates may adjust the wealth advantage, so add it here after the weaponBlock update
      }
      if (encumbranceBlock != null) {
         uiBlocks.add(encumbranceBlock);
      }
      if (spellsBlock != null) {
         uiBlocks.add(spellsBlock);
      }
      // Put the top block at the bottom, so it can update the add/edit/delete button
      // after all other block have been updated.
      uiBlocks.add(topBlock);
   }

   public CharacterFile getCharacterFile() {
      return topBlock.charFile;
   }
   public void buildCharSheet(Composite parent)
   {
      parent.setLayout(new GridLayout(2 /*columns*/,false/*sameWidth*/));

      Composite leftBlock = Helper.createComposite(parent, 1, GridData.FILL_BOTH);
      Composite midRightBlock = Helper.createComposite(parent, 1, GridData.FILL_BOTH);
      midRightBlock.setLayout(new GridLayout(2 /*columns*/,false/*sameWidth*/));
      Composite topBlock = Helper.createComposite(midRightBlock, 2, GridData.FILL_BOTH);
      Composite middleBlock = Helper.createComposite(midRightBlock, 1, GridData.FILL_BOTH);
      Composite rightBlock = Helper.createComposite(midRightBlock, 1, GridData.FILL_BOTH);

      this.topBlock.buildBlock(leftBlock);
      if (attributesBlock != null) {
         attributesBlock.buildBlock(leftBlock);
      }
      if (diceBlock != null) {
         diceBlock.buildBlock(leftBlock);
      }
      if (advantagesBlock != null) {
         advantagesBlock.buildBlock(leftBlock);
      }
      if (encumbranceBlock != null) {
         encumbranceBlock.buildBlock(leftBlock);
      }

      if (weaponBlock != null) {
         weaponBlock.buildBlock(topBlock);
      }

      if (skillsBlock != null) {
         skillsBlock.buildBlock(middleBlock);
      }
      if (spellsBlock != null) {
         spellsBlock.buildBlock(middleBlock);
      }

      if (armorBlock != null) {
         armorBlock.buildBlock(rightBlock);
      }
      if (defenseBlock != null) {
         defenseBlock.buildBlock(rightBlock);
      }

      blocksInitialized = true;
      updateDisplayFromCharacter();
   }

   public void updateDisplayFromCharacter() {
      if (character != null) {
         inModify = true;
         for (IUIBlock helper : uiBlocks) {
            helper.updateDisplayFromCharacter(character);
         }
         refreshDisplay();
         inModify = false;
      }
   }

   public void refreshDisplay() {
      if (inRefreshDisplay) {
         return;
      }
      inRefreshDisplay = true;
      try {
         if (blocksInitialized) {
            for (IUIBlock helper : uiBlocks) {
//               long startTime = System.currentTimeMillis();
               helper.refreshDisplay(character);
//               long endTime = System.currentTimeMillis();
//Rules.diag("received Character - refreshDisplay of " + block.getClass().getName() + " took " + ((endTime - startTime) / 1000.0) + " seconds");
            }
         }
      }
      finally {
         inRefreshDisplay = false;
      }
   }

   public void updateCharacterFromDisplay() {
      if (character != null) {
         for (IUIBlock helper : uiBlocks) {
            helper.updateCharacterFromDisplay(character);
         }
         character.refreshDefenses();
      }
   }


   public void setControls(boolean enabledFlag, boolean visibleFlag)
   {
      if (character == null) {
          enabledFlag = false;
      }
      if (attributesBlock != null) {
         attributesBlock.enableControls(enabledFlag);
      }
                                    topBlock.enableControls(enabledFlag);
      if (skillsBlock != null) {
         skillsBlock.enableControls(enabledFlag);
      }
      if (weaponBlock != null) {
         weaponBlock.enableControls(enabledFlag);
      }
      if (spellsBlock != null) {
         spellsBlock.enableControls(enabledFlag);
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
      modifyText(e, null);
   }
   public void modifyText(ModifyEvent e, IUIBlock sourceBlock) {
      if (!inModify) {
         inModify = true;
         if (sourceBlock != null) {
            // If one block was changed, the only change to the character should be from data within that block:
            sourceBlock.updateCharacterFromDisplay(character);
         }
         else {
            updateCharacterFromDisplay();
         }
         refreshDisplay();
         inModify = false;
      }
   }

   public void updateCharacter(Character character)
   {
      if (this.character != null) {
         if (character.uniqueID == this.character.uniqueID) {
            this.character.copyData(character);
            refreshDisplay();
         }
      }
      setControls(true, true);
   }

   public void setUniqueConnectionID(int id) {
      uniqueConnectionID = id;
      Rules.setDiagComponentName("Client"+id);
   }

   public void setCharacter(Character character) {
      this.character = character;
      if (this.character != null) {
         this.character.uniqueID = uniqueConnectionID;
      }
//      refreshDisplay();
      if (!inModify) {
         inModify = true;
         refreshDisplay();
         inModify = false;
      }
   }

   public void setAIEngine(String aiEngine) {
      if (aiEngine.equalsIgnoreCase("Off")) {
         ai = null;
      }
      else {
         ai = new AI(character, AI_Type.getByString(aiEngine));
      }
   }
}
