package tsunami.features.modules.combat;

import tsunami.features.modules.Module;
import tsunami.setting.Setting;

import static tsunami.core.manager.client.ServerManager.round2;

public final class HitBox extends Module {
    public HitBox() {
        super("HitBoxes", Category.NONE);
    }

    public static final Setting<Float> XZExpand = new Setting<>("XZExpand", 1.0f, 0.0f, 5.0f);
    public static final Setting<Float> YExpand = new Setting<>("YExpand", 0.0f, 0.0f, 5.0f);
    public static final Setting<Boolean> affectToAura = new Setting<>("AffectToAura", false);

    @Override
    public String getDisplayInfo() {
        return "H: " + round2(XZExpand.getValue()) + " V: " + round2(YExpand.getValue());
    }
}
