package tsunami.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.KelpBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import tsunami.gui.font.FontRenderers;
import tsunami.features.modules.Module;
import tsunami.features.modules.client.HudEditor;
import tsunami.setting.Setting;
import tsunami.setting.impl.ColorSetting;
import tsunami.utility.render.Render2DEngine;
import tsunami.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KelpEsp extends Module {
    public KelpEsp() {
        super("KelpESP", Category.DONUT);
    }

    private final Setting<Boolean> shadow = new Setting<>("Shadow", true);
    private final Setting<ColorSetting> scolor = new Setting<>("ShadowColor", new ColorSetting(new Color(0x000000).getRGB()));
    private final Setting<ColorSetting> tcolor = new Setting<>("TextColor", new ColorSetting(new Color(-1).getRGB()));

    // Cache for performance
    private final List<BlockPos> kelpPositions = new ArrayList<>();
    private int lastUpdateTick = 0;

    public void onRender2D(DrawContext context) {
        // Update kelp positions every 20 ticks for performance
        if (mc.player.age - lastUpdateTick > 20) {
            updateKelpPositions();
            lastUpdateTick = mc.player.age;
        }

        for (BlockPos pos : kelpPositions) {
            Vec3d[] vectors = getPoints(pos);

            Vector4d position = null;
            for (Vec3d vector : vectors) {
                vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null)
                        position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }

            if (position != null) {
                float posX = (float) position.x;
                float posY = (float) position.y;
                float endPosX = (float) position.z;

                float diff = (endPosX - posX) / 2f;
                String displayText = String.format("%.1fm", Math.sqrt(mc.player.squaredDistanceTo(Vec3d.of(pos))));
                float textWidth = (FontRenderers.sf_bold.getStringWidth(displayText) * 1);
                float tagX = (posX + diff - textWidth / 2f) * 1;

                if (shadow.getValue())
                    Render2DEngine.drawBlurredShadow(context.getMatrices(), tagX - 2, posY - 13, FontRenderers.sf_bold.getStringWidth(displayText) + 4, 10, 14, scolor.getValue().getColorObject());

                FontRenderers.sf_bold.drawString(context.getMatrices(), displayText, tagX, (float) posY - 10, tcolor.getValue().getColor());
            }
        }

        boolean any = false;

        for (BlockPos pos : kelpPositions) {
            Vec3d[] vectors = getPoints(pos);

            Vector4d position = null;
            for (Vec3d vector : vectors) {
                vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null)
                        position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }

            if (position != null)
                any = true;
        }

        if (!any)
            return;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Render2DEngine.setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (BlockPos pos : kelpPositions) {
            Vec3d[] vectors = getPoints(pos);

            Vector4d position = null;
            for (Vec3d vector : vectors) {
                vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null)
                        position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }

            if (position != null) {
                float posX = (float) position.x;
                float posY = (float) position.y;
                float endPosX = (float) position.z;
                float endPosY = (float) position.w;

                drawRect(bufferBuilder, matrix, posX, posY, endPosX, endPosY);
            }
        }
        Render2DEngine.endBuilding(bufferBuilder);
        Render2DEngine.endRender();
    }

    private void updateKelpPositions() {
        kelpPositions.clear();
        if (mc.world == null || mc.player == null) return;

        int renderDistance = mc.options.getViewDistance().getValue() * 16;
        BlockPos playerPos = mc.player.getBlockPos();

        // Search for kelp that touches the water surface
        for (int x = playerPos.getX() - renderDistance; x <= playerPos.getX() + renderDistance; x += 1) {
            for (int z = playerPos.getZ() - renderDistance; z <= playerPos.getZ() + renderDistance; z += 1) {
                // Check from sea level down to find kelp that touches water surface
                for (int y = Math.min(80, mc.world.getTopY()); y >= Math.max(30, mc.world.getBottomY()); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isKelpTouchingWaterSurface(pos)) {
                        kelpPositions.add(pos);
                        break; // Only need one kelp per column that touches surface
                    }
                }
            }
        }
    }

    private boolean isKelpTouchingWaterSurface(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);

        // Must be kelp
        if (!(state.getBlock() instanceof KelpBlock)) {
            return false;
        }

        // Check if kelp is at water surface level (Y=62 or Y=63 typically)
        // and has air above it, indicating it's touching the surface
        BlockPos above = pos.up();
        BlockState aboveState = mc.world.getBlockState(above);

        // Kelp touches water surface if:
        // 1. There's air directly above it (surface level)
        // 2. OR there's water above but air above that (just under surface)
        if (aboveState.isOf(Blocks.AIR)) {
            return true; // Kelp is right at surface with air above
        }

        if (aboveState.isOf(Blocks.WATER)) {
            BlockPos twoAbove = above.up();
            BlockState twoAboveState = mc.world.getBlockState(twoAbove);
            // Kelp is one block under water surface
            return twoAboveState.isOf(Blocks.AIR);
        }

        return false;
    }

    private void drawRect(BufferBuilder bufferBuilder, Matrix4f stack, float posX, float posY, float endPosX, float endPosY) {
        Color black = Color.BLACK;
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1F, posY, (posX + 0.5f), endPosY + 0.5f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1F, (posY - 0.5f), endPosX + 0.5f, posY + 1f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, endPosX - 1f, posY, endPosX + 0.5f, endPosY + 0.5f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1, endPosY - 1f, endPosX + 0.5f, endPosY + 0.5f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 0.5f, posY, posX, endPosY, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270));
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX, endPosY - 0.5f, endPosX, endPosY, HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(180), HudEditor.getColor(0));
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 0.5f, posY, endPosX, (posY + 0.5f), HudEditor.getColor(180), HudEditor.getColor(90), HudEditor.getColor(90), HudEditor.getColor(180));
        Render2DEngine.setRectPoints(bufferBuilder, stack, endPosX - 0.5f, posY, endPosX, endPosY, HudEditor.getColor(90), HudEditor.getColor(270), HudEditor.getColor(270), HudEditor.getColor(90));
    }

    @NotNull
    private static Vec3d[] getPoints(BlockPos pos) {
        Box axisAlignedBB = getBox(pos);
        Vec3d[] vectors = new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
        return vectors;
    }

    @NotNull
    private static Box getBox(BlockPos pos) {
        return new Box(pos);
    }
}