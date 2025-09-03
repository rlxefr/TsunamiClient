package tsunami.features.modules.misc;

import tsunami.core.manager.client.ModuleManager;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;

public class NameProtect extends Module {
    public NameProtect() {
        super("NameProtect", Category.MISC);
    }

    public static Setting<String> newName = new Setting<>("name", "TsunamiClient");
    public static Setting<Boolean> hideFriends = new Setting<>("Hide friends", true);

    public static String getCustomName() {
        return ModuleManager.nameProtect.isEnabled() ? newName.getValue().replaceAll("&", "\u00a7") : mc.getGameProfile().getName();
    }
}