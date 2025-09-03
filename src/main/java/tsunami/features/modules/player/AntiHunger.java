package tsunami.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.jetbrains.annotations.NotNull;
import tsunami.events.impl.PacketEvent;
import tsunami.injection.accesors.IPlayerMoveC2SPacket;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;

public class AntiHunger extends Module {
    public AntiHunger() {
        super("AntiHunger", Category.NONE);
    }

    private final Setting<Boolean> ground = new Setting<>("CancelGround", true);
    private final Setting<Boolean> sprint = new Setting<>("CancelSprint", true);


    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send e) {
        if (e.getPacket() instanceof PlayerMoveC2SPacket pac && ground.getValue())
            ((IPlayerMoveC2SPacket) pac).setOnGround(false);

        if (e.getPacket() instanceof ClientCommandC2SPacket pac && sprint.getValue())
            if (pac.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING)
                e.cancel();
    }
}
