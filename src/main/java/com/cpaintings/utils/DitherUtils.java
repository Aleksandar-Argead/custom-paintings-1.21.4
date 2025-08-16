package com.cpaintings.utils;

import java.awt.image.BufferedImage;
import net.minecraft.item.map.MapState;

/**
 * Utility class for various dithering algorithms used in image quantization for Minecraft map paintings.
 * Supports multiple dithering methods to improve image quality given the limited map color palette.
 */
public class DitherUtils {

    /**
     * Applies dithering to the image based on the specified method and updates the map state.
     * @param mapState The MapState to update with dithered colors
     * @param image The input image to dither
     * @param ditherMethod The dithering method to use (none, floyd, bayer, sierra, blue)
     */
    public static void applyDithering(MapState mapState, BufferedImage image, String ditherMethod) {
        int width = 128;
        int height = 128;

        switch (ditherMethod.toLowerCase()) {
            case "none":
                applyNoDithering(mapState, image, width, height);
                break;
            case "floyd":
                applyFloydSteinbergDithering(mapState, image, width, height);
                break;
            case "bayer":
                applyBayerDithering(mapState, image, width, height);
                break;
            case "sierra":
                applySierraDithering(mapState, image, width, height);
                break;
            case "blue":
                applyBlueNoiseDithering(mapState, image, width, height);
                break;
            default:
                applyBayerDithering(mapState, image, width, height); // Default to Bayer
                break;
        }
        mapState.markDirty();
    }

