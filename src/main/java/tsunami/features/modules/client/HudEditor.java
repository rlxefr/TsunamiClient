package tsunami.features.modules.client;

import tsunami.gui.hud.HudEditorGui;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;
import tsunami.setting.impl.ColorSetting;
import tsunami.utility.render.Render2DEngine;

import java.awt.*;

public final class HudEditor extends Module {
    public static final Setting<Boolean> sticky = new Setting<>("Sticky", true);
    public static final Setting<HudStyle> hudStyle = new Setting<>("HudStyle", HudStyle.Blurry);
    public static final Setting<ArrowsStyle> arrowsStyle = new Setting<>("ArrowsStyle", ArrowsStyle.Default);
    public static final Setting<ClickGui.colorModeEn> colorMode = new Setting<>("ColorMode", ClickGui.colorModeEn.Static);
    public static final Setting<Integer> colorSpeed = new Setting<>("ColorSpeed", 18, 2, 54);
    public static final Setting<Boolean> glow = new Setting<>("Light", true);

    public static final Setting<ColorSetting> hcolor1 = new Setting<>("Color", new ColorSetting(0xFF144646));
    public static final Setting<ColorSetting> acolor = new Setting<>("Color2", new ColorSetting(0xFF1B5D5D));
    public static final Setting<ColorSetting> plateColor = new Setting<>("PlateColor", new ColorSetting(0xFF227474));
    public static final Setting<ColorSetting> textColor = new Setting<>("TextColor", new ColorSetting(0xFFFFFFFF));
    public static final Setting<ColorSetting> textColor2 = new Setting<>("TextColor2", new ColorSetting(0xFFBDBDBD));
    public static final Setting<ColorSetting> blurColor = new Setting<>("BlurColor", new ColorSetting(0xE6000000));

    public static final Setting<Float> hudRound = new Setting<>("HudRound", 4f, 1f, 7f);
    public static final Setting<Float> alpha = new Setting<>("Alpha", 0.7f, 0f, 1f);
    public static final Setting<Float> blend = new Setting<>("Blend", 8f, 1f, 15f);
    public static final Setting<Float> outline = new Setting<>("Outline", 0.3f, 0f, 2.5f);
    public static final Setting<Float> glow1 = new Setting<>("Glow", 0.15f, 0f, 1f);
    public static final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.95f, 0f, 1f);
    public static final Setting<Float> blurStrength = new Setting<>("BlurStrength", 15f, 5f, 50f);

    public HudEditor() {
        super("HudEditor", Module.Category.CLIENT);
    }

    public static Color getColor(int count) {
        return switch (colorMode.getValue()) {
            case Sky -> Render2DEngine.skyRainbow(colorSpeed.getValue(), count);
            case LightRainbow -> Render2DEngine.rainbow(colorSpeed.getValue(), count, .6f, 1, 1);
            case Rainbow -> Render2DEngine.rainbow(colorSpeed.getValue(), count, 1f, 1, 1);
            case Fade -> Render2DEngine.fade(colorSpeed.getValue(), count, hcolor1.getValue().getColorObject(), 1);
            case DoubleColor ->
                    Render2DEngine.TwoColoreffect(hcolor1.getValue().getColorObject(), acolor.getValue().getColorObject(), colorSpeed.getValue(), count);
            case Analogous ->
                    Render2DEngine.interpolateColorsBackAndForth(colorSpeed.getValue(), count, hcolor1.getValue().getColorObject(), Render2DEngine.getAnalogousColor(acolor.getValue().getColorObject()), true);
            default -> hcolor1.getValue().getColorObject();
        };
    }

    @Override
    public void onEnable() {
        // Debug check
        HudEditorGui hudGui = HudEditorGui.getHudGui();
        if (hudGui != null && mc != null) {
            mc.setScreen(hudGui);
        } else {
            System.out.println("HudEditor: GUI or MC is null!");
        }
        disable();
    }

    public enum ArrowsStyle {
        Default, New
    }

    public enum HudStyle {
        Blurry, Glowing
    }
}