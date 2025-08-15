package com.cpaintings.utils;

import java.awt.Color;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/**
 * Utility class for handling Minecraft map color mappings and brightness levels.
 * This class contains the standard color palette, brightness adjustments, and block mappings used
 * for rendering images as Minecraft map paintings.
 */
public class MapColorUtils {

    // Standard Minecraft map base colors
    public static final int[] MINECRAFT_MAP_COLORS = {
        0x000000, // 0  (NONE / Transparent)
        0x7FB238, // 1  (GRASS)
        0xF7E9A3, // 2  (SAND)
        0xC7C7C7, // 3  (WOOL)
        0xFF0000, // 4  (FIRE)
        0xA0A0FF, // 5  (ICE)
        0xA7A7A7, // 6  (METAL)
        0x007C00, // 7  (PLANT)
        0xFFFFFF, // 8  (SNOW)
        0xA4A8B8, // 9  (CLAY)
        0x976D4D, // 10 (DIRT)
        0x707070, // 11 (STONE)
        0x4040FF, // 12 (WATER)
        0x8F7748, // 13 (WOOD)
        0xFFFCF5, // 14 (QUARTZ)
        0xD87F33, // 15 (COLOR_ORANGE)
        0xB24CD8, // 16 (COLOR_MAGENTA)
        0x6699D8, // 17 (COLOR_LIGHT_BLUE)
        0xE5E533, // 18 (COLOR_YELLOW)
        0x7FCC19, // 19 (COLOR_LIGHT_GREEN)
        0xF27FA5, // 20 (COLOR_PINK)
        0x4C4C4C, // 21 (COLOR_GRAY)
        0x999999, // 22 (COLOR_LIGHT_GRAY)
        0x4C7F99, // 23 (COLOR_CYAN)
        0x7F3FB2, // 24 (COLOR_PURPLE)
        0x334CB2, // 25 (COLOR_BLUE)
        0x664C33, // 26 (COLOR_BROWN)
        0x667F33, // 27 (COLOR_GREEN)
        0x993333, // 28 (COLOR_RED)
        0x191919, // 29 (COLOR_BLACK)
        0xFAEE4D, // 30 (GOLD)
        0x5CDBD5, // 31 (DIAMOND)
        0x4A80FF, // 32 (LAPIS)
        0x00D93A, // 33 (EMERALD)
        0x815631, // 34 (PODZOL / SPRUCE)
        0x700200, // 35 (NETHER)
        0xD1B1A1, // 36 (TERRACOTTA_WHITE)
        0x9F5224, // 37 (TERRACOTTA_ORANGE)
        0x95576C, // 38 (TERRACOTTA_MAGENTA)
        0x706C8A, // 39 (TERRACOTTA_LIGHT_BLUE)
        0xBA8524, // 40 (TERRACOTTA_YELLOW)
        0x677535, // 41 (TERRACOTTA_LIGHT_GREEN)
        0xA04D4E, // 42 (TERRACOTTA_PINK)
        0x392923, // 43 (TERRACOTTA_GRAY)
        0x876B62, // 44 (TERRACOTTA_LIGHT_GRAY)
        0x575C5C, // 45 (TERRACOTTA_CYAN)
        0x7A4958, // 46 (TERRACOTTA_PURPLE)
        0x4C3E5C, // 47 (TERRACOTTA_BLUE)
        0x4C3223, // 48 (TERRACOTTA_BROWN)
        0x4C522A, // 49 (TERRACOTTA_GREEN)
        0x8E3C2E, // 50 (TERRACOTTA_RED)
        0x251610, // 51 (TERRACOTTA_BLACK)
        0xBD3031, // 52 (CRIMSON_NYLIUM)
        0x943F61, // 53 (CRIMSON_STEM)
        0x5C191D, // 54 (CRIMSON_HYPHAE)
        0x167E86, // 55 (WARPED_NYLIUM)
        0x3A8E8C, // 56 (WARPED_STEM)
        0x562C3E, // 57 (WARPED_HYPHAE)
        0x14B485, // 58 (WARPED_WART_BLOCK)
        0x646464, // 59 (DEEPSLATE)
        0xD8AF93, // 60 (RAW_IRON)
        0x7FA796  // 61 (GLOW_LICHEN)
    };

    // Standard brightness levels for Minecraft maps
    public static final float[] BRIGHTNESS_LEVELS = {
        0.71f, // Shade 1
        0.86f, // Shade 2
        1.00f, // Shade 3
        0.53f  // Shade 4 (not used)
    };

