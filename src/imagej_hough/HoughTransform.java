package imagej_hough;

import java.awt.Color;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class HoughTransform implements PlugInFilter {

  private final static int pixelClip = 2; // clip this amount of pixels from
                                          // image frame
  private ImagePlus mInputImage;
  private ImagePlus mOriginalImage;
  private String mImageName;
  private int mNrLines; // number of lines to search
  private double mAngleFrom; // minimal angle in Bogenmass
  private double mAngleTo; // maximal angle in Bogenmass
  private int mNonMaxR; // TODO: use as radius for non-max-suppression
  private Color mRadiusColor;
  private Color mLineColor;
  private HoughLine[] mFoundLines; // array for strongest lines
  private boolean mShowResult;

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

  public HoughTransform(ImagePlus inputImage, ImagePlus originalImage, int nrLines, double angleFrom, double angleTo, int nonMaxR, Color radiusColor,
      Color lineColor) {
    mInputImage = inputImage.duplicate();
    mOriginalImage = originalImage.duplicate();
    mNrLines = nrLines;
    mAngleFrom = angleFrom;
    mAngleTo = angleTo;
    mNonMaxR = nonMaxR;
    mRadiusColor = radiusColor;
    mLineColor = lineColor;
    mFoundLines = new HoughLine[mNrLines];

    mImageName = inputImage.getShortTitle();
  }

  public ImagePlus process(boolean showResult) {
    System.out.println("[hough] processing image " + mImageName);
    mShowResult = showResult;

    // convert image to grayscale for hough analysis
    ImageConverter ic = new ImageConverter(mInputImage);
    ic.convertToGray8();
    mInputImage.updateAndDraw();

    setup("", mInputImage);
    run(mInputImage.getProcessor());

    return mOriginalImage;
  }

  public HoughLine[] getLines() {
    return mFoundLines;
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

    int xC = ip.getWidth() / 2; // x-coordinate of image center
    int yC = ip.getHeight() / 2; // y-coordinate of image center
    double dAng = (Math.PI / nAng); // step size of angle
    int minAng = (int) (mAngleFrom / dAng);
    int maxAng = (int) (mAngleTo / dAng);
    double rMax = Math.sqrt(xC * xC + yC * yC); // max. radius (center to
                                                // corner)
    double dRad = (2 * rMax) / nRad; // step size radius
    int[][] hough1 = new int[nAng][nRad]; // Hough accumulator space
    int[][] hough2 = new int[nAng][nRad]; // Hough accumulator space w. non max.
                                          // supression

    int maxAccum = 0; // max. value in Hough space
    double[] cos = new double[nAng]; // array for precalculated cos values
    double[] sin = new double[nAng]; // array for precalculated sin values

    // Precalculate cos & sin values
    for (int t = 0; t < nAng; t++) {
      double theta = dAng * t;
      cos[t] = Math.cos(theta);
      sin[t] = Math.sin(theta);
    }

    // Fill Hough array & keep maximum
    for (int y = (0 + pixelClip); y < (ip.getHeight() - pixelClip); y++) {
      for (int x = (0 + pixelClip); x < (ip.getWidth() - pixelClip); x++) {
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

    // Build non maximum supression with mask (size: mNonMaxR) into hough2
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

    while (found < mNrLines) {
      do {
        value--;
        amount = hist[value];
      } while (amount == 0);

      for (int ang = minAng; ang < maxAng; ang++) {
        for (int rad = 0; rad < nRad; rad++) {
          if (hough2[ang][rad] == value) {
            mFoundLines[found] = new HoughLine(dAng * ang, dRad * rad - rMax);
            found++;
          }
          if (found >= mNrLines)
            break;
        }
        if (found >= mNrLines)
          break;
      }

    }

    long msLines = System.currentTimeMillis();

    createImage(hough1, maxAccum, mImageName + "_hough-space");
    createImage(hough2, maxAccum, mImageName + "_hough-space_nonMaxEl");

    // Add distance lines to image
    ImageProcessor ipTransformed = mOriginalImage.getProcessor();

    double x1 = xC, y1 = yC;
    for (HoughLine line : mFoundLines) {
      double x2 = Math.cos(line.angle()) * line.radius() + xC;
      double y2 = Math.sin(line.angle()) * line.radius() + yC;
      ipTransformed.setColor(mRadiusColor);
      ipTransformed.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

      // draw line itself
      // ------------------------------------------------------------------->
      // calculate direction vector of line: use vector perpendicular to (x1,y1)->(x2, y2)
      double vx = y1 - y2; // vector in x direction of line
      double vy = x2 - x1; // vector in y direction of line

      if (vx == 0 || vy == 0)
        continue; // do not draw lines right at the center

      // calculate parameters dx and dy to reach borders of image: (x2, y2) + (dx * vx, dy * vy)
      double dx0 = -(x2 / vx); // use this parameter to reach x=0 of line (x2 + dx0 * vx = 0)
      double dxE = (ip.getWidth() - x2) / vx; // use this parameter to reach x=imageWidth of line (x2 + dxE * vx = ip.getWidth())
      double dy0 = -(y2 / vy); // use this parameter to reach y=0 of line (y2 + dy0 * vy = 0)
      double dyE = (ip.getHeight() - y2) / vy; // use this parameter to reach y=imageHeigth of line (y2 + dyE * vy = ip.getHeight())

      double x3 = 0, y3 = 0, x4 = 0, y4 = 0;

      // check which parameter to use to reach top / left border
      if (y2 + dx0 * vy >= 0) {
        x3 = x2 + dx0 * vx;
        y3 = y2 + dx0 * vy;
      } else {
        x3 = x2 + dy0 * vx;
        y3 = y2 + dy0 * vy;
      }

      // check which parameter to use to reach bottom / right border
      if (y2 + dxE * vy <= ip.getHeight()) {
        x4 = x2 + dxE * vx;
        y4 = y2 + dxE * vy;
      } else {
        x4 = x2 + dyE * vx;
        y4 = y2 + dyE * vy;
      }

      ipTransformed.setColor(mLineColor);
      ipTransformed.drawLine((int) x2, (int) y2, (int) x3, (int) y3);
      ipTransformed.drawLine((int) x2, (int) y2, (int) x4, (int) y4);
    }

    mOriginalImage.updateAndDraw();
    if (mShowResult)
      mOriginalImage.show();
    PNG_Writer png = new PNG_Writer();
    try {
      System.out.println("[hough] saving image " + mImageName + "_hough.png");
      png.writeImage(mOriginalImage, "img/" + mImageName + "_hough.png", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("maxAccum: " + maxAccum);
    System.out.println("Time for Hough space: " + (msSpace - msStart) + " ms");
    System.out.println("Time for Hough lines: " + (msLines - msSpace) + " ms");
  }

  private static void createImage(int[][] values, float maxValue, String outputName) {
    // Create RGB image and fill in Hough space as gray scale image
    ImagePlus imgAccum = NewImage.createRGBImage(outputName, values.length, values[0].length, 1, NewImage.FILL_BLACK);
    ImageProcessor ipAccum = imgAccum.getProcessor();
    int[] rgb = new int[3];
    for (int y = 0; y < ipAccum.getHeight(); y++) {
      for (int x = 0; x < ipAccum.getWidth(); x++) { // Scale Hough space so
                                                     // that
        rgb[0] = rgb[1] = rgb[2] = (int) (((float) values[x][y] / (float) maxValue) * 255.0f);
        ipAccum.putPixel(x, y, rgb);
      }
    }

    // Show and save Hough space image
    imgAccum.show();
    imgAccum.updateAndDraw();
    PNG_Writer png = new PNG_Writer();
    try {
      System.out.println("[hough] saving image " + outputName + ".png");
      png.writeImage(imgAccum, "img/" + outputName + ".png", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
