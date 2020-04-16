package ostrowski.combat.common;

import org.eclipse.swt.graphics.*;
import ostrowski.DebugBreak;

import java.util.ArrayList;
import java.util.List;

public class DrawnObject
{
   private       List<Point> _points = new ArrayList<>();
   private       RGB         _fgRGB;
   private       RGB               _bgRGB;
   private final List<DrawnObject> _children = new ArrayList<>();

   public DrawnObject(RGB fgRGB, RGB bgRGB) {
      _fgRGB = fgRGB;//==null) ? null : new RGB(fgRGB.red, fgRGB.green, fgRGB.blue);
      _bgRGB = bgRGB;//==null) ? null : new RGB(bgRGB.red, bgRGB.green, bgRGB.blue);
   }
   public void addChild(DrawnObject child) {
      if (child == null) {
         DebugBreak.debugBreak();
         return;
      }
      _children.add(child);
   }

   public void addPoint(int x, int y) {
      _points.add(new Point(x,y));
   }
   public void addPoint(double x, double y) {
      _points.add(new Point((int)(Math.round(x)), (int)(Math.round(y))));
   }

   public void draw(GC gc, Device display) {
      Color bgColor = (_bgRGB == null) ? null : new Color(display, _bgRGB);
      Color fgColor = (_fgRGB == null) ? null : new Color(display, _fgRGB);

      if (bgColor != null) {
         gc.setBackground(bgColor);
      }
      if (fgColor != null) {
         gc.setForeground(fgColor);
      }

      int[] points = new int[_points.size()*2];
      int i=0;
      for (Point point : _points) {
         points[i++] = point.x;
         points[i++] = point.y;
      }
      if (bgColor != null) {
         gc.fillPolygon(points);
      }
      if (fgColor != null) {
         gc.drawPolygon(points);
      }

      if (bgColor != null) {
         bgColor.dispose();
      }
      if (fgColor != null) {
         fgColor.dispose();
      }

      for (DrawnObject child : _children) {
         child.draw(gc, display);
      }
   }

   public void flipPoints(boolean alongHorizontalAxis) {
      for (Point point : _points) {
         if (alongHorizontalAxis) {
            point.y *= -1;
         }
         else {
            point.x *= -1;
         }
      }
   }
   public void offsetPoints(int x, int y) {
      if (((x==0) && (y==0)) || (_points == null)) {
         return;
      }
      for (Point point : _points) {
         point.x += x;
         point.y += y;
      }
      for (DrawnObject child : _children) {
         child.offsetPoints(x, y);
      }
   }
   public void rotatePoints(double angle) {
      if (angle == 0)
       {
         return; // nothing to do
      }

      if (_points == null) {
         return;
      }

      for (Point point : _points) {
         double dist = Math.sqrt((point.x*point.x) + (point.y*point.y));
         double curAng = Math.atan2(point.y, point.x);
         double newAng = curAng + angle;
         point.x = (int) Math.round(dist * Math.cos(newAng));
         point.y = (int) Math.round(dist * Math.sin(newAng));
      }
      for (DrawnObject child : _children) {
         child.rotatePoints(angle);
      }
   }
   public int getXPoint(int index) {
      return _points.get(index).x;
   }
   public int getYPoint(int index) {
      return _points.get(index).y;
   }

   public static DrawnObject createElipse(int wideDiameter, int narrowDiameter, int pointCount, RGB foreground, RGB background) {
      DrawnObject obj = new DrawnObject(foreground, background);
      double stepSize  = (2 * Math.PI)/pointCount;
      for (int point=0 ; point<pointCount ; point++) {
         double ang = point * stepSize;
         int x = (int) Math.round((Math.sin(ang) * wideDiameter)/2 );
         int y = (int) Math.round((Math.cos(ang) * narrowDiameter)/2);
         obj.addPoint(x, y);
      }
      return obj;
   }

