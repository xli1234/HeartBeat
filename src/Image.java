import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Image {

    private static final String IMAGE_FILE = "hb.PNG";
    private static final double[][] TILE_RGB = { // magic numbers from experiment
            {148.93,  188.54,  198.10}, // unknown
            {121.61,  193.14,  223.83}, // blue
            { 86.76,  205.38,  182.37}, // green
            {178.13,  158.67,  246.59}, // purple
            {250.98,  161.80,  192.76}, // red
            {246.35,  207.38,  129.93}  // yellow
    };
    
    private int rgb2type(double[] rgb) {
        int type = 0;
        double msq = Double.MAX_VALUE;
        for(int i = 0; i < TILE_RGB.length; ++i) {
            double tmsq = 0;
            for(int j = 0; j < TILE_RGB[0].length; ++j) {
                tmsq += (TILE_RGB[i][j] - rgb[j]) * (TILE_RGB[i][j] - rgb[j]);
            }
            if (tmsq < msq) {
                msq = tmsq;
                type = i;
            }
        }
        return type;
    }
    
    private BufferedImage image;
    
    public Image() throws Exception {
        image = ImageIO.read(new File(IMAGE_FILE));
    }
    
    private double[] getAvgRGB(double row, double col, double len) {
        len /= 5; // let's get the center square
        double[] rgb = new double[3];
        double cnt = 0;
        for(double r = -len; r <= len; ++r) {
            for(double c = -len; c <= len; ++c ) {
                Color color = new Color(image.getRGB((int) (row + r), (int) (col + c)));
                ++cnt;
                rgb[0] += color.getRed();
                rgb[1] += color.getGreen();
                rgb[2] += color.getBlue();
            }
        }
        // Compute Average
        for(int i = 0; i < 3; ++i) {
            rgb[i] /= cnt;
        }
        return rgb;
    }
    
    public int[][] read(int rowSize, int colSize) {
        int[][] tileTypes = new int[rowSize][colSize];
        // We need more accuracy on position
        // Don't cast to int till very end
        double width = image.getWidth();
        double height = image.getHeight();
        double len = (width + height) / (rowSize + colSize);
        double start = len / 2;
        for(int r = 0; r < rowSize; ++r) {
            for(int c = 0; c < colSize; ++c) {
                double[] rgb = getAvgRGB(start + c * len, start + r * len, len);
                tileTypes[r][c] = rgb2type(rgb);
            }
        }
        return tileTypes;
    }
}
