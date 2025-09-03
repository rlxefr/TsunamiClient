package tsunami.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import tsunami.TsunamiClient;
import tsunami.core.Managers;
import tsunami.core.manager.client.ModuleManager;
import tsunami.events.impl.EventTick;
import tsunami.features.modules.Module;

public class TpsSync extends Module {
    public TpsSync() {
        super("TpsSync", Module.Category.NONE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(EventTick e) {
        if (ModuleManager.timer.isEnabled()) return;
        if (Managers.SERVER.getTPS() > 1)
            TsunamiClient.TICK_TIMER = Managers.SERVER.getTPS() / 20f;
        else TsunamiClient.TICK_TIMER = 1f;
    }

    @Override
    public void onDisable() {
        TsunamiClient.TICK_TIMER = 1f;
    }
}
