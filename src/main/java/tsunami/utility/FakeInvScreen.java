package tsunami.utility;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class FakeInvScreen extends InventoryScreen {
    
    public FakeInvScreen(PlayerEntity playerEntity) {
        super(playerEntity);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        // Allow programmatic slot interactions but prevent user clicks
        if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.SWAP) {
            super.onMouseClick(slot, slotId, button, actionType);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Prevent user mouse clicks but allow programmatic interactions
        return false;
    }
}
