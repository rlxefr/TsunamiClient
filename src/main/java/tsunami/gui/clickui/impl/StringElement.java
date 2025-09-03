package tsunami.gui.clickui.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;
import tsunami.TsunamiClient;
import tsunami.gui.clickui.AbstractElement;
import tsunami.gui.clickui.ClickGUI;
import tsunami.gui.font.FontRenderers;
import tsunami.setting.Setting;
import tsunami.utility.render.Render2DEngine;

import java.awt.*;

import static tsunami.features.modules.Module.mc;

public class StringElement extends AbstractElement {
    public StringElement(Setting setting) {
        super(setting);
    }

    public boolean listening;
    private String currentString = "";

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Render2DEngine.drawRect(context.getMatrices(), getX() + 5, getY() + 2, getWidth() - 11f, 10, new Color(0x94000000, true));
        FontRenderers.sf_medium_mini.drawString(context.getMatrices(), listening ? currentString + (mc.player == null || mc.player.age % 5 == 0 ? "_" : "") : (String) setting.getValue(), x + 6, y + height / 2, -1);

        if (Render2DEngine.isHovered(mouseX, mouseY, getX() + 5, getY() + 2, getWidth() - 11f, 10)) {
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND) {
                GLFW.glfwSetCursor(mc.getWindow().getHandle(),
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
            }
            ClickGUI.anyHovered = true;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (hovered && button == 0)
            listening = !listening;
        if (listening) {
            TsunamiClient.currentKeyListener = TsunamiClient.KeyListening.Strings;
            currentString = (String) setting.getValue();
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void charTyped(char key, int keyCode) {
        if (StringHelper.isValidChar(key)) {
            currentString = currentString + key;
        }
    }

    @Override
    public void keyTyped(int keyCode) {
        if (TsunamiClient.currentKeyListener != TsunamiClient.KeyListening.Strings)
            return;

        if (listening) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE -> {
                }
                case GLFW.GLFW_KEY_ENTER -> {
                    setting.setValue(currentString == null || currentString.isEmpty() ? setting.getDefaultValue() : currentString);
                    currentString = "";
                    listening = !listening;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> currentString = SliderElement.removeLastChar(currentString);
                case GLFW.GLFW_KEY_SPACE -> currentString = currentString + " ";
            }
        }
    }
}
