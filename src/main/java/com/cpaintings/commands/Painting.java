package com.cpaintings.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.map.MapState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Painting {

    @SuppressWarnings("unchecked")
    public static final ComponentType<MapIdComponent> mapIdComponentType = (ComponentType<MapIdComponent>) net.minecraft.registry.Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "map_id"));

    // Standard Minecraft map base colors
    private static final int[] MINECRAFT_MAP_COLORS = {
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
        0x7FA796, // 61 (GLOW_LICHEN)
    };

    // Standard brightness levels for Minecraft maps
    private static final float[] BRIGHTNESS_LEVELS = {
        0.71f, // Shade 1
        0.86f, // Shade 2
        1.00f, // Shade 3
        0.53f, // Shade 4 (not used)
    };

    // Precomputed palette for dithering
    private static final Color[] PALETTE = new Color[256];

    static {
        for (int base = 1; base < MINECRAFT_MAP_COLORS.length; base++) {
            int baseColor = MINECRAFT_MAP_COLORS[base];
            int br = (baseColor >> 16) & 0xFF;
            int bg = (baseColor >> 8) & 0xFF;
            int bb = baseColor & 0xFF;
            for (int shade = 0; shade < BRIGHTNESS_LEVELS.length; shade++) {
                float factor = BRIGHTNESS_LEVELS[shade];
                int r = (int) (br * factor);
                int g = (int) (bg * factor);
                int b = (int) (bb * factor);
                int index = base * 4 + shade;
                PALETTE[index] = new Color(r, g, b);
            }
        }
    }

    // Map color indices to blocks
    private static final Block[] COLOR_TO_BLOCK = new Block[MINECRAFT_MAP_COLORS.length];

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

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("painting").then(
                        CommandManager.argument("url", StringArgumentType.string())
                            .executes(context -> {
                                String url = StringArgumentType.getString(context, "url");
                                ServerCommandSource source = context.getSource();
                                new Thread(() -> processPainting(source, url, 1, 1)).start();
                                return 1;
                            })
                            .then(
                                CommandManager.argument("blocksx", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        String url = StringArgumentType.getString(context, "url");
                                        int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                        ServerCommandSource source = context.getSource();
                                        new Thread(() -> processPainting(source, url, blocksx, 1)).start();
                                        return 1;
                                    })
                                    .then(
                                        CommandManager.argument("blocksy", IntegerArgumentType.integer(1)).executes(context -> {
                                                String url = StringArgumentType.getString(context, "url");
                                                int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                int blocksy = IntegerArgumentType.getInteger(context, "blocksy");
                                                ServerCommandSource source = context.getSource();
                                                new Thread(() -> processPainting(source, url, blocksx, blocksy)).start();
                                                return 1;
                                            })
                                    )
                            )
                    )
            )
        );
    }

    public static boolean isInventoryFull(PlayerEntity player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void processPainting(ServerCommandSource source, String url, int blocksx, int blocksy) {
        try {
            BufferedImage originalImage = downloadImage(url);
            if (originalImage == null) {
                throw new Exception("Image could not be read (null).");
            }

            int totalWidth = 128 * blocksx;
            int totalHeight = 128 * blocksy;
            BufferedImage resized = resizeImage(originalImage, totalWidth, totalHeight);
            byte[] ditheredColors = ditherImageToMapColors(resized);

            ServerWorld world = source.getWorld();
            PlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("Player not found."));
                return;
            }

            // Start at player's position, adjust to chunk boundary
            BlockPos startPos = player.getBlockPos();
            int startX = startPos.getX() - (startPos.getX() % 16);
            int startZ = startPos.getZ() - (startPos.getZ() % 16);
            int baseY = startPos.getY();

            // Clear a 3-block height area for -1, 0, +1 elevation
            for (int y = baseY - 1; y <= baseY + 1; y++) {
                for (int z = 0; z < totalHeight; z++) {
                    for (int x = 0; x < totalWidth; x++) {
                        world.setBlockState(new BlockPos(startX + x, y, startZ + z), Blocks.AIR.getDefaultState());
                    }
                }
            }

            // Place blocks with staircase elevation
            for (int y = 0; y < blocksy; y++) {
                int subY = (blocksy - 1 - y); // Invert Y for Minecraft's map
                for (int x = 0; x < blocksx; x++) {
                    for (int zz = 0; zz < 128; zz++) {
                        for (int xx = 0; xx < 128; xx++) {
                            int globalX = x * 128 + xx;
                            int globalY = subY * 128 + zz;
                            int index = ditheredColors[globalX + globalY * totalWidth] & 0xFF;
                            if (index == 0) continue; // Skip transparent

                            int baseColor = index / 4;
                            int shade = index % 4;
                            if (shade == 3) continue; // Shade 4 not supported

                            Block block = COLOR_TO_BLOCK[baseColor];
                            if (block == null) {
                                block = Blocks.STONE; // Fallback
                            }

                            // Determine elevation based on shade
                            int elevation;
                            if (shade == 0) elevation = -1;
                            // Shade 1: lower
                            else if (shade == 1) elevation = 0;
                            // Shade 2: same
                            else elevation = 1; // Shade 3: higher

                            // Adjust elevation relative to northern neighbor
                            int northZ = startZ + zz - 1;
                            BlockPos pos = new BlockPos(startX + xx, baseY + elevation, startZ + zz);
                            if (zz > 0) {
                                BlockPos northPos = new BlockPos(startX + xx, baseY, northZ);
                                int northElevation = world.getBlockState(northPos).isAir() ? baseY : world.getBlockState(northPos).getBlock().equals(Blocks.AIR) ? baseY : northPos.getY();
                                pos = new BlockPos(startX + xx, northElevation + elevation, startZ + zz);
                            }

                            world.setBlockState(pos, block.getDefaultState());
                        }
                    }
                }
            }

            // Create a single map item to view the result
            MapIdComponent mapId = world.increaseAndGetMapId();
            MapState mapState = net.minecraft.world.map.MapState.of(
                startX + totalWidth / 2,
                startZ + totalHeight / 2,
                (byte) 2, // scale 1:4
                false,
                false,
                world.getRegistryKey()
            );
            world.putMapState(mapId, mapState);

            ItemStack mapItem = new ItemStack(Items.FILLED_MAP);
            mapItem.set(mapIdComponentType, mapId);
            if (isInventoryFull(player)) {
                player.dropItem(mapItem, false);
            } else {
                player.getInventory().insertStack(mapItem);
            }

            source.sendFeedback(() -> Text.literal("Created " + (blocksx * blocksy) + "x map structure at " + startX + "," + startZ + ". Map in inventory!"), false);
        } catch (Exception e) {
            source.sendError(Text.literal("An error occurred: " + e.getMessage()));
        }
    }

    private static BufferedImage downloadImage(String url) throws Exception {
        try (InputStream in = new URI(url).toURL().openStream()) {
            return ImageIO.read(in);
        }
    }

    private static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    private static byte[] ditherImageToMapColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][][] pixels = new float[width][height][3];
        byte[] mapColors = new byte[width * height];

        // Extract RGB
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                pixels[x][y][0] = ((argb >> 16) & 0xFF) / 255f;
                pixels[x][y][1] = ((argb >> 8) & 0xFF) / 255f;
                pixels[x][y][2] = (argb & 0xFF) / 255f;
            }
        }

        // Floyd-Steinberg dithering
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    mapColors[x + y * width] = 0; // Transparent
                    continue;
                }

                float[] pixel = pixels[x][y];
                int pr = (int) (pixel[0] * 255f);
                int pg = (int) (pixel[1] * 255f);
                int pb = (int) (pixel[2] * 255f);

                int index = findClosestIndex(pr, pg, pb);
                mapColors[x + y * width] = (byte) index;

                Color palColor = getPaletteColor(index);
                float errR = pixel[0] - palColor.getRed() / 255f;
                float errG = pixel[1] - palColor.getGreen() / 255f;
                float errB = pixel[2] - palColor.getBlue() / 255f;

                // Diffuse error
                if (x + 1 < width) {
                    pixels[x + 1][y][0] += (errR * 7) / 16f;
                    pixels[x + 1][y][1] += (errG * 7) / 16f;
                    pixels[x + 1][y][2] += (errB * 7) / 16f;
                }
                if (y + 1 < height) {
                    if (x - 1 >= 0) {
                        pixels[x - 1][y + 1][0] += (errR * 3) / 16f;
                        pixels[x - 1][y + 1][1] += (errG * 3) / 16f;
                        pixels[x - 1][y + 1][2] += (errB * 3) / 16f;
                    }
                    pixels[x][y + 1][0] += (errR * 5) / 16f;
                    pixels[x][y + 1][1] += (errG * 5) / 16f;
                    pixels[x][y + 1][2] += (errB * 5) / 16f;
                    if (x + 1 < width) {
                        pixels[x + 1][y + 1][0] += (errR * 1) / 16f;
                        pixels[x + 1][y + 1][1] += (errG * 1) / 16f;
                        pixels[x + 1][y + 1][2] += (errB * 1) / 16f;
                    }
                }
            }
        }
        return mapColors;
    }

    private static int findClosestIndex(int tr, int tg, int tb) {
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = 4;
        for (int i = 4; i < PALETTE.length; i += 4) {
            for (int shade = 0; shade < 3; shade++) {
                int index = i + shade;
                if (PALETTE[index] == null) continue;
                Color c = PALETTE[index];
                int dr = tr - c.getRed();
                int dg = tg - c.getGreen();
                int db = tb - c.getBlue();
                double distance = dr * dr + dg * dg + db * db;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = index;
                }
            }
        }
        return bestIndex;
    }

    private static Color getPaletteColor(int index) {
        return PALETTE[index];
    }
}
