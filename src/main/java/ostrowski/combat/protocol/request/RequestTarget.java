/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import java.util.ArrayList;
import java.util.StringTokenizer;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestTarget extends SyncRequest
{
   private ArrayList<Byte> _optionsTeam;

   public RequestTarget() {
      _message = "Please select your target, in order of priority." +
                 " Put the highest priority targets at the top of the list.";
   }
   @Override
   public boolean isCancelable() {
      return false;
   }
   public ArrayList<Integer> getOrderedTargetIds() {
      ArrayList<Integer> ids = new ArrayList<>();
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
   public void setOrderedTargetIds(ArrayList<Character> newOrder) {
      _options  = new ArrayList<>();
      _optionsTeam = new ArrayList<>();
      for (Character target : newOrder) {
         _options.add(new RequestOption(target.getName(), target._uniqueID, true));
         _optionsTeam.add(new Byte(target._teamID));
      }
   }
   public ArrayList<Character> getTargetCharacters() {
      ArrayList<Character> targets = new ArrayList<>();
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
      for (int i=0 ; i<_options.size() ; i++) {
         sb.append(_options.get(i)).append(",");
      }
      return sb.toString();
   }
   @Override
   public synchronized void setCustAnswer(String answer) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         int index = 0;
         StringTokenizer st = new StringTokenizer(answer, ",");
         while (st.hasMoreTokens()) {
            _options.set(index++, new RequestOption("", Integer.valueOf(st.nextToken()), true));
            // TODO: we currently don't copy the name or teamID
         }
      }
   }
}
