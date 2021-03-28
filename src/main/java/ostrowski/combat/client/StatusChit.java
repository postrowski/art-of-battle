package ostrowski.combat.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.Condition;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.server.CombatServer;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


public class StatusChit extends Dialog implements PaintListener
{
   private static final HashMap<String, Point> LOCATION_BY_CHARACTER_NAME = new HashMap<>();
   private final        List<TextWheel> textWheels = new ArrayList<>();
   private              Shell           shell      = null;

   public String            name;
   private static final int   NAME_FONT_SIZE = 20;
   private final        Point nameCenterLoc;

   private final TextWheel position;
   private final TextWheel posAdj;
   private final TextWheel actions;
   private final TextWheel initiative;
   private final TextWheel wounds;
   private final TextWheel pain;
   private final TextWheel weapon;
   private final TextWheel magic;
   private final TextWheel misc;

   public Point offsetFromParent = null; // if this is null, we are not pinned to the parent, otherwise we are.

   public StatusChit(Shell parent, int style) {
      super (parent, SWT.MODELESS | SWT.NO_TRIM | style);
      TextWheel.fullWidth = 450;
      TextWheel.fullHeight = 350;

      nameCenterLoc = new Point((int) (TextWheel.fullWidth * .6), (int)(TextWheel.fullHeight * .48));
      //                           percentages   angle         edge       angle  labelOffset    label       label  text  text
      //                         radius cw block Open          edge       Width  Vert Horiz     label      Height  text  Height
      textWheels.add(position = new TextWheel(.79, .52, .70, 6, TextWheel.Edge.LEFT, 40, .10, -.60, "Position", (18.0 / 500.0), "Standing", (16.0 / 500.0)));
      textWheels.add(posAdj = new TextWheel(.79, .52, .15, 6, TextWheel.Edge.LEFT, 40, -.10, -.40, " a  p  d  r ", (18.0 / 500.0), " 0  0  0  0 ", (16.0 / 500.0)));
      textWheels.add(actions = new TextWheel(.15, .30, .50, 60, TextWheel.Edge.TOP, 0, -.035, .00, "Actions", (20.0 / 500.0), "4", (14.0 / 500.0)));
      textWheels.add(initiative = new TextWheel(.20, .60, .60, 20, TextWheel.Edge.TOP, 0, -.08, .00, "Initiative", (20.0 / 500.0), "2", (14.0 / 500.0)));
      textWheels.add(wounds = new TextWheel(.09, .20, .50, 50, TextWheel.Edge.RIGHT, 0, -.10, -.04, "Wounds", (20.0 / 500.0), "0", (14.0 / 500.0)));
      textWheels.add(pain = new TextWheel(.21, .48, .65, 25, TextWheel.Edge.RIGHT, 0, -.10, .09, "Pain", (20.0 / 500.0), "0", (16.0 / 500.0)));
      textWheels.add(weapon = new TextWheel(.12, .78, .25, 45, TextWheel.Edge.RIGHT, 0, -.10, -.04, "Weapon", (20.0 / 500.0), "Ready", (12.0 / 500.0)));
      textWheels.add(magic = new TextWheel(.35, .50, .70, 25, TextWheel.Edge.BOTTOM, 0, .12, .00, " Magic\nPoints", (20.0 / 500.0), "0", (14.0 / 500.0)));
      textWheels.add(misc = new TextWheel(.15, .75, .55, 40, TextWheel.Edge.BOTTOM, 0, .045, .00, "Misc", (18.0 / 500.0), "+0", (14.0 / 500.0)));
   }

