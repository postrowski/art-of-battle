package ostrowski.combat.protocol.request;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import ostrowski.combat.common.enums.Enums.Side;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SyncRequest;

public enum RequestActionType {
   OPT_NO_ACTION                      (0, 1, ""),
   OPT_CANCEL_ACTION                  (0, 0, "<esc>"),
   OPT_MOVE                           (0, 1, "m"),
   OPT_MOVE_EVASIVE                   (0, 1, "M"),
   OPT_CLOSE_AND_ATTACK_1             (1, 2, "cC<ctrl>c<ctrl>C<alt>c<alt>C"),
   OPT_CLOSE_AND_ATTACK_2             (2, 3, "cC<ctrl>c<ctrl>C<alt>c<alt>C"),
   OPT_CLOSE_AND_ATTACK_3             (3, 4, "cC<ctrl>c<ctrl>C<alt>c<alt>C"),
   OPT_ATTACK_MELEE_1                 (1, 1, "a","l","k","h"),
   OPT_ATTACK_MELEE_2                 (2, 2, "A","L","K","H"),
   OPT_ATTACK_MELEE_3                 (3, 3, "<ctrl>a","<ctrl>l","<ctrl>k","<ctrl>h"),
   OPT_ATTACK_THROW_1                 (1, 1, "at"),
   OPT_ATTACK_THROW_2                 (2, 2, "AT"),
   OPT_ATTACK_THROW_3                 (3, 3, "<ctrl>a<ctrl>t"),
   OPT_ATTACK_MISSILE                 (1, 1, "aAtT"),
   OPT_PREPARE_RANGED                 (0, 1, "p"),
   OPT_TARGET_ENEMY                   (0, 1, "g"),
   OPT_READY_1                        (0, 1, "rR<ctrl>r<ctrl>R<alt>r<alt>R"),
   OPT_READY_2                        (0, 2, "rR<ctrl>r<ctrl>R<alt>r<alt>R"),
   OPT_READY_3                        (0, 3, "rR<ctrl>r<ctrl>R<alt>r<alt>R"),
   OPT_READY_4                        (0, 4, "rR<ctrl>r<ctrl>R<alt>r<alt>R"),
   OPT_READY_5                        (0, 5, "rR<ctrl>r<ctrl>R<alt>r<alt>R"),
   OPT_ON_GAURD                       (0, 0, "d"),
   OPT_FINAL_DEFENSE_1                (0, 1, "D"),
   OPT_FINAL_DEFENSE_2                (0, 2, "D"),
   OPT_FINAL_DEFENSE_3                (0, 3, "D"),
   OPT_FINAL_DEFENSE_4                (0, 4, "D"),
   OPT_FINAL_DEFENSE_5                (0, 5, "D"),
   OPT_CHANGE_POS                     (0, 2, "P"),
   OPT_CHANGE_POS_STAND               (0, 2, "s"),
   OPT_CHANGE_POS_KNEEL               (0, 2, "k"),
   OPT_CHANGE_POS_CROUCH              (0, 2, "c"),
   OPT_CHANGE_POS_SIT                 (0, 2, "t"),
   OPT_CHANGE_POS_LAYDOWN_BACK        (0, 2, "b"),
   OPT_CHANGE_POS_LAYDOWN_FRONT       (0, 2, "f"),
   OPT_WAIT_TO_ATTACK                 (0, 0, "w"),
   OPT_EQUIP_UNEQUIP                  (0, 2, "q"), // Total actions is variable!
   OPT_CHANGE_TARGET_PRIORITIES       (0, 0, "k"),
   OPT_LOCATION_ACTION_0              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_1              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_2              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_3              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_4              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_5              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_6              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_7              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_8              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_9              (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_10             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_11             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_12             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_13             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_14             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_15             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_16             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_17             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_18             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_19             (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_FORWARD_OPEN   (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_FORWARD_CLOSE  (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_FORWARD_UNLOCK (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_FORWARD_LOCK   (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_FORWARD_ASSIST (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_RIGHT_OPEN     (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_RIGHT_CLOSE    (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_RIGHT_UNLOCK   (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_RIGHT_LOCK     (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_RIGHT_ASSIST   (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_LEFT_OPEN      (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_LEFT_CLOSE     (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_LEFT_UNLOCK    (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_LEFT_LOCK      (0, 1, "1234567890"),
   OPT_LOCATION_ACTION_LEFT_ASSIST    (0, 1, "1234567890"),
   OPT_CHARGE_ATTACK_1                (1, 2, "h"),
   OPT_CHARGE_ATTACK_2                (2, 3, "H"),
   OPT_CHARGE_ATTACK_3                (3, 4, "<ctrl>h"),
   OPT_BEGIN_SPELL                    (0, 1, "s"),
   OPT_CONTINUE_INCANTATION           (0, 1, "i"),
   OPT_CHANNEL_ENERGY_1               (0, 1, "1"),
   OPT_CHANNEL_ENERGY_2               (0, 2, "2"),
   OPT_CHANNEL_ENERGY_3               (0, 3, "3"),
   OPT_CHANNEL_ENERGY_4               (0, 4, "4"),
   OPT_CHANNEL_ENERGY_5               (0, 5, "5"),
   OPT_MAINTAIN_SPELL                 (0, 1, "i"),
   OPT_DISCARD_SPELL                  (0, 0, "X"),
   OPT_COMPLETE_SPELL_1               (1, 1, "s"),
   OPT_COMPLETE_SPELL_2               (2, 2, "S"),
   OPT_COMPLETE_SPELL_3               (3, 3, "<ctrl>s"),
   OPT_COMPLETE_PRIEST_SPELL_1        (1, 1, "1"),
   OPT_COMPLETE_PRIEST_SPELL_2        (1, 1, "2"),
   OPT_COMPLETE_PRIEST_SPELL_3        (1, 1, "3"),
   OPT_COMPLETE_PRIEST_SPELL_4        (1, 1, "4"),
   OPT_COMPLETE_PRIEST_SPELL_5        (1, 1, "5"),
   OPT_PREPARE_INITATE_SPELL_1        (0, 1, "1!"),
   OPT_PREPARE_INITATE_SPELL_2        (0, 1, "2@"),
   OPT_PREPARE_INITATE_SPELL_3        (0, 1, "3#"),
   OPT_PREPARE_INITATE_SPELL_4        (0, 1, "4$"),
   OPT_PREPARE_INITATE_SPELL_5        (0, 1, "5%"),
   OPT_APPLY_ITEM                     (0, 1, "y"),
   OPT_ATTACK_GRAPPLE_1               (1, 1, "g"),
   OPT_ATTACK_GRAPPLE_2               (2, 2, "G"),
   OPT_ATTACK_GRAPPLE_3               (3, 3, "<ctrl>g"),
   OPT_CLOSE_AND_GRAPPLE_1            (1, 2, "g"),
   OPT_CLOSE_AND_GRAPPLE_2            (2, 3, "G"),
   OPT_CLOSE_AND_GRAPPLE_3            (3, 4, "<ctrl>g"),
   OPT_COUNTER_ATTACK_GRAB_1          (1, 1, ""),
   OPT_COUNTER_ATTACK_GRAB_2          (2, 2, ""),
   OPT_COUNTER_ATTACK_GRAB_3          (3, 3, ""),
   OPT_COUNTER_ATTACK_THROW_1         (1, 1, ""),
   OPT_COUNTER_ATTACK_THROW_2         (2, 2, ""),
   OPT_COUNTER_ATTACK_THROW_3         (3, 3, ""),
   OPT_BREAK_FREE_1                   (0, 1, "b"),
   OPT_BREAK_FREE_2                   (0, 2, "B"),
   OPT_BREAK_FREE_3                   (0, 3, "<ctrl>b"),
   OPT_MAINTAIN_HOLD_1                (0, 1, "1"),
   OPT_MAINTAIN_HOLD_2                (0, 2, "2"),
   OPT_MAINTAIN_HOLD_3                (0, 3, "3"),
   OPT_MAINTAIN_HOLD_4                (0, 4, "4"),
   OPT_MAINTAIN_HOLD_5                (0, 5, "5"),
   OPT_EQUIP_UNEQUIP_DROP             (0, 0, ""),
   OPT_EQUIP_UNEQUIP_SHEATH           (0, 2, ""),
   OPT_EQUIP_UNEQUIP_READY_0          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_1          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_2          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_3          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_4          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_5          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_6          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_7          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_8          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_9          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_10         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_11         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_12         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_13         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_14         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_15         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_16         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_17         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_18         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_READY_19         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_0          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_1          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_2          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_3          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_4          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_5          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_6          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_7          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_8          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_9          (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_10         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_11         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_12         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_13         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_14         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_15         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_16         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_17         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_18         (0, 1, ""),
   OPT_EQUIP_UNEQUIP_APPLY_19         (0, 1, ""),
   OPT_USE_HERO_POINT                 (0, 0, "y"),
   OPT_DONT_USE_HERO_POINT            (0, 0, "n");

   private final String keys;
   private final String keysLeft;
   private final String keysLegs;
   private final String keysHead;
   private final byte attackActions;
   private final byte totalActions;
   RequestActionType(int attackActions, int totalActions, String keys) {
      this(attackActions, totalActions, keys, keys, keys, keys);
   }
   RequestActionType(int attackActions, int totalActions, String keysRight, String keysLeft, String keysLegs, String keysHead) {
      this.attackActions = (byte) attackActions;
      this.totalActions  = (byte) totalActions;
      this.keys = keysRight;
      this.keysLeft = keysLeft;
      this.keysLegs = keysLegs;
      this.keysHead = keysHead;
   }

   private static final HashMap<Integer, RequestActionType> ORDINAL_LOOKUP = new HashMap<>();
   static {
      for (RequestActionType type : values()) {
         ORDINAL_LOOKUP.put(type.ordinal(), type);
      }
   }
   public static RequestActionType lookupByOrdinal(int ordinal) {
      if (ordinal == SyncRequest.OPT_CANCEL_ACTION) {
         return RequestActionType.OPT_CANCEL_ACTION;
      }
      return ORDINAL_LOOKUP.get(ordinal);
   }

   public String getAllowedKeyStrokesForOption() {
      return keys;
   }
   public String getAllowedKeyStrokesForOption(LimbType limb) {
      if (limb.isHead()) {
         return keysHead;
      }
      if (limb.isLeg()) {
         return keysLegs;
      }
      if (limb.isHand() && (limb.side == Side.LEFT)) {
         return keysLeft;
      }
      return keys;
   }
   static List<RequestActionType> forwardActions = Arrays.asList(OPT_LOCATION_ACTION_FORWARD_CLOSE,
                                                                 OPT_LOCATION_ACTION_FORWARD_ASSIST,
                                                                 OPT_LOCATION_ACTION_FORWARD_LOCK,
                                                                 OPT_LOCATION_ACTION_FORWARD_OPEN,
                                                                 OPT_LOCATION_ACTION_FORWARD_UNLOCK);
   static List<RequestActionType> rightActions = Arrays.asList(OPT_LOCATION_ACTION_RIGHT_CLOSE,
                                                               OPT_LOCATION_ACTION_RIGHT_ASSIST,
                                                               OPT_LOCATION_ACTION_RIGHT_LOCK,
                                                               OPT_LOCATION_ACTION_RIGHT_OPEN,
                                                               OPT_LOCATION_ACTION_RIGHT_UNLOCK);
   static List<RequestActionType> leftActions = Arrays.asList(OPT_LOCATION_ACTION_LEFT_CLOSE,
                                                              OPT_LOCATION_ACTION_LEFT_ASSIST,
                                                              OPT_LOCATION_ACTION_LEFT_LOCK,
                                                              OPT_LOCATION_ACTION_LEFT_OPEN,
                                                              OPT_LOCATION_ACTION_LEFT_UNLOCK);
   static List<RequestActionType> closeActions = Arrays.asList(OPT_LOCATION_ACTION_LEFT_CLOSE,
                                                              OPT_LOCATION_ACTION_RIGHT_CLOSE,
                                                              OPT_LOCATION_ACTION_FORWARD_CLOSE);
   static List<RequestActionType> assistActions = Arrays.asList(OPT_LOCATION_ACTION_LEFT_ASSIST,
                                                                OPT_LOCATION_ACTION_RIGHT_ASSIST,
                                                                OPT_LOCATION_ACTION_FORWARD_ASSIST);
   static List<RequestActionType> lockActions = Arrays.asList(OPT_LOCATION_ACTION_LEFT_LOCK,
                                                              OPT_LOCATION_ACTION_RIGHT_LOCK,
                                                              OPT_LOCATION_ACTION_FORWARD_LOCK);
   static List<RequestActionType> openActions = Arrays.asList(OPT_LOCATION_ACTION_LEFT_OPEN,
                                                              OPT_LOCATION_ACTION_RIGHT_OPEN,
                                                              OPT_LOCATION_ACTION_FORWARD_OPEN);
   static List<RequestActionType> unlockActions = Arrays.asList(OPT_LOCATION_ACTION_LEFT_UNLOCK,
                                                                OPT_LOCATION_ACTION_RIGHT_UNLOCK,
                                                                OPT_LOCATION_ACTION_FORWARD_UNLOCK);
   static List<RequestActionType> locationActions = Arrays.asList(OPT_LOCATION_ACTION_0,
                                                                  OPT_LOCATION_ACTION_1,
                                                                  OPT_LOCATION_ACTION_2,
                                                                  OPT_LOCATION_ACTION_3,
                                                                  OPT_LOCATION_ACTION_4,
                                                                  OPT_LOCATION_ACTION_5,
                                                                  OPT_LOCATION_ACTION_6,
                                                                  OPT_LOCATION_ACTION_7,
                                                                  OPT_LOCATION_ACTION_8,
                                                                  OPT_LOCATION_ACTION_9,
                                                                  OPT_LOCATION_ACTION_10,
                                                                  OPT_LOCATION_ACTION_11,
                                                                  OPT_LOCATION_ACTION_12,
                                                                  OPT_LOCATION_ACTION_13,
                                                                  OPT_LOCATION_ACTION_14,
                                                                  OPT_LOCATION_ACTION_15,
                                                                  OPT_LOCATION_ACTION_16,
                                                                  OPT_LOCATION_ACTION_17,
                                                                  OPT_LOCATION_ACTION_18,
                                                                  OPT_LOCATION_ACTION_19);
   static List<RequestActionType> readyEquipActions = Arrays.asList(OPT_EQUIP_UNEQUIP_READY_0,
                                                                    OPT_EQUIP_UNEQUIP_READY_1,
                                                                    OPT_EQUIP_UNEQUIP_READY_2,
                                                                    OPT_EQUIP_UNEQUIP_READY_3,
                                                                    OPT_EQUIP_UNEQUIP_READY_4,
                                                                    OPT_EQUIP_UNEQUIP_READY_5,
                                                                    OPT_EQUIP_UNEQUIP_READY_6,
                                                                    OPT_EQUIP_UNEQUIP_READY_7,
                                                                    OPT_EQUIP_UNEQUIP_READY_8,
                                                                    OPT_EQUIP_UNEQUIP_READY_9,
                                                                    OPT_EQUIP_UNEQUIP_READY_10,
                                                                    OPT_EQUIP_UNEQUIP_READY_11,
                                                                    OPT_EQUIP_UNEQUIP_READY_12,
                                                                    OPT_EQUIP_UNEQUIP_READY_13,
                                                                    OPT_EQUIP_UNEQUIP_READY_14,
                                                                    OPT_EQUIP_UNEQUIP_READY_15,
                                                                    OPT_EQUIP_UNEQUIP_READY_16,
                                                                    OPT_EQUIP_UNEQUIP_READY_17,
                                                                    OPT_EQUIP_UNEQUIP_READY_18,
                                                                    OPT_EQUIP_UNEQUIP_READY_19);
   static List<RequestActionType> applyEquipActions = Arrays.asList(OPT_EQUIP_UNEQUIP_APPLY_0,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_1,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_2,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_3,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_4,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_5,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_6,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_7,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_8,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_9,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_10,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_11,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_12,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_13,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_14,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_15,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_16,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_17,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_18,
                                                                    OPT_EQUIP_UNEQUIP_APPLY_19);

   static List<RequestActionType> prepareInitateSpellActions = Arrays.asList(OPT_PREPARE_INITATE_SPELL_1,
                                                                             OPT_PREPARE_INITATE_SPELL_2,
                                                                             OPT_PREPARE_INITATE_SPELL_3,
                                                                             OPT_PREPARE_INITATE_SPELL_4,
                                                                             OPT_PREPARE_INITATE_SPELL_5);
   static List<RequestActionType> completePriestSpellActions = Arrays.asList(OPT_COMPLETE_PRIEST_SPELL_1,
                                                                             OPT_COMPLETE_PRIEST_SPELL_2,
                                                                             OPT_COMPLETE_PRIEST_SPELL_3,
                                                                             OPT_COMPLETE_PRIEST_SPELL_4,
                                                                             OPT_COMPLETE_PRIEST_SPELL_5);
   public boolean isHexOptionForward()       { return forwardActions.contains(this);             }
   public boolean isHexOptionRight()         { return rightActions.contains(this);               }
   public boolean isHexOptionLeft()          { return leftActions.contains(this);                }
   public boolean isHexOptionOpen()          { return openActions.contains(this);                }
   public boolean isHexOptionClose()         { return closeActions.contains(this);               }
   public boolean isHexOptionUnlock()        { return unlockActions.contains(this);              }
   public boolean isHexOptionLock()          { return lockActions.contains(this);                }
   public boolean isHexOptionAssist()        { return assistActions.contains(this);              }
   public boolean isLocationAction()         { return locationActions.contains(this);            }
   public boolean isReadyEquip()             { return readyEquipActions.contains(this);          }
   public boolean isApplyEquip()             { return applyEquipActions.contains(this);          }
   public boolean isPrepareInateSpell()      { return prepareInitateSpellActions.contains(this); }
   public boolean isCompletePriestSpell()    { return completePriestSpellActions.contains(this); }
   public int getIndexOfLocationAction()     { return locationActions.indexOf(this);             }
   public int getIndexOfPrepareInateSpell()  { return prepareInitateSpellActions.indexOf(this);  }
   public int getIndexOfCompletePriestSpell(){ return completePriestSpellActions.indexOf(this);  }
   public int getIndexOfApplyEquipActions()  { return applyEquipActions.indexOf(this);           }
   public int getIndexOfReadyEquipActions()  { return readyEquipActions.indexOf(this);           }

   public static RequestActionType getEquipUnequipApplyActionByIndex(int index) { return applyEquipActions.get(index); }
   public static RequestActionType getEquipUnequipReadyActionByIndex(int index) { return readyEquipActions.get(index); }
   public static RequestActionType getLocationActionByIndex(int index)          {
      if (index < locationActions.size()) {
         return locationActions.get(index);
      }
      return null;
   }

   public boolean isCharge() { return  (this == OPT_CHARGE_ATTACK_1 )||
                                       (this == OPT_CHARGE_ATTACK_2) ||
                                       (this == OPT_CHARGE_ATTACK_3); }
   public boolean isAdvance(){ return  (this == OPT_MOVE               ) ||
                                       (this == OPT_MOVE_EVASIVE       ) ||
                                       (this == OPT_CLOSE_AND_ATTACK_1 ) ||
                                       (this == OPT_CLOSE_AND_ATTACK_2 ) ||
                                       (this == OPT_CLOSE_AND_ATTACK_3 ) ||
                                       (this == OPT_CLOSE_AND_GRAPPLE_1) ||
                                       (this == OPT_CLOSE_AND_GRAPPLE_2) ||
                                       (this == OPT_CLOSE_AND_GRAPPLE_3) ||
                                       isCharge(); }
   public boolean isEvasiveMove()      { return (this == OPT_MOVE_EVASIVE); }
   public boolean isReadyWeapon()      { return ((this == OPT_READY_1 ) ||
                                                 (this == OPT_READY_2 ) ||
                                                 (this == OPT_READY_3 ) ||
                                                 (this == OPT_READY_4 ) ||
                                                 (this == OPT_READY_5 )); }
   public boolean isChangePosition()   { return this == OPT_CHANGE_POS; }
   public boolean isEquipUnequip()     { return (this == OPT_EQUIP_UNEQUIP); }
   public boolean isChangeTargets()    { return (this == OPT_CHANGE_TARGET_PRIORITIES); }
   public boolean isBeginSpell()       { return (this == OPT_BEGIN_SPELL);}

   public boolean isContinueSpell()    { return (this == OPT_CONTINUE_INCANTATION);}
   public boolean isChannelEnergy()    { return ((this == OPT_CHANNEL_ENERGY_1) ||
                                                 (this == OPT_CHANNEL_ENERGY_2) ||
                                                 (this == OPT_CHANNEL_ENERGY_3) ||
                                                 (this == OPT_CHANNEL_ENERGY_4) ||
                                                 (this == OPT_CHANNEL_ENERGY_5));
   }
   public boolean isMaintainSpell()     { return (this == OPT_MAINTAIN_SPELL); }
   public boolean isDiscardSpell()      { return (this == OPT_DISCARD_SPELL); }
   public boolean isCompleteMageSpell() { return ((this == OPT_COMPLETE_SPELL_1) ||
                                                  (this == OPT_COMPLETE_SPELL_2) ||
                                                  (this == OPT_COMPLETE_SPELL_3));
   }
   public boolean isCompleteSpell() {
      return isCompleteMageSpell() || isCompletePriestSpell();
   }
   public boolean isApplyItem()       { return this == OPT_APPLY_ITEM; }
   public boolean isFinalDefense()    { return ((this == OPT_FINAL_DEFENSE_1) ||
                                                (this == OPT_FINAL_DEFENSE_2) ||
                                                (this == OPT_FINAL_DEFENSE_3) ||
                                                (this == OPT_FINAL_DEFENSE_4) ||
                                                (this == OPT_FINAL_DEFENSE_5)); }

   public boolean isTargetEnemy()    { return (this == OPT_TARGET_ENEMY); }
   public boolean isPrepareRanged()  { return (this == OPT_PREPARE_RANGED); }
   public boolean isWaitForAttack()  { return (this == OPT_WAIT_TO_ATTACK);}
   public boolean isAnyLocationAction()
   {
      return (isHexOptionForward() ||
              isHexOptionRight()   ||
              isHexOptionLeft()    ||
              isHexOptionOpen()    ||
              isHexOptionClose()   ||
              isHexOptionUnlock()  ||
              isHexOptionLock()    ||
              isHexOptionAssist()  ||
              isLocationAction());
   }


   public boolean isRangedAttack() {
      return ((this == OPT_ATTACK_THROW_1)  ||
              (this == OPT_ATTACK_THROW_2)  ||
              (this == OPT_ATTACK_THROW_3)  ||
              (this == OPT_ATTACK_MISSILE));
   }
   public boolean isGrappleAttack() {
      return ((this == OPT_ATTACK_GRAPPLE_1)  ||
              (this == OPT_ATTACK_GRAPPLE_2)  ||
              (this == OPT_ATTACK_GRAPPLE_3)  ||
              (this == OPT_CLOSE_AND_GRAPPLE_1) ||
              (this == OPT_CLOSE_AND_GRAPPLE_2) ||
              (this == OPT_CLOSE_AND_GRAPPLE_3));
   }
   public boolean isBreakFree() {
      return ((this == OPT_BREAK_FREE_1) ||
              (this == OPT_BREAK_FREE_2) ||
              (this == OPT_BREAK_FREE_3));
   }
   public boolean isCounterAttack() {
      return (isCounterAttackThrow() || isCounterAttackGrab());
   }
   public boolean isCounterAttackThrow() {
      return ((this == OPT_COUNTER_ATTACK_THROW_1) ||
              (this == OPT_COUNTER_ATTACK_THROW_2) ||
              (this == OPT_COUNTER_ATTACK_THROW_3));
   }
   public boolean isCounterAttackGrab() {
      return ((this == OPT_COUNTER_ATTACK_GRAB_1) ||
              (this == OPT_COUNTER_ATTACK_GRAB_2) ||
              (this == OPT_COUNTER_ATTACK_GRAB_3));
   }
   public byte getAttackActions(boolean considerSpellAsAttack)
   {
      if ((!considerSpellAsAttack) && (isCompletePriestSpell() || isCompleteMageSpell())) {
         return 0;
      }
      return this.attackActions;
   }
   public byte getActionsUsed(byte equipmentActionsUsed)
   {
      if (this == OPT_EQUIP_UNEQUIP) {
         return equipmentActionsUsed;
      }
      return this.totalActions;
   }

   public boolean isSheath() { return (this == RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH);}
   public boolean isDrop()   { return (this == RequestActionType.OPT_EQUIP_UNEQUIP_DROP);}

   @Override
   public String toString() {
      return this.name();
   }
}
