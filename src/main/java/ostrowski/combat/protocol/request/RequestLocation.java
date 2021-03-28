/*
 * Created on Jun 2, 2006
 *
 */
package ostrowski.combat.protocol.request;

import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestLocation extends SyncRequest
{
   List<ArenaCoordinates> selectableCoordinates;
   String                 cursorResourceName;

   public RequestLocation() {
   }
   public RequestLocation(String cursorResourceName) {
      this.cursorResourceName = cursorResourceName;
   }
   public String getCursorResourceName() {
      return cursorResourceName;
   }
   public boolean setAnswer(short xLoc, short yLoc) {
      for (int i = 0; i < selectableCoordinates.size() ; i++) {
         if ((xLoc == selectableCoordinates.get(i).x) &&
             (yLoc == selectableCoordinates.get(i).y)) {
            setAnswerID(i);
            return true;
         }
      }
      return false;
   }
   public List<ArenaCoordinates> getSelectableCoordinates() {  return selectableCoordinates;  }
   public void setCoordinates(List<ArenaCoordinates> selectableCoordinates) {
      this.selectableCoordinates = selectableCoordinates;
   }

   @Override
   public int getActionCount() {
      return selectableCoordinates.size();
   }
   @Override
   public int[] getOptionIDs() {
      int[] results = new int[selectableCoordinates.size()];
      for (int i = 0; i < selectableCoordinates.size() ; i++) {
         results[i] = i;
      }
      return results;
   }
   @Override
   public String[] getOptions() {
      String[] results = new String[selectableCoordinates.size()];
      for (int i = 0; i < selectableCoordinates.size() ; i++) {
         results[i] = selectableCoordinates.get(i).x + "," +
                      selectableCoordinates.get(i).y;
      }
      return results;
   }
   @Override
   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         answer = new RequestOption(selectableCoordinates.get(i).x + "," +
                                    selectableCoordinates.get(i).y, i, true);
      }
   }
   @Override
   public synchronized boolean setAnswerID(int answerID) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         if (answerID != -1) {
            setAnswerByOptionIndex(answerID);
         }
      }
      return true;
   }
   @Override
   public int getAnswerIndex() {
      return answer.getIntValue();
   }

   public ArenaCoordinates getAnswerCoordinates() {
      return selectableCoordinates.get(getAnswerIndex());
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         cursorResourceName = readString(in);
         selectableCoordinates = new ArrayList<>();
         int size = in.readInt();
         for (int i=0 ; i<size ; i++) {
            ArenaCoordinates coord = new ArenaCoordinates();
            coord.serializeFromStream(in);
            selectableCoordinates.add(coord);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(cursorResourceName, out);
         writeToStream(selectableCoordinates.size(), out);
         for (ArenaCoordinates selectableCoordinate : selectableCoordinates) {
            writeToStream(selectableCoordinate.x, out);
            writeToStream(selectableCoordinate.y, out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public String toString()
   {
      return "RequestLocation: " +
             "selectableLoc:" + selectableCoordinates + "\n" +
             super.toString();
   }

}
