import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HeartBeat {
    // Settings
    private static final int TILE_SIZE = 45;
    private static final int MID_TILE_SIZE = TILE_SIZE * 2;
    private static final int LONG_TILE_SIZE = TILE_SIZE * 5;
    private static final int ROW_SIZE = 10;
    private static final int COL_SIZE = 9;
    
    //
    private enum TileType {
        unkown,
        blue,
        green,
        purple,
        red,
        yellow
    }
    
    private enum ButtonType {
        Color,
        Run,
        Tile
    }
    
    
    // Common
    private TileType[][] tiles;
    private JButton setButton, runButton;
    private JButton[][] tileButtons;
    private JLabel resultLabel;
    
    private Color type2color(TileType type) {
        switch(type) {
        case unkown:
            return Color.LIGHT_GRAY;
        case blue:
            return Color.blue;
        case green:
            return Color.green;
        case purple:
            return new Color(0x93, 0x70, 0xDB);
        case red:
            return Color.red;
        case yellow:
            return Color.yellow;
        default:
            // unknown error let's exit
            System.exit(1);
        }
        return Color.gray;
    }
    
    private TileType color2type(Color color) {
        for(TileType type : TileType.values()) {
            if (type2color(type).equals(color)) {
                return type;
            }
        }
        return TileType.unkown;
    }
    
    
    // UI
    private JButton createButton(ButtonType type) {
        // create uses default and adds a callback action
        JButton button = new JButton();
        switch(type) {
        case Color:
            button.setText("Set Color");
            button.setPreferredSize(new Dimension(MID_TILE_SIZE, TILE_SIZE));
            button.setBackground(type2color(TileType.unkown));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    JButton bt = (JButton) event.getSource();
                    int tp = (color2type(bt.getBackground()).ordinal() + 1) % TileType.values().length;
                    bt.setBackground(type2color(TileType.values()[tp]));
                }
            });
            break;
        case Run:
            button.setPreferredSize(new Dimension(MID_TILE_SIZE, TILE_SIZE));
            button.setText("Start");
            button.addActionListener(new ActionListener() {
                boolean done = true;
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (done) {
                        // clean up tiles and then show suggest button
                        cleanTiles(1);
                        refreshColor();
                        button.setText("Suggest");
                        enableTileButtons(true);
                        done = false;
                    } else {
                        if (readTiles()) {
                            suggestAlgo();
                            button.setText("Done!");
                            enableTileButtons(false);
                            done = true;
                        }
                    }
                }
            });
            break;
        default:
            button.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
            button.setBackground(type2color(TileType.unkown));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    JButton bt = (JButton) event.getSource();
                    setButton(bt);
                }
            });
            break;
        }
        return button;
    }
    
    private void setColor(JButton button, TileType type) {
        button.setBackground(type2color(type));
    }
    
    private void setButton(JButton button) {
        // set button color same as Color button
        button.setBackground(setButton.getBackground());
        
    }
    
    private void refreshColor() {
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                setColor(tileButtons[row][col], tiles[row][col]);
            }
        }
    }
    
    private JButton createTile() {
        return createButton(ButtonType.Tile);
    }
    
    private void enableTileButtons(boolean enable) {
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                tileButtons[row][col].setEnabled(enable);
            }
        }
    }
    
    private void init() {        
        // create a container
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        
        // create top line
        setButton = createButton(ButtonType.Color);
        runButton = createButton(ButtonType.Run);
        resultLabel = new JLabel();
        resultLabel.setPreferredSize(new Dimension(LONG_TILE_SIZE, TILE_SIZE));
        resultLabel.setBackground(Color.white);
        resultLabel.setOpaque(true);
        JPanel topLine = new JPanel();
        topLine.add(setButton);
        topLine.add(resultLabel);
        topLine.add(runButton);
        pane.add(topLine);
        
        // create bottom grid
        tiles = new TileType[ROW_SIZE][COL_SIZE];
        tileButtons = new JButton[ROW_SIZE][COL_SIZE];
        for(int row = 0; row < ROW_SIZE; ++row) {
            JPanel line = new JPanel();
            for(int col = 0; col < COL_SIZE; ++col) {
                tiles[row][col] = TileType.unkown;
                tileButtons[row][col] = createTile();
                line.add(tileButtons[row][col]);
            }
            pane.add(line);
        }
        
        // create a window
        JFrame window = new JFrame("HeartBeat");
        window.setSize(650, 630);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setContentPane(pane);
        window.setVisible(true);
    }
    
    // Algorithm - Only learning and tune this !!!!
    private int[] computeScore(int level, int count) {
        int[] ret = new int[2];
        ret[0] = level - 1; // TODO calibrate 0 when count
        ret[1] = level; // TMP let level decide
        return ret; // tune here
    }
    
    private boolean cleanable(TileType[][] ts, int row, int col) {
        if (ts[row][col] == TileType.unkown) {
            return false;
        }
        int rowCnt = 0, colCnt = 0;
        if (col-1 >= 0 && ts[row][col] == ts[row][col-1]) {
            ++rowCnt;
            if (col-2 >= 0 && ts[row][col] == ts[row][col-2]) {
                ++rowCnt;
            }
        }
        if (col+1 < COL_SIZE && ts[row][col] == ts[row][col+1]) {
            ++rowCnt;
            if (col+2 < COL_SIZE && ts[row][col] == ts[row][col+2]) {
                ++rowCnt;
            }
        }
        if (row-1 >= 0 && ts[row][col] == ts[row-1][col]) {
            ++colCnt;
            if (row-2 >= 0 && ts[row][col] == ts[row-2][col]) {
                ++colCnt;
            }
        }
        if (row+1 < ROW_SIZE && ts[row][col] == ts[row+1][col]) {
            ++colCnt;
            if (row+2 < ROW_SIZE && ts[row][col] == ts[row+2][col]) {
                ++colCnt;
            }
        }
        
        return rowCnt >= 2 || colCnt >= 2;
    }
    private int[] cleanTiles(int level) {
        // compute next tiles result
        int count = 0;
        TileType[][] nextTs = new TileType[ROW_SIZE][COL_SIZE];
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                if (cleanable(tiles, row, col)) {
                    ++count;
                    nextTs[row][col] = TileType.unkown;
                } else {
                    nextTs[row][col] = tiles[row][col];
                }
            }
        }
        
        int[] result = computeScore(level, count);
        if (count != 0) {
            // push to bottom
            for(int col = 0; col < COL_SIZE; ++col) {
                int wtRow = ROW_SIZE - 1;
                for(int rdRow = wtRow; rdRow >= 0; --rdRow) {
                    if (nextTs[rdRow][col] != TileType.unkown) {
                        tiles[wtRow--][col] = nextTs[rdRow][col];
                    }
                }
                while(wtRow >= 0) {
                    tiles[wtRow--][col] = TileType.unkown;
                }
            }
            int[] nextResult = cleanTiles(level+1);
            result[0] = nextResult[0];
            result[1] += nextResult[1];
        }
        return result; 
    }
    
    private boolean readTiles() {
        // read tile type from current board
        // must not have unknown tile for initial clean and suggest
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                tiles[row][col] = color2type(tileButtons[row][col].getBackground());
                if (tiles[row][col] == TileType.unkown) {
                    return false; // suggest failure
                }
            }
        }
        return true;
    }
    
    private void suggestAlgo() {
        int row0 = -1, col0 = -1, row1 = -1, col1 = -1;
        int[] tmpResult, goodResult = new int[2];
        TileType tmp;
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 1; col < COL_SIZE; ++col) {
                readTiles();
                tmp = tiles[row][col-1];
                tiles[row][col-1] = tiles[row][col];
                tiles[row][col] = tmp;
                tmpResult = cleanTiles(1);
                if (tmpResult[1] >= goodResult[1]) {
                    goodResult[0] = tmpResult[0];
                    goodResult[1] = tmpResult[1];
                    row0 = row;
                    col0 = col-1;
                    row1 = row;
                    col1 = col;
                }
            }
        }
        for(int row = 1; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                readTiles();
                tmp = tiles[row-1][col];
                tiles[row-1][col] = tiles[row][col];
                tiles[row][col] = tmp;
                tmpResult = cleanTiles(1);
                if (tmpResult[1] >= goodResult[1]) {
                    goodResult[0] = tmpResult[0];
                    goodResult[1] = tmpResult[1];
                    row0 = row-1;
                    col0 = col;
                    row1 = row;
                    col1 = col;
                }
            }
        }
        
        // update tiles that this is our intended move
        readTiles();
        tmp = tiles[row0][col0];
        tiles[row0][col0] = tiles[row1][col1];
        tiles[row1][col1] = tmp;
        // suggest user what move
        tileButtons[row0][col0].setBackground(Color.BLACK);
        tileButtons[row1][col1].setBackground(Color.BLACK);
        // update resultLabel
        resultLabel.setText("level:" + goodResult[0] + ", Score:" + goodResult[1]);
    }
    
    // Main    
    public HeartBeat() {
        init();
    }
    
    public static void main(String[] args) {
        new HeartBeat();
    }

}
