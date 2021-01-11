import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Image {

    private static final String IMAGE_FILE = "hb.PNG";
    
    private BufferedImage image;
    
    public Image() {
        try {
            image = ImageIO.read(new File(IMAGE_FILE));
         } catch (Exception e) {
             
         }
    }
    
    private int getAvgRGB(int row, int col, int len) {
        len /= 5; // let's get the center square
        int sum = 0, cnt = 0;
        for(int r = -len; r <= len; ++r) {
            for(int c = -len; c <= len; ++c ) {
                sum += image.getRGB(row + r, col + c);
                ++cnt; // cnt can be derived though
            }
        }
        return sum / cnt;
    }
    
    private String rgb2Color(int rgb) {
        Color c = new Color(rgb);
        return "" + c.getBlue() + "," + c.getGreen() + "," + c.getRed();
//        return Integer.toString(c.getRed());
    }
    
    public void  read(int rowSize, int colSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int len = (width + height) / (rowSize + colSize);
        int start = len / 2;
        
        for(int r = 0; r < rowSize; ++r) {
            for(int c = 0; c < colSize; ++c) {
                System.out.printf("%10s, ", rgb2Color(getAvgRGB(start + c * len, start + r * len, len)));
            }
            System.out.println();
        }
    }
    
    public static void main(String[] args) {
        new Image().read(HeartBeat.ROW_SIZE, HeartBeat.COL_SIZE);
    }
}
