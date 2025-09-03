package tsunami.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import tsunami.features.modules.Module;
import tsunami.features.modules.client.HudEditor;
import tsunami.setting.Setting;
import tsunami.setting.impl.ColorSetting;
import tsunami.utility.render.Render2DEngine;
import tsunami.utility.render.Render3DEngine;

import java.awt.*;

public class WanderingESP extends Module {
    public WanderingESP() {
        super("WanderingESP", Category.DONUT);
    }

    // ESP Settings
    private final Setting<Boolean> wanderingTraders = new Setting<>("WanderingTraders", true);
    private final Setting<Boolean> wanderingLlamas = new Setting<>("WanderingLlamas", true);
    private final Setting<Boolean> outline = new Setting<>("Outline", true);
    private final Setting<Colors> colorMode = new Setting<>("ColorMode", Colors.Custom);

    // Color Settings
    private final Setting<ColorSetting> traderColor = new Setting<>("TraderColor", new ColorSetting(new Color(0x00FF00)));
    private final Setting<ColorSetting> llamaColor = new Setting<>("LlamaColor", new ColorSetting(new Color(0xFFAA00)));

    // Tracer Settings
    private final Setting<Boolean> tracers = new Setting<>("Tracers", true);
    private final Setting<Float> tracerHeight = new Setting<>("TracerHeight", 0f, 0f, 2f, v -> tracers.getValue());
    private final Setting<ColorSetting> tracerTraderColor = new Setting<>("TracerTraderColor", new ColorSetting(new Color(0x9300FF00, true)), v -> tracers.getValue());
    private final Setting<ColorSetting> tracerLlamaColor = new Setting<>("TracerLlamaColor", new ColorSetting(new Color(0x93FFAA00, true)), v -> tracers.getValue());

    public void onRender3D(MatrixStack stack) {
        if (mc.options.hudHidden) return;

        if (tracers.getValue()) {
            renderTracers();
        }
    }

    public void onRender2D(DrawContext context) {
        if (mc.options.hudHidden) return;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Render2DEngine.setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Entity ent : mc.world.getEntities()) {
            if (shouldRender(ent)) {
                drawBox(bufferBuilder, ent, matrix);
            }
        }

