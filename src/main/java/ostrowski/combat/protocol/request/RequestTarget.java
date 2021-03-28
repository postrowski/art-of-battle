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
   private List<Byte> optionsTeam;

   public RequestTarget() {
      message = "Please select your target, in order of priority." +
                " Put the highest priority targets at the top of the list.";
   }
   @Override
   public boolean isCancelable() {
      return false;
   }
   public List<Integer> getOrderedTargetIds() {
      List<Integer> ids = new ArrayList<>();
      for (IRequestOption opt : options) {
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
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestTarget) {
            ((RequestTarget)newObj).optionsTeam = optionsTeam;
         }
      }
   }
   public void setOrderedTargetIds(List<Character> newOrder) {
      options = new ArrayList<>();
      optionsTeam = new ArrayList<>();
      for (Character target : newOrder) {
         options.add(new RequestOption(target.getName(), target.uniqueID, true));
         optionsTeam.add(target.teamID);
      }
   }
   public List<Character> getTargetCharacters() {
      List<Character> targets = new ArrayList<>();
      for (int i = 0; i < options.size() ; i++) {
         Character target = new Character();
         target.setName(options.get(i).getName());
         target.uniqueID = options.get(i).getIntValue();
         target.teamID = optionsTeam.get(i);
         targets.add(target);
      }
      return targets;
   }
   @Override
   public String getAnswer() {
      StringBuilder sb = new StringBuilder();
      for (IRequestOption option : options) {
         sb.append(option).append(",");
      }
      return sb.toString();
   }
   @Override
   public synchronized void setCustAnswer(String answer) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         int index = 0;
         StringTokenizer st = new StringTokenizer(answer, ",");
         while (st.hasMoreTokens()) {
            options.set(index++, new RequestOption("", Integer.parseInt(st.nextToken()), true));
            // TODO: we currently don't copy the name or teamID
         }
      }
   }
}
