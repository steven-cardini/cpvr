package imagej_billard;

import java.awt.Color;

public class BayerProcessor {

  private static int MAX_VALUE = 255;
  private static float RED_WEIGHT = 0.299f;
  private static float GREEN_WEIGHT = 0.587f;
  private static float BLUE_WEIGHT = 0.114f;

  private byte[] pixels;
  private int width;
  private int height;

  public BayerProcessor(byte[] pixels, int width, int height) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
  }

  public int getValue(int index) {
    int[] rgb = calculateRGB(index);
    int value = (int) (RED_WEIGHT * rgb[0] + GREEN_WEIGHT * rgb[1] + BLUE_WEIGHT * rgb[2]);

    return Math.min(value, MAX_VALUE);
  }

  public int getRGB(int index) {
    int[] rgb = calculateRGB(index);
    int value = rgb[2];
    value += rgb[0] << 16;
    value += rgb[1] << 8;

    return value;
  }

  public float getHue(int index) {
    int[] rgb = calculateRGB(index);
    float[] hsb = new float[3];
    Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsb);

    return (hsb[0] * MAX_VALUE);
  }

  public float getBrightness(int index) {
    int[] rgb = calculateRGB(index);
    float[] hsb = new float[3];
    Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsb);

    return (hsb[2] * MAX_VALUE);
  }

  private int[] calculateRGB(int index) {

    int x = index % width;
    int y = index / width;
    PixelType pixelType = getPixelType(x, y);

    // final color values
    int red = 0, green = 0, blue = 0;
    int[] rgb = new int[3];

    switch (pixelType) {
    case GREEN_A:
      green = getUnsignedValue(x, y);
      blue = getMeanValueHorizontal(x, y);
      red = getMeanValueVertical(x, y);
      break;

    case GREEN_B:
      green = getUnsignedValue(x, y);
      blue = getMeanValueVertical(x, y);
      red = getMeanValueHorizontal(x, y);
      break;

    case RED:
      red = getUnsignedValue(x, y);
      blue = getMeanValueDiagonal(x, y);
      green = (getMeanValueHorizontal(x, y) + getMeanValueVertical(x, y)) / 2;
      break;

    case BLUE:
      blue = getUnsignedValue(x, y);
      red = getMeanValueDiagonal(x, y);
      green = (getMeanValueHorizontal(x, y) + getMeanValueVertical(x, y)) / 2;
      break;
    }

    rgb[0] = red;
    rgb[1] = green;
    rgb[2] = blue;
    return rgb;
  }

  private int getMeanValueHorizontal(int x, int y) {
    int counter = 0, value = 0;
    if (x > 0) {
      value += getUnsignedValue(x - 1, y);
      counter++;
    }
    if (x < width - 1) {
      value += getUnsignedValue(x + 1, y);
      counter++;
    }
    return Math.min(value / counter, MAX_VALUE);
  }

  private int getMeanValueVertical(int x, int y) {
    int counter = 0, value = 0;
    if (y > 0) {
      value += getUnsignedValue(x, y - 1);
      counter++;
    }
    if (y < height - 1) {
      value += getUnsignedValue(x, y + 1);
      counter++;
    }
    return Math.min(value / counter, MAX_VALUE);
  }

  private int getMeanValueDiagonal(int x, int y) {
    int counter = 0, value = 0;
    if (y > 0) {
      if (x > 0) {
        value += getUnsignedValue(x - 1, y - 1);
        counter++;
      }
      if (x < width - 1) {
        value += getUnsignedValue(x + 1, y - 1);
        counter++;
      }
    }
    if (y < height - 1) {
      if (x > 0) {
        value += getUnsignedValue(x - 1, y + 1);
        counter++;
      }
      if (x < width - 1) {
        value += getUnsignedValue(x + 1, y + 1);
        counter++;
      }
    }
    return Math.min(value / counter, MAX_VALUE);
  }

  private enum PixelType {
    GREEN_A, GREEN_B, RED, BLUE;
  }

  private PixelType getPixelType(int x, int y) {
    if (y % 2 == 0) { // even rows (0, 2, ..)
      if (x % 2 == 0) {
        return PixelType.GREEN_A;
      } else {
        return PixelType.BLUE;
      }
    } else { // odd rows (1, 3, ..)
      if (x % 2 == 0) {
        return PixelType.RED;
      } else {
        return PixelType.GREEN_B;
      }
    }
  }

  private int getUnsignedValue(int x, int y) {
    int index = y * width + x;
    byte rawValue = pixels[index];
    return (int) (rawValue & 0xff);
  }

}
