package ostrowski.combat.common;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.util.AnglePair;

public class CharacterTest
{

   @Test
   public void testAnglePairs() {
      AnglePair pair = new AnglePair((30 * 2*Math.PI)/360, (50 * 2*Math.PI)/360);
      assertTrue(Math.abs(pair.width() - ((20 * 2*Math.PI)/360)) < 0.00000000001);
      assertTrue(Math.abs(pair._startAngle - ((30 * 2*Math.PI)/360)) < 0.00000000001);
   }

   @Test
   public void testSerializeToStream() {
      PriestSpell.generateHtmlTablePriestSpells();
      Character sourceChar = new Character();
      sourceChar.setRace("Human", Gender.MALE);
      sourceChar.setName("Joe");
//      CombatMap map = new CombatMap((short)10, (short)10, null);
//      ArenaLocation startLoc = map.getLocation((short)5, (short)5);
//      map.addCharacter(sourceChar, startLoc, null);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream();
           DataOutputStream dos = new DataOutputStream(out))
      {
         sourceChar.serializeToStream(dos);

         byte[] dataArray = out.toByteArray();
         int msgSize = dataArray.length;
         byte[] newBuf = new byte[msgSize+4];
         newBuf[0] = (byte)(msgSize >>> 24);
         newBuf[1] = (byte)(msgSize >>> 16);
         newBuf[2] = (byte)(msgSize >>>  8);
         newBuf[3] = (byte)(msgSize >>>  0);
         System.arraycopy(dataArray, 0, newBuf, 4, msgSize);
         try {
            dos.write(newBuf);
         } catch (IOException e2) {
            e2.printStackTrace();
         }


         String sourceStr = sourceChar.toString();

         //deserialize
         byte[] pickled = out.toByteArray();
//         pickled[0] = (byte)(out.size() >>> 24);
//         pickled[1] = (byte)(out.size() >>> 16);
//         pickled[2] = (byte)(out.size() >>>  8);
//         pickled[3] = (byte)(out.size() >>>  0);
//         System.arraycopy(msgBuf, 0, diagBuf, 4, msgSize);
//         appendByteBufferDump(sb, diagBuf);
         try (
            InputStream in = new ByteArrayInputStream(pickled);
            //ObjectInputStream ois = new ObjectInputStream(in);
            DataInputStream dis = new DataInputStream(in))
         {
            Character inChar = new Character();
            inChar.serializeFromStream(dis );
            String inStr = inChar.toString();

            assertTrue("serialization worked", inStr.equals(sourceStr));
         }
      } catch (IOException e3) {
         e3.printStackTrace();
      }
   }

   @Test
   public void testSerializeFromStream() {
      //fail("Not yet implemented");
   }

}
