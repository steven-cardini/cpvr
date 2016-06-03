package imagej_hough;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class LaplaceTransform implements PlugInFilter {

  private final static int pixelClip = 2; // clip this amount of pixels from
                                          // image frame
  private ImagePlus mInputImage;
  private ImagePlus mTransformedImage;
  private String mImageName;
  private boolean mShowResult;

  public LaplaceTransform(ImagePlus image) {
    mInputImage = image.duplicate();
    mImageName = image.getShortTitle();
  }

  public ImagePlus process(boolean showResult) {
    System.out.println("[laplace] processing image " + mImageName);
    mShowResult = showResult;

    // convert image to grayscale for hough analysis
    ImageConverter ic = new ImageConverter(mInputImage);
    ic.convertToGray8();
    mInputImage.updateAndDraw();

    setup("", mInputImage);
    run(mInputImage.getProcessor());

    return mTransformedImage;
  }

  @Override
  public int setup(String arg, ImagePlus imp) {
    return DOES_8G;
  }

  @Override
  public void run(ImageProcessor ip) {

    mTransformedImage = NewImage.createRGBImage(mImageName + "_laplace", ip.getWidth(), ip.getHeight(), 1,
        NewImage.FILL_BLACK);
    ImageProcessor ipTransformed = mTransformedImage.getProcessor();

    // Perform laplace transform and fill new image
    int[] rgb = new int[3];
    for (int y = (0 + pixelClip); y < (ip.getHeight() - pixelClip); y++) {
      for (int x = (0 + pixelClip); x < (ip.getWidth() - pixelClip); x++) {
        ip.getPixel(x, y, rgb);
        int pixel = rgb[0];
        int value = 0;
        int counter = 0;
        if (x > 0) {
          ip.getPixel(x - 1, y, rgb);
          value += rgb[0];
          counter++;
        }
        if (x < ip.getWidth() - 1) {
          ip.getPixel(x + 1, y, rgb);
          value += rgb[0];
          counter++;
        }
        if (y > 0) {
          ip.getPixel(x, y - 1, rgb);
          value += rgb[0];
          counter++;
          if (x > 0) {
            ip.getPixel(x - 1, y - 1, rgb);
            value += rgb[0];
            counter++;
          }
          if (x < ip.getWidth() - 1) {
            ip.getPixel(x + 1, y - 1, rgb);
            value += rgb[0];
            counter++;
          }
        }
        if (y < ip.getHeight() - 1) {
          ip.getPixel(x, y + 1, rgb);
          value += rgb[0];
          counter++;
          if (x > 0) {
            ip.getPixel(x - 1, y + 1, rgb);
            value += rgb[0];
            counter++;
          }
          if (x < ip.getWidth() - 1) {
            ip.getPixel(x + 1, y + 1, rgb);
            value += rgb[0];
            counter++;
          }
        }

        int newVal = (counter > 0) ? value - counter * pixel : pixel;
        newVal = Math.max(0, newVal);
        newVal = Math.min(255, newVal);

        rgb[0] = rgb[1] = rgb[2] = newVal;
        ipTransformed.putPixel(x, y, rgb);
      }

    }

    // Show and save Laplace transformed image
    mTransformedImage.updateAndDraw();
    if (mShowResult)
      mTransformedImage.show();
    PNG_Writer png = new PNG_Writer();
    try {
      png.writeImage(mTransformedImage, "img/" + mImageName + "_laplace.png", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
