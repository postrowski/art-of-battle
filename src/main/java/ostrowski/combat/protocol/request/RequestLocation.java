/*
 * Created on Jun 2, 2006
 *
 */
package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestLocation extends SyncRequest
{
   ArrayList<ArenaCoordinates> _selectableCoordinates;
   String _cursorResourceName;

   public RequestLocation() {
   }
   public RequestLocation(String cursorResourceName) {
      _cursorResourceName = cursorResourceName;
   }
   public String getCursorResourceName() {
      return _cursorResourceName;
   }
   public boolean setAnswer(short xLoc, short yLoc) {
      for (int i=0 ; i<_selectableCoordinates.size() ; i++) {
         if ((xLoc == _selectableCoordinates.get(i)._x) &&
             (yLoc == _selectableCoordinates.get(i)._y)) {
            setAnswerID(i);
            return true;
         }
      }
      return false;
   }
   public ArrayList<ArenaCoordinates> getSelectableCoordinates() {  return _selectableCoordinates;  }
   public void setCoordinates(ArrayList<ArenaCoordinates> selectableCoordinates) {
      _selectableCoordinates = selectableCoordinates;
   }
   public short getLocX() { return _selectableCoordinates.get(getAnswerIndex())._x; }
   public short getLocY() { return _selectableCoordinates.get(getAnswerIndex())._y; }
   @Override
   public int getActionCount() {
      return _selectableCoordinates.size();
   }
   @Override
   public int[] getOptionIDs() {
      int[] results = new int[_selectableCoordinates.size()];
      for (int i=0 ; i<_selectableCoordinates.size() ; i++) {
         results[i] = i;
      }
      return results;
   }
   @Override
   public String[] getOptions() {
      String[] results = new String[_selectableCoordinates.size()];
      for (int i=0 ; i<_selectableCoordinates.size() ; i++) {
         results[i] = _selectableCoordinates.get(i)._x+","+
                      _selectableCoordinates.get(i)._y;
      }
      return results;
   }
   @Override
   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         _answer = new RequestOption(_selectableCoordinates.get(i)._x + "," +
                                     _selectableCoordinates.get(i)._y, i, true);
      }
   }
   @Override
   public synchronized boolean setAnswerID(int answerID) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         if (answerID != -1) {
            setAnswerByOptionIndex(answerID);
         }
      }
      return true;
   }
   @Override
   public int getAnswerIndex() {
      return _answer.getIntValue();
   }

   public ArenaCoordinates getAnswerCoordinates() {
      return _selectableCoordinates.get(getAnswerIndex());
   }
   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _cursorResourceName = readString(in);
         _selectableCoordinates = new ArrayList<>();
         int size = in.readInt();
         for (int i=0 ; i<size ; i++) {
            ArenaCoordinates coord = new ArenaCoordinates();
            coord.serializeFromStream(in);
            _selectableCoordinates.add(coord);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(_cursorResourceName, out);
         writeToStream(_selectableCoordinates.size(), out);
         for (int i=0 ; i<_selectableCoordinates.size(); i++) {
            writeToStream(_selectableCoordinates.get(i)._x, out);
            writeToStream(_selectableCoordinates.get(i)._y, out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("RequestLocation: ");
      sb.append("selectableLoc:").append(_selectableCoordinates.toString()).append("\n");
      sb.append(super.toString());
      return sb.toString();
   }

}
