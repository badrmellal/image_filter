package event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PhotoFilterApp extends JFrame {
    private static final String APP_TITLE = "Instagram Filters";
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private JPanel mainPanel;
    private ImagePanel imagePanel;
    private FilterPanel filterPanel;
    private DatabaseManager dbManager;

    public PhotoFilterApp() {
        setTitle(APP_TITLE);
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize database connection
        dbManager = new DatabaseManager();

        // Setup UI components
        initializeComponents();

        // Setup menu bar
        setJMenuBar(createMenuBar());

        // Apply modern look and feel
        setupLookAndFeel();
    }

    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create main components
        imagePanel = new ImagePanel();
        filterPanel = new FilterPanel(imagePanel);

        // Add components to main panel
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        mainPanel.add(filterPanel, BorderLayout.EAST);

        // Add main panel to frame
        add(mainPanel);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Image");
        JMenuItem saveItem = new JMenuItem("Save Image");
        JMenuItem exitItem = new JMenuItem("Exit");

        openItem.addActionListener(e -> imagePanel.loadImage());
        saveItem.addActionListener(e -> imagePanel.saveImage());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Filter Menu
        JMenu filterMenu = new JMenu("Filters");
        JMenuItem saveFilterItem = new JMenuItem("Save Filter");
        JMenuItem loadFilterItem = new JMenuItem("Load Filter");

        saveFilterItem.addActionListener(e -> filterPanel.saveCurrentFilter());
        loadFilterItem.addActionListener(e -> filterPanel.loadSavedFilter());

        filterMenu.add(saveFilterItem);
        filterMenu.add(loadFilterItem);

        menuBar.add(fileMenu);
        menuBar.add(filterMenu);

        return menuBar;
    }

    private void setupLookAndFeel() {
        try {
            // Set cross-platform look and feel
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );

            // Update all components
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PhotoFilterApp app = new PhotoFilterApp();
            app.setVisible(true);
        });
    }
}