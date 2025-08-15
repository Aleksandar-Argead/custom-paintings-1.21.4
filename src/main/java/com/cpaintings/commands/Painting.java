package com.cpaintings.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.InputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.cpaintings.utils.MapColorUtils;
import java.awt.Color;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Painting {

    @SuppressWarnings("unchecked")
    public static final ComponentType<MapIdComponent> mapIdComponentType = (ComponentType<MapIdComponent>) net.minecraft.registry.Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "map_id"));

    // Reference to color mapping utility
    // See MapColorUtils for Minecraft map color data and brightness levels
    // Palette is now managed in MapColorUtils

    // Map color indices to blocks - moved to MapColorUtils
    // Access via MapColorUtils.COLOR_TO_BLOCK

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("painting").then(
                        CommandManager.argument("url", StringArgumentType.string())
                            .executes(context -> {
                                String url = StringArgumentType.getString(context, "url");
                                ServerCommandSource source = context.getSource();
                                source.sendFeedback(() -> Text.literal("Starting painting processing from " + url), false);
                                ServerWorld world = source.getWorld();
                                world.getServer().execute(() -> processPainting(source, url, 1, 1));
                                return 1;
                            })
                            .then(
                                CommandManager.argument("blocksx", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        String url = StringArgumentType.getString(context, "url");
                                        int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                        ServerCommandSource source = context.getSource();
                                        source.sendFeedback(() -> Text.literal("Starting painting processing from " + url + " with width " + blocksx), false);
                                        ServerWorld world = source.getWorld();
                                        world.getServer().execute(() -> processPainting(source, url, blocksx, 1));
                                        return 1;
                                    })
                                    .then(
                                        CommandManager.argument("blocksy", IntegerArgumentType.integer(1)).executes(context -> {
                                                String url = StringArgumentType.getString(context, "url");
                                                int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                int blocksy = IntegerArgumentType.getInteger(context, "blocksy");
                                                ServerCommandSource source = context.getSource();
                                                source.sendFeedback(() -> Text.literal("Starting painting processing from " + url + " with dimensions " + blocksx + "x" + blocksy), false);
                                                ServerWorld world = source.getWorld();
                                                world.getServer().execute(() -> processPainting(source, url, blocksx, blocksy));
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

                            Block block = MapColorUtils.COLOR_TO_BLOCK[baseColor];
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
            MapState mapState = MapState.of(
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
            // g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

                byte index = MapColorUtils.findClosestColor(pr, pg, pb);
                mapColors[x + y * width] = index;

                Color palColor = MapColorUtils.PALETTE[index & 0xFF];
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

    // Color utility methods moved to MapColorUtils
    // Use MapColorUtils.findClosestColor() and MapColorUtils.PALETTE[index] for color operations
}
