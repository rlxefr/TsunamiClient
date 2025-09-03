package tsunami.features.modules.client;

import tsunami.features.modules.Module;
import tsunami.setting.Setting;

public final class Notifications extends Module {
    public Notifications() {
        super("Notifications", Category.CLIENT);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.CrossHair);

    public enum Mode {
        Default, CrossHair, Text
    }
}
