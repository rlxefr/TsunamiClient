package tsunami.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import tsunami.core.Managers;
import tsunami.events.impl.TotemPopEvent;
import tsunami.features.modules.Module;
import tsunami.features.modules.combat.AntiBot;
import tsunami.gui.notification.Notification;
import tsunami.setting.Setting;

import static tsunami.features.modules.client.ClientSettings.isRu;

public class TotemPopCounter extends Module {
    public TotemPopCounter() {
        super("TotemPopCounter", Category.MISC);
    }

    public Setting<Boolean> notification = new Setting<>("Notification", true);

    @EventHandler
    public void onTotemPop(@NotNull TotemPopEvent event) {
        if (event.getEntity() == mc.player) return;

        String s;
        if (isRu()) s = Formatting.GREEN + event.getEntity().getName().getString() + Formatting.WHITE + " попнул " + Formatting.AQUA + (event.getPops() > 1 ? event.getPops() + "" + Formatting.WHITE + " тотемов!" : Formatting.WHITE + "тотем!");
        else s = Formatting.GREEN + event.getEntity().getName().getString() + Formatting.WHITE + " popped " + Formatting.AQUA + (event.getPops() > 1 ? event.getPops() + "" + Formatting.WHITE + " totems!" : Formatting.WHITE + " a totem!");

        sendMessage(s);
        if (notification.getValue())
            Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
    }

    @Override
    public void onUpdate() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || AntiBot.bots.contains(player) || player.getHealth() > 0 || !Managers.COMBAT.popList.containsKey(player.getName().getString()))
                continue;

            String s;
            if (isRu()) s = Formatting.GREEN + player.getName().getString() + Formatting.WHITE + " попнул " + (Managers.COMBAT.popList.get(player.getName().getString()) > 1 ? Managers.COMBAT.popList.get(player.getName().getString()) + "" + Formatting.WHITE + " тотемов и сдох!" : Formatting.WHITE + "тотем и сдох!");
            else s = Formatting.GREEN + player.getName().getString() + Formatting.WHITE + " popped " + (Managers.COMBAT.popList.get(player.getName().getString()) > 1 ? Managers.COMBAT.popList.get(player.getName().getString()) + "" + Formatting.WHITE + " totems and died EZ LMAO!" : Formatting.WHITE + "totem and died EZ LMAO!");

            sendMessage(s);
            if (notification.getValue())
                Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
        }
    }
}
