package com.cpaintings.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.cpaintings.utils.DitherUtils;
import com.cpaintings.utils.MapColorUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Painting {
    @SuppressWarnings("unchecked")
    public static final ComponentType<MapIdComponent> mapIdComponentType =
            (ComponentType<MapIdComponent>) Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "map_id"));
    @SuppressWarnings("unchecked")
    public static final ComponentType<LoreComponent> loreComponentType =
            (ComponentType<LoreComponent>) Registries.DATA_COMPONENT_TYPE.get(Identifier.of("minecraft", "lore"));

    private static final Set<Long> usedChunks = new HashSet<>();

    private static long allocateChunk() {
        long base = 100000;
        long chunk;
        do {
            chunk = base++;
        } while (usedChunks.contains(chunk));
        usedChunks.add(chunk);
        return chunk;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("painting")
                        .then(
                                CommandManager.argument("url", StringArgumentType.string())
                                        .executes(context -> {
                                            String url = StringArgumentType.getString(context, "url");
                                            ServerCommandSource source = context.getSource();
                                            new Thread(() -> processPainting(source, url, 1, 1, "bayer")).start();
                                            return 1;
                                        })
                                        .then(
                                                CommandManager.argument("blocksx", IntegerArgumentType.integer(1))
                                                        .executes(context -> {
                                                            String url = StringArgumentType.getString(context, "url");
                                                            int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                            ServerCommandSource source = context.getSource();
                                                            new Thread(() -> processPainting(source, url, blocksx, 1, "bayer")).start();
                                                            return 1;
                                                        })
                                                        .then(
                                                                CommandManager.argument("blocksy", IntegerArgumentType.integer(1))
                                                                        .executes(context -> {
                                                                            String url = StringArgumentType.getString(context, "url");
                                                                            int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                                            int blocksy = IntegerArgumentType.getInteger(context, "blocksy");
                                                                            ServerCommandSource source = context.getSource();
                                                                            new Thread(() -> processPainting(source, url, blocksx, blocksy, "bayer")).start();
                                                                            return 1;
                                                                        })
                                                                        .then(
                                                                                CommandManager.argument("dither", StringArgumentType.string())
                                                                                        .suggests((context, builder) -> {
                                                                                            builder.suggest("none");
                                                                                            builder.suggest("floyd");
                                                                                            builder.suggest("bayer");
                                                                                            builder.suggest("sierra");
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .executes(context -> {
                                                                                            String url = StringArgumentType.getString(context, "url");
                                                                                            int blocksx = IntegerArgumentType.getInteger(context, "blocksx");
                                                                                            int blocksy = IntegerArgumentType.getInteger(context, "blocksy");
                                                                                            String dither = StringArgumentType.getString(context, "dither");
                                                                                            ServerCommandSource source = context.getSource();
                                                                                            new Thread(() -> processPainting(source, url, blocksx, blocksy, dither)).start();
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                        )
        ));
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

    private static void processPainting(ServerCommandSource source, String url, int blocksx, int blocksy, String ditherMethod) {
        try {
            BufferedImage originalImage = downloadImage(url);
            if (originalImage == null) {
                throw new Exception("Image could not be read (null).");
            }
            int totalWidth = 128 * blocksx;
            int totalHeight = 128 * blocksy;
            BufferedImage resized = resizeImage(originalImage, totalWidth, totalHeight);
            ServerWorld world = source.getWorld();
            PlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("Player not found."));
                return;
            }
            for (int y = 0; y < blocksy; y++) {
                int subY = (blocksy - 1 - y);
                for (int x = 0; x < blocksx; x++) {
                    BufferedImage tile = resized.getSubimage(x * 128, subY * 128, 128, 128);
                    long chunkPos = allocateChunk();
                    MapState mapState = createMapStateForTile(world, chunkPos, tile, ditherMethod);
                    MapIdComponent mapId = world.increaseAndGetMapId();
                    world.putMapState(mapId, mapState);
                    ItemStack mapItem = new ItemStack(Items.FILLED_MAP);
                    mapItem.set(mapIdComponentType, mapId);
                    LoreComponent lore = new LoreComponent(Collections.singletonList(Text.literal("[" + x + "," + y + "]")));
                    if (blocksx > 1 && blocksy > 1) {
                        mapItem.set(loreComponentType, lore);
                    }
                    if (isInventoryFull(player)) {
                        player.dropItem(mapItem, false);
                    } else {
                        player.getInventory().insertStack(mapItem);
                    }
                }
            }
            source.sendFeedback(
                    () -> Text.literal("Created " + (blocksx * blocksy) + " maps using " + ditherMethod + " dithering. Check your inventory!"),
                    false
            );
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
        java.awt.Graphics2D g = resized.createGraphics();
        try {
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    private static MapState createMapStateForTile(ServerWorld world, long chunkPos, BufferedImage image, String ditherMethod) {
        int centerX = (int) (chunkPos >> 32) << 4;
        int centerZ = (int) (chunkPos & 0xFFFFFFFFL) << 4;
        MapState mapState = MapState.of(
                centerX + 64,
                centerZ + 64,
                (byte) 2,
                false,
                false,
                world.getRegistryKey()
        );
        DitherUtils.applyDithering(mapState, image, ditherMethod);
        return mapState;
    }
}
