package imagej_billard;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class BillardTracker implements PlugInFilter {

  @Override
  public int setup(String arg, ImagePlus imp) {
    return DOES_8G;
  }

  @Override
  public void run(ImageProcessor ip1) {

    int width = ip1.getWidth();
    int height = ip1.getHeight();
    byte[] pixels = (byte[]) ip1.getPixels();

    BayerProcessor bayerProcessor = new BayerProcessor(pixels, width, height);

    ImagePlus imgGray = NewImage.createByteImage("GrayDeBayered", width, height, 1, NewImage.FILL_BLACK);
    ImageProcessor ipGray = imgGray.getProcessor();
    byte[] pixGray = (byte[]) ipGray.getPixels();

    ImagePlus imgRGB = NewImage.createRGBImage("RGBDeBayered", width, height, 1, NewImage.FILL_BLACK);
    ImageProcessor ipRGB = imgRGB.getProcessor();
    int[] pixRGB = (int[]) ipRGB.getPixels();

    long msStart = System.currentTimeMillis();

    ImagePlus imgHue = NewImage.createByteImage("Hue", width, height, 1, NewImage.FILL_BLACK);
    ImageProcessor ipHue = imgHue.getProcessor();
    byte[] pixHue = (byte[]) ipHue.getPixels();
    
    ImagePlus imgBrightness = NewImage.createByteImage("Brightness", width, height, 1, NewImage.FILL_BLACK);
    ImageProcessor ipBrightness = imgBrightness.getProcessor();
    byte[] pixBrightness = (byte[]) ipBrightness.getPixels();

    for (int i = 0; i < pixRGB.length; i++) {
      pixGray[i] = (byte) bayerProcessor.getValue(i);
      pixRGB[i] = bayerProcessor.getRGB(i);
      pixHue[i] = (byte) bayerProcessor.getHue(i);
      pixBrightness[i] = (byte) bayerProcessor.getBrightness(i);
    }

    long ms = System.currentTimeMillis() - msStart;
    System.out.println(ms);
    ImageStatistics stats = ipGray.getStatistics();
    System.out.println("Mean:" + stats.mean);

    PNG_Writer png = new PNG_Writer();
    try {
      png.writeImage(imgGray, "img/Billard1024x544x1B.png", 0);
      png.writeImage(imgRGB, "img/Billard1024x544x3.png", 0);
      png.writeImage(imgHue, "img/Billard1024x544x1H.png", 0);
      png.writeImage(imgBrightness, "img/Billard1024x544x1H.png", 0);

    } catch (Exception e) {
      e.printStackTrace();
    }

    imgGray.show();
    imgGray.updateAndDraw();
    imgRGB.show();
    imgRGB.updateAndDraw();
    imgHue.show();
    imgHue.updateAndDraw();
    imgBrightness.show();
    imgBrightness.updateAndDraw();
  }

  public static void main(String[] args) {
    BillardTracker plugin = new BillardTracker();

    ImagePlus im = new ImagePlus("img/Billard2048x1088x1.png");
    im.show();
    plugin.setup("", im);
    plugin.run(im.getProcessor());
  }
}
