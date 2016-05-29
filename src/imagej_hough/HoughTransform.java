package imagej_hough;

import java.awt.Color;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class HoughTransform implements PlugInFilter {

  class HoughLine {
    private double mAngle, mRadius;

    public HoughLine(double angle, double radius) {
      mAngle = angle;
      mRadius = radius;
    }

    public double radius() {
      return mRadius;
    }

    public double angle() {
      return mAngle;
    }

  }

  @Override
  public int setup(String arg, ImagePlus imp) {
    return DOES_8G;
  }

  @Override
  public void run(ImageProcessor ip) {
    long msStart = System.currentTimeMillis();

    // Set up Hough space
    final int nAng = 256; // number of angels
    final int nRad = 256; // number of radii
    final int nLines = 4; // number of lines to search

    int xC = ip.getWidth() / 2; // x-coordinate of image center
    int yC = ip.getHeight() / 2; // y-coordinate of image center
    double dAng = (Math.PI / nAng); // step size of angle
    double rMax = Math.sqrt(xC * xC + yC * yC); // max. radius (center to
                                                // corner)
    double dRad = (2 * rMax) / nRad; // step size radius
    int[][] hough1 = new int[nAng][nRad]; // Hough accumulator space
    int[][] hough2 = new int[nAng][nRad]; // Hough accumulator space w. non max.
                                          // supression

    int maxAccum = 0; // max. value in Hough space
    double[] cos = new double[nAng]; // array for precalculated cos values
    double[] sin = new double[nAng]; // array for precalculated sin values
    HoughLine[] lines = new HoughLine[nLines]; // array for strongest lines

    // Precalculate cos & sin values
    for (int t = 0; t < nAng; t++) {
      double theta = dAng * t;
      cos[t] = Math.cos(theta);
      sin[t] = Math.sin(theta);
    }

    // Fill Hough array & keep maximum
    for (int y = 0; y < ip.getHeight(); y++) {
      for (int x = 0; x < ip.getWidth(); x++) {
        int pixel = ip.getPixel(x, y);
        if (pixel > 0) {
          int cx = x - xC, cy = y - yC;

          // Calculate for all theta angles the radius r & increment the hough
          // array
          for (int t = 0; t < nAng; t++) {
            int r = (int) Math.round((cx * cos[t] + cy * sin[t]) / dRad) + nRad / 2;
            if (r >= 0 && r < nRad) {
              hough1[t][r]++;
              if (hough1[t][r] > maxAccum) {
                maxAccum = hough1[t][r];
              }
            }
          }
        }
      }
    }

    long msSpace = System.currentTimeMillis();

    // Build non maximum supression with 3x3 mask into hough2
    // algorithm with nonMaxR not implemented because this is really unnecessary
    // .. !
    for (int ang = 0; ang < nAng; ang++) {
      for (int rad = 0; rad < nRad; rad++) {
        int max = 0;
        if (ang > 0) {
          max = Math.max(max, hough1[ang - 1][rad]); // pixel left
          if (rad > 0) {
            max = Math.max(max, hough1[ang - 1][rad - 1]); // pixel bottom left
            max = Math.max(max, hough1[ang][rad - 1]); // pixel bottom
          }
          if (rad < nRad - 1) {
            max = Math.max(max, hough1[ang - 1][rad + 1]); // pixel top left
            max = Math.max(max, hough1[ang][rad + 1]); // pixel top
          }
        }
        if (ang < nAng - 1) {
          max = Math.max(max, hough1[ang + 1][rad]); // pixel right
          if (rad > 0) {
            max = Math.max(max, hough1[ang + 1][rad - 1]); // pixel bottom right
          }
          if (rad < nRad - 1) {
            max = Math.max(max, hough1[ang + 1][rad + 1]); // pixel top right
          }
        }

        if (hough1[ang][rad] < max) {
          hough2[ang][rad] = 0; // non maximum suppression
        } else {
          hough2[ang][rad] = hough1[ang][rad];
        }

      }
    }

    // Build histogram of the array hough2
    int[] hist = new int[maxAccum + 1];
    for (int ang = 0; ang < nAng; ang++) {
      for (int rad = 0; rad < nRad; rad++) {
        hist[hough2[ang][rad]]++;
      }
    }

    // Get n strongest lines into array lines
    int found = 0;
    int value = maxAccum + 1;
    int amount = 0;

    while (found < nLines) {
      do {
        value--;
        amount = hist[value];
      } while (amount == 0);

      for (int ang = 0; ang < nAng; ang++) {
        for (int rad = 0; rad < nRad; rad++) {
          if (hough2[ang][rad] == value) {
            lines[found] = new HoughLine(dAng * ang, dRad * rad - rMax);
            found++;
          }
        }
      }

    }

    long msLines = System.currentTimeMillis();

    // Create RGB image and fill in Hough space as gray scale image
    ImagePlus imgAccum = NewImage.createRGBImage("imgAccum", nAng, nRad, 1, NewImage.FILL_BLACK);
    ImageProcessor ipAccum = imgAccum.getProcessor();
    int[] rgb = new int[3];
    for (int y = 0; y < ipAccum.getHeight(); y++) {
      for (int x = 0; x < ipAccum.getWidth(); x++) { // Scale Hough space so
                                                     // that
        rgb[0] = rgb[1] = rgb[2] = (int) (((float) hough1[x][y] / (float) maxAccum) * 255.0f);
        ipAccum.putPixel(x, y, rgb);
      }
    }

    // Show and save Hough space image
    imgAccum.show();
    imgAccum.updateAndDraw();
    PNG_Writer png = new PNG_Writer();
    try {
      png.writeImage(imgAccum, "img/PolygonAccum.png", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Add distance lines to image
    ImagePlus im_new = new ImagePlus("img/Polygon2.png");
    ImageProcessor ip_new = im_new.getProcessor();
    ip_new.setColor(Color.WHITE);

    int x1 = xC, y1 = yC;
    for (HoughLine line : lines) {
      int x2 = (int) (Math.cos(line.angle()) * line.radius()) + xC;
      int y2 = (int) (Math.sin(line.angle()) * line.radius()) + yC;
      ip_new.drawLine(x1, y1, x2, y2);
    }
    im_new.show();

    System.out.println("maxAccum: " + maxAccum);
    System.out.println("Time for Hough space: " + (msSpace - msStart) + " ms");
    System.out.println("Time for Hough lines: " + (msLines - msSpace) + " ms");
  }

  public static void main(String[] args) {
    HoughTransform plugin = new HoughTransform();
    ImagePlus im = new ImagePlus("img/Polygon2.png");
    im.show();
    plugin.setup("", im);
    plugin.run(im.getProcessor());
  }
}
