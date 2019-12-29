package ostrowski;

/*
 * Drawing with transformations, paths and alpha blending
 *
 * For a list of all SWT example snippets see
 * http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/platform-swt-home/dev.html#snippets
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import ostrowski.combat.common.DiceSet;

import java.util.*;

//import java.io.File;
//import java.util.ArrayList;
//
//
//import ostrowski.combat.common.Character;
//import ostrowski.combat.common.CharacterGenerator;
//import ostrowski.combat.common.CombatMap;
//import ostrowski.combat.common.spells.Spell;
//import ostrowski.combat.server.ArenaLocation;
//import ostrowski.combat.server.CombatServer;
//
public class Tester
{

   //   /**
   //    * @param args
   //    */
   //   public static void main(String[] args)
   //   {
   //      Spell.generateHtmlTable();
   //      CombatServer._isServer = true;
   //      CombatServer._pseudoRandomNumberSeed = 123;
   //      for (int i=1 ; i<=10 ; i++) {
   //         Character c = CharacterGenerator.generateRandomCharacter(i*50, null/*race*/, false/*genNewPseudoRndNumber*/);
   //         System.out.println(c.getPointTotal() + "/" + i*50+ ":" + c.toString());
   //      }
   //
   //      CombatMap map = new CombatMap();
   //      map.serializeFromFile(new File("arenas\\town.xml"));
   //      ArenaLocation fromLoc = map.getLocation((short)5, (short)45);
   //      ArenaLocation toLoc = map.getLocation((short)15, (short)49);
   //      ArrayList<ArenaLocation> path1 = map.getPath(fromLoc, toLoc, false/*trimPath*/);
   //      ArrayList<ArenaLocation> path2 = map.getPath(toLoc, fromLoc, false/*trimPath*/);
   //      path1.size();
   //      path2.size();
   //   }
   //
   //
   //}



   public static void main(String[] args) {

      HashSet<DiceSet> diceSetBase = new HashSet<>();
      diceSetBase.add(new DiceSet("1d4"));
      diceSetBase.add(new DiceSet("1d6"));
      diceSetBase.add(new DiceSet("1d8"));
      //diceSetBase.add(new DiceSet("1d10"));
      //diceSetBase.add(new DiceSet("1d12"));
      //diceSetBase.add(new DiceSet("1d20"));
      HashSet<DiceSet> diceSet = new HashSet<>();
      for (DiceSet set : diceSetBase) {
         diceSet.add(set);
         diceSet.add(set.addBonus(-1));
         diceSet.add(set.addBonus(-2));
         diceSet.add(set.addBonus(-3));
         diceSet.add(set.addBonus(-4));
         diceSet.add(set.addBonus(-5));
         diceSet.add(set.addBonus(-6));
         diceSet.add(set.addBonus(-7));
         diceSet.add(set.addBonus(-8));
      }

      HashMap<DiceSet, Integer> accumulatorWithNegs = new HashMap<>();
      HashMap<DiceSet, Integer> accumulatorNoNegs = new HashMap<>();
      HashMap<DiceSet, Integer> nonZeroRollsCount = new HashMap<>();
      for (DiceSet set : diceSet) {
         accumulatorNoNegs.put(set, 0);
         accumulatorWithNegs.put(set, 0);
         nonZeroRollsCount.put(set, 0);
      }
      int i=0;
      for (i=0 ; i< 100000 ; i++)
      {
         for (DiceSet dice : diceSet) {
            int roll = dice.roll(true);
            Integer val = accumulatorNoNegs.get(dice);
            if (roll > 0 ) {
               val += roll;
               nonZeroRollsCount.put(dice, nonZeroRollsCount.get(dice) + 1);
            }
            accumulatorNoNegs.put(dice, val);
            accumulatorWithNegs.put(dice, accumulatorNoNegs.get(dice) + roll);
         }
      }
      List<Integer> counts = new ArrayList<>(accumulatorNoNegs.values());
      Collections.sort(counts);
      System.out.println("die type\tchanceNoZero\taverageNoNegs\taverageWithNegs\texpected roll");
      for (Integer count : counts) {
         for (DiceSet set : diceSet) {
            if (accumulatorNoNegs.get(set) == count) {
               StringBuilder name = new StringBuilder(set.toString());
               while (name.length() < 6) {
                  name.insert(0, " ");
               }
               String averageNoNegs   = TrimAndPad((((float)count) / i));
               String averageWithNegs = TrimAndPad((((float)accumulatorWithNegs.get(set)) / i));
               String chanceNoZero    = TrimAndPad((((float)nonZeroRollsCount.get(set)) / i));
               String expectedRoll    = TrimAndPad(set.getAverageRoll(true));
               System.out.println(name + "\t" + chanceNoZero + "\t" + averageNoNegs + "\t" + averageWithNegs + "\t" + expectedRoll );
            }
         }
      }
   }

   private static String TrimAndPad(double sourceVal) {

      StringBuilder sourceString = new StringBuilder("" + ((Math.round(sourceVal * 10000)) / 10000.0));
      while (sourceString.length() < 5) {
         sourceString.append("0");
      }
      return sourceString.toString();
   }

   public static void main2(String[] args) {

      final Display display = new Display();
      final Shell shell = new Shell(display);
      shell.setText("Advanced Graphics");
      FontData fd = shell.getFont().getFontData()[0];
      final Font font = new Font(display, fd.getName(), 60, SWT.BOLD
                                 | SWT.ITALIC);
      final Image image = new Image(display, 640, 480);
      final Rectangle rect = image.getBounds();
      GC newGc = new GC(image);
      newGc.setBackground(display.getSystemColor(SWT.COLOR_RED));
      newGc.fillOval(rect.x, rect.y, rect.width, rect.height);
      newGc.dispose();
      shell.addListener(SWT.Paint, new Listener() {
         @Override
         public void handleEvent(Event event) {
            GC gc = event.gc;
            Transform tr = new Transform(display);
            tr.translate(100, 120);
            tr.rotate(-30);
            gc.drawImage(image, 0, 0, rect.width, rect.height, 0, 0,
                         rect.width / 2, rect.height / 2);
            gc.setAlpha(128);
            gc.setTransform(tr);
            Path path = new Path(display);
            path.addString("SWT", 0, 0, font);
            gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
            gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
            gc.fillPath(path);
            gc.drawPath(path);
            tr.dispose();
            path.dispose();
         }
      });
      shell.setSize(shell.computeSize(rect.width / 2, rect.height / 2));
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
      }
      image.dispose();
      font.dispose();
      display.dispose();
   }
}

