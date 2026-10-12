package com.mas6y6.sablefinder.client;

import com.mas6y6.sablefinder.sable.SableContraptionData;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Contraption3DViewComponent extends BaseComponent {

    public record RenderBlock(BlockPos pos, BlockState state) {}

    private final SableContraptionData contraptionData;
    private final List<RenderBlock> blocks = new ArrayList<>();

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private double centerX, centerY, centerZ;
    private double maxDim = 1.0;

    private float yaw = -45.0f;
    private float pitch = 30.0f;
    private float zoom = 1.0f;
    private float panX = 0.0f;
    private float panY = 0.0f;

    private boolean autoRotate = true;
    private boolean showBoundingBox = true;
    private Consumer<Boolean> onAutoRotateChanged = null;

    public Contraption3DViewComponent(SableContraptionData contraptionData) {
        this.contraptionData = contraptionData;

        this.horizontalSizing(Sizing.fill(100));
        this.verticalSizing(Sizing.expand());

        this.cursorStyle(CursorStyle.HAND);

        loadContraptionBlocks();
    }

    private void loadContraptionBlocks() {
        blocks.clear();
        if (contraptionData == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            contraptionData.ensureChunksLoaded(mc.level);
        }

        contraptionData.forEachBlock((pos, state) -> {
            blocks.add(new RenderBlock(pos.immutable(), state));
        });

        if (!blocks.isEmpty()) {
            minX = Integer.MAX_VALUE;
            minY = Integer.MAX_VALUE;
            minZ = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE;
            maxY = Integer.MIN_VALUE;
            maxZ = Integer.MIN_VALUE;

            for (RenderBlock b : blocks) {
                int bx = b.pos().getX();
                int by = b.pos().getY();
                int bz = b.pos().getZ();

                if (bx < minX) minX = bx;
                if (by < minY) minY = by;
                if (bz < minZ) minZ = bz;

                if (bx + 1 > maxX) maxX = bx + 1;
                if (by + 1 > maxY) maxY = by + 1;
                if (bz + 1 > maxZ) maxZ = bz + 1;
            }
        } else {
            BoundingBox3d bounds = contraptionData.bounds();
            if (bounds != null) {
                minX = bounds.minX;
                minY = bounds.minY;
                minZ = bounds.minZ;
                maxX = bounds.maxX;
                maxY = bounds.maxY;
                maxZ = bounds.maxZ;
            } else {
                minX = 0;
                minY = 0;
                minZ = 0;
                maxX = 1;
                maxY = 1;
                maxZ = 1;
            }
        }

        centerX = (minX + maxX) / 2.0;
        centerY = (minY + maxY) / 2.0;
        centerZ = (minZ + maxZ) / 2.0;

        double sizeX = Math.max(0.1, maxX - minX);
        double sizeY = Math.max(0.1, maxY - minY);
        double sizeZ = Math.max(0.1, maxZ - minZ);
        maxDim = Math.max(1.0, Math.max(sizeX, Math.max(sizeY, sizeZ)));
    }

    public void setOnAutoRotateChanged(Consumer<Boolean> onAutoRotateChanged) {
        this.onAutoRotateChanged = onAutoRotateChanged;
    }

    public void resetView() {
        this.yaw = -45.0f;
        this.pitch = 30.0f;
        this.zoom = 1.0f;
        this.panX = 0.0f;
        this.panY = 0.0f;
        this.autoRotate = false;
        if (this.onAutoRotateChanged != null) {
            this.onAutoRotateChanged.accept(false);
        }
    }

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
        if (this.onAutoRotateChanged != null) {
            this.onAutoRotateChanged.accept(autoRotate);
        }
    }

    public void toggleAutoRotate() {
        setAutoRotate(!this.autoRotate);
    }

    public boolean isShowBoundingBox() {
        return showBoundingBox;
    }

    public void setShowBoundingBox(boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    public void toggleBoundingBox() {
        this.showBoundingBox = !this.showBoundingBox;
    }

    public int getBlockCount() {
        return blocks.size();
    }

    @Override
    public CursorStyle cursorStyle() {
        return CursorStyle.HAND;
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return true;
    }

    @Override
    public void onFocusLost() {
        super.onFocusLost();
    }

    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        return super.onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return 160;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        super.onMouseDown(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (button == 0) {
            if (this.autoRotate) {
                this.autoRotate = false;
                if (this.onAutoRotateChanged != null) {
                    this.onAutoRotateChanged.accept(false);
                }
            }
            this.yaw = (float) ((this.yaw + deltaX * 2) % 360.0);
            this.pitch = Mth.clamp((float) (this.pitch + deltaY * 2), -89.0f, 89.0f);
            super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
            return true;
        } else if (button == 1 || button == 2) {
            this.panX += (float) deltaX;
            this.panY += (float) deltaY;
            super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
            return true;
        }
        return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        float factor = amount > 0 ? 1.15f : (1.0f / 1.15f);
        this.zoom = Mth.clamp(this.zoom * factor, 0.05f, 20.0f);
        super.onMouseScroll(mouseX, mouseY, amount);
        return true;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        // Draw container background and border
        context.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF121216);
        drawBorder(context, this.x, this.y, this.width, this.height, 0xFF2A2A35);

        Font font = Minecraft.getInstance().font;

        if (blocks.isEmpty()) {
            Component emptyText = Component.translatable("sablefinder.ui.view.empty");
            int textWidth = font.width(emptyText);
            context.drawString(font, emptyText, this.x + (this.width - textWidth) / 2, this.y + (this.height - 8) / 2, 0xFFFFFFFF, false);
            return;
        }

        if (autoRotate) {
            this.yaw = (this.yaw + delta * 4.0f) % 360.0f;
        }

        // Enable scissor to keep 3D model inside the viewport box
        context.enableScissor(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1);

        context.flush();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();

        PoseStack poseStack = context.pose();
        poseStack.pushPose();

        // Position camera in center of the component viewport
        poseStack.translate(this.x + this.width / 2.0 + panX, this.y + this.height / 2.0 + panY, 150.0);

        float baseScale = (float) ((Math.min(this.width, this.height) * 0.55) / maxDim);
        float currentScale = baseScale * zoom;
        poseStack.scale(currentScale, -currentScale, currentScale);

        // Apply rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        // Center the model at origin
        poseStack.translate(-centerX, -centerY, -centerZ);

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        // Render all non-air blocks in the contraption
        for (RenderBlock block : blocks) {
            poseStack.pushPose();
            poseStack.translate(block.pos.getX(), block.pos.getY(), block.pos.getZ());
            blockRenderer.renderSingleBlock(
                    block.state, poseStack,
                    context.bufferSource(),
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    null
            );
            poseStack.popPose();
        }

        // Render wireframe bounding box if enabled
        if (showBoundingBox) {
            VertexConsumer lineConsumer = context.bufferSource().getBuffer(RenderType.lines());
            LevelRenderer.renderLineBox(
                    poseStack, lineConsumer,
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    0.3f, 0.8f, 1.0f, 0.6f
            );
        }

        context.flush();
        Lighting.setupForFlatItems();
        RenderSystem.disableDepthTest();
        poseStack.popPose();

        context.disableScissor();

        // Top-left HUD badge: Block count
        Component countBadge = Component.translatable("sablefinder.ui.view.block_count", blocks.size());
        int badgeWidth = font.width(countBadge);
        context.fill(this.x + 4, this.y + 4, this.x + 10 + badgeWidth, this.y + 16, 0x88000000);
        context.drawString(font, countBadge, this.x + 7, this.y + 6, 0xFFFFFF, false);

        // Bottom-left hints
        context.drawString(font, Component.translatable("sablefinder.ui.view.hint.rotate"), this.x + 6, this.y + this.height - 32, 0xFFFFFF, false);
        context.drawString(font, Component.translatable("sablefinder.ui.view.hint.pan"), this.x + 6, this.y + this.height - 22, 0xFFFFFF, false);
        context.drawString(font, Component.translatable("sablefinder.ui.view.hint.zoom"), this.x + 6, this.y + this.height - 12, 0xFFFFFF, false);
    }

    private void drawBorder(OwoUIDrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
