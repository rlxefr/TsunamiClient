package tsunami.features.modules.player;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import tsunami.core.Managers;
import tsunami.features.modules.Module;
import tsunami.gui.notification.Notification;
import tsunami.setting.Setting;
import tsunami.utility.player.InventoryUtility;
import tsunami.utility.player.SearchInvResult;

import static tsunami.features.modules.client.ClientSettings.isRu;

public class ElytraReplace extends Module {
    public ElytraReplace() {
        super("ElytraReplace", Category.NONE);
    }

    private final Setting<Integer> durability = new Setting<>("Durability", 5, 0, 100);

    @Override
    public void onUpdate() {
        ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if(is.isOf(Items.ELYTRA) && ((100f - ((float) is.getDamage() / (float) is.getMaxDamage()) * 100f) <= durability.getValue())){

            SearchInvResult result = InventoryUtility.findInInventory(stack -> {
                if (stack.getItem() instanceof ElytraItem)
                    return (100f - ((float) stack.getDamage() / (float) stack.getMaxDamage()) * 100f) > durability.getValue();
                return false;
            });

            if (result.found()) {
                clickSlot(result.slot());
                clickSlot(6);
                clickSlot(result.slot());
                sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                Managers.NOTIFICATION.publicity("ElytraReplace", isRu() ? "Меняем элитру на новую!" : "Swapping the old elytra for a new one!",2, Notification.Type.SUCCESS);
                sendMessage(isRu() ? "Меняем элитру на новую!" : "Swapping the old elytra for a new one!");
            }
        }
    }
}
