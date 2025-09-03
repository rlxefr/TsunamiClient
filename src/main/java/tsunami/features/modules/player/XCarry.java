package tsunami.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import tsunami.events.impl.PacketEvent;
import tsunami.features.modules.Module;

public class XCarry extends Module {
    public XCarry() {
        super("XCarry", Category.NONE);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof CloseHandledScreenC2SPacket) e.cancel();
    }
}
