package ostrowski.combat.common.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.IInstantaneousSpell;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.protocol.request.RequestAction;

public enum DefenseOption {
   DEF_PD                (0, "no defense",                      "does not defend"),
   DEF_DODGE             (1, "dodge",                           "dodges"),
   DEF_RETREAT           (2, "retreat",                         "retreats"),
   DEF_RIGHT             (1, "parry",                           "parries"),
   DEF_LEFT              (1, "block",                           "blocks"),
   DEF_RIGHT_2           (1, "parry",                           "parries"),
   DEF_LEFT_2            (1, "block",                           "blocks"),
   DEF_RIGHT_3           (1, "parry",                           "parries"),
   DEF_LEFT_3            (1, "block",                           "blocks"),
   DEF_MAGIC_1           (1, " spell (1 actions)",              null),
   DEF_MAGIC_2           (2, " spell (2 actions)",              null),
   DEF_MAGIC_3           (3, " spell (3 actions)",              null),
   DEF_MAGIC_4           (4, " spell (4 actions)",              null),
   DEF_MAGIC_5           (5, " spell (5 actions)",              null),
   DEF_COUNTER_GRAB_1    (1, "1-action counter-attack (grab)",  null),
   DEF_COUNTER_GRAB_2    (2, "2-action counter-attack (grab)",  null),
   DEF_COUNTER_GRAB_3    (3, "3-action counter-attack (grab)",  null),
   DEF_COUNTER_THROW_1   (1, "1-action counter-attack (throw)", null),
   DEF_COUNTER_THROW_2   (2, "2-action counter-attack (throw)", null),
   DEF_COUNTER_THROW_3   (3, "3-action counter-attack (throw)", null),
   DEF_COUNTER_DEFENSE_1 (1, "1-action counter-attack defense", null),
   DEF_COUNTER_DEFENSE_2 (2, "2-action counter-attack defense", null),
   DEF_COUNTER_DEFENSE_3 (3, "3-action counter-attack defense", null);

   private static final HashMap<DefenseOption, Integer> _MAGIC_POINTS_USED = new HashMap<>();
   private static final HashMap<DefenseOption, Integer> _DEFENSE_COUNTER_POINTS_USED = new HashMap<>();
   private static final List<List<DefenseOption>> _INCOMPATIBLE_SETS = new ArrayList<>();
   static {
      _MAGIC_POINTS_USED.put(DEF_MAGIC_1, 1);
      _MAGIC_POINTS_USED.put(DEF_MAGIC_2, 2);
      _MAGIC_POINTS_USED.put(DEF_MAGIC_3, 3);
      _MAGIC_POINTS_USED.put(DEF_MAGIC_4, 4);
      _MAGIC_POINTS_USED.put(DEF_MAGIC_5, 5);
      _DEFENSE_COUNTER_POINTS_USED.put(DEF_COUNTER_GRAB_1, 1);
      _DEFENSE_COUNTER_POINTS_USED.put(DEF_COUNTER_DEFENSE_2, 2);
      _DEFENSE_COUNTER_POINTS_USED.put(DEF_COUNTER_DEFENSE_3, 3);
      _INCOMPATIBLE_SETS.add(new ArrayList<>(Arrays.asList(DEF_DODGE,
                                                           DEF_RETREAT)));
      _INCOMPATIBLE_SETS.add(new ArrayList<>(Arrays.asList(DEF_RETREAT,
                                                           DEF_COUNTER_DEFENSE_1,
                                                           DEF_COUNTER_DEFENSE_2,
                                                           DEF_COUNTER_DEFENSE_3,
                                                           DEF_COUNTER_GRAB_1,
                                                           DEF_COUNTER_GRAB_2,
                                                           DEF_COUNTER_GRAB_3,
                                                           DEF_COUNTER_THROW_1,
                                                           DEF_COUNTER_THROW_2,
                                                           DEF_COUNTER_THROW_3)));
      _INCOMPATIBLE_SETS.add(new ArrayList<>(Arrays.asList(DEF_MAGIC_1,
                                                           DEF_MAGIC_2,
                                                           DEF_MAGIC_3,
                                                           DEF_MAGIC_4,
                                                           DEF_MAGIC_5,
                                                           DEF_COUNTER_DEFENSE_1,
                                                           DEF_COUNTER_DEFENSE_2,
                                                           DEF_COUNTER_DEFENSE_3)));
   }
   public boolean isCompatibleWith(DefenseOption otherOpt) {
      if (this == otherOpt) {
         return true;
      }
      for (List<DefenseOption> opts : _INCOMPATIBLE_SETS) {
         if (opts.contains(this) && opts.contains(otherOpt)) {
            return false;
         }
      }
      return true;
   }

