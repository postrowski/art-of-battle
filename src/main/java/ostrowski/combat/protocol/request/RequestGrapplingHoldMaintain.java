/*
 * Created on May 16, 2006
 */
package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.IHolder;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.spells.mage.SpellSpiderWeb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.protocol.SyncRequest;

public class RequestGrapplingHoldMaintain extends SyncRequest implements Enums
{
   private int                             _escaperID                 = -1;
   private byte                            _escapeActions             = 0;
   private final HashMap<Integer, Integer> _mapOfActionsToTN          = new HashMap<>();
   private final HashMap<Integer, String>  _mapOfActionsToExplanation = new HashMap<>();

   public RequestGrapplingHoldMaintain() {
      // default c'tor used by factor method to serialize in from a stream.
   }

   public RequestGrapplingHoldMaintain(Character escaper, RequestAction attack, byte range) {
      _escaperID = escaper._uniqueID;
      _escapeActions = attack.getAttackActions(true/*considerSpellAsAttack*/);
   }

   @Override
   public boolean isCancelable() {
      return false;
   }

   public RequestActionType getActionType() {
      if (_answer instanceof RequestActionOption) {
         RequestActionOption reqActOpt = (RequestActionOption) _answer;
         return reqActOpt.getValue();
      }
      return null;
   }

   public byte getActionsUsed() {
      RequestActionType actType = getActionType();
      if (actType != null) {
         if (actType == RequestActionType.OPT_NO_ACTION) {
            return 0;
         }
         return actType.getActionsUsed((byte)0);
      }
      return (byte) getAnswerIndex();
   }

   public void addMaintainHoldOptions(int maxActions, IHolder holder, Character escaper, int skill, byte holdersPainPenalty) {
      byte adjustedStrength = holder.getAdjustedStrength();
      Byte holdingLevel = holder.getHoldingLevel();

      byte escaperSize = 0;
      if (!(holder instanceof SpellSpiderWeb)) {
         escaperSize = escaper.getRace().getBuildModifier();
      }
      for (int actions = 0; actions <= maxActions; actions++) {
         int tn = (adjustedStrength + holdingLevel + (actions * skill)) - holdersPainPenalty - escaperSize;
         StringBuilder sb = new StringBuilder();
         sb.append("<table border=1>");
         if (holder instanceof Character) {
            sb.append("<tr><td>").append(adjustedStrength).append("</td><td>holder adjusted strength</td></tr>");
         }
         //         else {
         //            sb.append("<tr><td>").append(adjustedStrength).append("</td><td>caster's racial size adjuster</td></tr>");
         //         }
         sb.append("<tr><td>").append(holdingLevel).append("</td><td>hold level</td></tr>");
         if (actions > 0) {
            sb.append("<tr><td>").append(actions * skill).append("</td><td>actions * skill (").append(actions).append("*").append(skill).append(")</td></tr>");
         }
         if (holdersPainPenalty != 0) {
            sb.append("<tr><td>").append(holdersPainPenalty * -1).append("</td><td>holders pain & wounds</td></tr>");
         }
         if (escaperSize != 0) {
            sb.append("<tr><td>").append(escaperSize * -1).append("</td><td>escaper's racial size adjuster</td></tr>");
         }
         sb.append("<tr><td><b>").append(tn).append("</b></td><td><b>holders TN</b></td></tr>");
         sb.append("</table>");

         RequestActionType actType = null;
         switch (actions) {
            case 0: actType = RequestActionType.OPT_NO_ACTION; break;
            case 1: actType = RequestActionType.OPT_MAINTAIN_HOLD_1; break;
            case 2: actType = RequestActionType.OPT_MAINTAIN_HOLD_2; break;
            case 3: actType = RequestActionType.OPT_MAINTAIN_HOLD_3; break;
            case 4: actType = RequestActionType.OPT_MAINTAIN_HOLD_4; break;
            case 5: actType = RequestActionType.OPT_MAINTAIN_HOLD_5; break;
            default:
               DebugBreak.debugBreak("no actions for 'maintain hold'");
         }
         RequestActionOption rao = new RequestActionOption("Maintain hold (" + actions + " actions), escape TN = " + tn,
                                                           actType, LimbType.BODY, true/*enabled*/);
         addOption(rao);
         _mapOfActionsToTN.put(actions, tn);
         _mapOfActionsToExplanation.put(actions, sb.toString());
         //addOption(actions, "Maintain hold (" + actions + " actions), escape TN = " + tn, true/*enabled*/);
      }
   }

   public void addMaintainHoldOptions(int maxActions, Character holder, Character escaper, int skill, byte holdersPainPenalty) {
      //byte adjustedStrength = holder.getAdjustedStrength();
      Byte holdingLevel = holder.getHoldingLevel();

      byte escaperSize = escaper.getRace().getBuildModifier();
      for (int actions = 0; actions <= maxActions; actions++) {
         byte holdersStrength = holder.getAttributeLevel(Attribute.Strength);
         int tn = (holdersStrength + holdingLevel + (actions * skill)) - holdersPainPenalty - escaperSize;
         StringBuilder sb = new StringBuilder();
         sb.append("<table border=1><tr><td>").append(holdersStrength).append("</td><td>holder strength</td></tr>");
         sb.append("<tr><td>").append(holdingLevel).append("</td><td>hold level</td></tr>");
         sb.append("<tr><td>").append(actions * skill).append("</td><td>actions * skill (").append(actions).append("*").append(skill).append(")</td></tr>");
         sb.append("<tr><td>").append(holdersPainPenalty * -1).append("</td><td>holders pain & wounds</td></tr>");
         if (escaperSize != 0) {
            sb.append("<tr><td>").append(escaperSize * -1).append("</td><td>escaper's racial size adjuster</td></tr>");
         }
         sb.append("<tr><td><b>").append(tn).append("</b></td><td><b>holders TN</b></td></tr>");
         sb.append("</table>");
         _mapOfActionsToTN.put(actions, tn);
         _mapOfActionsToExplanation.put(actions, sb.toString());
         RequestActionType actType = null;
         switch (actions) {
            case 0: actType = RequestActionType.OPT_NO_ACTION; break;
            case 1: actType = RequestActionType.OPT_MAINTAIN_HOLD_1; break;
            case 2: actType = RequestActionType.OPT_MAINTAIN_HOLD_2; break;
            case 3: actType = RequestActionType.OPT_MAINTAIN_HOLD_3; break;
            case 4: actType = RequestActionType.OPT_MAINTAIN_HOLD_4; break;
            case 5: actType = RequestActionType.OPT_MAINTAIN_HOLD_5; break;
         }
         addOption(new RequestActionOption("Maintain hold (" + actions + " actions), escape TN = " + tn,
                                            actType, LimbType.BODY, true/*enabled*/));
         //addOption(actions, "Maintain hold (" + actions + " actions), escape TN = " + tn, true/*enabled*/);
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _escaperID = readInt(in);
         _escapeActions = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(_escaperID, out);
         writeToStream(_escapeActions, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public int getTn() {
      return _mapOfActionsToTN.get((int)getActionsUsed());
   }

   public String getTnExplanation() {
      return _mapOfActionsToExplanation.get((int)getActionsUsed());
   }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      return "1234567890";
   }

}
