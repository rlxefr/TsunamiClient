package tsunami.gui.thundergui.components;

import net.minecraft.client.util.math.MatrixStack;
import tsunami.gui.font.FontRenderers;
import tsunami.gui.thundergui.ThunderGui;
import tsunami.setting.Setting;
import tsunami.setting.impl.SettingGroup;
import tsunami.utility.render.Render2DEngine;

import java.awt.*;

public class ParentComponent extends SettingElement {
    public ParentComponent(Setting setting) {
        super(setting);
        SettingGroup settingGroup = (SettingGroup) setting.getValue();
        settingGroup.setExtended(true);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);
        if ((getY() > ThunderGui.getInstance().main_posY + ThunderGui.getInstance().height) || getY() < ThunderGui.getInstance().main_posY) {
            return;
        }
        FontRenderers.modules.drawCenteredString(stack, getSetting().getName(), getX() + width / 2f, getY() + 2, new Color(0xB0FFFFFF, true).getRGB());
        Render2DEngine.draw2DGradientRect(stack, getX() + 10, getY() + 6, (getX() + width / 2f) - 20, getY() + 7, new Color(0x0FFFFFF, true), new Color(0x0FFFFFF, true), new Color(0xB0FFFFFF, true), new Color(0xB0FFFFFF, true));
        Render2DEngine.draw2DGradientRect(stack, getX() + width / 2f + 20f, getY() + 6, getX() + width - 10, getY() + 7, new Color(0xB0FFFFFF, true), new Color(0xB0FFFFFF, true), new Color(0x0FFFFFF, true), new Color(0x0FFFFFF, true));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if ((getY() > ThunderGui.getInstance().main_posY + ThunderGui.getInstance().height) || getY() < ThunderGui.getInstance().main_posY) {
            return;
        }
        if (hovered) {
            SettingGroup settingGroup = (SettingGroup) setting.getValue();
            settingGroup.setExtended(!settingGroup.isExtended());
        }
    }
}