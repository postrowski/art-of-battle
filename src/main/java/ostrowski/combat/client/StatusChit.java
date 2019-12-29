package ostrowski.combat.client;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Condition;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.server.CombatServer;


public class StatusChit extends Dialog implements PaintListener
{
   private static HashMap<String, Point> LOCATION_BY_CHARACTER_NAME = new HashMap<>();
   private final ArrayList<TextWheel> _textWheels = new ArrayList<>();
   private Shell _shell = null;

   public String _name;
   private final int _nameFontSize = 20;
   private Point _nameCenterLoc = new Point(0,0);

   private TextWheel _position, _posAdj, _actions, _initiative, _wounds, _pain, _weapon, _magic, _misc;

   public Point _offsetFromParent = null; // if this is null, we are not pinned to the parent, otherwise we are.

   public StatusChit(Shell parent, int style) {
      super (parent, SWT.MODELESS | SWT.NO_TRIM | style);
      TextWheel._fullWidth = 450;
      TextWheel._fullHieght = 350;

      _nameCenterLoc = new Point((int) (TextWheel._fullWidth * .6), (int)(TextWheel._fullHieght * .48));
      //                           percentages   angle         edge       angle  labelOffset    label       label  text  text
      //                         radius cw block Open          edge       Width  Vert Horiz     label      Height  text  Height
      _textWheels.add(_position   = new TextWheel(.79, .52, .70,  6,   TextWheel.Edge.LEFT, 40,  .10, -.60, "Position",      (18.0/500.0), "Standing", (16.0/500.0)));
      _textWheels.add(_posAdj     = new TextWheel(.79, .52, .15,  6,   TextWheel.Edge.LEFT, 40, -.10, -.40, " a  p  d  r ",  (18.0/500.0), " 0  0  0  0 ", (16.0/500.0)));
      _textWheels.add(_actions    = new TextWheel(.15, .30, .50, 60,    TextWheel.Edge.TOP,  0,-.035,  .00, "Actions",       (20.0/500.0), "4", (14.0/500.0)));
      _textWheels.add(_initiative = new TextWheel(.20, .60, .60, 20,    TextWheel.Edge.TOP,  0, -.08,  .00, "Initiative",    (20.0/500.0), "2", (14.0/500.0)));
      _textWheels.add(_wounds     = new TextWheel(.09, .20, .50, 50,  TextWheel.Edge.RIGHT,  0, -.10, -.04, "Wounds",        (20.0/500.0), "0", (14.0/500.0)));
      _textWheels.add(_pain       = new TextWheel(.21, .48, .65, 25,  TextWheel.Edge.RIGHT,  0, -.10,  .09, "Pain",          (20.0/500.0), "0", (16.0/500.0)));
      _textWheels.add(_weapon     = new TextWheel(.12, .78, .25, 45,  TextWheel.Edge.RIGHT,  0, -.10, -.04, "Weapon",        (20.0/500.0), "Ready", (12.0/500.0)));
      _textWheels.add(_magic      = new TextWheel(.35, .50, .70, 25, TextWheel.Edge.BOTTOM,  0,  .12,  .00, " Magic\nPoints",(20.0/500.0), "0", (14.0/500.0)));
      _textWheels.add(_misc       = new TextWheel(.15, .75, .55, 40, TextWheel.Edge.BOTTOM,  0, .045,  .00, "Misc",          (18.0/500.0), "+0", (14.0/500.0)));
   }

