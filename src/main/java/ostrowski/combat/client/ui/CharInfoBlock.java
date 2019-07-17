/*
 * Created on May 31, 2006
 */
package ostrowski.combat.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.Condition;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;
import ostrowski.ui.Helper;

public class CharInfoBlock extends Helper implements IUIBlock, ModifyListener
{
   private Combo            _targetName;
   private Text             _distance;
   private Text             _attributes1;
   private Text             _attributes2;
   private Text             _weapon;
   private Text             _rightHandSkill;
   private Text             _leftHandSkill;
   private Text             _shield;
   private Text             _armor;
   private Text             _buildImp;
   private Text             _buildCut;
   private Text             _buildBlunt;
   private Text             _raceName;
   private Text             _position;
   private Text             _actions;
   private Text             _readyTime;
   private Text             _painAndWounds;
   private Text             _magicPoints;

   private Character        _self;
   private List<Character>  _combatants;
   private final CharacterDisplay _display;

   public CharInfoBlock(CharacterDisplay display) {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group group = createGroup(parent, "Character Info", 4/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "name:", SWT.RIGHT, 1, null);
      _targetName = createCombo(group, SWT.READ_ONLY, 3, new ArrayList<String>());

      createLabel(group, "race:", SWT.RIGHT, 1, null);
      _raceName = createText(group, Race.NAME_Human, false, 3);

      createLabel(group, "position (enc):", SWT.RIGHT, 1, null);
      _position = createText(group, Position.STANDING.name + " (0)", false, 3);

      createLabel(group, "actions rnd/turn:", SWT.RIGHT, 1, null);
      _actions = createText(group, "3 / 5", false, 3);

      createLabel(group, "pain / wounds:", SWT.RIGHT, 1, null);
      _painAndWounds = createText(group, "0 / 0", false, 3);

      createLabel(group, "mage/prst pnts:", SWT.RIGHT, 1, null);
      _magicPoints = createText(group, "0 / 0", false, 3);

      // If display is null, then this is a server-side object, so there will never
      // be a self object with which to measure distances to/from.
      if (_display != null) {
         createLabel(group, "distance:", SWT.RIGHT, 1, null);
         _distance = createText(group, "0", false, 3);
      }
      else {
         createLabel(group, "right skill:", SWT.RIGHT, 1, null);
         // server display - show skill levels
         _rightHandSkill = createText(group, " ", false, 3);
         createLabel(group, "left skill:", SWT.RIGHT, 1, null);
         // server display - show skill levels
         _leftHandSkill = createText(group, " ", false, 3);
         createLabel(group, "attributes:", SWT.CENTER, 4, null);
         String attrStrings1 = getAttrString1(null);
         String attrStrings2 = getAttrString2(null);
         _attributes1 = createText(group, attrStrings1, false, 4);
         _attributes2 = createText(group, attrStrings2, false, 4);
      }

      createLabel(group, "weapon/ready:", SWT.RIGHT, 1, null);
      _weapon = createText(group, "none", false, 2);
      _readyTime = createText(group, "ready", false, 1);

      createLabel(group, "shield:", SWT.RIGHT, 1, null);
      _shield = createText(group, "none", false, 3);
      createLabel(group, "armor:", SWT.RIGHT, 1, null);
      _armor = createText(group, "none", false, 3);

      createLabel(group, "", SWT.RIGHT, 1, null);
      createLabel(group, "imp", SWT.CENTER, 1, null);
      createLabel(group, "cut", SWT.CENTER, 1, null);
      createLabel(group, "blunt", SWT.CENTER, 1, null);

      createLabel(group, "build-vs:", SWT.RIGHT, 1, null);
      _buildImp = createText(group, "0", false, 1);
      _buildCut = createText(group, "0", false, 1);
      _buildBlunt = createText(group, "0", false, 1);

      if (_display == null) {
         createLabel(group, "distance:", SWT.RIGHT, 1, null);
         _distance = createText(group, "0", false, 3);
      }

      _targetName.addModifyListener(this);
   }

   public static String getAttrString1(Character target) {
      return getAttrString(target, Attribute.Strength, Attribute.Toughness);
   }

   public static String getAttrString2(Character target) {
      return getAttrString(target, Attribute.Intelligence, Attribute.Social);
   }

   private static String getAttrString(Character target, Attribute start, Attribute end) {
      StringBuilder sb = new StringBuilder();
      for (Attribute att : Attribute.values()) {
         if ((att.value < start.value) || (att.value > end.value)) {
            continue;
         }
         if (sb.length() > 0) {
            sb.append("   ");
         }
         StringBuilder attrName = new StringBuilder();
         attrName.append(att.shortName.charAt(0));
         attrName.append(att.shortName.substring(1).toLowerCase());

         sb.append(attrName);
         sb.append(':');
         sb.append((target == null) ? 0 : target.getAttributeLevel(att));
         if ((att == Attribute.Strength) && (target != null) && (target.getAdjustedStrength() != target.getAttributeLevel(Attribute.Strength))) {
            sb.append("(").append((target == null) ? 0 : target.getAdjustedStrength()).append(")");
         }
         else if ((att == Attribute.Health) && (target != null) && (target.getBuildBase() != target.getAttributeLevel(Attribute.Health))) {
            sb.append("(").append((target == null) ? 0 : target.getBuildBase()).append(")");
         }
         else {
            sb.append("   ");
         }
      }
      return sb.toString();
   }

