package tsunami.features.modules.player;

import tsunami.injection.accesors.ILivingEntity;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super("NoJumpDelay", Category.NONE);
    }

    private final Setting<Integer> delay = new Setting<>("Delay", 1, 0, 4);

    @Override
    public void onUpdate() {
        if (((ILivingEntity)mc.player).getLastJumpCooldown() > delay.getValue()) {
            ((ILivingEntity)mc.player).setLastJumpCooldown(delay.getValue());
        }
    }
}
