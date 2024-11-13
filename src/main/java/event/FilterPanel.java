package event;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FilterPanel extends JPanel {
    private ImagePanel imagePanel;
    private DatabaseManager dbManager;

    // UI Components
    private JPanel presetsPanel;
    private JPanel adjustmentsPanel;
    private Map<String, JSlider> sliders;

    // Current filter values
    private Map<String, Integer> filterValues;

    // Constants
    private static final int SLIDER_MIN = -100;
    private static final int SLIDER_MAX = 100;
    private static final int SLIDER_INIT = 0;

    public FilterPanel(ImagePanel imagePanel) {
        this.imagePanel = imagePanel;
        this.dbManager = new DatabaseManager();
        this.sliders = new HashMap<>();
        this.filterValues = new HashMap<>();

        setPreferredSize(new Dimension(300, 600));
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initializeComponents();
    }

    private void initializeComponents() {
        // Create main container with vertical box layout
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));

        // Add preset filters section
        createPresetsPanel();
        mainContainer.add(presetsPanel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add adjustments section
        createAdjustmentsPanel();
        mainContainer.add(adjustmentsPanel);

        // Add reset button at bottom
        JButton resetButton = new JButton("Reset All");
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.addActionListener(e -> resetAllFilters());

        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        mainContainer.add(resetButton);

        // Add scroll capability
        JScrollPane scrollPane = new JScrollPane(mainContainer);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createPresetsPanel() {
        presetsPanel = new JPanel();
        presetsPanel.setLayout(new BoxLayout(presetsPanel, BoxLayout.Y_AXIS));

        JLabel presetLabel = new JLabel("Preset Filters");
        presetLabel.setFont(new Font("Arial", Font.BOLD, 14));
        presetLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        // Add preset filter buttons
        String[] presets = {
                "Vintage", "Summer", "Noir", "Sepia",
                "Vivid", "Fade", "Cool", "Warm"
        };

        for (String preset : presets) {
            JButton button = createStyledButton(preset);
            button.addActionListener(e -> applyPresetFilter(preset));
            buttonPanel.add(button);
        }

        presetsPanel.add(presetLabel);
        presetsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        presetsPanel.add(buttonPanel);
    }

    private void createAdjustmentsPanel() {
        adjustmentsPanel = new JPanel();
        adjustmentsPanel.setLayout(new BoxLayout(adjustmentsPanel, BoxLayout.Y_AXIS));

        JLabel adjustLabel = new JLabel("Adjustments");
        adjustLabel.setFont(new Font("Arial", Font.BOLD, 14));
        adjustLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        adjustmentsPanel.add(adjustLabel);
        adjustmentsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add adjustment sliders
        addSlider("Brightness", "Adjust image brightness");
        addSlider("Contrast", "Adjust image contrast");
        addSlider("Saturation", "Adjust color saturation");
        addSlider("Temperature", "Adjust color temperature");
        addSlider("Fade", "Add vintage fade effect");
        addSlider("Vignette", "Add dark corners effect");
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        return button;
    }

    private void addSlider(String name, String tooltip) {
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(name);
        label.setFont(new Font("Arial", Font.PLAIN, 12));

        JSlider slider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, SLIDER_INIT);
        slider.setToolTipText(tooltip);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);

        // Add value label
        JLabel valueLabel = new JLabel("0");
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Update value label and apply filter when slider changes
        slider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            int value = source.getValue();
            valueLabel.setText(String.valueOf(value));
            filterValues.put(name, value);

            if (!source.getValueIsAdjusting()) {
                applyCurrentFilters();
            }
        });

        sliders.put(name, slider);
        filterValues.put(name, SLIDER_INIT);

        sliderPanel.add(label);
        sliderPanel.add(slider);
        sliderPanel.add(valueLabel);
        sliderPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        adjustmentsPanel.add(sliderPanel);
    }

    private void applyPresetFilter(String preset) {
        // Reset current adjustments
        resetAllFilters();

        // Apply preset filter settings
        switch (preset.toLowerCase()) {
            case "vintage":
                setSliderValue("Contrast", 20);
                setSliderValue("Fade", 40);
                setSliderValue("Temperature", -20);
                break;
            case "summer":
                setSliderValue("Brightness", 10);
                setSliderValue("Saturation", 30);
                setSliderValue("Temperature", 20);
                break;
            case "noir":
                setSliderValue("Contrast", 40);
                setSliderValue("Saturation", -100);
                setSliderValue("Vignette", 50);
                break;
            // Add more presets here
        }

        applyCurrentFilters();
    }

    private void setSliderValue(String name, int value) {
        JSlider slider = sliders.get(name);
        if (slider != null) {
            slider.setValue(value);
            filterValues.put(name, value);
        }
    }

    private void applyCurrentFilters() {
        ImageFilter filter = new ImageFilter(filterValues);
        imagePanel.applyFilter(filter);
    }

    private void resetAllFilters() {
        for (JSlider slider : sliders.values()) {
            slider.setValue(SLIDER_INIT);
        }
        filterValues.replaceAll((k, v) -> SLIDER_INIT);
        imagePanel.resetImage();
    }

    public void saveCurrentFilter() {
        String name = JOptionPane.showInputDialog(
                this,
                "Enter a name for this filter combination:",
                "Save Filter",
                JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            dbManager.saveFilter(name, new HashMap<>(filterValues));
        }
    }

    public void loadSavedFilter() {
        Map<String, Map<String, Integer>> savedFilters = dbManager.loadFilters();
        if (savedFilters.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No saved filters found!",
                    "Load Filter",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String[] filterNames = savedFilters.keySet().toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Choose a filter to load:",
                "Load Filter",
                JOptionPane.QUESTION_MESSAGE,
                null,
                filterNames,
                filterNames[0]
        );

        if (selected != null) {
            Map<String, Integer> values = savedFilters.get(selected);
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                setSliderValue(entry.getKey(), entry.getValue());
            }
            applyCurrentFilters();
        }
    }
}
