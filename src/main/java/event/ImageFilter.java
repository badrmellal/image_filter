package event;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Map;

public class ImageFilter {
    private Map<String, Integer> filterValues;

    public ImageFilter(Map<String, Integer> filterValues) {
        this.filterValues = filterValues;
    }

    public BufferedImage apply(BufferedImage original) {
        BufferedImage filtered = copyImage(original);

        // Apply adjustments in specific order for best results
        filtered = adjustBrightness(filtered);
        filtered = adjustContrast(filtered);
        filtered = adjustSaturation(filtered);
        filtered = adjustTemperature(filtered);
        filtered = applyFade(filtered);
        filtered = applyVignette(filtered);

        return filtered;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    private BufferedImage adjustBrightness(BufferedImage image) {
        int brightness = filterValues.getOrDefault("Brightness", 0);
        if (brightness == 0) return image;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        float brightnessScale = 1.0f + (brightness / 100.0f);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (int) (((rgb >> 16) & 0xff) * brightnessScale);
                int g = (int) (((rgb >> 8) & 0xff) * brightnessScale);
                int b = (int) ((rgb & 0xff) * brightnessScale);

                // Clamp values
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    private BufferedImage adjustContrast(BufferedImage image) {
        int contrast = filterValues.getOrDefault("Contrast", 0);
        if (contrast == 0) return image;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        float factor = (259.0f * (contrast + 255)) / (255.0f * (259 - contrast));

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                r = Math.min(255, Math.max(0, (int)(factor * (r - 128) + 128)));
                g = Math.min(255, Math.max(0, (int)(factor * (g - 128) + 128)));
                b = Math.min(255, Math.max(0, (int)(factor * (b - 128) + 128)));

                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    private BufferedImage adjustSaturation(BufferedImage image) {
        int saturation = filterValues.getOrDefault("Saturation", 0);
        if (saturation == 0) return image;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        float saturationScale = 1.0f + (saturation / 100.0f);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;

                // Convert RGB to HSB
                float[] hsb = Color.RGBtoHSB(
                        (rgb >> 16) & 0xff,
                        (rgb >> 8) & 0xff,
                        rgb & 0xff,
                        null
                );

                // Adjust saturation
                hsb[1] = Math.min(1.0f, Math.max(0.0f, hsb[1] * saturationScale));

                // Convert back to RGB
                int adjustedRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                result.setRGB(x, y, (a << 24) | (adjustedRGB & 0x00ffffff));
            }
        }

        return result;
    }

    private BufferedImage adjustTemperature(BufferedImage image) {
        int temperature = filterValues.getOrDefault("Temperature", 0);
        if (temperature == 0) return image;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        float tempScale = temperature / 100.0f;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // Warm: increase red, decrease blue
                // Cool: increase blue, decrease red
                r = Math.min(255, Math.max(0, r + (int)(tempScale * 30)));
                b = Math.min(255, Math.max(0, b - (int)(tempScale * 30)));

                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    private BufferedImage applyFade(BufferedImage image) {
        int fade = filterValues.getOrDefault("Fade", 0);
        if (fade == 0) return image;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        float fadeStrength = fade / 100.0f;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // Add fade by mixing with a light gray
                r = (int)(r * (1 - fadeStrength) + 220 * fadeStrength);
                g = (int)(g * (1 - fadeStrength) + 220 * fadeStrength);
                b = (int)(b * (1 - fadeStrength) + 220 * fadeStrength);

                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    private BufferedImage applyVignette(BufferedImage image) {
        int vignette = filterValues.getOrDefault("Vignette", 0);
        if (vignette == 0) return image;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        float vignetteStrength = vignette / 100.0f;
        int centerX = image.getWidth() / 2;
        int centerY = image.getHeight() / 2;
        float maxDistance = (float) Math.sqrt(centerX * centerX + centerY * centerY);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // Calculate distance from center
                float dx = x - centerX;
                float dy = y - centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float vignetteFactor = 1.0f - (distance / maxDistance) * vignetteStrength;
                vignetteFactor = Math.max(0.0f, vignetteFactor);

                // Apply vignette
                r = (int)(r * vignetteFactor);
                g = (int)(g * vignetteFactor);
                b = (int)(b * vignetteFactor);

                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }
}
