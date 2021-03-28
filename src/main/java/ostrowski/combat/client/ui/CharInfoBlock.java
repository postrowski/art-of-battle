/*
 * Created on May 31, 2006
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CharInfoBlock extends Helper implements IUIBlock, ModifyListener
{
   private Combo targetName;
   private Text distance;
   private Text attributes1;
   private Text attributes2;
   private Text weapon;
   private Text rightHandSkill;
   private Text leftHandSkill;
   private Text shield;
   private Text armor;
   private Text buildImp;
   private Text buildCut;
   private Text buildBlunt;
   private Text raceName;
   private Text position;
   private Text actions;
   private Text readyTime;
   private Text painAndWounds;
   private Text magicPoints;

   private       Character        self;
   private       List<Character>  combatants;
   private final CharacterDisplay display;

   public CharInfoBlock(CharacterDisplay display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group group = createGroup(parent, "Character Info", 4/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "name:", SWT.RIGHT, 1, null);
      targetName = createCombo(group, SWT.READ_ONLY, 3, new ArrayList<>());

      createLabel(group, "race:", SWT.RIGHT, 1, null);
      raceName = createText(group, Race.NAME_Human, false, 3);

      createLabel(group, "position (enc):", SWT.RIGHT, 1, null);
      position = createText(group, Position.STANDING.name + " (0)", false, 3);

      createLabel(group, "actions rnd/turn:", SWT.RIGHT, 1, null);
      actions = createText(group, "3 / 5", false, 3);

      createLabel(group, "pain / wounds:", SWT.RIGHT, 1, null);
      painAndWounds = createText(group, "0 / 0", false, 3);

      createLabel(group, "mage/prst pnts:", SWT.RIGHT, 1, null);
      magicPoints = createText(group, "0 / 0", false, 3);

      // If display is null, then this is a server-side object, so there will never
      // be a self object with which to measure distances to/from.
      if (display != null) {
         createLabel(group, "distance:", SWT.RIGHT, 1, null);
         distance = createText(group, "0", false, 3);
      }
      else {
         createLabel(group, "right skill:", SWT.RIGHT, 1, null);
         // server display - show skill levels
         rightHandSkill = createText(group, " ", false, 3);
         createLabel(group, "left skill:", SWT.RIGHT, 1, null);
         // server display - show skill levels
         leftHandSkill = createText(group, " ", false, 3);
         createLabel(group, "attributes:", SWT.CENTER, 4, null);
         String attrStrings1 = getAttrString1(null);
         String attrStrings2 = getAttrString2(null);
         attributes1 = createText(group, attrStrings1, false, 4);
         attributes2 = createText(group, attrStrings2, false, 4);
      }

      createLabel(group, "weapon/ready:", SWT.RIGHT, 1, null);
      weapon = createText(group, "none", false, 2);
      readyTime = createText(group, "ready", false, 1);

      createLabel(group, "shield:", SWT.RIGHT, 1, null);
      shield = createText(group, "none", false, 3);
      createLabel(group, "armor:", SWT.RIGHT, 1, null);
      armor = createText(group, "none", false, 3);

      createLabel(group, "", SWT.RIGHT, 1, null);
      createLabel(group, "imp", SWT.CENTER, 1, null);
      createLabel(group, "cut", SWT.CENTER, 1, null);
      createLabel(group, "blunt", SWT.CENTER, 1, null);

      createLabel(group, "build-vs:", SWT.RIGHT, 1, null);
      buildImp = createText(group, "0", false, 1);
      buildCut = createText(group, "0", false, 1);
      buildBlunt = createText(group, "0", false, 1);

      if (display == null) {
         createLabel(group, "distance:", SWT.RIGHT, 1, null);
         distance = createText(group, "0", false, 3);
      }

      targetName.addModifyListener(this);
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

         String attrName = att.shortName.charAt(0) + att.shortName.substring(1).toLowerCase();
         sb.append(attrName);
         sb.append(':');
         sb.append((target == null) ? 0 : target.getAttributeLevel(att));
         if ((att == Attribute.Strength) && (target != null) && (target.getAdjustedStrength() != target.getAttributeLevel(Attribute.Strength))) {
            sb.append("(").append(target.getAdjustedStrength()).append(")");
         }
         else if ((att == Attribute.Health) && (target != null) && (target.getBuildBase() != target.getAttributeLevel(Attribute.Health))) {
            sb.append("(").append(target.getBuildBase()).append(")");
         }
         else {
            sb.append("   ");
         }
      }
      return sb.toString();
   }

   public void updateTargetFromCharacter(Character target) {
      targetName.setText(target.getName());
      if (display != null) {
         Orientation orient = display.charWidget.character.getOrientation();
         if ((orient != null) && (orient.getCoordinates().size() > 0)) {
            short minDist = Arena.getMinDistance(display.charWidget.character, target);
            short maxDist = Arena.getMaxDistance(display.charWidget.character, target);
            StringBuilder dist = new StringBuilder();
            dist.append(String.valueOf(minDist));
            if (minDist != maxDist) {
               dist.append(" - ").append(String.valueOf(maxDist));
            }
            distance.setText(dist.toString());
         }
      }
      if ((attributes1 != null) && (attributes2 != null)) {
         attributes1.setText(getAttrString1(target));
         attributes2.setText(getAttrString2(target));
      }
      Limb rightHand = target.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = target.getLimb(LimbType.HAND_LEFT);
      Limb head = target.getLimb(LimbType.HEAD);
      String weaponName = (rightHand == null) ? "" : rightHand.getHeldThingName();
      weapon.setText(weaponName);
      if (rightHandSkill != null) {
         if (rightHand != null) {
            rightHandSkill.setText(getSkillDescriptionForHand(rightHand, target));
         }
         else {
            if (head != null) {
               rightHandSkill.setText(getSkillDescriptionForHand(head, target));
               head = null;
            }
            else {
               rightHandSkill.setText("");
            }
         }
      }
      if (leftHandSkill != null) {
         if (leftHand != null) {
            leftHandSkill.setText(getSkillDescriptionForHand(leftHand, target));
         }
         else {
            if (head != null) {
               leftHandSkill.setText(getSkillDescriptionForHand(head, target));
            }
            else {
               leftHandSkill.setText("");
            }
         }
      }

      shield.setText((leftHand == null) ? "" : leftHand.getHeldThingName());
      armor.setText(target.getArmor().getName());
      buildImp.setText(String.valueOf(target.getBuild(DamageType.IMP)));
      buildCut.setText(String.valueOf(target.getBuild(DamageType.CUT)));
      buildBlunt.setText(String.valueOf(target.getBuild(DamageType.BLUNT)));
      painAndWounds.setText(getPainAndWoundsText(target, false));

      magicPoints.setText(target.getCondition().getMageSpellPointsAvailable() + " / " + target.getCondition().getPriestSpellPointsAvailable());
      raceName.setText(target.getRace().getName());
      position.setText(target.getPositionName() + " (" + Rules.getEncumbranceLevel(target) + ")");
      actions.setText(getActionsText(target.getCondition()));
      readyTime.setText(getReadyTime(rightHand));
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

   public static String getPainAndWoundsText(Character target, boolean extendedText) {
      if (!target.getCondition().isAlive()) {
         return "DEAD";
      }
      if (!target.getCondition().isConscious()) {
         return "K.O.";
      }

      Limb rightHand = target.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = target.getLimb(LimbType.HAND_LEFT);
      StringBuilder sb = new StringBuilder();
      if (target.getCondition().isCollapsed()) {
         sb.append("Collapsed ");
      }
      byte pain = target.getPainPenalty(false/*accountForBerserking*/);
      if (extendedText) {
         if (pain> 0) {
            sb.append("pain ").append(pain);
         }
      }
      else {
         sb.append(pain);
      }
      if ((pain > 0) && target.isBerserking()) {
         if (extendedText) {
            sb.append("(0 for Berserking)");
         }
         else {
            sb.append("(0)");
         }
      }
      int wounds = target.getWounds();
      if (extendedText) {
         if (wounds> 0) {
            if (pain > 0) {
               sb.append(", ");
            }
            sb.append("wounds ").append(wounds);
         }
      }
      else {
         sb.append(" / ").append(wounds);
      }
      if (wounds > 0) {
         sb.append(" (");
         boolean first = true;
         for (Wound wnd : target.getCondition().getWoundsList()) {
            byte effectiveWounds = wnd.getEffectiveWounds();
            if (effectiveWounds <= 0) {
               continue;
            }
            if (!first) {
               sb.append("+");
            }
            first = false;
            sb.append(effectiveWounds);
         }
         sb.append(")");
      }
      if (leftHand != null) {
         if (leftHand.isCrippled()) {
            if (extendedText) {
               sb.append("\nleft hand crippled");
            } else {
               sb.append("!");
            }
         }
         else {
            byte leftPenalty = leftHand.getWoundPenalty();
            if (extendedText) {
               if (leftPenalty > 0) {
                  sb.append("\nleft hand penalty ").append(leftPenalty);
               }
            } else {
               while (leftPenalty-- > 0) {
                  sb.append("`");
               }
            }
         }
      }

      if (rightHand != null) {
         if (rightHand.isCrippled()) {
            if (extendedText) {
               sb.append("\nright hand crippled");
            } else {
               sb.append("!");
            }
         }
         else {
            byte rightPenalty = rightHand.getWoundPenalty();
            if (extendedText) {
               if (rightPenalty > 0) {
                  sb.append("\nright hand penalty ").append(rightPenalty);
               }
            } else {
               while (rightPenalty-- > 0) {
                  sb.append("'");
               }
            }
         }
      }

      byte movePenalty = target.getCondition().getPenaltyMove();
      if (extendedText) {
         if (movePenalty > 0) {
            sb.append("\nmovement penalty ").append(movePenalty);
         }
      }
      else {
         while (movePenalty-- > 0) {
            sb.append(",");
         }
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
      self = character;
   }

   @Override
   public void refreshDisplay(Character character) {
      self = character;
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      self = character;
   }

   public void updateCombatants(List<Character> combatants) {
      this.combatants = combatants;
      if (!targetName.isDisposed()) {
         String previousSelection = targetName.getText();
         targetName.removeAll();
         TreeSet<String> names = new TreeSet<>();
         for (Character combatant : this.combatants) {
            if ((self == null) || (!combatant.getName().equals(self.getName()))) {
               if (combatant.getCondition().isConscious()) {
                  names.add(combatant.getName());
               }
            }
         }
         for (String name : names) {
            targetName.add(name);
         }

         if ((previousSelection == null) || (previousSelection.length() == 0)) {
            if ((targetName != null) && (targetName.getItemCount() > 0)) {
               previousSelection = targetName.getItem(0);
            }
         }
         if ((previousSelection != null) && (previousSelection.length() > 0)) {
            targetName.setText(previousSelection);
         }
      }
   }

   public void updateCombatant(Character character) {
      for (Character combatant : combatants) {
         if (combatant.uniqueID == character.uniqueID) {
            combatant.copyData(character);
            updateCombatants(combatants);
            return;
         }
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
      // prevent an infinite loop:
      if (!CharacterWidget.inModify) {
         CharacterWidget.inModify = true;
         if (e.widget == targetName) {
            setTargetName(targetName.getText());
            if (display != null) {
               display.refreshDisplay();
            }
         }
         CharacterWidget.inModify = false;
      }
   }

   public int getTargetUniqueID() {
      if (combatants != null) {
         for (Character combatant : combatants) {
            if (combatant.getName().equals(targetName.getText())) {
               return combatant.uniqueID;
            }
         }
      }
      return -1;
   }

   public void setTargetName(String targetName) {
      for (Character combatant : combatants) {
         if (combatant.getName().equals(targetName)) {
            updateTargetFromCharacter(combatant);
            break;
         }
      }
   }

   public static Object getToolTipSummary(Character target) {
      StringBuilder sb = new StringBuilder();
      sb.append("Character: ").append(target.getName())
        .append(" (").append(target.getPointTotal()).append(" point ")
        .append(target.getRace().getName()).append(")");

      sb.append("\n  Actions rnd/turn: ").append(getActionsText(target.getCondition()));

      Limb rightHand = target.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = target.getLimb(LimbType.HAND_LEFT);
      sb.append("\n  Right skill: ").append(getSkillDescriptionForHand(rightHand, target));
      sb.append("\n  Left skill:  ").append(getSkillDescriptionForHand(leftHand, target));
      sb.append("\n    ").append(getAttrString1(target));
      sb.append("\n    ").append(getAttrString2(target));

      sb.append("\n  pain/wounds:   ").append(getPainAndWoundsText(target, true));
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
