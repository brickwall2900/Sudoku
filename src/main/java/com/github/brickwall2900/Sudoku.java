package com.github.brickwall2900;

import de.ad.sudoku.Generator;
import de.ad.sudoku.Grid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.awt.event.KeyEvent.*;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;

public class Sudoku extends JFrame {
    public static final String NAME = "Sudoku";
    public static final int CELL_SIZE = 48;
    public static final int STATUS_BAR_HEIGHT = 20;
    public static final Dimension GRID_SIZE = new Dimension(CELL_SIZE * 3, CELL_SIZE * 3);
    public static final Dimension BOARD_SIZE = new Dimension(GRID_SIZE.width * 3, GRID_SIZE.height * 3);

    public static void main(String[] args) {
        Sudoku sudoku = new Sudoku();
        sudoku.run();
    }

    private final Color defaultColor = getBackground();

    private final Map<JLabel, Point> cellToIndex = new HashMap<>();
    private final ArrayList<JLabel> lockedCells = new ArrayList<>();
    private JLabel[][] cellLabels;
    private JLabel selectedCell;
    private JLabel statusBar;
    private Grid grid;
    private boolean hooksEnabled, completed;

    private void run() {
        setTitle(NAME);

        setLayout(null);
        createBoardVisuals();
        initBoard(Level.BASIC);
        initStatusBar();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
        Insets insets = getInsets();
        Dimension windowSize = new Dimension(BOARD_SIZE.width + insets.left, BOARD_SIZE.height + insets.top + STATUS_BAR_HEIGHT + 6);
        setSize(windowSize);
        setMaximumSize(windowSize);
        setMinimumSize(windowSize);
        setLocationRelativeTo(null);

        JOptionPane.showMessageDialog(this, """
                It's a game of Sudoku! Try to complete with all the numbers on each cell,
                but no such number must be repeated in each row, column, or a grid.
                
                Controls:
                - Mouse Click -> Select a cell
                - 1-9 -> Put a number
                - 0 or Backspace -> Remove a number
                - Enter -> Submit solution
                - End -> New game
                
                Sudoku engine by https://github.com/a11n""", NAME, JOptionPane.INFORMATION_MESSAGE);
        initGlobalHook();
    }

    private void createBoardVisuals() {
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(null);
        boardPanel.setSize(BOARD_SIZE);
        cellLabels = new JLabel[9][9];
        for (int g = 0; g < 9; g++) {
            JPanel grid = new JPanel();
            grid.setLayout(null);
            grid.setSize(GRID_SIZE.width - 8, GRID_SIZE.height - 8);
            grid.setBorder(BorderFactory.createLineBorder(Color.GRAY, 6));

            // position grid
            boardPanel.add(grid);
            int gxPos = g % 3 * GRID_SIZE.width;
            int gyPos = g / 3 * GRID_SIZE.height;
            grid.setLocation(gxPos + 4, gyPos + 4);

            for (int c = 0; c < 9; c++) {
                JPanel panel = new JPanel();
                panel.setLayout(null);
                panel.setSize(CELL_SIZE, CELL_SIZE);
                JLabel cell = createCell();
                panel.add(cell);
                grid.add(panel);
                int cxPos = c % 3 * CELL_SIZE;
                int cyPos = c / 3 * CELL_SIZE;
                panel.setLocation(cxPos, cyPos);
                cellLabels[g][c] = cell;
                cellToIndex.put(cell, new Point(g, c));
            }
        }

        this.add(boardPanel);
    }

    private JLabel createCell() {
        JLabel cell = new JLabel();
        cell.setText("");
        cell.setHorizontalAlignment(SwingConstants.CENTER);
        cell.setVerticalAlignment(SwingConstants.CENTER);
        cell.setOpaque(false);
        cell.setSize(CELL_SIZE, CELL_SIZE);
        cell.setBorder(BorderFactory.createLoweredBevelBorder());
        return cell;
    }

