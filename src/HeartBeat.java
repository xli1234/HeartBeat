import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

public class HeartBeat {
    /********** Config **********/
    private static final int ROW_SIZE = 10;
    private static final int COL_SIZE = 9;
    private static final int WINDOW_ROW = 600;
    private static final int WINDOW_COL = 800;
    private static final int TILE_SIZE = 55;
    private static final int MID_TILE_SIZE = TILE_SIZE * 2;
    private static final int RESULT_LABEL_SIZE = 190;
    private static final int THICK_BORDER = 5;
    private static final String TILES_FILE = "tiles.csv";
    private static final String TILE_ABBRE = "ubgpry";
    private static final String TILE_CODE =
            "bgyyrbygr,"
          + "uuuuuuuuu,"
          + "byrbgypyb,"
          + "ryprbyppb,"
          + "bbgpprbyy,"
          + "bgyyuprbg,"
          + "pryguybgy,"
          + "ypuupugrp,"
          + "pggprbygb,"
          + "byprbryrb";

    // Strategy:
    // 0. Try max levels >= 3
    // 1. Try at least 240 score
    // 2. Try max levels in two turns
    private static final int[] STRATEGIES = {0, 2}; // ordered by priority

    /********** Enums **********/
    private enum TileType { unknown, blue, green, purple, red, yellow }
    private enum ButtonType { Tile, Color, Run, Update, Save, Load }

    /********** Components **********/
    private TileType[][] tiles; // temporary global to compute tiles grid and store result to refresh tileButtons
    private JButton setButton;
    private JButton[][] tileButtons; // tile button, actual current color should always be from tileButtons
    private JLabel resultLabel;
    private JTextArea[] rowTextAreas;
    private JTextArea[] colTextAreas;
    
