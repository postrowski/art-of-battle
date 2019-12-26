/*
 * Created on May 31, 2006
 *
 */
package ostrowski.combat.common;

import org.eclipse.swt.widgets.Event;

import ostrowski.combat.server.ArenaLocation;

public interface IMapListener
{
   void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter);
   void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter);
   default void onRightMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }
   default void onRightMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }
   void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter);
   void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter);
}
