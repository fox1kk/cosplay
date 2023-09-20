package ru.sis.cosplay.jphash.util;

import java.awt.image.BufferedImage;

public class HexUtil {

  public static String byteArrayToString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(Integer.toHexString((b & 0xF0) >> 4));
      sb.append(Integer.toHexString(b & 0xF));
    }
    return sb.toString();
  }

  public static String intArrayToString(int[] ints) {
    StringBuilder sb = new StringBuilder();
    for (int b : ints) {
      sb.append(Integer.toHexString((b & 0xF0) >> 4));
      sb.append(Integer.toHexString(b & 0xF));
    }
    return sb.toString();
  }

  public static byte[] stringToByteArray(String string, byte[] destination) {
    if (destination == null) {
      destination = new byte[string.length() / 2];
    }
    int j = 0;
    for (int i = 0; i < string.length(); i += 2) {
      destination[j++] = Integer.valueOf(string.substring(i, i + 2), 16).byteValue();
    }
    return destination;
  }

  public static int[] stringToIntArray(String string, int[] destination) {
    if (destination == null) {
      destination = new int[string.length() / 2];
    }
    int j = 0;
    for (int i = 0; i < string.length(); i += 2) {
      destination[j++] = Integer.valueOf(string.substring(i, i + 2), 16);
    }
    return destination;
  }

  public static BufferedImage convertImg(BufferedImage bufferedImage) {
    if (bufferedImage.getType() == BufferedImage.TYPE_4BYTE_ABGR
        || bufferedImage.getType() == BufferedImage.TYPE_BYTE_BINARY) {
      BufferedImage bff =
          new BufferedImage(
              bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
      for (int y = 0; y < bufferedImage.getHeight(); ++y) {
        for (int x = 0; x < bufferedImage.getWidth(); ++x) {
          int argb = bufferedImage.getRGB(x, y);
          if ((argb & 0x00FFFFFF) == 0x00FFFFFF) { // if the pixel is transparent
            bff.setRGB(x, y, 0xFFFFFFFF); // white color.
          } else {
            bff.setRGB(x, y, argb);
          }
        }
      }
      return bff;
    }
    return bufferedImage;
  }
}
