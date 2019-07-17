/*
 * Created on Jun 6, 2006
 *
 */
package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.protocol.RequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestMovement extends SyncRequest implements Enums
{
   private List<Orientation>                 _completeOrientations;
   private List<Orientation>                 _cancelOrientations;
   private List<Orientation>                 _selectedPath;
//   private List<Orientation>                 _orientations;
   private List<Orientation>                 _futureOrientations;
   private HashMap<Orientation, Orientation> _mapOfFutureOrientToSourceOrient;
   private int                               _actorID;

   public RequestMovement() {
      _completeOrientations = new ArrayList<>();
      _cancelOrientations = new ArrayList<>();
   }

   public boolean setOrientation(Orientation orientation) {
      if (_futureOrientations.contains(orientation)) {
         if (_cancelOrientations.contains(orientation)) {
            setAnswerID(OPT_CANCEL_ACTION);
         }
         _selectedPath = getRouteToFutureOrientation(orientation);
         return true;
      }
      return false;
   }
   @Override
   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         //setAnswerID(i);
         _answer = new RequestOption("", i, true);
         if (_futureOrientations.size() > i) {
            if (_cancelOrientations.contains(_futureOrientations.get(i))) {
               setAnswerID(OPT_CANCEL_ACTION);
            }
         }
         //_answerStr = _futureOrientations.get(i).toString();
      }
   }
   public List<Orientation> getOrientations() {
      return _futureOrientations;
   }
   public void setOrientations(List<Orientation> futureOrientations,
                               HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient, Character actor) {
      _futureOrientations = futureOrientations;
      _mapOfFutureOrientToSourceOrient = mapOfFutureOrientToSourceOrient;
      _actorID = actor._uniqueID;
      _cancelOrientations.clear();
      _completeOrientations.clear();
      if (actor.hasMovedThisRound()) {
         _completeOrientations.add(actor.getOrientation());
      }
      else {
         _cancelOrientations.add(actor.getOrientation());
      }
   }
   public Orientation getAnswerOrientation(boolean removeEntry) {
      if (_selectedPath.isEmpty()) {
         return null;
      }
      if (removeEntry) {
         return _selectedPath.remove(_selectedPath.size()-1);
      }
      return _selectedPath.get(_selectedPath.size()-1);
   }

   @Override
   public boolean isAnswered() {
      return (_selectedPath != null);
   }

   @Override
   public int getActionCount() {
      return super.getActionCount() + _futureOrientations.size();
   }

   @Override
   public void copyAnswer(SyncRequest source) {
      if (source instanceof RequestMovement) {
         RequestMovement sourceMove = (RequestMovement) source;
         _selectedPath                    = sourceMove._selectedPath;
         _futureOrientations              = sourceMove._futureOrientations;
         _completeOrientations            = sourceMove._completeOrientations;
         _cancelOrientations              = sourceMove._cancelOrientations;

         _mapOfFutureOrientToSourceOrient = sourceMove._mapOfFutureOrientToSourceOrient;
      }
      //super.copyAnswer(source);
   }

   static public int readIntoListOrientation(List<Orientation> data, DataInputStream in) throws IOException {
      data.clear();
      int size = in.readShort();
      for (int i=0 ; i<size ; i++) {
         data.add(Orientation.serializeOrientationFromStream(in));
      }
      return size;
   }

   static public void writeListOrientationToStream(List<Orientation> data, DataOutputStream out) throws IOException {
      if (data == null) {
         out.writeShort(0);
         return;
      }
      out.writeShort(data.size());
      for (Orientation obj : data) {
         obj.serializeToStream(out);
      }
   }


   @Override
   public void serializeFromStream(DataInputStream in) {
      super.serializeFromStream(in);
      try {
         _selectedPath = new ArrayList<>();
         _futureOrientations = new ArrayList<>();
         _cancelOrientations = new ArrayList<>();
         _completeOrientations = new ArrayList<>();
         ArrayList<Integer> futureOrientationsSourceIndexIntoFutureOrientations = new ArrayList<>();
         _actorID = readInt(in);
         readIntoListOrientation(_selectedPath, in);
         readIntoListOrientation(_futureOrientations, in);
         readIntoListOrientation(_cancelOrientations, in);
         readIntoListOrientation(_completeOrientations, in);
         readIntoListInteger(futureOrientationsSourceIndexIntoFutureOrientations, in);
         {
            // load up the HashMap with all the known routes
            _mapOfFutureOrientToSourceOrient = new HashMap<>();
            for (Orientation futureOrientation : _futureOrientations) {
               while (futureOrientation != null) {
                  int index = _futureOrientations.indexOf(futureOrientation);
                  if (index != -1) {
                     Integer sourceIndex = futureOrientationsSourceIndexIntoFutureOrientations.get(index);
                     if (sourceIndex >= 0) {
                        Orientation fromOrientation = _futureOrientations.get(sourceIndex.intValue());
                        _mapOfFutureOrientToSourceOrient.put(futureOrientation, fromOrientation);
                        futureOrientation = fromOrientation;
                     }
                     else {
                        break;
                     }
                  }
               }
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out) {
      super.serializeToStream(out);
      try {
         writeToStream(_actorID, out);
         writeListOrientationToStream(_selectedPath, out);
         writeListOrientationToStream(_futureOrientations, out);
         writeListOrientationToStream(_cancelOrientations, out);
         writeListOrientationToStream(_completeOrientations, out);
         ArrayList<Integer> futureOrientationsSourceIndexIntoFutureOrientations = new ArrayList<> ();
         for (Orientation futureOrientation : _futureOrientations) {
            Orientation sourceOrientation = _mapOfFutureOrientToSourceOrient.get(futureOrientation);
            if (sourceOrientation == null) {
               futureOrientationsSourceIndexIntoFutureOrientations.add(Integer.valueOf(-1));
            }
            else {
               int indexIntoFutureOrientations = _futureOrientations.indexOf(sourceOrientation);
               if (indexIntoFutureOrientations != -1) {
                  futureOrientationsSourceIndexIntoFutureOrientations.add(Integer.valueOf(indexIntoFutureOrientations));
               }
               else {
                  DebugBreak.debugBreak();
                  futureOrientationsSourceIndexIntoFutureOrientations.add(Integer.valueOf(-1));
               }
            }
         }
         writeToStream(futureOrientationsSourceIndexIntoFutureOrientations, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("RequestMovement: ");
      sb.append(", futureOrientations:").append(_futureOrientations.toString());
      sb.append(", cancelOrientations:").append(_cancelOrientations.toString());
      sb.append(", completeOrientations:").append(_completeOrientations.toString());
      sb.append(", actorID:").append(_actorID);
      return sb.toString();
   }

   public boolean setOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter) {
      Orientation orient;
      orient = getBestFutureOrientation(loc, angleFromCenter, normalizedDistFromCenter);
      if (orient != null) {
         _selectedPath = getRouteToFutureOrientation(orient);
         if (_selectedPath != null) {
            // Once we have selected the path, we can release all the possible orientations
            // and their route-to mapping, so these memories can be freed
            _futureOrientations.clear();
            _mapOfFutureOrientToSourceOrient.clear();
            return true;
         }
      }
      return false;
   }
   public ArrayList<Orientation> getRouteToFutureOrientation(Orientation futureOrientation) {
      ArrayList<Orientation> path = new ArrayList<>();
      while (futureOrientation != null) {
         path.add(futureOrientation);
         futureOrientation = _mapOfFutureOrientToSourceOrient.get(futureOrientation);
//         int index = _futureOrientations.indexOf(futureOrientation);
//         if (index == -1)
//            break;
//         Integer sourceIndex = _futureOrientationsSourceIndexIntoFutureOrientations.get(index);
//         if (sourceIndex >= 0)
//            futureOrientation = _futureOrientations.get(sourceIndex.intValue());
//         else if (sourceIndex != -1)
//            break;
      }
      // Remove the starting location so if we use this as the move path,
      // we aren't selecting our self, causing the move to end.
      // However, if they clicked on the start location, then the size of
      // the path will only be 1, in which case, we allow that location.
      if (path.size() > 1) {
         path.remove(path.size()-1);
      }
      return path;
   }
//   public Orientation getBestOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter) {
//      return getBestOrientation(loc, angleFromCenter, normalizedDistFromCenter, _orientations);
//   }
   public Orientation getBestFutureOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter) {
      return getBestOrientation(loc, angleFromCenter, normalizedDistFromCenter, _futureOrientations);
   }
   static private Orientation getBestOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter, List<Orientation> orientations) {
      Facing facing = getFacingFromAngle(angleFromCenter);
      ArrayList<Orientation> orientationsAtLocation = new ArrayList<>();
      for (int i=0 ; i<orientations.size() ; i++) {
         Orientation orient = orientations.get(i);
         if (orient.getHeadCoordinates().sameCoordinates(loc)) {
            if (orient.getFacing() == facing) {
               return orient;
            }
            orientationsAtLocation.add(orient);
         }
      }
      if (orientationsAtLocation.size() == 0) {
         return null;
      }
      if (orientationsAtLocation.size() == 1) {
         return orientationsAtLocation.get(0);
      }
      // find the next closest facing, by looking for a single
      // orientation that is 1 facing change away, then look for one
      // that is 2 facing changes away.
      for (int facingDif = 1 ; facingDif <= 2 ; facingDif++) {
         ArrayList<Orientation> orientationsFacing = new ArrayList<>();
         for (Orientation orient : orientationsAtLocation) {
            if ((orient.getFacing() == facing.turn(facingDif)) ||
                (orient.getFacing().turn(facingDif) == facing)) {
               orientationsFacing.add(orient);
            }
         }
         if (orientationsFacing.size() == 1) {
            return orientationsFacing.get(0);
         }
         if (orientationsFacing.size() > 1) {
            // TODO: maybe look at the angle to see which one is closer?
            return null;
         }
      }
      return null;
   }

   static private Facing getFacingFromAngle(double angleFromCenter)
   {
      while (angleFromCenter < 0) {
         angleFromCenter += (Math.PI * 2);
      }
      if (angleFromCenter < (Math.PI / 3.0)) {
         return Facing._10_OCLOCK;
      }
      if (angleFromCenter < ((2*Math.PI) / 3.0)) {
         return Facing.NOON;
      }
      if (angleFromCenter < ((3*Math.PI) / 3.0)) {
         return Facing._2_OCLOCK;
      }
      if (angleFromCenter < ((4*Math.PI) / 3.0)) {
         return Facing._4_OCLOCK;
      }
      if (angleFromCenter < ((5*Math.PI) / 3.0)) {
         return Facing._6_OCLOCK;
      }
      return Facing._8_OCLOCK;
   }

   public ArrayList<ArenaCoordinates> getFutureCoordinates() {
      ArrayList<ArenaCoordinates> locations = new ArrayList<>();
      for (Orientation orient : _futureOrientations) {
         locations.add(orient.getHeadCoordinates());
      }
      return locations;
   }

   public int getActorID() {
      return _actorID;
   }

   public boolean hasMovesLeft() {
      return (_selectedPath != null) && (_selectedPath.size()>0);
   }
   public void forceEndOfMovement() {
      if (_selectedPath != null) {
         _selectedPath.clear();
      }
   }

   public List<Orientation> getCancelOrientations() {
      return _cancelOrientations;
   }

   public List<Orientation> getCompletionOrientations() {
      return _completeOrientations;
   }

   public boolean moveByKeystroke(KeyEvent arg0, CombatMap map) {
      Orientation currentOrientation = null;
      if (_completeOrientations.size() == 1) {
         currentOrientation = _completeOrientations.get(0);
      }
      else if (_cancelOrientations.size() == 1) {
         currentOrientation = _cancelOrientations.get(0);
      }
      else {
         return false;
      }
      ArenaCoordinates headLoc = currentOrientation.getHeadCoordinates();
      Facing moveDir = null;
      Facing facingDir = currentOrientation.getFacing();
      if (arg0.keyCode == ' ') {
         return setOrientation(currentOrientation);
      }
      else if (arg0.keyCode == SWT.ARROW_UP) {
         moveDir = currentOrientation.getFacing();
      }
      else if (arg0.keyCode == SWT.ARROW_RIGHT) {
         if ((arg0.stateMask & SWT.CTRL) != 0) { // ctrl key IS down
            moveDir = facingDir.turn(2);
         }
         else {
            facingDir = facingDir.turn(1);
            if ((arg0.stateMask & SWT.SHIFT) == 0) { // shift key NOT down
               moveDir = facingDir;
            }
         }
      }
      else if (arg0.keyCode == SWT.ARROW_LEFT) {
         if ((arg0.stateMask & SWT.CTRL) != 0) { // ctrl key IS down
            moveDir = facingDir.turn(4);
         }
         else {
            facingDir = facingDir.turn(5);
            if ((arg0.stateMask & SWT.SHIFT) == 0) { // shift key NOT down
               moveDir = facingDir;
            }
         }
      }
      else if (arg0.keyCode == SWT.ARROW_DOWN) {
         moveDir = facingDir.turn(3);
      }
      else {
         return false;
      }

      if (moveDir != null) {
         headLoc = map.getLocation((short)(headLoc._x + moveDir.moveX),
                                   (short)(headLoc._y + moveDir.moveY));
      }

      for (Orientation possibleMove : _futureOrientations) {
         if (possibleMove.getHeadCoordinates().sameCoordinates(headLoc)) {
            if (possibleMove.getFacing() == facingDir) {
               return setOrientation(possibleMove);
            }
         }
      }
      return false;
   }
}
