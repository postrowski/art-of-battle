/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class RequestTarget extends SyncRequest
{
   private List<Byte> _optionsTeam;

   public RequestTarget() {
      _message = "Please select your target, in order of priority." +
                 " Put the highest priority targets at the top of the list.";
   }
   @Override
   public boolean isCancelable() {
      return false;
   }
   public List<Integer> getOrderedTargetIds() {
      List<Integer> ids = new ArrayList<>();
      for (IRequestOption opt : _options) {
         ids.add(opt.getIntValue());
      }
      return ids;
   }
   @Override
   public void addOption(int optionID, String optionStr, boolean enabled)
   {
      DebugBreak.debugBreak();
      throw new NullPointerException();
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestTarget) {
            ((RequestTarget)newObj)._optionsTeam = _optionsTeam;
         }
      }
   }
   public void setOrderedTargetIds(List<Character> newOrder) {
      _options  = new ArrayList<>();
      _optionsTeam = new ArrayList<>();
      for (Character target : newOrder) {
         _options.add(new RequestOption(target.getName(), target._uniqueID, true));
         _optionsTeam.add(target._teamID);
      }
   }
   public List<Character> getTargetCharacters() {
      List<Character> targets = new ArrayList<>();
      for (int i=0 ; i<_options.size() ; i++) {
         Character target = new Character();
         target.setName(_options.get(i).getName());
         target._uniqueID = _options.get(i).getIntValue();
         target._teamID   = _optionsTeam.get(i);
         targets.add(target);
      }
      return targets;
   }
   @Override
   public String getAnswer() {
      StringBuilder sb = new StringBuilder();
      for (IRequestOption option : _options) {
         sb.append(option).append(",");
      }
      return sb.toString();
   }
   @Override
   public synchronized void setCustAnswer(String answer) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         int index = 0;
         StringTokenizer st = new StringTokenizer(answer, ",");
         while (st.hasMoreTokens()) {
            _options.set(index++, new RequestOption("", Integer.parseInt(st.nextToken()), true));
            // TODO: we currently don't copy the name or teamID
         }
      }
   }
}