   public void trimToHexBounds(int[] hexBounds) {
      trimToYBounds(hexBounds[(2*4)+1], hexBounds[(2*1)+1]);
      rotatePoints(0-(Math.PI/3));
      trimToYBounds(hexBounds[(2*4)+1], hexBounds[(2*1)+1]);
      rotatePoints(0-(Math.PI/3));
      trimToYBounds(hexBounds[(2*4)+1], hexBounds[(2*1)+1]);
      rotatePoints((2*Math.PI)/3);
   }
   public void trimToYBounds(int minY, int maxY) {
      // This only trims the vertical.
      // First test all point for being below the min,
      // and then test for all points being above in the max.
      // This will let us catch the case where one line goes from below the min to above the max.
      boolean minTest=false;
      do {
         List<Point> newPoints = new ArrayList<>();
         Point prevPoint = null;
         int skipsAllowed = _points.size();
         Point lastAddedPoint = null;
         while (_points.size() > 0) {
            Point thisPoint = _points.remove(0);
            if (isInBounds(thisPoint, minTest, minY, maxY)) {
               if (prevPoint != null) {
                  if (!isInBounds(prevPoint, minTest, minY, maxY)) {
                     // add the re-entry intersection point
                     Point intersectPoint = getIntersection(prevPoint, thisPoint, (minTest ? minY : maxY));
                     if (intersectPoint == null) {
                        DebugBreak.debugBreak();
                     }
                     if ((lastAddedPoint == null) || (!lastAddedPoint.equals(intersectPoint))) {
                        newPoints.add(intersectPoint);
                        lastAddedPoint = intersectPoint;
                     }
                  }
               }
               // Add the valid interior point
               if ((lastAddedPoint == null) || (!lastAddedPoint.equals(thisPoint))) {
                  // If the last point was out-of-bounds, then we re-added the
                  // first point back to the end of the _points list to get the
                  // intersection point. If this is the case, don't re-add the
                  // thisPoint, because its already at the front of the list.
                  if ((_points.size() > 0) ||
                      (newPoints.size() ==0) ||
                      (!newPoints.get(0).equals(thisPoint))) {
                     newPoints.add(thisPoint);
                     lastAddedPoint = thisPoint;
                  }
               }
            }
            else {
               // have we found a point inside the valid range yet? if so, prevPoint will be non-null.
               if (prevPoint == null) {
                  // put this point back at the end of the original list, so we'll process it again later.
                  _points.add(thisPoint);
                  if (skipsAllowed-- == 0) {
                     // The entire set of points is out of bounds!
                     _points.clear();
                     return;
                  }
                  continue;
               }
               if (isInBounds(prevPoint, minTest, minY, maxY)) {
                  // add the exit intersection point
                  Point intersectPoint = getIntersection(prevPoint, thisPoint, (minTest ? minY : maxY));
                  if (intersectPoint == null) {
                     DebugBreak.debugBreak();
                  }
                  if ((lastAddedPoint == null) || (!lastAddedPoint.equals(intersectPoint))) {
                     newPoints.add(intersectPoint);
                     lastAddedPoint = intersectPoint;
                  }
               }
               else {
                  // Neither the previous point or the current point are in bounds
                  // so we can skip adding the current point into the new list.
                  // This effectively throws away prevPoint.
                  if (_points.size() == 0) {
                     // If this is the last point, we still need to reconnect to the first point,
                     _points.add(newPoints.get(0));
                  }
               }
            }
            prevPoint = thisPoint;
         }
         _points = newPoints;
         minTest=!minTest;
      } while (minTest);

      for (DrawnObject child : _children) {
         child.trimToYBounds(minY, maxY);
      }
   }

