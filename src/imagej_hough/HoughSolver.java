package imagej_hough;

import java.awt.Color;

import ij.ImagePlus;
import imagej_hough.HoughTransform.HoughLine;

public class HoughSolver {

  public static void main(String[] args) {

    ImagePlus imgPolygonOriginal = new ImagePlus("img/Polygon2.png");
    HoughTransform htPolygon = new HoughTransform(imgPolygonOriginal, imgPolygonOriginal, 4, 0, Math.PI, 1, Color.WHITE, Color.GRAY);
    htPolygon.process(true);

    ImagePlus imgShuttleOriginal = new ImagePlus("img/Shuttle2.png");
    imgShuttleOriginal.show();

    LaplaceTransform lpShuttle = new LaplaceTransform(imgShuttleOriginal);
    ImagePlus imgShuttleLaplace = lpShuttle.process(false);

    BinaryTransform btShuttle = new BinaryTransform(imgShuttleLaplace, 220);
    ImagePlus imgShuttleBinary = btShuttle.process(false);

    HoughTransform htShuttle = new HoughTransform(imgShuttleBinary, imgShuttleOriginal, 16, Math.PI / 4, Math.PI / 2, 2, Color.RED, Color.BLUE);
    htShuttle.process(true);

    HoughLine[] lines = htShuttle.getLines();
    double angle = 0;

    for (HoughLine line : lines) {
      angle += line.angle();
    }

    angle /= lines.length;

    int rotAng = (int) (90 - 180 * (angle / Math.PI));
    System.out.println("calculated rotation angle: " + rotAng);

    imgShuttleOriginal.getProcessor().rotate(rotAng);
    imgShuttleOriginal.updateAndDraw();
    imgShuttleOriginal.show();

  }

}
