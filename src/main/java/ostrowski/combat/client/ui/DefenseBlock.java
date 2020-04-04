/*
 * Created on May 22, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.DefenseOptions;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.ui.Helper;

import java.util.HashMap;

public class DefenseBlock extends Helper implements IUIBlock
{
   static public boolean INCLUDE_RANGE = false;

   CharacterWidget _display;
   private final HashMap<RANGE, HashMap<DefenseOption, Text>>    _baseDefenseOpt = new HashMap<>();
   private final HashMap<RANGE, HashMap<DefenseOptions, Text>>   _defenseOpts    = new HashMap<>();
   private static final HashMap<DefenseOptions, String> OPTIONS_TO_SHOW = new HashMap<>();
   static {
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_DODGE), "Dodge:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_LEFT), "Block:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_RIGHT), "Parry:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_LEFT, DefenseOption.DEF_DODGE), "Dodge && Block:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_DODGE), "Dodge && Parry:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_LEFT, DefenseOption.DEF_RIGHT), "Block && Parry:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_RETREAT), "Retreat:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_LEFT, DefenseOption.DEF_RETREAT), "Retreat && Block:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_RIGHT, DefenseOption.DEF_RETREAT), "Retreat && Parry:");
      OPTIONS_TO_SHOW.put(new DefenseOptions(DefenseOption.DEF_LEFT, DefenseOption.DEF_RIGHT, DefenseOption.DEF_DODGE), "Dodge, Block && Parry:");
   }

   private Text             _sizeAdj      = null;
   private Text             _pdShield     = null;
   private Text             _pdArmor      = null;
   public DefenseBlock(CharacterWidget display)
   {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group defGroup = createGroup(parent, "Defenses", (2+RANGE.values().length)/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
      defGroup.setTabList(new Control[] {});

      createLabel(defGroup, "Size adjuster PD adj.:", SWT.LEFT, 2, null);
      _sizeAdj = createText(defGroup, "0", false/*editable*/, 1);
      createLabel(defGroup, "", SWT.LEFT, 4, null);
      createLabel(defGroup, "Shield PD:", SWT.LEFT, 2, null);
      _pdShield = createText(defGroup, "0", false/*editable*/, 1);
      createLabel(defGroup, "Range", SWT.CENTER, 4, null);
      createLabel(defGroup, "Armor PD:", SWT.LEFT, 2, null);
      _pdArmor = createText(defGroup, "0", false/*editable*/, 1);
      createLabel(defGroup, "P.B.", SWT.CENTER, 1, null);
      createLabel(defGroup, "Short", SWT.CENTER, 1, null);
      createLabel(defGroup, "Med.", SWT.CENTER, 1, null);
      createLabel(defGroup, "Long", SWT.CENTER, 1, null);
      createBaseDefense(defGroup, "Passive Defense:", DefenseOption.DEF_PD, new FontData("Arial", 10, SWT.BOLD));
      createBaseDefense(defGroup, "Base Retreat:",    DefenseOption.DEF_RETREAT, null);
      createBaseDefense(defGroup, "Base Dodge:",      DefenseOption.DEF_DODGE, null);
      createBaseDefense(defGroup, "Base Block:",      DefenseOption.DEF_LEFT, null);
      createBaseDefense(defGroup, "Base Parry:",      DefenseOption.DEF_RIGHT, null);

      _defenseOpts.clear();
      for (RANGE range : RANGE.values()) {
         _defenseOpts.put(range, new HashMap<>());
      }

      for (int actions = 1 ; actions <= 3 ; actions++) {
         createLabel(defGroup, actions + "-action defenses:", SWT.LEFT, 7, new FontData("Arial", 10, SWT.BOLD));
         for (DefenseOptions defOptionToShow : OPTIONS_TO_SHOW.keySet()) {
            if (defOptionToShow.getDefenseActionsUsed() == actions) {
               createDefense(defGroup, OPTIONS_TO_SHOW.get(defOptionToShow), defOptionToShow);
            }
         }
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character)
   {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   }

   @Override
   public void refreshDisplay(Character character)
   {
      byte sizeAdjustForRace = 0;
      if (character != null) {
         sizeAdjustForRace = (byte) (-character.getRace().getBonusToBeHit());
      }
      _sizeAdj.setText((sizeAdjustForRace>=0) ? ("+"+sizeAdjustForRace) : String.valueOf(sizeAdjustForRace));
      if (character != null) {
         _pdArmor.setText(String.valueOf(character.getArmor().getPassiveDefense()));
      }
      int pdShields = 0;
      if (character != null) {
         for (LimbType limbType : character.getRace().getLimbSet()) {
            Limb limb = character.getLimb(limbType);
            if (limb != null) {
               Thing heldThing = limb.getHeldThing();
               if (heldThing != null) {
                  pdShields += heldThing.getPassiveDefense();
               }
            }
         }
      }
      _pdShield.setText(String.valueOf(pdShields));
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      HashMap<RANGE, HashMap<DefenseOption, Byte>> defBase = null;
      if (character != null) {
         defBase = character.getDefenseOptionsBase(DamageType.NONE, false/*isGrappleAttack*/,
                                                   false/*includeWoundPenalty*/, false/*includePosition*/,
                                                   false/*computePdOnly*/, (short)1/*distance*/);
      }
      for (RANGE range : RANGE.values()) {
         HashMap<DefenseOption, Text> baseDefMapToText = _baseDefenseOpt.get(range);
         for (DefenseOption defOption : baseDefMapToText.keySet()) {
            String value = "";
            if ((defOption != null) && (defBase != null)) {
               value = String.valueOf(defBase.get(range).get(defOption));
            }
            baseDefMapToText.get(defOption).setText(value);
         }

         HashMap<DefenseOptions, Text> defMapToText = _defenseOpts.get(range);
         for (DefenseOptions defOptions : defMapToText.keySet()) {
            Text text = defMapToText.get(defOptions);
            if (character == null) {
               text.setText("");
            }
            else {
               text.setText(String.valueOf(character.getBaseDefenseOptionTN(defBase, defOptions, range,
                                                                            false/*isGrappleAttack*/,
                                                                            DamageType.NONE,
                                                                            false/*includeWoundPenalty*/,
                                                                            false/*includePosition*/,
                                                                            false/*includeHolds*/)));
            }
         }
      }
      if (character != null) {
         Limb leftArm = character.getLimb(LimbType.HAND_LEFT);
         Limb rightArm = character.getLimb(LimbType.HAND_RIGHT);
         boolean leftDefenseRanged = (leftArm != null) && leftArm.canDefendAgainstRangedWeapons();
         boolean rightDefenseRanged = (rightArm != null) && rightArm.canDefendAgainstRangedWeapons();
         boolean leftDefense = (leftArm != null) && (leftArm.canDefend(character, false/*rangedAttack*/,
                                                                       (short) 0/*distance*/, false/*attackIsCharge*/,
                                                                       false/*grappleAttack*/, DamageType.NONE, false));
         boolean rightDefense = (rightArm != null) && (rightArm.canDefend(character, false/*rangedAttack*/,
                                                                          (short) 0/*distance*/, false/*attackIsCharge*/,
                                                                          false/*grappleAttack*/, DamageType.NONE, false));
         // If we are holding a two handed weapon, we can't use the left defense:
         if ((rightArm != null) && leftDefense) {
            Thing rightThing = rightArm.getHeldThing();
            if ((rightThing instanceof Weapon) &&
                ((Weapon) rightThing).isOnlyTwoHanded()) {
               leftDefense = false;
            }
         }
         // disable/enable all block defenses:
         for (RANGE range : RANGE.values()) {
            boolean isRanged = range != RANGE.OUT_OF_RANGE;
            HashMap<DefenseOption, Text> mapBaseDefOptToTn = _baseDefenseOpt.get(range);
            mapBaseDefOptToTn.get(DefenseOption.DEF_LEFT).setVisible(leftDefense && (!isRanged || leftDefenseRanged));
            mapBaseDefOptToTn.get(DefenseOption.DEF_RIGHT).setVisible(rightDefense && (!isRanged || rightDefenseRanged));
            mapBaseDefOptToTn.get(DefenseOption.DEF_RETREAT).setVisible(true);

            for (DefenseOptions defOptionToShow : OPTIONS_TO_SHOW.keySet()) {
               boolean visible = true;
               if (defOptionToShow.contains(DefenseOption.DEF_LEFT) && (!leftDefense || (isRanged && !leftDefenseRanged))) {
                  visible = false;
               }
               if (defOptionToShow.contains(DefenseOption.DEF_RIGHT) && (!rightDefense || (isRanged && !rightDefenseRanged))) {
                  visible = false;
               }
               HashMap<DefenseOptions, Text> mapDefOptsToTn = _defenseOpts.get(range);
               if ((mapDefOptsToTn != null) && (mapDefOptsToTn.containsKey(defOptionToShow))) {
                  mapDefOptsToTn.get(defOptionToShow).setVisible(visible);
               }
            }
         }
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character)
   {
   }

   private void createDefense(Composite parent, String name, DefenseOptions defType) {
      createLabel(parent, null, SWT.LEFT, 1, null);
      createLabel(parent, name, SWT.LEFT, 1, null);
      for (RANGE range : RANGE.values()) {
         HashMap<DefenseOptions, Text> defMapToText = _defenseOpts.get(range);
         defMapToText.put(defType, createText(parent, " ", false, 1));
      }
   }
   public void createBaseDefense(Composite parent, String name, DefenseOption defType, FontData fontData) {
      createLabel(parent, name, SWT.LEFT, 2, fontData);
      for (RANGE range : RANGE.values()) {
         if (!_baseDefenseOpt.containsKey(range)) {
            _baseDefenseOpt.put(range, new HashMap<>());
         }
         HashMap<DefenseOption, Text> rangeMap = _baseDefenseOpt.get(range);
         rangeMap.put(defType, createText(parent, " ", false, 1));
      }
   }
}