   public void updateFromCharacter(Character chr) {
      Condition cond = chr.getCondition();
      _name = chr.getName();
      if (_shell != null) {
         Point currentLoc = LOCATION_BY_CHARACTER_NAME.get(_name);
         if (currentLoc == null) {
            currentLoc = _shell.getLocation();
            LOCATION_BY_CHARACTER_NAME.put(_name, currentLoc);
         }
         else {
            _shell.setLocation(currentLoc);
         }
      }

      _position._text =  cond.getPosition().name;
      int attackAdj = cond.getPosition().adjustmentToAttack;
      int parryAdj = cond.getPosition().adjustmentToDefenseParry;
      int dodgeAdj = cond.getPosition().adjustmentToDefenseDodge;
      int retreatAdj = cond.getPosition().adjustmentToDefenseRetreat;

      _posAdj._text = " " + ((attackAdj  < 0) ? attackAdj  : ((attackAdj == 0) ? " 0" : ("+" + attackAdj)))
                    + " " + ((parryAdj   < 0) ? parryAdj   : ((parryAdj  == 0) ? " 0" : ("+" + parryAdj)))
                    + " " + ((dodgeAdj   < 0) ? dodgeAdj   : ((dodgeAdj  == 0) ? " 0" : ("+" + dodgeAdj)))
                    + " " + ((retreatAdj < 0) ? retreatAdj : ((retreatAdj== 0) ? " 0" : ("+" + retreatAdj)));
      _actions._text = String.valueOf(cond.getActionsAvailable(false/*defOnly*/));
      _initiative._text = String.valueOf(cond.getInitiative());
      _wounds._text = String.valueOf(cond.getWounds());
      _pain._text = String.valueOf(cond.getPenaltyPain());
      Limb weaponHand = chr.getLimb(LimbType.HAND_RIGHT);
      if ((weaponHand == null) || weaponHand.isCrippled()) {
         weaponHand = chr.getLimb(LimbType.HAND_LEFT);
      }
      if (weaponHand != null) {
         byte actionsNeededToReady = weaponHand.getActionsNeededToReady();
         if (actionsNeededToReady == 0) {
            _weapon._text = "ready";
         }
         else {
            _weapon._text = String.valueOf(actionsNeededToReady);
         }
      }
      else {
         _weapon._text = "";
      }
      _magic._text = String.valueOf(cond.getMageSpellPointsAvailable() + cond.getPriestSpellPointsAvailable());
      _misc._text = "+0";
      if (_shell != null) {
         _shell.redraw();
      }
   }

   public void open() {
      Shell parent = getParent();
      _shell = new Shell(parent, SWT.MODELESS | SWT.NO_BACKGROUND | SWT.TRANSPARENT | SWT.NO_TRIM | SWT.NO_FOCUS);

      ControlListener parentControlListener = new ControlListener() {
         @Override
         public void controlResized(ControlEvent arg0) {
         }
         @Override
         public void controlMoved(ControlEvent arg0) {
            if (_offsetFromParent != null) {
               Point parentLoc = getParent().getLocation();
               setLocation(new Point(parentLoc.x + _offsetFromParent.x,
                                     parentLoc.y + _offsetFromParent.y));
            }
         }
      };
      parent.addControlListener(parentControlListener);

      //new Snippet134(parent);

      _shell.setText("Status Chit");
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.makeColumnsEqualWidth = true;
      layout.horizontalSpacing = SWT.FILL;
      _shell.setLayout(layout);

      _shell.setLayout(new GridLayout(1/*numcolumns*/, false/*makeColumnsEqualWidth*/));

//      shell.setSize(TextWheel._fullWidth + 25, TextWheel._fullHieght + 50);
      _shell.addPaintListener(this);

      //add ability to move shell around
      Listener l = new Listener() {
         Point origin;
         @Override
         public void handleEvent(Event e) {
            switch (e.type) {
               case SWT.MouseDown:
                  origin = new Point(e.x, e.y);
                  break;
               case SWT.MouseUp:
                  origin = null;
                  break;
               case SWT.MouseMove:
                  if (origin != null) {
                     Point p = _shell.getDisplay().map(_shell, null, e.x, e.y);
                     setLocation(new Point(p.x - origin.x, p.y - origin.y));

                     if (CombatServer._uses3dMap) {
                        CombatServer._this.redrawMap();
                     }
                  }
                  break;
               case SWT.KeyDown:
               case SWT.KeyUp:
                  // bubble the keystrokes up to the parent dialog, so it can handle them:
                  getParent().notifyListeners(e.type, e);
                  break;
            }
         }
      };
      // Register listeners for all the events we do something with:
      _shell.addListener(SWT.MouseDown, l);
      _shell.addListener(SWT.MouseUp, l);
      _shell.addListener(SWT.MouseMove, l);
      _shell.addListener(SWT.KeyDown, l);
      _shell.addListener(SWT.KeyUp, l);
      _shell.pack();

      Region region = TextWheel.getRectRegion();
      for (TextWheel wheel : _textWheels) {
         Region wheelRegion = wheel.getRegion();
         region.add(wheelRegion );
         wheelRegion.dispose();
      }
      for (TextWheel wheel : _textWheels) {
         Region holeRegion = wheel.getHoleRegion();
         region.subtract(holeRegion );
         holeRegion.dispose();
      }

      Rectangle size = region.getBounds();
      _shell.setRegion(region);
      region.dispose();
      _shell.setSize(size.width, size.height);

      _shell.open();
      //_shell.setSize(TextWheel._fullWidth + 10, TextWheel._fullHieght + 10);

      if (_name != null) {
         Point currentLoc = LOCATION_BY_CHARACTER_NAME.get(_name);
         if (currentLoc == null) {
            currentLoc = _shell.getLocation();
            LOCATION_BY_CHARACTER_NAME.put(_name, currentLoc);
         }
         else {
            _shell.setLocation(currentLoc);
         }
      }
      checkForPinning();
   }