    private static void applyNoDithering(MapState mapState, BufferedImage image, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapState.colors[x + y * width] = 0;
                    continue;
                }
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                byte colorIndex = MapColorUtils.findClosestColor(r, g, b);
                mapState.colors[x + y * width] = colorIndex;
            }
        }
    }

    private static void applyFloydSteinbergDithering(MapState mapState, BufferedImage image, int width, int height) {
        float[][] errorR = new float[width][height];
        float[][] errorG = new float[width][height];
        float[][] errorB = new float[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapState.colors[x + y * width] = 0;
                    continue;
                }

                float r = ((argb >> 16) & 0xFF) + errorR[x][y];
                float g = ((argb >> 8) & 0xFF) + errorG[x][y];
                float b = (argb & 0xFF) + errorB[x][y];

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                byte colorIndex = MapColorUtils.findClosestColor((int) r, (int) g, (int) b);
                mapState.colors[x + y * width] = colorIndex;

                java.awt.Color chosenColor = MapColorUtils.PALETTE[colorIndex & 0xFF];
                float actualR = chosenColor.getRed();
                float actualG = chosenColor.getGreen();
                float actualB = chosenColor.getBlue();

                float errR = r - actualR;
                float errG = g - actualG;
                float errB = b - actualB;

                if (x + 1 < width) {
                    errorR[x + 1][y] += (errR * 7.0f) / 16.0f;
                    errorG[x + 1][y] += (errG * 7.0f) / 16.0f;
                    errorB[x + 1][y] += (errB * 7.0f) / 16.0f;
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    errorR[x - 1][y + 1] += (errR * 3.0f) / 16.0f;
                    errorG[x - 1][y + 1] += (errG * 3.0f) / 16.0f;
                    errorB[x - 1][y + 1] += (errB * 3.0f) / 16.0f;
                }
                if (y + 1 < height) {
                    errorR[x][y + 1] += (errR * 5.0f) / 16.0f;
                    errorG[x][y + 1] += (errG * 5.0f) / 16.0f;
                    errorB[x][y + 1] += (errB * 5.0f) / 16.0f;
                }
                if (x + 1 < width && y + 1 < height) {
                    errorR[x + 1][y + 1] += (errR * 1.0f) / 16.0f;
                    errorG[x + 1][y + 1] += (errG * 1.0f) / 16.0f;
                    errorB[x + 1][y + 1] += (errB * 1.0f) / 16.0f;
                }
            }
        }
    }

    private static void applyBayerDithering(MapState mapState, BufferedImage image, int width, int height) {
        // 4x4 Bayer matrix for ordered dithering (scaled to range 0-255)
        int[][] bayerMatrix = { { 0, 8, 2, 10 }, { 12, 4, 14, 6 }, { 3, 11, 1, 9 }, { 15, 7, 13, 5 } };
        int matrixSize = 4;
        float scale = 255.0f / 16.0f; // Scale factor to map matrix values to 0-255 range

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapState.colors[x + y * width] = 0;
                    continue;
                }

                float r = ((argb >> 16) & 0xFF);
                float g = ((argb >> 8) & 0xFF);
                float b = (argb & 0xFF);

                int matrixX = x % matrixSize;
                int matrixY = y % matrixSize;
                float threshold = bayerMatrix[matrixY][matrixX] * scale - 127.5f;

                r += threshold;
                g += threshold;
                b += threshold;

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                byte colorIndex = MapColorUtils.findClosestColor((int) r, (int) g, (int) b);
                mapState.colors[x + y * width] = colorIndex;
            }
        }
    }

    private static void applySierraDithering(MapState mapState, BufferedImage image, int width, int height) {
        float[][] errorR = new float[width][height];
        float[][] errorG = new float[width][height];
        float[][] errorB = new float[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapState.colors[x + y * width] = 0;
                    continue;
                }

                float r = ((argb >> 16) & 0xFF) + errorR[x][y];
                float g = ((argb >> 8) & 0xFF) + errorG[x][y];
                float b = (argb & 0xFF) + errorB[x][y];

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                byte colorIndex = MapColorUtils.findClosestColor((int) r, (int) g, (int) b);
                mapState.colors[x + y * width] = colorIndex;

                java.awt.Color chosenColor = MapColorUtils.PALETTE[colorIndex & 0xFF];
                float actualR = chosenColor.getRed();
                float actualG = chosenColor.getGreen();
                float actualB = chosenColor.getBlue();

                float errR = r - actualR;
                float errG = g - actualG;
                float errB = b - actualB;

                if (x + 1 < width) {
                    errorR[x + 1][y] += (errR * 5.0f) / 32.0f;
                    errorG[x + 1][y] += (errG * 5.0f) / 32.0f;
                    errorB[x + 1][y] += (errB * 5.0f) / 32.0f;
                }
                if (x + 2 < width) {
                    errorR[x + 2][y] += (errR * 3.0f) / 32.0f;
                    errorG[x + 2][y] += (errG * 3.0f) / 32.0f;
                    errorB[x + 2][y] += (errB * 3.0f) / 32.0f;
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    errorR[x - 1][y + 1] += (errR * 2.0f) / 32.0f;
                    errorG[x - 1][y + 1] += (errG * 2.0f) / 32.0f;
                    errorB[x - 1][y + 1] += (errB * 2.0f) / 32.0f;
                }
                if (y + 1 < height) {
                    errorR[x][y + 1] += (errR * 4.0f) / 32.0f;
                    errorG[x][y + 1] += (errG * 4.0f) / 32.0f;
                    errorB[x][y + 1] += (errB * 4.0f) / 32.0f;
                }
                if (x + 1 < width && y + 1 < height) {
                    errorR[x + 1][y + 1] += (errR * 4.0f) / 32.0f;
                    errorG[x + 1][y + 1] += (errG * 4.0f) / 32.0f;
                    errorB[x + 1][y + 1] += (errB * 4.0f) / 32.0f;
                }
                if (x + 2 < width && y + 1 < height) {
                    errorR[x + 2][y + 1] += (errR * 2.0f) / 32.0f;
                    errorG[x + 2][y + 1] += (errG * 2.0f) / 32.0f;
                    errorB[x + 2][y + 1] += (errB * 2.0f) / 32.0f;
                }
                if (x - 1 >= 0 && y + 2 < height) {
                    errorR[x - 1][y + 2] += (errR * 1.0f) / 32.0f;
                    errorG[x - 1][y + 2] += (errG * 1.0f) / 32.0f;
                    errorB[x - 1][y + 2] += (errB * 1.0f) / 32.0f;
                }
                if (y + 2 < height) {
                    errorR[x][y + 2] += (errR * 2.0f) / 32.0f;
                    errorG[x][y + 2] += (errG * 2.0f) / 32.0f;
                    errorB[x][y + 2] += (errB * 2.0f) / 32.0f;
                }
                if (x + 1 < width && y + 2 < height) {
                    errorR[x + 1][y + 2] += (errR * 1.0f) / 32.0f;
                    errorG[x + 1][y + 2] += (errG * 1.0f) / 32.0f;
                    errorB[x + 1][y + 2] += (errB * 1.0f) / 32.0f;
                }
            }
        }
    }

    private static void applyBlueNoiseDithering(MapState mapState, BufferedImage image, int width, int height) {
        // Using a simplified 8x8 blue noise-like matrix for dithering thresholds
        int[][] blueNoiseMatrix = {
            { 42, 10, 58, 26, 46, 14, 62, 30 },
            { 18, 50, 2, 34, 22, 54, 6, 38 },
            { 60, 28, 44, 12, 64, 32, 48, 16 },
            { 4, 36, 20, 52, 8, 40, 24, 56 },
            { 43, 11, 59, 27, 47, 15, 63, 31 },
            { 19, 51, 3, 35, 23, 55, 7, 39 },
            { 61, 29, 45, 13, 65, 33, 49, 17 },
            { 5, 37, 21, 53, 9, 41, 25, 57 },
        };
        int matrixSize = 8;
        float scale = 255.0f / 66.0f; // Scale factor to map matrix values (0-65) to 0-255 range

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapState.colors[x + y * width] = 0;
                    continue;
                }

                float r = ((argb >> 16) & 0xFF);
                float g = ((argb >> 8) & 0xFF);
                float b = (argb & 0xFF);

                int matrixX = x % matrixSize;
                int matrixY = y % matrixSize;
                float threshold = blueNoiseMatrix[matrixY][matrixX] * scale - 127.5f;

                r += threshold;
                g += threshold;
                b += threshold;

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                byte colorIndex = MapColorUtils.findClosestColor((int) r, (int) g, (int) b);
                mapState.colors[x + y * width] = colorIndex;
            }
        }
    }
}
