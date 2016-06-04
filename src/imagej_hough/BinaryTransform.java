package imagej_hough;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class BinaryTransform implements PlugInFilter {

  private final static int pixelClip = 2; // clip this amount of pixels from
                                          // image frame
  private ImagePlus mInputImage;
  private ImagePlus mTransformedImage;
  private String mImageName;
  private int mThreshold;
  private boolean mShowResult;

  public BinaryTransform(ImagePlus image, int threshold) {
    mInputImage = image.duplicate();
    mThreshold = threshold;
    mImageName = image.getShortTitle();
  }

  public ImagePlus process(boolean showResult) {
    System.out.println("[binary] processing image " + mImageName);
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

    mTransformedImage = NewImage.createByteImage(mImageName + "_binary", ip.getWidth(), ip.getHeight(), 1, NewImage.FILL_BLACK);
    ImageProcessor ipTransformed = mTransformedImage.getProcessor();

    // Perform binary transform and fill new image
    int[] rgb = new int[3];
    for (int y = (0 + pixelClip); y < (ip.getHeight() - pixelClip); y++) {
      for (int x = (0 + pixelClip); x < (ip.getWidth() - pixelClip); x++) {
        ip.getPixel(x, y, rgb);
        int pixel = rgb[0];
        int newVal = (pixel > mThreshold) ? 255 : 0;
        rgb[0] = rgb[1] = rgb[2] = newVal;
        ipTransformed.putPixel(x, y, rgb);
      }

    }
    // Show and save binary transformed image
    mTransformedImage.updateAndDraw();
    if (mShowResult)
      mTransformedImage.show();

    PNG_Writer png = new PNG_Writer();
    try {
      System.out.println("[binary] saving image " + mImageName + "_binary.png");
      png.writeImage(mTransformedImage, "img/" + mImageName + "_binary.png", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
