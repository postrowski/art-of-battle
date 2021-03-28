/*
 * Created on Jun 6, 2006
 *
 */
package ostrowski.combat.protocol.request;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RequestMovement extends SyncRequest implements Enums
{
   private List<Orientation>                 completeOrientations;
   private List<Orientation>                 cancelOrientations;
   private List<Orientation>                 selectedPath;
   //private List<Orientation>                 orientations;
   private List<Orientation>                 futureOrientations;
   private HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient;
   private int                               actorID;

   public RequestMovement() {
      completeOrientations = new ArrayList<>();
      cancelOrientations = new ArrayList<>();
   }

   public boolean setOrientation(Orientation orientation) {
      if (futureOrientations.contains(orientation)) {
         if (cancelOrientations.contains(orientation)) {
            setAnswerID(OPT_CANCEL_ACTION);
         }
         selectedPath = getRouteToFutureOrientation(orientation);
         return true;
      }
      return false;
   }
   @Override
   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         //setAnswerID(i);
         answer = new RequestOption("", i, true);
         if (futureOrientations.size() > i) {
            if (cancelOrientations.contains(futureOrientations.get(i))) {
               setAnswerID(OPT_CANCEL_ACTION);
            }
         }
         //_answerStr = futureOrientations.get(i).toString();
      }
   }
   public List<Orientation> getOrientations() {
      return futureOrientations;
   }
   public void setOrientations(List<Orientation> futureOrientations,
                               HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient, Character actor) {
      this.futureOrientations = futureOrientations;
      this.mapOfFutureOrientToSourceOrient = mapOfFutureOrientToSourceOrient;
      actorID = actor.uniqueID;
      cancelOrientations.clear();
      completeOrientations.clear();
      if (actor.hasMovedThisRound()) {
         completeOrientations.add(actor.getOrientation());
      }
      else {
         cancelOrientations.add(actor.getOrientation());
      }
   }
   public Orientation getAnswerOrientation(boolean removeEntry) {
      if (selectedPath.isEmpty()) {
         return null;
      }
      if (removeEntry) {
         return selectedPath.remove(selectedPath.size() - 1);
      }
      return selectedPath.get(selectedPath.size() - 1);
   }

   @Override
   public boolean isAnswered() {
      return (selectedPath != null);
   }

   @Override
   public int getActionCount() {
      return super.getActionCount() + futureOrientations.size();
   }

   @Override
   public void copyAnswer(SyncRequest source) {
      if (source instanceof RequestMovement) {
         RequestMovement sourceMove = (RequestMovement) source;
         selectedPath = sourceMove.selectedPath;
         futureOrientations = sourceMove.futureOrientations;
         completeOrientations = sourceMove.completeOrientations;
         cancelOrientations = sourceMove.cancelOrientations;

         mapOfFutureOrientToSourceOrient = sourceMove.mapOfFutureOrientToSourceOrient;
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
         selectedPath = new ArrayList<>();
         futureOrientations = new ArrayList<>();
         cancelOrientations = new ArrayList<>();
         completeOrientations = new ArrayList<>();
         List<Integer> futureOrientationsSourceIndexIntoFutureOrientations = new ArrayList<>();
         actorID = readInt(in);
         readIntoListOrientation(selectedPath, in);
         readIntoListOrientation(futureOrientations, in);
         readIntoListOrientation(cancelOrientations, in);
         readIntoListOrientation(completeOrientations, in);
         readIntoListInteger(futureOrientationsSourceIndexIntoFutureOrientations, in);
         {
            // load up the HashMap with all the known routes
            mapOfFutureOrientToSourceOrient = new HashMap<>();
            for (Orientation futureOrientation : futureOrientations) {
               while (futureOrientation != null) {
                  int index = futureOrientations.indexOf(futureOrientation);
                  if (index != -1) {
                     Integer sourceIndex = futureOrientationsSourceIndexIntoFutureOrientations.get(index);
                     if (sourceIndex >= 0) {
                        Orientation fromOrientation = futureOrientations.get(sourceIndex);
                        mapOfFutureOrientToSourceOrient.put(futureOrientation, fromOrientation);
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
         writeToStream(actorID, out);
         writeListOrientationToStream(selectedPath, out);
         writeListOrientationToStream(futureOrientations, out);
         writeListOrientationToStream(cancelOrientations, out);
         writeListOrientationToStream(completeOrientations, out);
         List<Integer> futureOrientationsSourceIndexIntoFutureOrientations = new ArrayList<> ();
         for (Orientation futureOrientation : futureOrientations) {
            Orientation sourceOrientation = mapOfFutureOrientToSourceOrient.get(futureOrientation);
            if (sourceOrientation == null) {
               futureOrientationsSourceIndexIntoFutureOrientations.add(-1);
            }
            else {
               int indexIntoFutureOrientations = futureOrientations.indexOf(sourceOrientation);
               if (indexIntoFutureOrientations != -1) {
                  futureOrientationsSourceIndexIntoFutureOrientations.add(indexIntoFutureOrientations);
               }
               else {
                  DebugBreak.debugBreak();
                  futureOrientationsSourceIndexIntoFutureOrientations.add(-1);
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
      return super.toString() +
             "RequestMovement: " +
             ", futureOrientations:" + futureOrientations +
             ", cancelOrientations:" + cancelOrientations +
             ", completeOrientations:" + completeOrientations +
             ", actorID:" + actorID;
   }

   public boolean setOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter) {
      Orientation orient;
      orient = getBestFutureOrientation(loc, angleFromCenter, normalizedDistFromCenter);
      if (orient != null) {
         selectedPath = getRouteToFutureOrientation(orient);
         if (selectedPath != null) {
            // Once we have selected the path, we can release all the possible orientations
            // and their route-to mapping, so these memories can be freed
            futureOrientations.clear();
            mapOfFutureOrientToSourceOrient.clear();
            return true;
         }
      }
      return false;
   }
   public List<Orientation> getRouteToFutureOrientation(Orientation futureOrientation) {
      List<Orientation> path = new ArrayList<>();
      while (futureOrientation != null) {
         path.add(futureOrientation);
         futureOrientation = mapOfFutureOrientToSourceOrient.get(futureOrientation);
//         int index = futureOrientations.indexOf(futureOrientation);
//         if (index == -1)
//            break;
//         Integer sourceIndex = futureOrientationsSourceIndexIntoFutureOrientations.get(index);
//         if (sourceIndex >= 0)
//            futureOrientation = futureOrientations.get(sourceIndex.intValue());
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
//      return getBestOrientation(loc, angleFromCenter, normalizedDistFromCenter, orientations);
//   }
   public Orientation getBestFutureOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter) {
      return getBestOrientation(loc, angleFromCenter, normalizedDistFromCenter, futureOrientations);
   }
   static private Orientation getBestOrientation(ArenaLocation loc, double angleFromCenter, double normalizedDistFromCenter, List<Orientation> orientations) {
      Facing facing = getFacingFromAngle(angleFromCenter);
      List<Orientation> orientationsAtLocation = new ArrayList<>();
      for (Orientation orient : orientations) {
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
         List<Orientation> orientationsFacing = new ArrayList<>();
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

   public List<ArenaCoordinates> getFutureCoordinates() {
      List<ArenaCoordinates> locations = new ArrayList<>();
      for (Orientation orient : futureOrientations) {
         locations.add(orient.getHeadCoordinates());
      }
      return locations;
   }

   public int getActorID() {
      return actorID;
   }

   public boolean hasMovesLeft() {
      return (selectedPath != null) && (selectedPath.size() > 0);
   }
   public void forceEndOfMovement() {
      if (selectedPath != null) {
         selectedPath.clear();
      }
   }

   public List<Orientation> getCancelOrientations() {
      return cancelOrientations;
   }

   public List<Orientation> getCompletionOrientations() {
      return completeOrientations;
   }

   public boolean moveByKeystroke(KeyEvent arg0, CombatMap map) {
      Orientation currentOrientation;
      if (completeOrientations.size() == 1) {
         currentOrientation = completeOrientations.get(0);
      }
      else if (cancelOrientations.size() == 1) {
         currentOrientation = cancelOrientations.get(0);
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
         headLoc = map.getLocation((short)(headLoc.x + moveDir.moveX),
                                   (short)(headLoc.y + moveDir.moveY));
      }

      for (Orientation possibleMove : futureOrientations) {
         if (possibleMove.getHeadCoordinates().sameCoordinates(headLoc)) {
            if (possibleMove.getFacing() == facingDir) {
               return setOrientation(possibleMove);
            }
         }
      }
      return false;
   }
}