   public void updateFromCharacter(Character chr) {
      Condition cond = chr.getCondition();
      name = chr.getName();
      if (shell != null) {
         Point currentLoc = LOCATION_BY_CHARACTER_NAME.get(name);
         if (currentLoc == null) {
            currentLoc = shell.getLocation();
            LOCATION_BY_CHARACTER_NAME.put(name, currentLoc);
         }
         else {
            shell.setLocation(currentLoc);
         }
      }

      position.text =  cond.getPosition().name;
      int attackAdj = cond.getPosition().adjustmentToAttack;
      int parryAdj = cond.getPosition().adjustmentToDefenseParry;
      int dodgeAdj = cond.getPosition().adjustmentToDefenseDodge;
      int retreatAdj = cond.getPosition().adjustmentToDefenseRetreat;

      posAdj.text = " " + ((attackAdj < 0) ? attackAdj : ((attackAdj == 0) ? " 0" : ("+" + attackAdj)))
                    + " " + ((parryAdj   < 0) ? parryAdj   : ((parryAdj  == 0) ? " 0" : ("+" + parryAdj)))
                    + " " + ((dodgeAdj   < 0) ? dodgeAdj   : ((dodgeAdj  == 0) ? " 0" : ("+" + dodgeAdj)))
                    + " " + ((retreatAdj < 0) ? retreatAdj : ((retreatAdj== 0) ? " 0" : ("+" + retreatAdj)));
      actions.text = String.valueOf(cond.getActionsAvailable(false/*defOnly*/));
      initiative.text = String.valueOf(cond.getInitiative());
      wounds.text = String.valueOf(cond.getWounds());
      pain.text = String.valueOf(cond.getPenaltyPain());
      Limb weaponHand = chr.getLimb(LimbType.HAND_RIGHT);
      if ((weaponHand == null) || weaponHand.isCrippled()) {
         weaponHand = chr.getLimb(LimbType.HAND_LEFT);
      }
      if (weaponHand != null) {
         byte actionsNeededToReady = weaponHand.getActionsNeededToReady();
         if (actionsNeededToReady == 0) {
            weapon.text = "ready";
         }
         else {
            weapon.text = String.valueOf(actionsNeededToReady);
         }
      }
      else {
         weapon.text = "";
      }
      magic.text = String.valueOf(cond.getMageSpellPointsAvailable() + cond.getPriestSpellPointsAvailable());
      misc.text = "+0";
      if (shell != null) {
         shell.redraw();
      }
   }

