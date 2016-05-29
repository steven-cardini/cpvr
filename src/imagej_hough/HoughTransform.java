package imagej_hough;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class HoughTransform implements PlugInFilter {
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
    final int nonMaxR = 1; // Radius for non max. suppression

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
    int[] lineRad = new int[nLines]; // array for radius of strongest lines
    int[] lineAng = new int[nLines]; // array for angle of strongest lines

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
              if (hough1[t][r] > maxAccum)
                maxAccum++;
            }
          }
        }
      }
    }

    long msSpace = System.currentTimeMillis();

    // Build non maximum supression with radius nonMaxR into hough2
    // ???

    // Build histogram of the array hough2
    // ???

    // Get n strongest lines into arrays lineAng & lineRad
    // ???

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
      png.writeImage(imgAccum, "../../Images/PolygonAccum.png", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("maxAccum: " + maxAccum);
    System.out.println("Time for Hough space: " + (msSpace - msStart) + " ms");
    System.out.println("Time for Hough lines: " + (msLines - msSpace) + " ms");
  }

  public static void main(String[] args) {
    HoughTransform plugin = new HoughTransform();
    ImagePlus im = new ImagePlus("../../Images/Polygon2.png");
    im.show();
    plugin.setup("", im);
    plugin.run(im.getProcessor());
  }
}