   public void updateTargetFromCharacter(Character target) {
      _targetName.setText(target.getName());
      if (_display != null) {
         Orientation orient = _display._charWidget._character.getOrientation();
         if ((orient != null) && (orient.getCoordinates().size() > 0)) {
            short minDist = Arena.getMinDistance(_display._charWidget._character, target);
            short maxDist = Arena.getMaxDistance(_display._charWidget._character, target);
            StringBuilder dist = new StringBuilder();
            dist.append(String.valueOf(minDist));
            if (minDist != maxDist) {
               dist.append(" - ").append(String.valueOf(maxDist));
            }
            _distance.setText(dist.toString());
         }
      }
      if ((_attributes1 != null) && (_attributes2 != null)) {
         _attributes1.setText(getAttrString1(target));
         _attributes2.setText(getAttrString2(target));
      }
      Limb rightHand = target.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = target.getLimb(LimbType.HAND_LEFT);
      Limb head = target.getLimb(LimbType.HEAD);
      String weaponName = (rightHand == null) ? "" : rightHand.getHeldThingName();
      _weapon.setText(weaponName);
      if (_rightHandSkill != null) {
         if (rightHand != null) {
            _rightHandSkill.setText(getSkillDescriptionForHand(rightHand, target));
         }
         else {
            if (head != null) {
               _rightHandSkill.setText(getSkillDescriptionForHand(head, target));
               head = null;
            }
            else {
               _rightHandSkill.setText("");
            }
         }
      }
      if (_leftHandSkill != null) {
         if (leftHand != null) {
            _leftHandSkill.setText(getSkillDescriptionForHand(leftHand, target));
         }
         else {
            if (head != null) {
               _leftHandSkill.setText(getSkillDescriptionForHand(head, target));
            }
            else {
               _leftHandSkill.setText("");
            }
         }
      }

      _shield.setText((leftHand == null) ? "" : leftHand.getHeldThingName());
      _armor.setText(target.getArmor().getName());
      _buildImp.setText(String.valueOf(target.getBuild(DamageType.IMP)));
      _buildCut.setText(String.valueOf(target.getBuild(DamageType.CUT)));
      _buildBlunt.setText(String.valueOf(target.getBuild(DamageType.BLUNT)));
      _painAndWounds.setText(getPainAndWoundsText(target));

      _magicPoints.setText(target.getCondition().getMageSpellPointsAvailable() + " / " + target.getCondition().getPriestSpellPointsAvailable());
      _raceName.setText(target.getRace().getName());
      _position.setText(target.getPositionName() + " (" + Rules.getEncumbranceLevel(target) + ")");
      _actions.setText(getActionsText(target.getCondition()));
      _readyTime.setText(getReadyTime(rightHand));
   }

   private static String getReadyTime(Limb rightHand) {
      byte actionsNeeded = (rightHand == null) ? 0 : rightHand.getActionsNeededToReady();
      if (actionsNeeded == 0) {
         return "ready";
      }
      return "" + actionsNeeded + " actions";
   }

   private static String getActionsText(Condition condition) {
      byte actionsThisRoundDef = condition.getActionsAvailableThisRound(true/*usedForDefenseOnly*/);
      byte actionsThisTurnAny = condition.getActionsAvailable(false/*usedForDefenseOnly*/);
      byte actionsThisTurnDef = condition.getActionsAvailable(true/*usedForDefenseOnly*/);
      StringBuilder actions = new StringBuilder().append(actionsThisRoundDef).append(" / ").append(actionsThisTurnDef);
      if (actionsThisTurnAny != actionsThisTurnDef) {
         actions.append("(def)");
      }
      return actions.toString();
   }

   public static String getPainAndWoundsText(Character target) {
      if (!target.getCondition().isAlive()) {
         return "DEAD";
      }
      if (!target.getCondition().isConscious()) {
         return "K.O.";
      }
      if (target.getCondition().isCollapsed()) {
         return "Collapsed";
      }

      Limb rightHand = target.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = target.getLimb(LimbType.HAND_LEFT);
      StringBuilder sb = new StringBuilder();
      byte pain = target.getPainPenalty(false/*accountForBerserking*/);
      sb.append(pain);
      if ((pain > 0) && target.isBerserking()) {
         sb.append("(0)");
      }
      int wounds = target.getWounds();
      sb.append(" / ").append(wounds);
      if (wounds > 0) {
         sb.append(" (");
         boolean first = true;
         for (Wound wnd : target.getCondition().getWoundsList()) {
            if (!first) {
               sb.append("+");
            }
            first = false;
            sb.append(wnd.getEffectiveWounds());
         }
         sb.append(")");
      }
      if (leftHand != null) {
         if (leftHand.isCrippled()) {
            sb.append("!");
         }
         else {
            byte leftPenalty = leftHand.getWoundPenalty();
            while (leftPenalty-- > 0) {
               sb.append("`");
            }
         }
      }

      if (rightHand != null) {
         if (rightHand.isCrippled()) {
            sb.append("!");
         }
         else {
            byte rightPenalty = rightHand.getWoundPenalty();
            while (rightPenalty-- > 0) {
               sb.append("'");
            }
         }
      }

      byte movePenalty = target.getCondition().getPenaltyMove();
      while (movePenalty-- > 0) {
         sb.append(",");
      }

      return sb.toString();
   }