   private void checkForPinning() {
      Rectangle parentRect = getParent().getBounds();
      Rectangle thisRect = _shell.getBounds();
      if (parentRect.intersects(thisRect)) {
         _offsetFromParent = new Point(thisRect.x - parentRect.x,
                                       thisRect.y - parentRect.y);
         if (_offsetFromParent.y < 0) {
            _offsetFromParent.y = _offsetFromParent.y;
         }
      }
      else {
         _offsetFromParent = null;
      }
   }

   public void close() {
      if (!_shell.isDisposed()) {
         _shell.close();
      }
   }

   @Override
   public void paintControl(PaintEvent event) {
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase1(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      TextWheel.drawPhase2(event.gc);
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase3(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase4(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase5(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase6(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));

      TextWheel.drawText(event.gc, _nameCenterLoc, _nameFontSize, _name);
   }

   public void setLocation(Point location) {
      _shell.setLocation(location);
      checkForPinning();
      if (_name != null) {
         LOCATION_BY_CHARACTER_NAME.put(_name, location);
      }
   }
}

class TextWheel {
   public static int _fullWidth  = 400;
   public static int _fullHieght = 300;

   public enum Edge { TOP, LEFT, BOTTOM, RIGHT}

   private int _radius;
   private final double _percentClockwiseAlongEdge;
   private final int _blockingSize;
   private Point _blockingPoint1;
   private Point _blockingPoint2;
   private final int _angleOpen;
   private final Edge _edge;
   private final String _label;
   private final int _labelFontSize;
   private Point _labelCenterLoc = new Point(0,0);
   public String _text;
   private final int _textFontSize;
   private final Point _textCenterLoc = new Point(0,0);
   private final Point _center = new Point(0,0);
   private int _centerAngle = 0;
   private int _angleWidth = 0;

   public TextWheel(double radiusPercentage, double percentClockwiseAlongEdge,
                    double percentBlocking, int angleOpen, Edge edge,
                    int angleWidth, double labelVertOffsetPercent,
                    double labelHorizontalOffsetPercentage, String label,
                    double labelFontSizePercentage, String text, double textFontSizePercentage) {
      _percentClockwiseAlongEdge = percentClockwiseAlongEdge;
      _angleOpen = angleOpen;
      _edge = edge;
      _angleWidth = angleWidth;
      _label = label;
      _labelFontSize = (int) (labelFontSizePercentage * _fullHieght);
      _text = text;
      _textFontSize = (int)(textFontSizePercentage * _fullHieght);
      if ((_edge == Edge.TOP) || (_edge == Edge.BOTTOM)) {
         _radius = (int) (radiusPercentage * _fullHieght);
      }
      else {
         _radius = (int) (radiusPercentage * _fullWidth);
      }
      _blockingSize = (int) (_radius * percentBlocking);

      switch (_edge) {
         case TOP:
            _center.x = (int) (_fullWidth * _percentClockwiseAlongEdge);
            _center.y = _radius;
            _centerAngle = 90;
            _textCenterLoc.x = _center.x;
            _textCenterLoc.y = (((_center.y - _radius) + _center.y) - _blockingSize) / 2;
            _blockingPoint1 = new Point(_center.x - _blockingSize, _center.y - _blockingSize);
            _blockingPoint2 = new Point(_center.x + _blockingSize, _center.y - _blockingSize);
            break;
         case BOTTOM:
            _center.x = (int) (_fullWidth * (1-_percentClockwiseAlongEdge));
            _center.y = _fullHieght - _radius;
            _centerAngle = 270;
            _textCenterLoc.x = _center.x;
            _textCenterLoc.y = ((_center.y + _radius) + _center.y + _blockingSize) / 2;
            _blockingPoint1 = new Point(_center.x + _blockingSize, _center.y + _blockingSize);
            _blockingPoint2 = new Point(_center.x - _blockingSize, _center.y + _blockingSize);
            break;
         case LEFT:
            _center.x = _radius;
            _center.y = (int) (_fullHieght * (1-_percentClockwiseAlongEdge));
            _centerAngle = 180;
            _textCenterLoc.y = _center.y;
            _textCenterLoc.x = (((_center.x - _radius) + _center.x) - _blockingSize) / 2;
            _blockingPoint1 = new Point(_center.x - _blockingSize, _center.y + (_blockingSize / 2));
            _blockingPoint2 = new Point(_center.x - _blockingSize, _center.y - (_blockingSize / 2));
            break;
         case RIGHT:
            _center.x = _fullWidth - _radius;
            _center.y = (int) (_fullHieght * _percentClockwiseAlongEdge);
            _centerAngle = 0;
            _textCenterLoc.y = _center.y;
            _textCenterLoc.x = ((_center.x + _radius) + _center.x + _blockingSize) / 2;
            _blockingPoint1 = new Point(_center.x + _blockingSize, _center.y - _blockingSize);
            _blockingPoint2 = new Point(_center.x + _blockingSize, _center.y + _blockingSize);
            break;
      }
      int labelVertOffset = (int) (labelVertOffsetPercent * _fullHieght);
      int labelHorizontalOffset = (int) (labelHorizontalOffsetPercentage * _fullWidth);
      _labelCenterLoc = new Point(_center.x + labelHorizontalOffset, _center.y + labelVertOffset);
      if (_edge == Edge.LEFT) {
         if (labelHorizontalOffsetPercentage > 0) {
            _textCenterLoc.x = _labelCenterLoc.x;
         }
         else {
            _labelCenterLoc.x = _textCenterLoc.x;
         }
      }
   }

   public void drawPhase1(GC gc) {
      if (_angleWidth != 0) {
         fillArc(gc, _center.x, _center.y, _radius, _centerAngle, _angleWidth);
      }
      else {
         fillCircle(gc, _center.x, _center.y, _radius);
      }
   }
   public static void drawPhase2(GC gc) {
      Rectangle rect = new Rectangle((int)(.03 * _fullWidth),
                                     (int)(.03 * _fullHieght),
                                     (int)(.94 * _fullWidth)/*width*/,
                                     (int)(.94 * _fullHieght)/*height*/);
      gc.fillRectangle(rect);
      gc.drawRectangle(rect);
   }
   public void drawPhase3(GC gc) {
      fillArc(gc, _center.x, _center.y, _radius, _centerAngle, _angleOpen);
   }
   public void drawPhase4(GC gc) {
      gc.fillPolygon(new int[] {_center.x, _center.y,
                                _blockingPoint1.x, _blockingPoint1.y,
                                _blockingPoint2.x, _blockingPoint2.y});
   }
   public void drawPhase5(GC gc) {
      drawText(gc, _labelCenterLoc, _labelFontSize, _label);
   }
   public void drawPhase6(GC gc) {
      drawText(gc, _textCenterLoc, _textFontSize, _text);
      int innerHoleRadius = _fullHieght / 50;
      drawCircle(gc, _center.x, _center.y, innerHoleRadius);
      fillCircle(gc, _center.x, _center.y, innerHoleRadius);
   }

   static FontData _fontData = null;
   static Font _font = null;
   static void drawText(GC gc, Point textCenterLoc, int textFontSize, String text) {
      if ((_fontData == null) || (_fontData.height != textFontSize)) {
         if (_fontData != null) {
            // TODO: dispose of this object:
            _fontData = null;
         }
         _fontData = new FontData("Arial", textFontSize/*height*/, SWT.BOLD);
         if (_font != null) {
            _font.dispose();
         }
         _font = new Font(Display.getCurrent(), _fontData);
         gc.setFont(_font);
      }
      drawTextCentered(gc, textCenterLoc.x, textCenterLoc.y, text);
   }
   public static void drawTextCentered(GC gc, int centerX, int centerY, String text) {
      if (text == null) {
         return;
      }
      Point textExtent = gc.textExtent(text);
      gc.drawText(text, centerX - (textExtent.x/2), centerY - (textExtent.y/2));
   }

   public static void drawCircle(GC gc, int centerX, int centerY, int radius) {
      gc.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
   }
   public static void fillCircle(GC gc, int centerX, int centerY, int radius) {
      gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
      gc.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
   }
   public static void fillArc(GC gc, int centerX, int centerY, int radius, int centerAngle, int angleWidth) {
      int startAngle = centerAngle - (angleWidth/2);
      gc.fillArc(centerX - radius/*x-topleft*/, centerY - radius/*y-topleft*/, radius * 2/*width*/, radius * 2/*height*/, startAngle, angleWidth);
      gc.drawArc(centerX - radius/*x-topleft*/, centerY - radius/*y-topleft*/, (radius * 2/*width*/), (radius * 2/*height*/), startAngle, angleWidth);
   }

   public static Region getRectRegion() {
      Region region = new Region();
      int[] polygon = new int[8];
      polygon[0] = (int)(.03 * _fullWidth);      polygon[1] = (int)(.03 * _fullHieght);
      polygon[2] = (int)(.97 * _fullWidth) + 2;  polygon[3] = (int)(.03 * _fullHieght);
      polygon[4] = (int)(.97 * _fullWidth) + 2;  polygon[5] = (int)(.97 * _fullHieght) + 2;
      polygon[6] = (int)(.03 * _fullWidth);      polygon[7] = (int)(.97 * _fullHieght) + 2;
      region.add(polygon);
      return region;

   }
   public Region getRegion() {
      double startAngle = 0;
      double stopAngle = 2*Math.PI;
      if (_angleWidth != 0) {
         double centerAngle = _centerAngle;
         switch (_edge) {
            case LEFT: centerAngle += 90; break;
            case RIGHT: centerAngle -= 90; break;
            case BOTTOM: centerAngle += 180; break;
         }
         startAngle = Math.toRadians(centerAngle - (_angleWidth/2));
         stopAngle = Math.toRadians(centerAngle + (_angleWidth/2));
      }
      return getRegion(_radius, _center, startAngle, stopAngle);
   }

   public Region getHoleRegion() {
      double innerHoleRadius = (_fullHieght / 50.0) - 2.5;
      double startAngle = 0;
      double stopAngle = 2*Math.PI;
      return getRegion(innerHoleRadius, _center, startAngle, stopAngle);
   }

   public static Region getRegion(double radius, Point center, double startAngle, double stopAngle) {
      radius += 0.5;
      ArrayList<Point> points = new ArrayList<>();
      points.add(center);
      double stepInRadians = 1.0/radius;
      for (double angle = startAngle ; angle < stopAngle ; angle += stepInRadians) {
         points.add(new Point((int)(center.x + (radius * Math.sin(angle))),
                              (int) (center.y  + (radius * Math.cos(angle)))));
      }
      int[] polygon = new int[points.size() * 2];
      for (int i=0 ; i< points.size() ; i++) {
         polygon[i*2] = points.get(i).x;
         polygon[(i*2)+1] = points.get(i).y;
         if (polygon[i*2] > center.x) {
            polygon[i*2]++;
         }
         if (polygon[(i*2)+1] > center.y) {
            polygon[(i*2)+1]++;
         }
      }
      Region region = new Region();
      region.add(polygon);
      return region;
   }
}