    private void initBoard(Level level) {
        Generator generator = new Generator();
        grid = generator.generate(level.cellsEmpty);
        int size = grid.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Grid.Cell cell = grid.getCell(i,j);
                int value = cell.getValue();
                JLabel label = cellLabels[i][j];
                putValue(label, value);
                if (!cell.isEmpty()) {
                    lockedCells.add(label);
                }
            }
        }
        completed = false;
    }

    private void initGlobalHook() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this::onGlobalKeyPressed);
        long eventMask = MOUSE_MOTION_EVENT_MASK + MOUSE_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> onCellSelected((MouseEvent) e), eventMask);

        hooksEnabled = true;
    }

    private void initStatusBar() {
        statusBar = new JLabel("Solve the game!");
        statusBar.setSize(BOARD_SIZE.width, STATUS_BAR_HEIGHT);
        statusBar.setLocation(0, BOARD_SIZE.height);
        this.add(statusBar);
    }

    public Grid.Cell getCell(JLabel label) {
        Point point = cellToIndex.get(label);
        return grid.getCell(point.x, point.y);
    }

    public void putValue(JLabel label, int value) {
        if (value != 0) {
            label.setText(String.valueOf(value));
        } else {
            label.setText("");
        }
        getCell(label).setValue(value);
        updateCells(label);
    }

    public void onCellSelected(MouseEvent e) {
        if (hooksEnabled && e.getID() == MOUSE_PRESSED) {
            if (SwingUtilities.getDeepestComponentAt(this, e.getX(), e.getY()) instanceof JLabel label) {
                selectedCell = label;
                updateCells(label);
            }
        }
    }

    public void updateCells(JLabel selectedCell) {
        if (selectedCell == null) return;
        Container parent = selectedCell.getParent();

        for (JLabel[] row : cellLabels) {
            for (JLabel cell : row) {
                Container parent2 = cell.getParent();
                parent2.setBackground(defaultColor);
                String text = cell.getText();
                if (!text.isEmpty() && text.equals(selectedCell.getText())) {
                    parent2.setBackground(Color.PINK);
                }
            }
        }

        if (!lockedCells.contains(selectedCell)) {
            parent.setBackground(Color.CYAN.brighter());
        } else {
            parent.setBackground(Color.YELLOW.brighter());
        }
    }

    public boolean onGlobalKeyPressed(KeyEvent event) {
        if (hooksEnabled && event.getID() == KEY_PRESSED) {
            int keyCode = event.getKeyCode();
            if (selectedCell != null) {
                if (!completed) {
                    if (VK_0 <= keyCode && keyCode <= VK_9) {
                        int number = keyCode - VK_0;
                        putValue(selectedCell, number);
                    } else if (VK_NUMPAD0 <= keyCode && keyCode <= VK_NUMPAD9) {
                        int number = keyCode - VK_NUMPAD0;
                        putValue(selectedCell, number);
                    } else if (keyCode == VK_BACK_SPACE) {
                        putValue(selectedCell, 0);
                    } else if (keyCode == VK_ENTER) {
                        if (validateBoard()) {
                            statusBar.setText("Completed!");
                            statusBar.setForeground(Color.GREEN);
                            JOptionPane.showMessageDialog(this, "You did it!! Press 'End' for a new game.", NAME, JOptionPane.INFORMATION_MESSAGE);
                            completed = true;
                        } else {
                            statusBar.setText("Failed!");
                            statusBar.setForeground(Color.RED);
                        }
                    }
                }
                if (keyCode == VK_END) {
                    hooksEnabled = false;
                    int answer = JOptionPane.showOptionDialog(this, "Choose a new difficulty.",
                            NAME, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                            null, Level.values(), Level.BABY);
                    if (answer != JOptionPane.CLOSED_OPTION) {
                        initBoard(Level.values()[answer]);
                    }
                    hooksEnabled = true;
                }
            }
        }
        return false;
    }

    public boolean validateBoard() {
        int size = grid.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Grid.Cell cell = grid.getCell(i,j);
                int value = cell.getValue();
                if (cell.isEmpty() || !grid.isValidValueForCell(cell, value)) {
                    cellLabels[i][j].getParent().setBackground(Color.RED);
                    return false;
                }
            }
        }
        return true;
    }

    public enum Level {
        BABY(1), CHILD(30), BASIC(40), INTERMEDIATE(50), BOSS_FIGHT(64);
        final int cellsEmpty;

        Level(int cellsEmpty) {
            this.cellsEmpty = cellsEmpty;
        }
    }
}