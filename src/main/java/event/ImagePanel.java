package event;

import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImagePanel extends JPanel {
    private BufferedImage originalImage;
    private BufferedImage currentImage;
    private double scale = 1.0;
    private int imageX = 0;
    private int imageY = 0;

    // For image dragging
    private Point dragStart;

    public ImagePanel() {
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        setupMouseListeners();
        setPreferredSize(new Dimension(800, 600));
    }

    private void setupMouseListeners() {
        // Mouse wheel for zooming
        addMouseWheelListener(e -> {
            if (currentImage != null) {
                if (e.getWheelRotation() < 0) {
                    scale *= 1.1;
                } else {
                    scale /= 1.1;
                }
                repaint();
            }
        });

        // Mouse listeners for dragging
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null && currentImage != null) {
                    Point current = e.getPoint();
                    imageX += (current.x - dragStart.x);
                    imageY += (current.y - dragStart.y);
                    dragStart = current;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void loadImage() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Images", "jpg", "jpeg", "png", "gif");
        chooser.setFileFilter(filter);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(chooser.getSelectedFile());
                resetImage();
                centerImage();
                repaint();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error loading image: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void saveImage() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this,
                    "No image to save!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "PNG Images", "png");
        chooser.setFileFilter(filter);

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                ImageIO.write(currentImage, "png", file);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error saving image: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void applyFilter(ImageFilter filter) {
        if (originalImage == null) return;

        currentImage = filter.apply(originalImage);
        repaint();
    }

    public void resetImage() {
        if (originalImage == null) return;

        currentImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = currentImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        scale = 1.0;
        centerImage();
        repaint();
    }

    private void centerImage() {
        if (currentImage != null) {
            imageX = (getWidth() - currentImage.getWidth()) / 2;
            imageY = (getHeight() - currentImage.getHeight()) / 2;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable antialiasing
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (currentImage != null) {
            // Calculate scaled dimensions
            int scaledWidth = (int) (currentImage.getWidth() * scale);
            int scaledHeight = (int) (currentImage.getHeight() * scale);

            // Draw the image
            g2d.drawImage(currentImage,
                    imageX, imageY,
                    scaledWidth, scaledHeight,
                    null);

            // Draw border around the image
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawRect(
                    imageX, imageY,
                    scaledWidth, scaledHeight
            );
        } else {
            // Draw placeholder text
            g2d.setColor(new Color(150, 150, 150));
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            String msg = "Drop an image here or use File → Open Image";
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(msg);
            int msgHeight = fm.getHeight();
            g2d.drawString(msg,
                    (getWidth() - msgWidth) / 2,
                    (getHeight() - msgHeight) / 2);
        }
    }

    // Getter for current image
    public BufferedImage getCurrentImage() {
        return currentImage;
    }
}