        Render2DEngine.endBuilding(bufferBuilder);
        Render2DEngine.endRender();
    }

    private void renderTracers() {
        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRenderTracer(entity)) continue;

            Color tracerColor = getTracerColor(entity);

            double x1 = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * Render3DEngine.getTickDelta();
            double y1 = mc.player.getEyeHeight(mc.player.getPose()) + mc.player.prevY + (mc.player.getY() - mc.player.prevY) * Render3DEngine.getTickDelta();
            double z1 = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * Render3DEngine.getTickDelta();

            Vec3d vec2 = new Vec3d(0, 0, 75)
                    .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                    .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                    .add(x1, y1, z1);

            double x = entity.prevX + (entity.getX() - entity.prevX) * Render3DEngine.getTickDelta();
            double y = entity.prevY + (entity.getY() - entity.prevY) * Render3DEngine.getTickDelta();
            double z = entity.prevZ + (entity.getZ() - entity.prevZ) * Render3DEngine.getTickDelta();

            Render3DEngine.drawLineDebug(vec2, new Vec3d(x, y + tracerHeight.getValue(), z), tracerColor);
        }
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null || mc.player == null)
            return false;

        if (entity instanceof WanderingTraderEntity)
            return wanderingTraders.getValue();

        if (entity instanceof TraderLlamaEntity)
            return wanderingLlamas.getValue();

        return false;
    }

    public boolean shouldRenderTracer(Entity entity) {
        if (!tracers.getValue() || entity == null || mc.player == null)
            return false;

        if (entity instanceof WanderingTraderEntity)
            return wanderingTraders.getValue();

        if (entity instanceof TraderLlamaEntity)
            return wanderingLlamas.getValue();

        return false;
    }

    public Color getEntityColor(Entity entity) {
        if (entity == null)
            return new Color(-1);

        if (entity instanceof WanderingTraderEntity)
            return traderColor.getValue().getColorObject();

        if (entity instanceof TraderLlamaEntity)
            return llamaColor.getValue().getColorObject();

        return new Color(-1);
    }

    public Color getTracerColor(Entity entity) {
        if (entity == null)
            return new Color(-1);

        if (entity instanceof WanderingTraderEntity)
            return tracerTraderColor.getValue().getColorObject();

        if (entity instanceof TraderLlamaEntity)
            return tracerLlamaColor.getValue().getColorObject();

        return new Color(-1);
    }

    public void drawBox(BufferBuilder bufferBuilder, @NotNull Entity ent, Matrix4f matrix) {
        Vec3d[] vectors = getVectors(ent);

        Color col = getEntityColor(ent);

        Vector4d position = null;
        for (Vec3d vector : vectors) {
            vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
                position.w = Math.max(vector.y, position.w);
            }
        }

        if (position != null) {
            double posX = position.x;
            double posY = position.y;
            double endPosX = position.z;
            double endPosY = position.w;

            if (outline.getValue()) {
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 1F), (float) posY, (float) (posX + 0.5), (float) (endPosY + 0.5), Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 1F), (float) (posY - 0.5), (float) (endPosX + 0.5), (float) (posY + 0.5 + 0.5), Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (endPosX - 0.5 - 0.5), (float) posY, (float) (endPosX + 0.5), (float) (endPosY + 0.5), Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 1), (float) (endPosY - 0.5 - 0.5), (float) (endPosX + 0.5), (float) (endPosY + 0.5), Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
            }

            switch (colorMode.getValue()) {
                case Custom -> {
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5f), (float) posY, (float) (posX + 0.5 - 0.5), (float) endPosY, col, col, col, col);
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) posX, (float) (endPosY - 0.5f), (float) endPosX, (float) endPosY, col, col, col, col);
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5), (float) posY, (float) endPosX, (float) (posY + 0.5), col, col, col, col);
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (endPosX - 0.5), (float) posY, (float) endPosX, (float) endPosY, col, col, col, col);
                }
                case SyncColor -> {
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5f), (float) posY, (float) (posX + 0.5 - 0.5), (float) endPosY, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270));
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) posX, (float) (endPosY - 0.5f), (float) endPosX, (float) endPosY, HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(180), HudEditor.getColor(0));
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5), (float) posY, (float) endPosX, (float) (posY + 0.5), HudEditor.getColor(180), HudEditor.getColor(90), HudEditor.getColor(90), HudEditor.getColor(180));
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (endPosX - 0.5), (float) posY, (float) endPosX, (float) endPosY, HudEditor.getColor(90), HudEditor.getColor(270), HudEditor.getColor(270), HudEditor.getColor(90));
                }
            }
        }
    }

    @NotNull
    private static Vec3d[] getVectors(@NotNull Entity ent) {
        double x = ent.prevX + (ent.getX() - ent.prevX) * Render3DEngine.getTickDelta();
        double y = ent.prevY + (ent.getY() - ent.prevY) * Render3DEngine.getTickDelta();
        double z = ent.prevZ + (ent.getZ() - ent.prevZ) * Render3DEngine.getTickDelta();
        Box axisAlignedBB2 = ent.getBoundingBox();
        Box axisAlignedBB = new Box(
                axisAlignedBB2.minX - ent.getX() + x - 0.05,
                axisAlignedBB2.minY - ent.getY() + y,
                axisAlignedBB2.minZ - ent.getZ() + z - 0.05,
                axisAlignedBB2.maxX - ent.getX() + x + 0.05,
                axisAlignedBB2.maxY - ent.getY() + y + 0.15,
                axisAlignedBB2.maxZ - ent.getZ() + z + 0.05
        );
        return new Vec3d[]{
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
        };
    }

    public enum Colors {
        SyncColor, Custom
    }
}