   private int value;
   private byte actionsUsed;
   private String defenseName;
   private String pastTenseDefenseName;

   DefenseOption(int actionsUsed, String defenseName, String pastTenseDefenseName) {
      this.value = 1 << this.ordinal();
      this.actionsUsed = (byte) actionsUsed;
      this.defenseName = defenseName;
      this.pastTenseDefenseName = (pastTenseDefenseName == null) ? defenseName : pastTenseDefenseName;
   }
   public int getValue() {
      return this.value;
   }
   public byte getActionsUsed() {
      return actionsUsed;
   }
   public byte getDefenseMagicPointsUsed() {
      Integer points = _MAGIC_POINTS_USED.get(this);
      if (points == null) {
         return 0;
      }
      return points.byteValue();
   }
   public byte getDefenseCounterActionsUsed() {
      Integer points = _DEFENSE_COUNTER_POINTS_USED.get(this);
      if (points == null) {
         return 0;
      }
      return points.byteValue();
   }

   public String getName(boolean pastTense, Character actor, RequestAction attack) {
      int magicActions = getDefenseMagicPointsUsed();
      if (magicActions > 0) {
         IInstantaneousSpell defensiveSpell_spell  = actor._bestDefensiveSpell_spell;
         IInstantaneousSpell defensiveSpell_ranged = actor._bestDefensiveSpell_ranged;
         IInstantaneousSpell defensiveSpell_melee  = actor._bestDefensiveSpell_melee;
         if (attack != null) {
            if (attack.isCompleteSpell()) {
               if (defensiveSpell_spell != null) {
                  return defensiveSpell_spell.getName() + defenseName;
               }
               DebugBreak.debugBreak();
            }
            else if (attack.isRanged()) {
               if (defensiveSpell_ranged != null) {
                  return defensiveSpell_ranged.getName() + defenseName;
               }
               DebugBreak.debugBreak();
            }
            else {
               if (defensiveSpell_melee != null) {
                  return defensiveSpell_melee.getName() + defenseName;
               }
               DebugBreak.debugBreak();
            }
         }
         // If we only have one type of spell we can use for defense, use that spells name.
         if (defensiveSpell_melee != null) {
            if ((defensiveSpell_melee == defensiveSpell_ranged) || (defensiveSpell_ranged == null)) {
               if ((defensiveSpell_melee == defensiveSpell_spell) || (defensiveSpell_spell == null)) {
                  return defensiveSpell_melee.getName() + defenseName;
               }
            }
         }
         if (defensiveSpell_ranged != null) {
            if ((defensiveSpell_ranged == defensiveSpell_melee) || (defensiveSpell_melee == null)) {
               if ((defensiveSpell_ranged == defensiveSpell_spell) || (defensiveSpell_spell == null)) {
                  return defensiveSpell_ranged.getName() + defenseName;
               }
            }
         }
         if (defensiveSpell_spell != null) {
            if ((defensiveSpell_spell == defensiveSpell_melee) || (defensiveSpell_melee == null)) {
               if ((defensiveSpell_spell == defensiveSpell_ranged) || (defensiveSpell_ranged == null)) {
                  return defensiveSpell_spell.getName() + defenseName;
               }
            }
         }
         return "Magic " + defenseName;
      }
      String defName = "";
      String limbName = "";
      for (Hand limb : actor.getArms()) {
         if ((limb != null) && (this == limb.getDefOption())) {
            defName = limb.getDefenseName(pastTense, actor);
            limbName = " (" + limb.getName() + ")";
            break;
         }
      }
      if ((defName == null) || (defName.length() == 0)) {
         return pastTense ? pastTenseDefenseName : defenseName;
      }
      // If any other hand has the same defense name, qualify the name with the hand name
      for (Hand limb : actor.getArms()) {
         if (this != limb.getDefOption()) {
            if (defName.equals(limb.getDefenseName(pastTense, actor))) {
               return defName + limbName;
            }
         }
      }
      return defName;

   }
   public static DefenseOption getByValue(int value) {
      for (DefenseOption opt : DefenseOption.values()) {
         if (opt.value == value) {
            return opt;
         }
      }
      return null;
   }
   public boolean isCounterAttackGrab() {
      return (this == DEF_COUNTER_GRAB_1) ||
             (this == DEF_COUNTER_GRAB_2) ||
             (this == DEF_COUNTER_GRAB_3);
   }
   public boolean isCounterAttackThrow() {
      return (this == DEF_COUNTER_THROW_1) ||
             (this == DEF_COUNTER_THROW_2) ||
             (this == DEF_COUNTER_THROW_3);
   }
   public boolean isCounterAttack() {
      return isCounterAttackGrab() || isCounterAttackThrow();
   }
}