   public void open() {
      Shell parent = getParent();
      shell = new Shell(parent, SWT.MODELESS | SWT.NO_BACKGROUND | SWT.TRANSPARENT | SWT.NO_TRIM | SWT.NO_FOCUS);

      ControlListener parentControlListener = new ControlListener() {
         @Override
         public void controlResized(ControlEvent arg0) {
         }
         @Override
         public void controlMoved(ControlEvent arg0) {
            if (offsetFromParent != null) {
               Point parentLoc = getParent().getLocation();
               setLocation(new Point(parentLoc.x + offsetFromParent.x,
                                     parentLoc.y + offsetFromParent.y));
            }
         }
      };
      parent.addControlListener(parentControlListener);

      //new Snippet134(parent);

      shell.setText("Status Chit");
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.makeColumnsEqualWidth = true;
      layout.horizontalSpacing = SWT.FILL;
      shell.setLayout(layout);

      shell.setLayout(new GridLayout(1/*numcolumns*/, false/*makeColumnsEqualWidth*/));

//      shell.setSize(TextWheel.fullWidth + 25, TextWheel.fullHieght + 50);
      shell.addPaintListener(this);

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
                     Point p = shell.getDisplay().map(shell, null, e.x, e.y);
                     setLocation(new Point(p.x - origin.x, p.y - origin.y));

                     if (CombatServer.uses3dMap) {
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
      shell.addListener(SWT.MouseDown, l);
      shell.addListener(SWT.MouseUp, l);
      shell.addListener(SWT.MouseMove, l);
      shell.addListener(SWT.KeyDown, l);
      shell.addListener(SWT.KeyUp, l);
      shell.pack();

      Region region = TextWheel.getRectRegion();
      for (TextWheel wheel : textWheels) {
         Region wheelRegion = wheel.getRegion();
         region.add(wheelRegion );
         wheelRegion.dispose();
      }
      for (TextWheel wheel : textWheels) {
         Region holeRegion = wheel.getHoleRegion();
         region.subtract(holeRegion );
         holeRegion.dispose();
      }

      Rectangle size = region.getBounds();
      shell.setRegion(region);
      region.dispose();
      shell.setSize(size.width, size.height);

      shell.open();
      //_shell.setSize(TextWheel.fullWidth + 10, TextWheel.fullHieght + 10);

      if (name != null) {
         Point currentLoc = LOCATION_BY_CHARACTER_NAME.get(name);
         if (currentLoc == null) {
            currentLoc = shell.getLocation();
            LOCATION_BY_CHARACTER_NAME.put(name, currentLoc);
         }
         else {
            shell.setLocation(currentLoc);
         }
      }
      checkForPinning();
   }

   private void checkForPinning() {
      Rectangle parentRect = getParent().getBounds();
      Rectangle thisRect = shell.getBounds();
      if (parentRect.intersects(thisRect)) {
         offsetFromParent = new Point(thisRect.x - parentRect.x,
                                       thisRect.y - parentRect.y);
      }
      else {
         offsetFromParent = null;
      }
   }

   public void close() {
      if (!shell.isDisposed()) {
         shell.close();
      }
   }

   @Override
   public void paintControl(PaintEvent event) {
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : textWheels) {
         tw.drawPhase1(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      TextWheel.drawPhase2(event.gc);
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : textWheels) {
         tw.drawPhase3(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : textWheels) {
         tw.drawPhase4(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : textWheels) {
         tw.drawPhase5(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : textWheels) {
         tw.drawPhase6(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));

      TextWheel.drawText(event.gc, nameCenterLoc, NAME_FONT_SIZE, name);
   }

   public void setLocation(Point location) {
      shell.setLocation(location);
      checkForPinning();
      if (name != null) {
         LOCATION_BY_CHARACTER_NAME.put(name, location);
      }
   }
}

class TextWheel {
   public static int fullWidth  = 400;
   public static int fullHeight = 300;
   static FontData   fontData   = null;
   static Font       font       = null;
   public enum Edge { TOP, LEFT, BOTTOM, RIGHT}

   private final int    radius;
   private       Point  blockingPoint1;
   private       Point  blockingPoint2;
   private final int    angleOpen;
   private final Edge   edge;
   private final String label;
   private final int    labelFontSize;
   private       Point  labelCenterLoc = new Point(0, 0);
   public        String text;
   private final int    textFontSize;
   private final Point  textCenterLoc  = new Point(0, 0);
   private final Point  center         = new Point(0, 0);
   private       int    centerAngle    = 0;
   private final int    angleWidth;

   public TextWheel(double radiusPercentage, double percentClockwiseAlongEdge,
                    double percentBlocking, int angleOpen, Edge edge,
                    int angleWidth, double labelVertOffsetPercent,
                    double labelHorizontalOffsetPercentage, String label,
                    double labelFontSizePercentage, String text, double textFontSizePercentage) {
      this.angleOpen = angleOpen;
      this.edge = edge;
      this.angleWidth = angleWidth;
      this.label = label;
      labelFontSize = (int) (labelFontSizePercentage * fullHeight);
      this.text = text;
      textFontSize = (int)(textFontSizePercentage * fullHeight);
      if ((this.edge == Edge.TOP) || (this.edge == Edge.BOTTOM)) {
         radius = (int) (radiusPercentage * fullHeight);
      }
      else {
         radius = (int) (radiusPercentage * fullWidth);
      }
      int blockingSize = (int) (radius * percentBlocking);

      switch (this.edge) {
         case TOP:
            center.x = (int) (fullWidth * percentClockwiseAlongEdge);
            center.y = radius;
            centerAngle = 90;
            textCenterLoc.x = center.x;
            textCenterLoc.y = (((center.y - radius) + center.y) - blockingSize) / 2;
            blockingPoint1 = new Point(center.x - blockingSize, center.y - blockingSize);
            blockingPoint2 = new Point(center.x + blockingSize, center.y - blockingSize);
            break;
         case BOTTOM:
            center.x = (int) (fullWidth * (1 - percentClockwiseAlongEdge));
            center.y = fullHeight - radius;
            centerAngle = 270;
            textCenterLoc.x = center.x;
            textCenterLoc.y = ((center.y + radius) + center.y + blockingSize) / 2;
            blockingPoint1 = new Point(center.x + blockingSize, center.y + blockingSize);
            blockingPoint2 = new Point(center.x - blockingSize, center.y + blockingSize);
            break;
         case LEFT:
            center.x = radius;
            center.y = (int) (fullHeight * (1 - percentClockwiseAlongEdge));
            centerAngle = 180;
            textCenterLoc.y = center.y;
            textCenterLoc.x = (((center.x - radius) + center.x) - blockingSize) / 2;
            blockingPoint1 = new Point(center.x - blockingSize, center.y + (blockingSize / 2));
            blockingPoint2 = new Point(center.x - blockingSize, center.y - (blockingSize / 2));
            break;
         case RIGHT:
            center.x = fullWidth - radius;
            center.y = (int) (fullHeight * percentClockwiseAlongEdge);
            centerAngle = 0;
            textCenterLoc.y = center.y;
            textCenterLoc.x = ((center.x + radius) + center.x + blockingSize) / 2;
            blockingPoint1 = new Point(center.x + blockingSize, center.y - blockingSize);
            blockingPoint2 = new Point(center.x + blockingSize, center.y + blockingSize);
            break;
      }
      int labelVertOffset = (int) (labelVertOffsetPercent * fullHeight);
      int labelHorizontalOffset = (int) (labelHorizontalOffsetPercentage * fullWidth);
      labelCenterLoc = new Point(center.x + labelHorizontalOffset, center.y + labelVertOffset);
      if (this.edge == Edge.LEFT) {
         if (labelHorizontalOffsetPercentage > 0) {
            textCenterLoc.x = labelCenterLoc.x;
         }
         else {
            labelCenterLoc.x = textCenterLoc.x;
         }
      }
   }

   public void drawPhase1(GC gc) {
      if (angleWidth != 0) {
         fillArc(gc, center.x, center.y, radius, centerAngle, angleWidth);
      }
      else {
         fillCircle(gc, center.x, center.y, radius);
      }
   }
   public static void drawPhase2(GC gc) {
      Rectangle rect = new Rectangle((int)(.03 * fullWidth),
                                     (int)(.03 * fullHeight),
                                     (int)(.94 * fullWidth)/*width*/,
                                     (int)(.94 * fullHeight)/*height*/);
      gc.fillRectangle(rect);
      gc.drawRectangle(rect);
   }
   public void drawPhase3(GC gc) {
      fillArc(gc, center.x, center.y, radius, centerAngle, angleOpen);
   }
   public void drawPhase4(GC gc) {
      gc.fillPolygon(new int[] {center.x, center.y,
                                blockingPoint1.x, blockingPoint1.y,
                                blockingPoint2.x, blockingPoint2.y});
   }
   public void drawPhase5(GC gc) {
      drawText(gc, labelCenterLoc, labelFontSize, label);
   }
   public void drawPhase6(GC gc) {
      drawText(gc, textCenterLoc, textFontSize, text);
      int innerHoleRadius = fullHeight / 50;
      drawCircle(gc, center.x, center.y, innerHoleRadius);
      fillCircle(gc, center.x, center.y, innerHoleRadius);
   }

   static void drawText(GC gc, Point textCenterLoc, int textFontSize, String text) {
      if ((fontData == null) || (fontData.height != textFontSize)) {
         if (fontData != null) {
            // TODO: dispose of this object:
            fontData = null;
         }
         fontData = new FontData("Arial", textFontSize/*height*/, SWT.BOLD);
         if (font != null) {
            font.dispose();
         }
         font = new Font(Display.getCurrent(), fontData);
         gc.setFont(font);
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
      polygon[0] = (int)(.03 * fullWidth);      polygon[1] = (int)(.03 * fullHeight);
      polygon[2] = (int)(.97 * fullWidth) + 2;  polygon[3] = (int)(.03 * fullHeight);
      polygon[4] = (int)(.97 * fullWidth) + 2;  polygon[5] = (int)(.97 * fullHeight) + 2;
      polygon[6] = (int)(.03 * fullWidth);      polygon[7] = (int)(.97 * fullHeight) + 2;
      region.add(polygon);
      return region;

   }
   public Region getRegion() {
      double startAngle = 0;
      double stopAngle = 2*Math.PI;
      if (angleWidth != 0) {
         double centerAngle = this.centerAngle;
         switch (edge) {
            case LEFT: centerAngle += 90; break;
            case RIGHT: centerAngle -= 90; break;
            case BOTTOM: centerAngle += 180; break;
         }
         startAngle = Math.toRadians(centerAngle - (angleWidth / 2));
         stopAngle = Math.toRadians(centerAngle + (angleWidth / 2));
      }
      return getRegion(radius, center, startAngle, stopAngle);
   }

   public Region getHoleRegion() {
      double innerHoleRadius = (fullHeight / 50.0) - 2.5;
      double startAngle = 0;
      double stopAngle = 2*Math.PI;
      return getRegion(innerHoleRadius, center, startAngle, stopAngle);
   }

   public static Region getRegion(double radius, Point center, double startAngle, double stopAngle) {
      radius += 0.5;
      List<Point> points = new ArrayList<>();
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
