package me.hxlixd;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

public class Main {
    private static final int GRID_SIZE = 5;
    private int clickedNonMineCount = 0;
    private JButton[][] buttons = new JButton[GRID_SIZE][GRID_SIZE];
    private Set<Point> mines = new HashSet<>();
    private Map<String, ImageIcon> iconCache = new HashMap<>();

    public Main() {
        FlatMacDarkLaf.setup();

        JFrame frame = new JFrame("JMines");
        frame.setSize(800, 600);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(260, frame.getHeight()));

        JLabel mineLabel = new JLabel("Number of mines:");
        mineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(mineLabel);

        Integer[] mineOptions = new Integer[24];
        for (int i = 0; i < mineOptions.length; i++) {
            mineOptions[i] = i + 1;
        }
        JComboBox<Integer> mineDropdown = new JComboBox<>(mineOptions);
        mineDropdown.setMaximumSize(new Dimension(200, 30));
        mineDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(mineDropdown);

        JButton startButton = new JButton("Play");
        startButton.setMaximumSize(new Dimension(200, 30));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(100, 10, 0, 0));
        leftPanel.add(startButton);


        panel.add(leftPanel, BorderLayout.WEST);

        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j] = new JButton();
                gridPanel.add(buttons[i][j]);
            }
        }
        panel.add(gridPanel, BorderLayout.CENTER);

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBorder(BorderFactory.createEmptyBorder(50, 10, 50, 50));
        gridWrapper.add(gridPanel, BorderLayout.CENTER);

        panel.add(gridWrapper, BorderLayout.CENTER);


        frame.add(panel);
        frame.setVisible(true);

        startButton.addActionListener(e -> {
            resetGrid();
            int mineCount = (int) Objects.requireNonNull(mineDropdown.getSelectedItem());
            placeMines(mineCount);
            enableGame();
            new Thread(() -> new AudioHandle("play")).start();
            System.out.println("Game started with " + mineCount + " mines.");
        });

        mineDropdown.addActionListener(e -> new Thread(() -> new AudioHandle("combobox")).start());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void resetGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j].setEnabled(true);
                buttons[i][j].setText("");
                buttons[i][j].setIcon(null);
                // Prevent audio and end game alert stacking
                for (ActionListener al : buttons[i][j].getActionListeners()) {
                    buttons[i][j].removeActionListener(al);
                }
            }
        }
        mines.clear();
        clickedNonMineCount = 0; // Reset the counter
    }


    private void placeMines(int mineCount) {
        Random random = new Random();
        while (mines.size() < mineCount) {
            int x = random.nextInt(GRID_SIZE);
            int y = random.nextInt(GRID_SIZE);
            mines.add(new Point(x, y));
        }
    }

    private void enableGame() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j].addActionListener(new ButtonClickListener(i, j));
            }
        }
    }

    private ImageIcon getCachedIcon(String iconName, int width, int height) {
        String key = iconName + width + "x" + height;
        if (!iconCache.containsKey(key)) {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/" + iconName + ".png")));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            iconCache.put(key, new ImageIcon(scaledImage));
        }
        return iconCache.get(key);
    }

    private class ButtonClickListener implements ActionListener {
        private final int row;
        private final int col;

        public ButtonClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = buttons[row][col];
            if (mines.contains(new Point(row, col))) {
                try {
                    Image bomb = getCachedIcon("bomb", button.getWidth(), button.getHeight()).getImage();
                    ImageIcon bombIcon = new ImageIcon(bomb);
                    button.setIcon(bombIcon);
                    button.setDisabledIcon(bombIcon);
                    new Thread(() -> new AudioHandle("explosion")).start();
                } catch (Exception ex) {
                    System.out.println("Error loading bomb image: " + ex);
                }

                button.setEnabled(false);
                JOptionPane.showMessageDialog(null, "Game over, you hit a mine.");
                disableAllButtons();
            } else {
                try {
                    Image gem = getCachedIcon("gem", button.getWidth(), button.getHeight()).getImage();
                    ImageIcon gemIcon = new ImageIcon(gem);
                    button.setIcon(gemIcon);
                    button.setDisabledIcon(gemIcon);
                    new Thread(() -> new AudioHandle("check")).start();
                } catch (Exception ex) {
                    System.out.println("Error loading gem image: " + ex);
                }

                button.setEnabled(false);
                clickedNonMineCount++;

                if (clickedNonMineCount == (GRID_SIZE * GRID_SIZE - mines.size())) {
                    new Thread(() -> new AudioHandle("jackpot")).start();
                    JOptionPane.showMessageDialog(null, "Congratulations! You cleared the board!");
                    disableAllButtons();
                }
            }
        }
    }

    private void disableAllButtons() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                JButton button = buttons[i][j];
                button.setEnabled(false);

                if (mines.contains(new Point(i, j))) {
                    try {
                        Image scaledBomb = getCachedIcon("bomb", button.getWidth(), button.getHeight()).getImage();
                        ImageIcon bombIcon = new ImageIcon(scaledBomb);
                        button.setIcon(bombIcon);
                        button.setDisabledIcon(bombIcon);
                    } catch (Exception ex) {
                        System.out.println("Error loading bomb image: " + ex);
                    }
                } else {
                    try {
                        Image scaledGem = getCachedIcon("gem", button.getWidth(), button.getHeight()).getImage();
                        ImageIcon gemIcon = new ImageIcon(scaledGem);
                        button.setIcon(gemIcon);
                        button.setDisabledIcon(gemIcon);
                    } catch (Exception ex) {
                        System.out.println("Error loading gem image: " + ex);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}
