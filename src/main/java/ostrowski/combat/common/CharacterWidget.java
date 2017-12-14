package ostrowski.combat.common;

import java.util.ArrayList;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.client.ui.AdvantagesBlock;
import ostrowski.combat.client.ui.ArmorBlock;
import ostrowski.combat.client.ui.AttributesBlock;
import ostrowski.combat.client.ui.DefenseBlock;
import ostrowski.combat.client.ui.DiceBlock;
import ostrowski.combat.client.ui.EncumbranceBlock;
import ostrowski.combat.client.ui.IUIBlock;
import ostrowski.combat.client.ui.MainBlock;
import ostrowski.combat.client.ui.SkillsBlock;
import ostrowski.combat.client.ui.SpellsBlock;
import ostrowski.combat.client.ui.WeaponBlock;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.ui.Helper;

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
   private final AdvantagesBlock     _advantagesBlock     = new AdvantagesBlock(this);
   private final ArmorBlock          _armorBlock          = new ArmorBlock(this);
   private final AttributesBlock     _attributesBlock     = new AttributesBlock(this);
   private final DefenseBlock        _defenseBlock        = new DefenseBlock(this);
   private final DiceBlock           _diceBlock           = new DiceBlock(this);
   private final SkillsBlock         _skillsBlock         = new SkillsBlock(this);
   private MainBlock           _topBlock            = null;
   private final WeaponBlock         _weaponBlock         = new WeaponBlock(this);
   private final EncumbranceBlock    _encumbranceBlock    = new EncumbranceBlock(this);
   private final SpellsBlock         _spellsBlock         = new SpellsBlock(this);
   // these don't:
   public Character            _character           = null;
   public ArrayList<Helper>    _uiBlocks            = new ArrayList<>();
   private int                 _uniqueConnectionID  = -1;
   public AI _ai;
   private boolean             _blocksInitialized   = false;
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
            ArrayList<String> names = charFile.getCharacterNames();
            if (names.size() > 0) {
               preferedCharName = names.get(0);
            }
         }
         if (preferedCharName != null) {
            setCharacter(charFile.getCharacter(preferedCharName));
         }
      }
      _topBlock= new MainBlock(this, charFile, display);

      if (_advantagesBlock != null) {
         _uiBlocks.add(_advantagesBlock);
      }
      if (_armorBlock != null) {
         _uiBlocks.add(_armorBlock);
      }
      if (_attributesBlock != null) {
         _uiBlocks.add(_attributesBlock);
      }
      if (_defenseBlock != null) {
         _uiBlocks.add(_defenseBlock);
      }
      if (_diceBlock != null) {
         _uiBlocks.add(_diceBlock);
      }
      if (_skillsBlock != null) {
         _uiBlocks.add(_skillsBlock);
      }
      if (_weaponBlock != null) {
         _uiBlocks.add(_weaponBlock);
      }
      if (_advantagesBlock != null)
       {
         _uiBlocks.add(_advantagesBlock); // weaponBlock updates may adjust the wealth advantage, so add it here after the _weaponBlock update
      }
      if (_encumbranceBlock != null) {
         _uiBlocks.add(_encumbranceBlock);
      }
      if (_spellsBlock != null) {
         _uiBlocks.add(_spellsBlock);
      }
      // Put the top block at the bottom, so it can update the add/edit/delete button
      // after all other block have been updated.
      _uiBlocks.add(_topBlock);
   }

   public CharacterFile getCharacterFile() {
      return _topBlock._charFile;
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

                                     _topBlock.buildBlock(leftBlock);
      if (_attributesBlock != null) {
         _attributesBlock.buildBlock(leftBlock);
      }
      if (_diceBlock != null) {
         _diceBlock.buildBlock(leftBlock);
      }
      if (_advantagesBlock != null) {
         _advantagesBlock.buildBlock(leftBlock);
      }
      if (_encumbranceBlock != null) {
         _encumbranceBlock.buildBlock(leftBlock);
      }

      if (_weaponBlock != null) {
         _weaponBlock.buildBlock(topBlock);
      }

      if (_skillsBlock != null) {
         _skillsBlock.buildBlock(middleBlock);
      }
      if (_spellsBlock != null) {
         _spellsBlock.buildBlock(middleBlock);
      }

      if (_armorBlock != null) {
         _armorBlock.buildBlock(rightBlock);
      }
      if (_defenseBlock != null) {
         _defenseBlock.buildBlock(rightBlock);
      }

      _blocksInitialized = true;
      updateDisplayFromCharacter();
   }

   public void updateDisplayFromCharacter() {
      if (_character != null) {
         _inModify = true;
         for (Helper helper : _uiBlocks) {
            IUIBlock block = (IUIBlock) helper;
            block.updateDisplayFromCharacter(_character);
         }
         refreshDisplay();
         _inModify = false;
      }
   }

   boolean _inRefreshDisplay = false;
   public void refreshDisplay() {
      if (_inRefreshDisplay) {
         return;
      }
      _inRefreshDisplay = true;
      try {
         if (_blocksInitialized) {
            for (Helper helper : _uiBlocks) {
               IUIBlock block = (IUIBlock) helper;
               long startTime = System.currentTimeMillis();
               block.refreshDisplay(_character);
               long endTime = System.currentTimeMillis();
Rules.diag("recieved Character - refreshDisplay of " + block.getClass().getName() + " took " + ((endTime - startTime) / 1000.0) + " seconds");
            }
         }
      }
      finally {
         _inRefreshDisplay = false;
      }
   }

   public void updateCharacterFromDisplay() {
      if (_character != null) {
         for (Helper helper : _uiBlocks) {
            IUIBlock block = (IUIBlock) helper;
            block.updateCharacterFromDisplay(_character);
         }
         _character.refreshDefenses();
      }
   }


   public void setControls(boolean enabledFlag, boolean visibleFlag)
   {
      if (_character == null) {
          enabledFlag = false;
      }
      if (_attributesBlock != null) {
         _attributesBlock.enableControls(enabledFlag);
      }
                                    _topBlock.enableControls(enabledFlag);
      if (_skillsBlock != null) {
         _skillsBlock.enableControls(enabledFlag);
      }
      if (_weaponBlock != null) {
         _weaponBlock.enableControls(enabledFlag);
      }
      if (_spellsBlock != null) {
         _spellsBlock.enableControls(enabledFlag);
      }
   }

   public static boolean _inModify = false;
   @Override
   public void modifyText(ModifyEvent e) {
      modifyText(e, null);
   }
   public void modifyText(ModifyEvent e, IUIBlock sourceBlock) {
      if (!_inModify) {
         _inModify = true;
         if (sourceBlock != null) {
            // If one block was changed, the only change to the character should be from data within that block:
            sourceBlock.updateCharacterFromDisplay(_character);
         }
         else {
            updateCharacterFromDisplay();
         }
         refreshDisplay();
         _inModify = false;
      }
   }

   public void updateCharacter(Character character)
   {
      if (_character != null) {
         if (character._uniqueID == _character._uniqueID) {
            _character.copyData(character);
            refreshDisplay();
         }
      }
      setControls(true, true);
   }

   public void setUniqueConnectionID(int id) {
      _uniqueConnectionID = id;
      Rules.setDiagComponentName("Client"+id);
   }

   public void setCharacter(Character character) {
      _character = character;
      if (_character != null) {
         _character._uniqueID = _uniqueConnectionID;
      }
//      refreshDisplay();
      if (!_inModify) {
         _inModify = true;
         refreshDisplay();
         _inModify = false;
      }
   }

   public void setAIEngine(String aiEngine) {
      if (aiEngine.equalsIgnoreCase("Off")) {
         _ai = null;
      }
      else {
         _ai = new AI(_character, AI_Type.getByString(aiEngine));
      }
   }
}
