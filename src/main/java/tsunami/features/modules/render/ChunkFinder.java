package tsunami.features.modules.render;

import net.minecraft.block.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;
import tsunami.setting.impl.ColorSetting;
import tsunami.utility.render.Render2DEngine;
import tsunami.utility.render.Render3DEngine;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static tsunami.features.modules.client.ClientSettings.isRu;

public class ChunkFinder extends Module {

    public ChunkFinder() {
        super("ChunkFinder", Category.DONUT);
    }

    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0xFF00FF00)); // Green by default
    private final Setting<Boolean> tracers = new Setting<>("Tracers", true);
    private final Setting<Boolean> fill = new Setting<>("Fill", true);
    private final Setting<Boolean> outline = new Setting<>("Outline", true);
    private final Setting<Integer> minY = new Setting<>("MinY", 8, -64, 320);
    private final Setting<Integer> maxUndergroundY = new Setting<>("MaxUndergroundY", 60, -64, 320);
    private final Setting<Integer> highlightY = new Setting<>("HighlightY", 30, -64, 320);
    private final Setting<Boolean> playSound = new Setting<>("PlaySound", true);
    private final Setting<Boolean> chatNotification = new Setting<>("ChatNotification", true);
    private final Setting<Boolean> findRotatedDeepslate = new Setting<>("FindRotatedDeepslate", true);

    // NEW SETTING: Toggle for block highlighting
    private final Setting<Boolean> highlightBlocks = new Setting<>("HighlightBlocks", true);
    private final Setting<ColorSetting> blockHighlightColor = new Setting<>("BlockHighlightColor", new ColorSetting(0xFFFF0000)); // Red by default

    // Thread-safe storage for flagged chunk positions
    private final Set<net.minecraft.util.math.ChunkPos> flaggedChunks = ConcurrentHashMap.newKeySet();
    private final Set<net.minecraft.util.math.ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Set<net.minecraft.util.math.ChunkPos> notifiedChunks = ConcurrentHashMap.newKeySet();

    // NEW: Storage for flagged blocks per chunk
    private final ConcurrentMap<net.minecraft.util.math.ChunkPos, Set<BlockPos>> flaggedBlocks = new ConcurrentHashMap<>();

    // Threading
    private ExecutorService scannerThread;
    private Future<?> currentScanTask;
    private volatile boolean shouldStop = false;

    // Performance tracking
    private long lastScanTime = 0;
    private static final long SCAN_COOLDOWN = 500; // Reduced to 0.5 seconds for better responsiveness
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 5000; // 5 seconds between cleanups

    @Override
    public void onEnable() {
        flaggedChunks.clear();
        scannedChunks.clear();
        notifiedChunks.clear();
        flaggedBlocks.clear(); // Clear flagged blocks
        shouldStop = false;

        // Create dedicated scanner thread
        scannerThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ChunkFinder-Scanner");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        // Initial scan
        scheduleChunkScan();
    }

    @Override
    public void onDisable() {
        shouldStop = true;

        // Cancel current task
        if (currentScanTask != null && !currentScanTask.isDone()) {
            currentScanTask.cancel(true);
        }

        // Shutdown thread pool
        if (scannerThread != null && !scannerThread.isShutdown()) {
            scannerThread.shutdownNow();
        }

        flaggedChunks.clear();
        scannedChunks.clear();
        notifiedChunks.clear();
        flaggedBlocks.clear(); // Clear flagged blocks
    }

    @Override
    public void onUpdate() {
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        // Schedule new chunk scan periodically
        if (currentTime - lastScanTime > SCAN_COOLDOWN) {
            scheduleChunkScan();
            lastScanTime = currentTime;
        }

        // Periodic cleanup of distant chunks
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupDistantChunks();
            lastCleanupTime = currentTime;
        }
    }

    private void scheduleChunkScan() {
        if (shouldStop || scannerThread == null || scannerThread.isShutdown()) return;

        // Cancel previous task if still running
        if (currentScanTask != null && !currentScanTask.isDone()) {
            currentScanTask.cancel(false);
        }

        currentScanTask = scannerThread.submit(this::scanChunksBackground);
    }

    private void scanChunksBackground() {
        if (shouldStop || mc.world == null || mc.player == null) return;

        try {
            // Use the same method as StorageEsp to get all loaded chunks
            List<WorldChunk> loadedChunks = getLoadedChunks();

            for (WorldChunk chunk : loadedChunks) {
                if (shouldStop || chunk == null || chunk.isEmpty()) continue;

                net.minecraft.util.math.ChunkPos chunkPos = chunk.getPos();

                // Skip if already scanned recently
                if (scannedChunks.contains(chunkPos)) continue;

                boolean wasAlreadyFlagged = flaggedChunks.contains(chunkPos);

                // NEW: Get flagged blocks from scanning
                Set<BlockPos> chunkFlaggedBlocks = scanChunkForCoveredOres(chunk);
                boolean shouldFlag = !chunkFlaggedBlocks.isEmpty();

                if (shouldFlag) {
                    flaggedChunks.add(chunkPos);
                    flaggedBlocks.put(chunkPos, chunkFlaggedBlocks); // Store flagged blocks

                    // Notify only for newly discovered chunks
                    if (!wasAlreadyFlagged && !notifiedChunks.contains(chunkPos)) {
                        notifyChunkFound(chunkPos);
                        notifiedChunks.add(chunkPos);
                    }
                } else {
                    flaggedChunks.remove(chunkPos);
                    notifiedChunks.remove(chunkPos);
                    flaggedBlocks.remove(chunkPos); // Remove flagged blocks
                }

                scannedChunks.add(chunkPos);

                // Small delay to prevent overwhelming the system
                Thread.sleep(5); // Reduced delay for better responsiveness
            }
        } catch (InterruptedException e) {
            // Thread interrupted, exit gracefully
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Log error but don't crash
            System.err.println("Error in ChunkFinder background scanning: " + e.getMessage());
        }
    }

    private void notifyChunkFound(net.minecraft.util.math.ChunkPos chunkPos) {
        // Schedule notifications on the main thread
        mc.execute(() -> {
            try {
                if (chatNotification.getValue() && mc.player != null) {
                    String message = isRu() ?
                            "§a🔍 [ChunkFinder] §fНайден подземный чанк: 📍" + chunkPos.x + ", " + chunkPos.z :
                            "§b🔍 [ChunkFinder] ✨ Sus Chunk Found: 📍 " + chunkPos.x + ", " + chunkPos.z;
                    mc.player.sendMessage(Text.literal(message), false);
                }

                if (playSound.getValue() && mc.player != null) {
                    mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);
                }
            } catch (Exception e) {
                // Ignore notification errors
            }
        });
    }

    // MODIFIED: Now returns Set<BlockPos> instead of boolean
    private Set<BlockPos> scanChunkForCoveredOres(WorldChunk chunk) {
        Set<BlockPos> foundBlocks = ConcurrentHashMap.newKeySet();

        if (shouldStop || chunk == null || chunk.isEmpty()) return foundBlocks;

        net.minecraft.util.math.ChunkPos cpos = chunk.getPos();
        int xStart = cpos.getStartX();
        int zStart = cpos.getStartZ();
        int yMin = Math.max(chunk.getBottomY(), minY.getValue());
        int yMax = Math.min(chunk.getBottomY() + chunk.getHeight(), maxUndergroundY.getValue());

        // --- Old Detection (with TUFF + DEEPSLATE Y-range check added) ---
        for (int x = xStart; x < xStart + 16; x++) {
            for (int z = zStart; z < zStart + 16; z++) {
                for (int y = yMin; y < yMax; y++) {
                    if (shouldStop) return foundBlocks;

                    BlockPos pos = new BlockPos(x, y, z);
                    try {
                        BlockState state = chunk.getBlockState(pos);
                        Block block = state.getBlock();

                        // ✅ TUFF and DEEPSLATE Y-level detection - NOW WITH COVERAGE CHECK
                        if ((block == Blocks.TUFF || block == Blocks.DEEPSLATE) && y > 8 && y < 60) {
                            // Only flag if ALL 6 sides are covered
                            if (isBlockCovered(chunk, pos) && isPositionUnderground(pos)) {
                                foundBlocks.add(pos);
                            }
                        }

                        // ✅ Normal target blocks (covered underground blocks)
                        if (isTargetBlock(state) && y >= minY.getValue() && y <= maxUndergroundY.getValue()) {
                            if (isBlockCovered(chunk, pos) && isPositionUnderground(pos)) {
                                foundBlocks.add(pos);
                            }
                        }

                        // ✅ Rotated deepslate detection
                        if (findRotatedDeepslate.getValue() && isRotatedDeepslate(state)) {
                            if (isBlockCovered(chunk, pos) && isPositionUnderground(pos)) {
                                foundBlocks.add(pos);
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }

        // --- New Detection: Plugged Tunnels ---
        for (int x = xStart; x < xStart + 16; x++) {
            for (int z = zStart; z < zStart + 16; z++) {
                int sameCount = 0;
                Block plugType = null;
                List<BlockPos> currentPlug = new ArrayList<>();

                for (int y = yMin; y <= yMax; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    Block block = state.getBlock();

                    if (isPlugBlock(block)) {
                        if (plugType == null || block == plugType) {
                            sameCount++;
                            plugType = block;
                            currentPlug.add(pos);
                        } else {
                            sameCount = 1;
                            plugType = block;
                            currentPlug.clear();
                            currentPlug.add(pos);
                        }

                        // If we find a "plug" of >= 23 same blocks stacked vertically
                        if (sameCount >= 25) {
                            if (isCoveredColumn(chunk, x, z, y - sameCount + 1, y, plugType)) {
                                // EXTRA CHECK: Verify each block in the plug is individually covered
                                List<BlockPos> verifiedBlocks = new ArrayList<>();
                                for (BlockPos plugPos : currentPlug) {
                                    if (isBlockCovered(chunk, plugPos) && isPositionUnderground(plugPos)) {
                                        verifiedBlocks.add(plugPos);
                                    }
                                }
                                foundBlocks.addAll(verifiedBlocks); // Only add verified covered blocks
                            }
                        }
                    } else {
                        sameCount = 0;
                        plugType = null;
                        currentPlug.clear();
                    }
                }
            }
        }
// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
        return foundBlocks;
    }
    // MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
    // Blocks we consider "suspicious plugs"
    private boolean isPlugBlock(Block block) {
        return block == Blocks.GRANITE || block == Blocks.ANDESITE || block == Blocks.DIRT|| block == Blocks.DIORITE;
    }
    // MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
    // Check if a vertical line of blocks is fully covered (no exposure to air)
    private boolean isCoveredColumn(WorldChunk chunk, int x, int z, int yStart, int yEnd, Block blockType) {
        for (int y = yStart; y <= yEnd; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!chunk.getBlockState(pos).isOf(blockType)) return false;
// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
            for (Direction dir : Direction.values()) {
                BlockPos adj = pos.offset(dir);
                BlockState adjState = chunk.getBlockState(adj);
                if (adjState.isAir() || !adjState.isSolidBlock(mc.world, adj)) {
                    return false;
                }
            }
        }
        return true;
    }
    // MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
    private boolean isPositionUnderground(BlockPos pos) {
        if (mc.world == null) return false;

        // Check if the position is actually underground by looking for non-air blocks above
        int checkHeight = Math.min(pos.getY() + 50, maxUndergroundY.getValue() + 20);
        int solidBlocksAbove = 0;

        for (int y = pos.getY() + 1; y <= checkHeight; y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState state = mc.world.getBlockState(checkPos);
            if (!state.isAir() && state.isSolidBlock(mc.world, checkPos)) {
                solidBlocksAbove++;
            }
        }
// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
        // Consider underground if there are at least 3 solid blocks above within the check range
        return solidBlocksAbove >= 3;
    }

    private boolean isTargetBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();

        // Check for regular deepslate (not rotated)
        if (block == Blocks.DEEPSLATE) {
            // Only flag regular deepslate if it's NOT rotated
            return !isRotatedDeepslate(state);
        }
// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
        // MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
        if (block == Blocks.TUFF) return true;

        return false;
    }
    // MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
    private boolean isRotatedDeepslate(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();

        // Check if it's deepslate
        if (block == Blocks.DEEPSLATE) {
            // Check if the deepslate has a non-default axis orientation
            if (state.contains(PillarBlock.AXIS)) {
                Direction.Axis axis = state.get(PillarBlock.AXIS);
                // Default deepslate has Y axis, so X and Z axis are "rotated"
                return axis == Direction.Axis.X || axis == Direction.Axis.Z;
            }
        }
        return false;// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD// MADE BY TERMEDD SKID THIS AND ILL FUCK U HARD
    }

    private boolean isBlockCovered(WorldChunk chunk, BlockPos pos) {
        // Check all 6 directions around the block
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : directions) {
            BlockPos adjacentPos = pos.offset(dir);
            try {
                BlockState adjacentState = null;

                // Use world data for reliable checks
                if (mc.world != null) {
                    adjacentState = mc.world.getBlockState(adjacentPos);
                } else {
                    return false;
                }

                // If any adjacent block is air or transparent, the block is NOT fully covered
                if (adjacentState.isAir() || isTransparentBlock(adjacentState)) {
                    return false;
                }

                // Also check if adjacent block is solid
                if (!adjacentState.isSolidBlock(mc.world, adjacentPos)) {
                    return false;
                }
            } catch (Exception e) {
                // If we can't check an adjacent block, assume it's not covered
                return false;
            }
        }

        // ALL 6 sides are covered by solid, non-transparent blocks
        return true;
    }

    private boolean isTransparentBlock(BlockState state) {
        Block block = state.getBlock();

        // Check for specific transparent/semi-transparent blocks
        if (block == Blocks.GLASS || block == Blocks.WATER || block == Blocks.LAVA ||
                block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) {
            return true;
        }

        // Check for all stained glass variants
        if (block == Blocks.WHITE_STAINED_GLASS || block == Blocks.ORANGE_STAINED_GLASS ||
                block == Blocks.MAGENTA_STAINED_GLASS || block == Blocks.LIGHT_BLUE_STAINED_GLASS ||
                block == Blocks.YELLOW_STAINED_GLASS || block == Blocks.LIME_STAINED_GLASS ||
                block == Blocks.PINK_STAINED_GLASS || block == Blocks.GRAY_STAINED_GLASS ||
                block == Blocks.LIGHT_GRAY_STAINED_GLASS || block == Blocks.CYAN_STAINED_GLASS ||
                block == Blocks.PURPLE_STAINED_GLASS || block == Blocks.BLUE_STAINED_GLASS ||
                block == Blocks.BROWN_STAINED_GLASS || block == Blocks.GREEN_STAINED_GLASS ||
                block == Blocks.RED_STAINED_GLASS || block == Blocks.BLACK_STAINED_GLASS ||
                block == Blocks.TINTED_GLASS) {
            return true;
        }

        // Check for leaves
        if (block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES ||
                block == Blocks.BIRCH_LEAVES || block == Blocks.JUNGLE_LEAVES ||
                block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES ||
                block == Blocks.MANGROVE_LEAVES || block == Blocks.CHERRY_LEAVES) {
            return true;
        }

        return false;
    }

    private void cleanupDistantChunks() {
        if (mc.player == null) return;

        // Use view distance to determine cleanup range (same as StorageEsp)
        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = (int) mc.player.getX() / 16;
        int playerChunkZ = (int) mc.player.getZ() / 16;

        flaggedChunks.removeIf(chunkPos -> {
            int dx = Math.abs(chunkPos.x - playerChunkX);
            int dz = Math.abs(chunkPos.z - playerChunkZ);
            boolean shouldRemove = dx > viewDist || dz > viewDist;
            if (shouldRemove) {
                flaggedBlocks.remove(chunkPos); // Also remove flagged blocks
            }
            return shouldRemove;
        });

        scannedChunks.removeIf(chunkPos -> {
            int dx = Math.abs(chunkPos.x - playerChunkX);
            int dz = Math.abs(chunkPos.z - playerChunkZ);
            return dx > viewDist || dz > viewDist;
        });

        notifiedChunks.removeIf(chunkPos -> {
            int dx = Math.abs(chunkPos.x - playerChunkX);
            int dz = Math.abs(chunkPos.z - playerChunkZ);
            return dx > viewDist || dz > viewDist;
        });
    }

    public void onRender3D(MatrixStack stack) {
        if (fullNullCheck() || flaggedChunks.isEmpty()) return;

        // Performance check
        if (mc.getCurrentFps() < 8 && mc.player.age > 100) {
            disable(isRu() ? "Спасаем твой ПК :)" : "Saving ur pc :)");
            return;
        }

        // Render all flagged chunks (no range limitation like StorageEsp)
        for (net.minecraft.util.math.ChunkPos chunkPos : flaggedChunks) {
            renderChunkHighlight(stack, chunkPos);

            // NEW: Render individual flagged blocks if enabled
            if (highlightBlocks.getValue()) {
                renderFlaggedBlocks(stack, chunkPos);
            }
        }
    }

    private void renderChunkHighlight(MatrixStack stack, net.minecraft.util.math.ChunkPos chunkPos) {
        // Calculate chunk boundaries
        int startX = chunkPos.x * 16;
        int startZ = chunkPos.z * 16;
        int endX = startX + 16;
        int endZ = startZ + 16;

        double y = highlightY.getValue();
        double height = 0.1f; // Create a thin layer

        Box chunkBox = new Box(startX, y, startZ, endX, y + height, endZ);
        Color renderColor = color.getValue().getColorObject();

        if (fill.getValue()) {
            Render3DEngine.drawFilledBox(stack, chunkBox, renderColor);
        }

        if (outline.getValue()) {
            Render3DEngine.drawBoxOutline(chunkBox, Render2DEngine.injectAlpha(renderColor, 255), 2f);
        }

        if (tracers.getValue()) {
            Vec3d tracerStart = new Vec3d(0, 0, 75)
                    .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                    .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                    .add(mc.cameraEntity.getEyePos());

            // Target the center of the chunk at the highlight Y level
            Vec3d chunkCenter = new Vec3d(startX + 8, y + height/2, startZ + 8);
            Render3DEngine.drawLineDebug(tracerStart, chunkCenter, renderColor);
        }
    }

    // NEW: Method to render individual flagged blocks
    private void renderFlaggedBlocks(MatrixStack stack, net.minecraft.util.math.ChunkPos chunkPos) {
        Set<BlockPos> blocks = flaggedBlocks.get(chunkPos);
        if (blocks == null || blocks.isEmpty()) return;

        Color blockColor = blockHighlightColor.getValue().getColorObject();

        for (BlockPos blockPos : blocks) {
            // Create a box around the block
            Box blockBox = new Box(blockPos);

            // Render block outline
            Render3DEngine.drawBoxOutline(blockBox, Render2DEngine.injectAlpha(blockColor, 255), 1.5f);

            // Render semi-transparent fill
            Render3DEngine.drawFilledBox(stack, blockBox, Render2DEngine.injectAlpha(blockColor, 80));
        }
    }

    // Use the same method as StorageEsp to get loaded chunks
    public static List<WorldChunk> getLoadedChunks() {
        List<WorldChunk> chunks = new ArrayList<>();
        int viewDist = mc.options.getViewDistance().getValue();

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(
                        (int) mc.player.getX() / 16 + x,
                        (int) mc.player.getZ() / 16 + z
                );
                if (chunk != null) chunks.add(chunk);
            }
        }
        return chunks;
    }

    // Block update handler for real-time updates
    public void onBlockUpdate(BlockPos pos, BlockState newState) {
        if (mc.world == null || mc.player == null) return;

        // Get the exact chunk this block belongs to
        net.minecraft.util.math.ChunkPos chunkPos = new net.minecraft.util.math.ChunkPos(pos);

        // Mark chunk as unscanned so it gets re-evaluated
        scannedChunks.remove(chunkPos);

        // If a target block was removed, remove the chunk from flagged list immediately
        if (newState.isAir() && flaggedChunks.contains(chunkPos)) {
            flaggedChunks.remove(chunkPos);
            notifiedChunks.remove(chunkPos);
            flaggedBlocks.remove(chunkPos); // Remove flagged blocks
        }
    }

    // Chunk handlers
    public void onChunkLoad(net.minecraft.util.math.ChunkPos chunkPos) {
        // Mark chunk as unscanned so it gets scanned
        scannedChunks.remove(chunkPos);
    }

    public void onChunkUnload(net.minecraft.util.math.ChunkPos chunkPos) {
        // Remove chunk from all lists
        flaggedChunks.remove(chunkPos);
        scannedChunks.remove(chunkPos);
        notifiedChunks.remove(chunkPos);
        flaggedBlocks.remove(chunkPos); // Remove flagged blocks
    }
}