package me.hxlixd;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int GRID_SIZE = 5;
    private int clickedNonMineCount = 0;
    private ExecutorService soundExecutor = Executors.newCachedThreadPool(); // Cached thread pool for sound playback
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

        // Left panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(260, frame.getHeight()));

        JLabel mineLabel = new JLabel("Number of mines:");
        mineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(mineLabel);

        // ComboBox for selecting mine count (1 to 24 mines)
        Integer[] mineOptions = new Integer[24];
        for (int i = 0; i < mineOptions.length; i++) {
            mineOptions[i] = i + 1;
        }
        JComboBox<Integer> mineDropdown = new JComboBox<>(mineOptions);
        mineDropdown.setMaximumSize(new Dimension(200, 30));
        mineDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(mineDropdown);

        // Start Game button
        JButton startButton = new JButton("Play");
        startButton.setMaximumSize(new Dimension(200, 30));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        leftPanel.add(startButton);

        // Add top margin to the leftPanel
        leftPanel.setBorder(BorderFactory.createEmptyBorder(100, 10, 0, 0));


        panel.add(leftPanel, BorderLayout.WEST);

        // Center panel for the game grid
        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j] = new JButton();
                gridPanel.add(buttons[i][j]);
            }
        }
        panel.add(gridPanel, BorderLayout.CENTER);

        // Wrap the gridPanel inside a JPanel with an empty border
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBorder(BorderFactory.createEmptyBorder(50, 10, 50, 50)); // Add margin: top, left, bottom, right
        gridWrapper.add(gridPanel, BorderLayout.CENTER);

        panel.add(gridWrapper, BorderLayout.CENTER);


        frame.add(panel);
        frame.setVisible(true);

        // Start button action listener
        startButton.addActionListener(e -> {
            resetGrid(); // Reset the game grid
            int mineCount = (int) Objects.requireNonNull(mineDropdown.getSelectedItem());
            placeMines(mineCount);
            enableGame();
            //playSound("play");
            new Thread(() -> new AudioHandle("play")).start();
            System.out.println("Game started with " + mineCount + " mines.");
        });

        // Mine dropdown menu action listener
        mineDropdown.addActionListener(e -> new Thread(() -> new AudioHandle("combobox")).start());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                soundExecutor.shutdown(); // Properly shut down the executor
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
                clickedNonMineCount++; // Increment the counter for non-mine clicks

                // Check if the player has clicked all non-mine buttons
                if (clickedNonMineCount == (GRID_SIZE * GRID_SIZE - mines.size())) {
                    // Play jackpot sound
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
                    // Show all mines
                    try {
                        Image scaledBomb = getCachedIcon("bomb", button.getWidth(), button.getHeight()).getImage();
                        ImageIcon bombIcon = new ImageIcon(scaledBomb);
                        button.setIcon(bombIcon);
                        button.setDisabledIcon(bombIcon);
                    } catch (Exception ex) {
                        System.out.println("Error loading bomb image: " + ex);
                    }
                } else {
                    // Show gems for non-mine buttons
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
