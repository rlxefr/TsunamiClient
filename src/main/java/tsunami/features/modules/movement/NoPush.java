package tsunami.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import tsunami.events.impl.PacketEvent;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;

public class NoPush extends Module {
    public NoPush() {
        super("NoPush", Category.NONE);
    }

    public Setting<Boolean> blocks = new Setting<>("Blocks", true);
    public Setting<Boolean> players = new Setting<>("Players", true);
    public Setting<Boolean> water = new Setting<>("Liquids", true);
    public Setting<Boolean> fishingHook = new Setting<>("FishingHook", true);

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 31 && pac.getEntity(mc.world) instanceof FishingBobberEntity hook && fishingHook.getValue())
            if (hook.getHookedEntity() == mc.player) e.cancel();
    }
}
