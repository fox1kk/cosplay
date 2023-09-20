package ru.sis.cosplay.jphash.image.radial;

import ru.sis.cosplay.jphash.util.HexUtil;

public class RadialHash {
  private final int[] coefficients;

  public RadialHash(int numberOfcoefficients1) {
    this.coefficients = new int[numberOfcoefficients1];
  }

  public int[] getCoefficients() {
    return coefficients;
  }

  @Override
  public String toString() {
    return HexUtil.intArrayToString(coefficients);
  }

  public static RadialHash fromString(String string) {
    RadialHash temp = new RadialHash(string.length() / 2);
    HexUtil.stringToIntArray(string, temp.coefficients);
    return temp;
  }
}
