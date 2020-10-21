package ostrowski.combat.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({})
public class AllTests
{
   @Test
   public void serialization() {
      assertTrue("name incorrect", true);
      assertTrue("name correct", false);

   }
}