    private boolean left2RightStrategy;

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
                boolean done = false;
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
                    } else {
                        if (readTilesFromButtons()) {
                            suggestAlgo();
//                            button.setText("Done!");
                            button.setText("Algo!");
//                            enableTileButtons(false);
//                            done = true;
                        } else {
                            resultLabel.setText("Missing tiles!");
                        }
                    }
                }
            });
            break;
        case Update:
            button.setPreferredSize(new Dimension(MID_TILE_SIZE, TILE_SIZE));
            button.setText("Update");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    readTilesFromButtons();
                    for(int row = 0; row < ROW_SIZE; ++row) {
                        String updateStr = rowTextAreas[row].getText();
                        if (updateStr.length() == COL_SIZE) {
                            rowTextAreas[row].setForeground(Color.BLACK);
                            rowTextAreas[row].setText("");
                            for(int col = 0; col < COL_SIZE; ++col) {
                                tiles[row][col] = TileType.values()[getColorCode(updateStr, col)];
                            }
                        } else if (!updateStr.isEmpty()) {
                            rowTextAreas[row].setForeground(Color.RED);
                        }
                    }
                    for(int col = 0; col < COL_SIZE; ++col) {
                        String updateStr = colTextAreas[col].getText();
                        if (updateStr.length() == ROW_SIZE) {
                            colTextAreas[col].setForeground(Color.BLACK);
                            colTextAreas[col].setText("");
                            for(int row = 0; row < ROW_SIZE; ++row) {
                                tiles[row][col] = TileType.values()[getColorCode(updateStr, row)];
                            }
                        } else if (!updateStr.isEmpty()) {
                            colTextAreas[col].setForeground(Color.RED);
                        }
                    }
                    writeTilesToButtons();
                }
            });
            break;
        case Save:
            button.setPreferredSize(new Dimension(MID_TILE_SIZE, TILE_SIZE));
            button.setText("Save");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    readTilesFromButtons();
                    saveTiles();
                }
            });
            break;
        case Load:
            button.setPreferredSize(new Dimension(MID_TILE_SIZE, TILE_SIZE));
            button.setText("Load");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    loadTiles();
                    writeTilesToButtons();
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
        // base score for 3,4,5,6,7
        int[] bases = {30, 50, 100, 60, 80};
        int score = 0;
        for(int i = 0; i < counts.size(); ++i) {
            int base = counts.get(i) >= bases.length ? 120 : bases[counts.get(i)-3];
            score += base * (i + 1);
        }
        return score;
    }
    
    private int computeNextLevel() {
        TileType[][] nextTiles = new TileType[ROW_SIZE][COL_SIZE];
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                nextTiles[row][col] = tiles[row][col];
            }
        }

        int goodLevel = 0;
        for(int dir = 0; dir <= 1; ++dir) {
            for(int row = dir; row < ROW_SIZE; ++row) {
                for(int tcol = 1 - dir; tcol < COL_SIZE; ++tcol) {
                    int col = left2RightStrategy ? tcol : (COL_SIZE - dir - tcol);
                    int r = row - dir;
                    int c = col - 1 + dir;
                    if (nextTiles[row][col] == TileType.unknown ||
                        nextTiles[r][c] == TileType.unknown) {
                        continue;
                    }
                    pushTiles(nextTiles);
                    TileType tmp = tiles[r][c];
                    tiles[r][c] = tiles[row][col];
                    tiles[row][col] = tmp;
                    int level = clearTiles().size();
                    if (level >= goodLevel) {
                        goodLevel = level;
                    }
                }
            }
        }

        return goodLevel;
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
        int row0 = -1, col0 = -1, row1 = -1, col1 = -1, goodLevel = 0, goodScore = 0, goodStrategy = 0;
        TileType tmp;
        for(int strategy : STRATEGIES) {
            // TODO maybe prepare if 1 and nextLevel is 5
            if (goodLevel >= 3 || goodLevel + goodScore > 15) {
                break;
            }
            goodStrategy = strategy;
            goodLevel = 0;
            goodScore = 0;
            // Check in below sequence:
            // 1. col combo -> row combo
            // 2. upper left -> bottom right
            for(int dir = 0; dir <= 1; ++dir) {
                for(int row = dir; row < ROW_SIZE; ++row) {
                    for(int tcol = 1 - dir; tcol < COL_SIZE; ++tcol) {
                        readTilesFromButtons();
                        int col = left2RightStrategy ? tcol : (COL_SIZE - dir - tcol);
                        int r = row - dir;
                        int c = col - 1 + dir;
                        tmp = tiles[r][c];
                        tiles[r][c] = tiles[row][col];
                        tiles[row][col] = tmp;
                        ArrayList<Integer> counts = clearTiles();
                        int score = 0, level = counts.size();
                        switch(strategy) {
                        case 0:
                            for(int i = 0; i < counts.size(); ++i) {
                                score += counts.get(i);
                            }
                            break;
                        case 1:
                            score = computeScore(counts);
                            break;
                        case 2:
                            score = level > 0 ? computeNextLevel() : 0;
                            break;
                        default:
                            System.exit(1);
                        }
                        if ((level == goodLevel && score >= goodScore) || // If same level, pick more score
                            (level >= 3 && level > goodLevel) || // If above 3+, more level the better, but if same level,
                                                                 // don't pick, it will be pick if more scores in previous check
                            (goodLevel < 3 && score >= goodScore)) {  // If 1 or 2 level, score is more important
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
        }
        
        // Change between left2RightStrategy and right2LeftStrategy
        left2RightStrategy = !left2RightStrategy;

        // Record suggested move in tiles
        readTilesFromButtons();
        tmp = tiles[row0][col0];
        tiles[row0][col0] = tiles[row1][col1];
        tiles[row1][col1] = tmp;
        // Highlight suggested move
        tileButtons[row0][col0].setBorder(BorderFactory.createLineBorder(Color.BLACK, THICK_BORDER));
        tileButtons[row1][col1].setBorder(BorderFactory.createLineBorder(Color.BLACK, THICK_BORDER));
        // Note resultLabel
        resultLabel.setText("Level:" + goodLevel + ", Strategy:" + goodStrategy + ", Score:" + goodScore);
    }
    
    /********** Load / Save **********/
    // For easy tuning
    private int getColorCode(String updateStr, int pos) {
        int colorCode = updateStr == null ? -1 : TILE_ABBRE.indexOf(updateStr.charAt(pos));
        if (colorCode < 0) {
            colorCode = 0;
        }
        return colorCode;
    }

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
        // Load trying sequence
        // 1. from save
//        System.out.println("Try load from saved data...");
        try {
            Scanner scanner = new Scanner(new File(TILES_FILE));
            for(int row = 0; row < ROW_SIZE; ++row) {
                String[] line = scanner.nextLine().split(",");
                for(int col = 0; col < COL_SIZE; ++col) {
                    tiles[row][col] = TileType.values()[Integer.valueOf(line[col])];
                }
            }
            scanner.close();
            return;
        } catch (Exception e) {
//            System.out.println("Try load from img...");
        }

        // 2. from img
        try {
            Image img = new Image();
            int[][] tileTypes = img.read(ROW_SIZE, COL_SIZE);
            for(int row = 0; row < ROW_SIZE; ++row) {
                for(int col = 0; col < COL_SIZE; ++col) {
                    tiles[row][col] = TileType.values()[tileTypes[row][col]];
                }
            }
            return;
        } catch (Exception e) {
            System.out.println(e);
//            System.out.println("Try load from hardcode...");
        }

        // 3. from hardcode
        String[] line = TILE_CODE.isEmpty() ? null : TILE_CODE.split(",");
        for(int row = 0; row < ROW_SIZE; ++row) {
            for(int col = 0; col < COL_SIZE; ++col) {
                tiles[row][col] = line == null ? TileType.unknown:
                    TileType.values()[getColorCode(line[row], col)];
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
        resultLabel = new JLabel();
        resultLabel.setPreferredSize(new Dimension(RESULT_LABEL_SIZE, TILE_SIZE));
        resultLabel.setBackground(Color.white);
        resultLabel.setOpaque(true);
        JPanel topLine = new JPanel();
        topLine.add(setButton);
        topLine.add(resultLabel);
        topLine.add(createButton(ButtonType.Run));
//        topLine.add(createButton(ButtonType.Update));
//        topLine.add(createButton(ButtonType.Save));
        topLine.add(createButton(ButtonType.Load));
        pane.add(topLine);

        // Create bottom grid
        rowTextAreas = new JTextArea[ROW_SIZE];
        colTextAreas = new JTextArea[COL_SIZE];
        loadTiles();
        tileButtons = new JButton[ROW_SIZE][COL_SIZE];
        for(int row = 0; row < ROW_SIZE; ++row) {
            JPanel line = new JPanel();
            for(int col = 0; col < COL_SIZE; ++col) {
                tileButtons[row][col] = createButton(ButtonType.Tile);
                tileButtons[row][col].setBackground(type2color(tiles[row][col]));
                line.add(tileButtons[row][col]);
            }
            rowTextAreas[row] = new JTextArea(3, 20);
            rowTextAreas[row].setBorder(new LineBorder(Color.BLACK));
//            line.add(rowTextAreas[row]);
            pane.add(line);
        }
        JPanel bottomLine = new JPanel(new FlowLayout(FlowLayout.LEFT));;
        for(int col = 0; col < COL_SIZE; ++col) {
            colTextAreas[col] = new JTextArea(8, 4);
            colTextAreas[col].setBorder(new LineBorder(Color.BLACK));
            bottomLine.add(colTextAreas[col]);
        }
//        pane.add(bottomLine);
        
        // Disable tileButtons before start
//        enableTileButtons(false);
        
        // Create a window
        JFrame window = new JFrame("HeartBeat");
        window.setSize(WINDOW_ROW, WINDOW_COL);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setContentPane(pane);
        window.setVisible(true);
    }
    
    public static void main(String[] args) {
        new HeartBeat();
    }

}
