package tsunami.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import tsunami.events.impl.PacketEvent;
import tsunami.injection.accesors.IPlayerInteractBlockC2SPacket;
import tsunami.features.modules.Module;

public class PearlBlockThrow extends Module {
    public PearlBlockThrow() {
        super("PearlBlockThrow", Category.NONE);
    }

    @EventHandler
    public void onPackerSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket p && mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL)
            ((IPlayerInteractBlockC2SPacket) p).setHand(Hand.OFF_HAND);
    }
}