    // Precomputed palette for dithering
    public static final Color[] PALETTE = new Color[256];

    static {
        for (int base = 1; base < MINECRAFT_MAP_COLORS.length; base++) {
            for (int shade = 0; shade < 4; shade++) {
                int index = (base * 4) + shade;
                if (index >= PALETTE.length) break;

                int color = MINECRAFT_MAP_COLORS[base];
                float brightness = BRIGHTNESS_LEVELS[shade];

                int red = (int) (((color >> 16) & 0xFF) * brightness);
                int green = (int) (((color >> 8) & 0xFF) * brightness);
                int blue = (int) ((color & 0xFF) * brightness);

                PALETTE[index] = new Color(red, green, blue);
            }
        }
    }

    // Map color indices to blocks
    public static final Block[] COLOR_TO_BLOCK = new Block[MINECRAFT_MAP_COLORS.length];

    static {
        COLOR_TO_BLOCK[0] = Blocks.AIR; // Transparent
        COLOR_TO_BLOCK[1] = Blocks.GRASS_BLOCK; // GRASS
        COLOR_TO_BLOCK[2] = Blocks.SAND; // SAND
        COLOR_TO_BLOCK[3] = Blocks.WHITE_WOOL; // WOOL
        COLOR_TO_BLOCK[4] = Blocks.REDSTONE_BLOCK; // FIRE
        COLOR_TO_BLOCK[5] = Blocks.PACKED_ICE; // ICE
        COLOR_TO_BLOCK[6] = Blocks.IRON_BLOCK; // METAL
        COLOR_TO_BLOCK[7] = Blocks.OAK_LEAVES; // PLANT
        COLOR_TO_BLOCK[8] = Blocks.SNOW_BLOCK; // SNOW
        COLOR_TO_BLOCK[9] = Blocks.CLAY; // CLAY
        COLOR_TO_BLOCK[10] = Blocks.DIRT; // DIRT
        COLOR_TO_BLOCK[11] = Blocks.STONE; // STONE
        COLOR_TO_BLOCK[12] = Blocks.WATER; // WATER
        COLOR_TO_BLOCK[13] = Blocks.OAK_PLANKS; // WOOD
        COLOR_TO_BLOCK[14] = Blocks.QUARTZ_BLOCK; // QUARTZ
        COLOR_TO_BLOCK[15] = Blocks.TERRACOTTA; // COLOR_ORANGE
        COLOR_TO_BLOCK[16] = Blocks.MAGENTA_TERRACOTTA; // COLOR_MAGENTA
        COLOR_TO_BLOCK[17] = Blocks.LIGHT_BLUE_TERRACOTTA; // COLOR_LIGHT_BLUE
        COLOR_TO_BLOCK[18] = Blocks.YELLOW_TERRACOTTA; // COLOR_YELLOW
        COLOR_TO_BLOCK[19] = Blocks.LIME_TERRACOTTA; // COLOR_LIGHT_GREEN
        COLOR_TO_BLOCK[20] = Blocks.PINK_TERRACOTTA; // COLOR_PINK
        COLOR_TO_BLOCK[21] = Blocks.GRAY_TERRACOTTA; // COLOR_GRAY
        COLOR_TO_BLOCK[22] = Blocks.LIGHT_GRAY_TERRACOTTA; // COLOR_LIGHT_GRAY
        COLOR_TO_BLOCK[23] = Blocks.CYAN_TERRACOTTA; // COLOR_CYAN
        COLOR_TO_BLOCK[24] = Blocks.PURPLE_TERRACOTTA; // COLOR_PURPLE
        COLOR_TO_BLOCK[25] = Blocks.BLUE_TERRACOTTA; // COLOR_BLUE
        COLOR_TO_BLOCK[26] = Blocks.BROWN_TERRACOTTA; // COLOR_BROWN
        COLOR_TO_BLOCK[27] = Blocks.GREEN_TERRACOTTA; // COLOR_GREEN
        COLOR_TO_BLOCK[28] = Blocks.RED_TERRACOTTA; // COLOR_RED
        COLOR_TO_BLOCK[29] = Blocks.BLACK_TERRACOTTA; // COLOR_BLACK
        COLOR_TO_BLOCK[30] = Blocks.GOLD_BLOCK; // GOLD
        COLOR_TO_BLOCK[31] = Blocks.DIAMOND_BLOCK; // DIAMOND
        COLOR_TO_BLOCK[32] = Blocks.LAPIS_BLOCK; // LAPIS
        COLOR_TO_BLOCK[33] = Blocks.EMERALD_BLOCK; // EMERALD
        COLOR_TO_BLOCK[34] = Blocks.PODZOL; // PODZOL
        COLOR_TO_BLOCK[35] = Blocks.NETHERRACK; // NETHER
        COLOR_TO_BLOCK[36] = Blocks.WHITE_TERRACOTTA; // TERRACOTTA_WHITE
        COLOR_TO_BLOCK[37] = Blocks.ORANGE_TERRACOTTA; // TERRACOTTA_ORANGE
        COLOR_TO_BLOCK[38] = Blocks.MAGENTA_TERRACOTTA; // TERRACOTTA_MAGENTA
        COLOR_TO_BLOCK[39] = Blocks.LIGHT_BLUE_TERRACOTTA; // TERRACOTTA_LIGHT_BLUE
        COLOR_TO_BLOCK[40] = Blocks.YELLOW_TERRACOTTA; // TERRACOTTA_YELLOW
        COLOR_TO_BLOCK[41] = Blocks.LIME_TERRACOTTA; // TERRACOTTA_LIGHT_GREEN
        COLOR_TO_BLOCK[42] = Blocks.PINK_TERRACOTTA; // TERRACOTTA_PINK
        COLOR_TO_BLOCK[43] = Blocks.GRAY_TERRACOTTA; // TERRACOTTA_GRAY
        COLOR_TO_BLOCK[44] = Blocks.LIGHT_GRAY_TERRACOTTA; // TERRACOTTA_LIGHT_GRAY
        COLOR_TO_BLOCK[45] = Blocks.CYAN_TERRACOTTA; // TERRACOTTA_CYAN
        COLOR_TO_BLOCK[46] = Blocks.PURPLE_TERRACOTTA; // TERRACOTTA_PURPLE
        COLOR_TO_BLOCK[47] = Blocks.BLUE_TERRACOTTA; // TERRACOTTA_BLUE
        COLOR_TO_BLOCK[48] = Blocks.BROWN_TERRACOTTA; // TERRACOTTA_BROWN
        COLOR_TO_BLOCK[49] = Blocks.GREEN_TERRACOTTA; // TERRACOTTA_GREEN
        COLOR_TO_BLOCK[50] = Blocks.RED_TERRACOTTA; // TERRACOTTA_RED
        COLOR_TO_BLOCK[51] = Blocks.BLACK_TERRACOTTA; // TERRACOTTA_BLACK
        COLOR_TO_BLOCK[52] = Blocks.CRIMSON_NYLIUM; // CRIMSON_NYLIUM
        COLOR_TO_BLOCK[53] = Blocks.CRIMSON_STEM; // CRIMSON_STEM
        COLOR_TO_BLOCK[54] = Blocks.CRIMSON_HYPHAE; // CRIMSON_HYPHAE
        COLOR_TO_BLOCK[55] = Blocks.WARPED_NYLIUM; // WARPED_NYLIUM
        COLOR_TO_BLOCK[56] = Blocks.WARPED_STEM; // WARPED_STEM
        COLOR_TO_BLOCK[57] = Blocks.WARPED_HYPHAE; // WARPED_HYPHAE
        COLOR_TO_BLOCK[58] = Blocks.WARPED_WART_BLOCK; // WARPED_WART_BLOCK
        COLOR_TO_BLOCK[59] = Blocks.DEEPSLATE; // DEEPSLATE
        COLOR_TO_BLOCK[60] = Blocks.RAW_IRON_BLOCK; // RAW_IRON
        COLOR_TO_BLOCK[61] = Blocks.GLOW_LICHEN; // GLOW_LICHEN
    }

    /**
     * Finds the closest Minecraft map color index for a given RGB color.
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return The index of the closest color in the Minecraft palette
     */
    public static byte findClosestColor(int r, int g, int b) {
        double minDistance = Double.MAX_VALUE;
        int closestIndex = 0;

        for (int i = 4; i < PALETTE.length; i++) {
            Color paletteColor = PALETTE[i];
            if (paletteColor == null) continue;

            int pr = paletteColor.getRed();
            int pg = paletteColor.getGreen();
            int pb = paletteColor.getBlue();

            double distance = Math.sqrt(
                Math.pow(r - pr, 2) +
                Math.pow(g - pg, 2) +
                Math.pow(b - pb, 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        return (byte) closestIndex;
    }
}