   public static String getSkillDescriptionForHand(Limb hand, Character target) {
      if (hand != null) {
         Weapon weapon = hand.getWeapon(target);
         if (weapon != null) {
            Skill bestSkill = target.getBestSkill(weapon);
            if (bestSkill != null) {
               return bestSkill.getName() + ':' + bestSkill.getLevel();
            }
         }
         else {
            Thing thing = hand.getHeldThing();
            if (thing instanceof Shield) {
               byte shieldLevel = target.getSkillLevel(SkillType.Shield, null/*useLimb*/, false, false/*adjustForEncumbrance*/, true/*adjustForHolds*/);
               if (shieldLevel > 0) {
                  return SkillType.Shield.getName() + ':' + shieldLevel;
               }
            }
         }
      }
      return "";
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      _self = character;
   }

   @Override
   public void refreshDisplay(Character character) {
      _self = character;
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      _self = character;
   }

   public void updateCombatants(List<Character> combatants) {
      _combatants = combatants;
      if (!_targetName.isDisposed()) {
         String previousSelection = _targetName.getText();
         _targetName.removeAll();
         TreeSet<String> names = new TreeSet<>();
         for (Character combatant : _combatants) {
            if ((_self == null) || (!combatant.getName().equals(_self.getName()))) {
               if (combatant.getCondition().isConscious()) {
                  names.add(combatant.getName());
               }
            }
         }
         for (String name : names) {
            _targetName.add(name);
         }

         if ((previousSelection == null) || (previousSelection.length() == 0)) {
            if ((_targetName != null) && (_targetName.getItemCount() > 0)) {
               previousSelection = _targetName.getItem(0);
            }
         }
         if ((previousSelection != null) && (previousSelection.length() > 0)) {
            _targetName.setText(previousSelection);
         }
      }
   }

   public void updateCombatant(Character character) {
      for (Character combatant : _combatants) {
         if (combatant._uniqueID == character._uniqueID) {
            combatant.copyData(character);
            updateCombatants(_combatants);
            return;
         }
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
      // prevent an infinite loop:
      if (!CharacterWidget._inModify) {
         CharacterWidget._inModify = true;
         if (e.widget == _targetName) {
            setTargetName(_targetName.getText());
            if (_display != null) {
               _display.refreshDisplay();
            }
         }
         CharacterWidget._inModify = false;
      }
   }

   public int getTargetUniqueID() {
      if (_combatants != null) {
         for (Character combatant : _combatants) {
            if (combatant.getName().equals(_targetName.getText())) {
               return combatant._uniqueID;
            }
         }
      }
      return -1;
   }

   public void setTargetName(String targetName) {
      for (Character combatant : _combatants) {
         if (combatant.getName().equals(targetName)) {
            updateTargetFromCharacter(combatant);
            break;
         }
      }
   }

   public static Object getToolTipSummary(Character target) {
      StringBuilder sb = new StringBuilder();
      sb.append("Character: ").append(target.getName());
      sb.append(" (").append(target.getRace().getName()).append(")");

      sb.append("\n  Actions rnd/turn: ").append(getActionsText(target.getCondition()));

      Limb rightHand = target.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = target.getLimb(LimbType.HAND_LEFT);
      sb.append("\n  Right skill: ").append(getSkillDescriptionForHand(rightHand, target));
      sb.append("\n  Left skill:  ").append(getSkillDescriptionForHand(leftHand, target));
      sb.append("\n    ").append(getAttrString1(target));
      sb.append("\n    ").append(getAttrString2(target));

      sb.append("\n  pain/wounds:   ").append(getPainAndWoundsText(target));
      if (target.getWeapon() != null) {
         sb.append("\n  ").append(target.getWeapon().getName()).append(": ").append(getReadyTime(rightHand));
      }
      if (leftHand != null) {
         sb.append("\n  Sheild: ").append(leftHand.getHeldThingName());
      }
      sb.append("\n  Armor:  ").append(target.getArmor().getName());

      Position pos = target.getPosition();
      if (pos != Position.STANDING) {
         sb.append("\n  ").append(pos.getName());
      }
      return sb.toString();
   }

}
