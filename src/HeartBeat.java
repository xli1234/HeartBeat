import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class HeartBeat {
    /********** Config **********/
    private static final int ROW_SIZE = 10;
    private static final int COL_SIZE = 9;
    private static final int TILE_SIZE = 45;
    private static final int MID_TILE_SIZE = TILE_SIZE * 2;
    private static final int LONG_TILE_SIZE = TILE_SIZE * 5;
    private static final int THICK_BORDER = 5;
    private static final String TILES_FILE = "tiles.csv";

    /********** Enums **********/
    private enum TileType { unknown, blue, green, purple, red, yellow }
    private enum ButtonType { Color, Run, Tile }

    /********** Components **********/
    private TileType[][] tiles; // temporary global to compute tiles grid and store result to refresh tileButtons
    private JButton setButton, runButton;
    private JButton[][] tileButtons; // tile button, actual current color should always be from tileButtons
    private JLabel resultLabel;
    
    /********** Basic **********/
    private Color type2color(TileType type) {
        switch(type) {
        case unknown:
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
        return Color.LIGHT_GRAY;
    }
    
    private TileType color2type(Color color) {
        for(TileType type : TileType.values()) {
            if (type2color(type).equals(color)) {
                return type;
            }
        }
        return TileType.unknown;
    }
    
    /********** UI **********/
    private JButton createButton(ButtonType type) {
        JButton button = new JButton();
        switch(type) {
        case Color:
            button.setText("Set Color");
            button.setPreferredSize(new Dimension(MID_TILE_SIZE, TILE_SIZE));
            button.setBackground(type2color(TileType.unknown));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    // Rotate color for setButton
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
                        // Clean up tiles with strategy from suggestAlgo
                        // Initial tiles shouldn't have any cleanup 
                        clearTiles();
                        writeTilesToButtons();
                        button.setText("Suggest");
                        enableTileButtons(true);
                        done = false;
                        // Save to a file in case we close app
                        saveTiles();
                    } else {
                        if (readTilesFromButtons()) {
                            suggestAlgo();
                            button.setText("Done!");
                            enableTileButtons(false);
                            done = true;
                        } else {
                            resultLabel.setText("Missing color for some tiles!");
                        }
                    }
                }
            });
            break;
        default:
            button.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    // Set button color same as Color button
                    JButton bt = (JButton) event.getSource();
                    bt.setBackground(setButton.getBackground());
                    // When updating tileButtons
                    // Push to bottom whenever a tile is cleared and possible
                    if(color2type(bt.getBackground()) == TileType.unknown) {
                        readTilesFromButtons();
                        pushTiles(tiles);
                        writeTilesToButtons();
                    }
                }
            });
            break;
        }
        return button;
    }
    
    // Enable/Disable tileButtons
    private void enableTileButtons(boolean enable) {
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                tileButtons[row][col].setEnabled(enable);
            }
        }
    }
    
    // Read type from current tileButtons and store in tiles
    // Return if all tiles have color
    private boolean readTilesFromButtons() {
        boolean allColor = true;
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                tiles[row][col] = color2type(tileButtons[row][col].getBackground());
                if (allColor && tiles[row][col] == TileType.unknown) {
                    allColor = false;
                }
            }
        }
        return allColor;
    }

    private void writeTilesToButtons() {
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                tileButtons[row][col].setBorder(new LineBorder(Color.BLACK));
                tileButtons[row][col].setBackground(type2color(tiles[row][col]));
            }
        }
    }
    
    /********** Algorithm **********/
    
    // Compute score based on rules
    private int computeScore(ArrayList<Integer> counts) {
        int score = 0;
        for(int i = 0; i < counts.size(); ++i) {
            score += counts.get(i) * (i + 1);
        }
        return score;
    }
    
    private boolean clearable(TileType[][] ts, int row, int col) {
        if (ts[row][col] == TileType.unknown) {
            return false;
        }
        int[] counts = new int[2];
        for(int move = 0; move <= 1; ++move) { // try row or col
            for(int dir = -1; dir <= 1; dir += 2) { // dir: -1, 1
                boolean connected = true;
                for(int multi = 1; connected && multi <= 2; ++multi) { // multi: 1 or 2 moves
                    int r = move * dir * multi + row;
                    int c = (1 - move) * dir * multi + col;
                    if (r < 0 || c < 0 || r >= ROW_SIZE || c >= COL_SIZE ||
                        ts[r][c] != tiles[row][col]) {
                        connected = false;
                    }
                    if (connected) {
                        ++counts[move];
                    }
                }
            }
        }
        // Counts greater than 2 means at least 3 if including ts[row][col]
        return counts[0] >= 2 || counts[1] >= 2;
    }

    // Update tiles with nextTiles grid
    // In the meantime, push tiles to bottom if there is a gap
    private void pushTiles(TileType[][] nextTiles) {
        for(int col = 0; col < COL_SIZE; ++col) {
            int wtRow = ROW_SIZE - 1;
            for(int rdRow = wtRow; rdRow >= 0; --rdRow) {
                if (nextTiles[rdRow][col] != TileType.unknown) {
                    tiles[wtRow--][col] = nextTiles[rdRow][col];
                }
            }
            while(wtRow >= 0) {
                tiles[wtRow--][col] = TileType.unknown;
            }
        }
    }
    
    // Return a array of clear count per level
    private ArrayList<Integer> clearTiles() {
        // tiles may be previously set by suggestAlgo
        // Compute nextTiles result from tiles
        ArrayList<Integer> counts = new ArrayList<>();
        int cnt;
        do {
            cnt = 0;
            TileType[][] nextTiles = new TileType[ROW_SIZE][COL_SIZE];
            for(int row = 0; row < ROW_SIZE; ++row) {
                for(int col = 0; col < COL_SIZE; ++col) {
                    if (clearable(tiles, row, col)) {
                        ++cnt;
                        nextTiles[row][col] = TileType.unknown;
                    } else {
                        nextTiles[row][col] = tiles[row][col];
                    }
                }
            }
            if (cnt != 0) {
                counts.add(cnt);
                pushTiles(nextTiles);
            }
        } while(cnt != 0);
        return counts; 
    }
    
    // Just check all possibilities O(n^2) * O(time to clear tiles per try)
    // Record best option based on good score
    // Good score criteria is defined in computeScore
    private void suggestAlgo() {
        int row0 = -1, col0 = -1, row1 = -1, col1 = -1, goodLevel = 0, goodScore = 0;
        TileType tmp;
        // Check in below sequence:
        // 1. col combo -> row combo
        // 2. upper left -> bottom right
        for(int dir = 0; dir <= 1; ++dir) {
            for(int row = dir; row < ROW_SIZE; ++row) {
                for(int col = 1 - dir; col < COL_SIZE; ++col) {
                    readTilesFromButtons();
                    int r = row - dir;
                    int c = col - 1 + dir;
                    tmp = tiles[r][c];
                    tiles[r][c] = tiles[row][col];
                    tiles[row][col] = tmp;
                    ArrayList<Integer> counts = clearTiles();
                    int level = counts.size();
                    int score = computeScore(counts);
                    if (score >= goodScore) {
                        goodScore = score;
                        goodLevel = level;
                        row0 = r;
                        col0 = c;
                        row1 = row;
                        col1 = col;
                    }
                }
            }
        }
        
        // Record suggested move in tiles
        readTilesFromButtons();
        tmp = tiles[row0][col0];
        tiles[row0][col0] = tiles[row1][col1];
        tiles[row1][col1] = tmp;
        // Highlight suggested move
        tileButtons[row0][col0].setBorder(BorderFactory.createLineBorder(Color.BLACK, THICK_BORDER));
        tileButtons[row1][col1].setBorder(BorderFactory.createLineBorder(Color.BLACK, THICK_BORDER));
        // Note resultLabel
        resultLabel.setText("level:" + goodLevel + ", Score:" + goodScore);
    }
    
    /********** Load / Save **********/
    // For easy tuning
    private void saveTiles() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(TILES_FILE));
            for(int row = 0; row < ROW_SIZE; ++row) {
                writer.append(Integer.toString(tiles[row][0].ordinal()));
                for(int col = 1; col < COL_SIZE; ++col) {
                    writer.append("," + Integer.toString(tiles[row][col].ordinal()));
                }
                writer.append("\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Save failed!");
        }
    }
    
    private void loadTiles() {
        tiles = new TileType[ROW_SIZE][COL_SIZE];
        try {
            Scanner scanner = new Scanner(new File(TILES_FILE));
            for(int row = 0; row < ROW_SIZE; ++row) {
                String[] line = scanner.nextLine().split(",");
                for(int col = 0; col < COL_SIZE; ++col) {
                    tiles[row][col] = TileType.values()[Integer.valueOf(line[col])];
                }
            }
            scanner.close();
        } catch (Exception e) {
            for(int row = 0; row < ROW_SIZE; ++row) {
                for(int col = 0; col < COL_SIZE; ++col) {
                    tiles[row][col] = TileType.unknown;
                }
            }
        }
    }
    
    /********** Main **********/
    public HeartBeat() {
        // create a container
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        
        // Create top line
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
        
        // Create bottom grid
        loadTiles();
        tileButtons = new JButton[ROW_SIZE][COL_SIZE];
        for(int row = 0; row < ROW_SIZE; ++row) {
            JPanel line = new JPanel();
            for(int col = 0; col < COL_SIZE; ++col) {
                tileButtons[row][col] = createButton(ButtonType.Tile);
                tileButtons[row][col].setBackground(type2color(tiles[row][col]));
                line.add(tileButtons[row][col]);
            }
            pane.add(line);
        }
        
        // Disable tileButtons before start
        enableTileButtons(false);
        
        // Create a window
        JFrame window = new JFrame("HeartBeat");
        window.setSize(650, 630);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setContentPane(pane);
        window.setVisible(true);
    }
    
    public static void main(String[] args) {
        new HeartBeat();
    }

}