   private static boolean isInBounds(Point point, boolean minTest, int minY, int maxY) {
      if (minTest) {
         return point.y >= minY;
      }
      return point.y <= maxY;
   }
   /**
    * Returns the intersection point of two lines, so long as the point of intersection exists on Line1
    * (between the two provided points {x1, y1}-{x2, y2}).
    *
    * @param   p1Line1   First line start point
    * @param   p2Line1   First line end point
    * @param   p1Line2   Second line start point
    * @param   p2Line2   Second line end point
    * @return  The Point object where the two lines intersect. This method
    * returns null if the two lines do not intersect.
    */
   static public Point getIntersection(Point p1Line1, Point p2Line1,
                                       Point p1Line2, Point p2Line2)
   {
     /* check to see if the segments have any endpoints in common. If they do,
        then return the endpoints as the intersection point */
      if (p1Line1.equals(p1Line2) || p1Line1.equals(p2Line2)) {
         return p1Line1;
      }
      if (p2Line1.equals(p1Line2) || p2Line1.equals(p2Line2)) {
         return p2Line1;
      }


     double dyLine1 = -( p2Line1.y - p1Line1.y );
     double dxLine1 = p2Line1.x - p1Line1.x;

     double dyLine2 = -( p2Line2.y - p1Line2.y );
     double dxLine2 = p2Line2.x - p1Line2.x;

     if( ((dyLine1 * dxLine2) - (dyLine2 * dxLine1)) == 0 ) {
        // These are parallel lines, so they meet at all overlapping points, or never at all.
        //throw new MultipleIntersectionException();
        return null;
     }

     double e = -(dyLine1 * p1Line1.x) - (dxLine1 * p1Line1.y);
     double f = -(dyLine2 * p1Line2.x) - (dxLine2 * p1Line2.y);

     // compute the intersection point using
     // ax+by+e = 0 and cx+dy+f = 0
     int xIntersect = (int) Math.round(((dxLine1 * f) - (e * dxLine2))/((dyLine1 * dxLine2) - (dyLine2 * dxLine1)));
     int yIntersect = (int) Math.round(((dyLine2 * e) - (dyLine1 * f))/((dyLine1 * dxLine2) - (dyLine2 * dxLine1)));
     if ((Math.min(p2Line1.x, p1Line1.x) <= xIntersect) && (Math.max(p2Line1.x, p1Line1.x) >= xIntersect) &&
         (Math.min(p2Line1.y, p1Line1.y) <= yIntersect) && (Math.max(p2Line1.y, p1Line1.y) >= yIntersect)) {
      return new Point(xIntersect, yIntersect);
   }
     return null;
   }

   /**
    * Returns the intersection point of the line y=c and the line (p1-p2), so long as the point of intersection exists on Line
    *
    * @param   p1   line start point
    * @param   p2   line end point
    * @param   y   The position of the horizontal line to intersect the line.
    * @return  The Point object where the two lines intersect. This method
    * returns null if the two lines do not intersect.
    */
   static private Point getIntersection( Point p1, Point p2, int y)
   {
     double dyLine = -( p2.y - p1.y );
     double dxLine = p2.x - p1.x;

     if( dyLine == 0 ) {
        // These are parallel lines, so they meet at all overlapping points, or never at all.
//        if (p1.y == y) {
//           throw new MultipleIntersectionException();
//        }
        return null;
     }

     double e = -(dyLine * p1.x) - (dxLine * p1.y);
     double f = - y;

     // compute the intersection point using
     // ax+by+e = 0 and cx+dy+f = 0
     int xIntersect = (int) Math.round(((dxLine * f) - e)/dyLine);
     int yIntersect = (int) Math.round((-dyLine * f)/dyLine);
     if ((Math.min(p2.x, p1.x) <= xIntersect) && (Math.max(p2.x, p1.x) >= xIntersect) &&
         (Math.min(p2.y, p1.y) <= yIntersect) && (Math.max(p2.y, p1.y) >= yIntersect)) {
      return new Point(xIntersect, yIntersect);
   }
     return null;
   }
   public int getPointCount() {
      return _points.size();
   }
}
