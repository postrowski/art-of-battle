/*
 * Created on Oct 4, 2006
 *
 */
package ostrowski.combat.common.wounds;

@SuppressWarnings("serial")
public class WoundCantBeAppliedToTargetException extends RuntimeException
{
   public WoundCantBeAppliedToTargetException(String errorCause) {
      super(errorCause);
   }
}
