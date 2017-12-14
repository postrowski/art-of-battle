/*
 * Created on Jun 8, 2007
 *
 */
package ostrowski.combat.common;

public class SpecialDamage
{
   // special damage types:
   public static final int MOD_NONE             = 0;
   public static final int MOD_FLAMING          = 1 << 0;
   public static final int MOD_FREEZING         = 1 << 1;
   public static final int MOD_EXPLODING        = 1 << 2;
   public static final int MOD_POISONED         = 1 << 3;
   public static final int MOD_ADDITIONAL_PAIN  = 1 << 4;
   public static final int MOD_REDUCED_PAIN     = 1 << 5;
   public static final int MOD_ADDITIONAL_WOUNDS= 1 << 6;
   public static final int MOD_REDUCED_WOUNDS   = 1 << 7;
   public static final int MOD_PAIN_1           = 1 << 8;
   public static final int MOD_PAIN_MASK        = MOD_PAIN_1 * 15; // allow 0-15 points of pain (bits 8,9,10,11)
   public static final int MOD_WOUND_1          = 1 << 12;
   public static final int MOD_WOUND_MASK       = MOD_WOUND_1 * 15; // allow 0-15 wounds (bits 12,13,14,15)
   public static final int MOD_NO_ARMOR         = 1 << 16;
   //public static final int MOD_NO_BUILD         = 1 << 17;

   private int _bits;

   public SpecialDamage(int modifiers) {
      _bits = modifiers;
   }
   public int getBits() {
      return _bits;
   }
   public void setBits(int bits) {
      _bits = bits;
   }
   public void setPainModifier(byte painModifier) {
      // clear the pain bits
      _bits &= (~(MOD_ADDITIONAL_PAIN | MOD_REDUCED_PAIN | MOD_PAIN_MASK));

      if (painModifier > 0) {
         _bits |= MOD_ADDITIONAL_PAIN;
      }
      if (painModifier < 0) {
         _bits |= MOD_REDUCED_PAIN;
      }
      _bits |= (MOD_PAIN_MASK & (MOD_PAIN_1 * Math.abs(painModifier)));
      if (getPainModifier() != painModifier) {
         throw new IllegalArgumentException("pain modifier out of range " + painModifier + " != " + getPainModifier());
      }
   }
   public byte getPainModifier()
   {
      int value = (_bits & MOD_PAIN_MASK) / MOD_PAIN_1;
      if (value != 0) {
         if ((_bits & MOD_ADDITIONAL_PAIN) != 0) {
            return (byte) value;
         }
         if ((_bits & MOD_REDUCED_PAIN) != 0) {
            return (byte) (0-value);
         }
      }
      return 0;
   }
   public void setWoundModifier(byte woundModifier) {
      // clear the wound bits
      _bits &= (~(MOD_ADDITIONAL_WOUNDS | MOD_REDUCED_WOUNDS | MOD_WOUND_MASK));

      if (woundModifier > 0) {
         _bits |= MOD_ADDITIONAL_WOUNDS;
      }
      if (woundModifier < 0) {
         _bits |= MOD_REDUCED_WOUNDS;
      }
      _bits |= (MOD_WOUND_MASK & (MOD_WOUND_1 * Math.abs(woundModifier)));
      if (getWoundModifier() != woundModifier) {
         throw new IllegalArgumentException("woundmodifier out of range " + woundModifier + " != " + getWoundModifier());
      }
   }
   public byte getWoundModifier()
   {
      int value = (_bits & MOD_WOUND_MASK) / MOD_WOUND_1;
      if (value != 0) {
         if ((_bits & MOD_ADDITIONAL_WOUNDS) != 0) {
            return (byte) value;
         }
         if ((_bits & MOD_REDUCED_WOUNDS) != 0) {
            return (byte) (0-value);
         }
      }
      return 0;
   }
}
