package tsunami.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import tsunami.core.Managers;
import tsunami.events.impl.EventSync;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;
import tsunami.utility.FakeInvScreen;
import tsunami.utility.player.InventoryUtility;
import tsunami.utility.player.SearchInvResult;

import java.util.function.Predicate;

public final class AutoTotem extends Module {
    
    // Mode setting
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Normal);
    
    // Normal mode settings
    private final Setting<Boolean> autoOpen = new Setting<>("Auto Open", true, v -> mode.getValue() == Mode.Normal);
    private final Setting<Integer> delay = new Setting<>("Delay", 5, 0, 20, v -> mode.getValue() == Mode.Normal);
    private final Setting<Boolean> useHotbar = new Setting<>("Use Hotbar", true, v -> mode.getValue() == Mode.Normal);
    
    // Auto mode settings
    private final Setting<Integer> autoDelay = new Setting<>("Delay", 50, 1, 100, v -> mode.getValue() == Mode.Auto);
    
    // Inventory mode settings
    private final Setting<Integer> invDelay = new Setting<>("Delay", 0, 0, 20, v -> mode.getValue() == Mode.Inventory);
    private final Setting<Boolean> hotbar = new Setting<>("Hotbar", true, v -> mode.getValue() == Mode.Inventory);
    private final Setting<Integer> totemSlot = new Setting<>("Totem Slot", 1, 1, 9, v -> mode.getValue() == Mode.Inventory);
    private final Setting<Boolean> autoSwitch = new Setting<>("Auto Switch", false, v -> mode.getValue() == Mode.Inventory);
    private final Setting<Boolean> forceTotem = new Setting<>("Force Totem", false, v -> mode.getValue() == Mode.Inventory);
    private final Setting<Boolean> invAutoOpen = new Setting<>("Auto Open", false, v -> mode.getValue() == Mode.Inventory);
    private final Setting<Integer> stayOpenDuration = new Setting<>("Stay Open For", 0, 0, 20, v -> mode.getValue() == Mode.Inventory);

    // Normal mode variables
    private int currentDelay;
    private boolean inventoryOpen;

    // Auto mode variables
    private int autoCurrentDelay;

    // Inventory mode variables
    private int delayCounter = -1;
    private int stayOpenCounter = -1;

    public enum Mode {
        Normal, Auto, Inventory
    }

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        this.delayCounter = -1;
        this.stayOpenCounter = -1;
        super.onEnable();
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.getValue() == Mode.Normal) {
            handleNormalMode();
        } else if (mode.getValue() == Mode.Auto) {
            handleAutoMode();
        } else if (mode.getValue() == Mode.Inventory) {
            handleInventoryMode();
        }
    }

    private void handleNormalMode() {
        if (currentDelay > 0) {
            currentDelay--;
            return;
        }

        // Already holding totem
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            if (inventoryOpen && autoOpen.getValue()) {
                mc.player.closeHandledScreen();
                inventoryOpen = false;
            }
            return;
        }

        // Only work when inventory GUI is open (if auto open is disabled) or when auto open is enabled
        if (!autoOpen.getValue() && !(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }

        // If auto open is disabled and we're in a different GUI, don't work
        if (!autoOpen.getValue() && mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }

        // Find a totem in inventory (excluding or including hotbar based on setting)
        SearchInvResult result = findTotemInInventory();
        if (!result.found()) return;

        int slot = result.slot();
        swapToOffhand(slot);

        currentDelay = delay.getValue();
    }

    private void handleAutoMode() {
        if (autoCurrentDelay > 0) {
            autoCurrentDelay--;
            return;
        }

        // Already holding totem
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            return;
        }

        // Find a totem in inventory
        SearchInvResult result = findTotemInInventory();
        if (!result.found()) return;

        int slot = result.slot();
        swapToOffhandDirect(slot);

        autoCurrentDelay = autoDelay.getValue();
    }

    private void handleInventoryMode() {
        if (this.shouldOpenInventory() && this.invAutoOpen.getValue()) {
            this.mc.setScreen(new FakeInvScreen(this.mc.player));
        }
        
        if (!(this.mc.currentScreen instanceof InventoryScreen) && !(this.mc.currentScreen instanceof FakeInvScreen)) {
            this.delayCounter = -1;
            this.stayOpenCounter = -1;
            return;
        }
        
        if (this.delayCounter == -1) {
            this.delayCounter = this.invDelay.getValue();
        }
        if (this.stayOpenCounter == -1) {
            this.stayOpenCounter = this.stayOpenDuration.getValue();
        }
        if (this.delayCounter > 0) {
            --this.delayCounter;
        }
        
        final PlayerInventory getInventory = this.mc.player.getInventory();
        
        if (this.delayCounter <= 0) {
            // Check if offhand needs totem
            if (getInventory.offHand.get(0).getItem() != Items.TOTEM_OF_UNDYING) {
                final int l = this.findTotemSlot();
                if (l != -1) {
                    // Convert slot to proper inventory screen index
                    int screenSlot = l < 9 ? l + 36 : l;
                    this.mc.interactionManager.clickSlot(((InventoryScreen) this.mc.currentScreen).getScreenHandler().syncId, screenSlot, 40, SlotActionType.SWAP, this.mc.player);
                    this.delayCounter = this.invDelay.getValue(); // Reset delay
                    return;
                }
            }
            
            // Check if hotbar needs totem (Auto Switch places totem in hotbar slot)
            if (this.hotbar.getValue() && this.autoSwitch.getValue()) {
                final ItemStack hotbarStack = this.mc.player.getInventory().getStack(this.totemSlot.getValue() - 1);
                if (hotbarStack.isEmpty() || (this.forceTotem.getValue() && hotbarStack.getItem() != Items.TOTEM_OF_UNDYING)) {
                    final int i = this.findTotemSlot();
                    if (i != -1) {
                        // Convert slot to proper inventory screen index
                        int screenSlot = i < 9 ? i + 36 : i;
                        int hotbarSlot = this.totemSlot.getValue() - 1 + 36; // Convert totem slot to screen index
                        this.mc.interactionManager.clickSlot(((InventoryScreen) this.mc.currentScreen).getScreenHandler().syncId, screenSlot, hotbarSlot, SlotActionType.SWAP, this.mc.player);
                        this.delayCounter = this.invDelay.getValue(); // Reset delay
                        return;
                    }
                }
            }
            
            // Close inventory if totem is equipped and auto-open is enabled
            if (this.isTotemEquipped() && this.invAutoOpen.getValue()) {
                if (this.stayOpenCounter != 0) {
                    --this.stayOpenCounter;
                    return;
                }
                if (this.mc.currentScreen instanceof InventoryScreen || this.mc.currentScreen instanceof FakeInvScreen) {
                    this.mc.currentScreen.close();
                }
                this.stayOpenCounter = this.stayOpenDuration.getValue();
            }
        }
    }

    private SearchInvResult findTotemInInventory() {
        if (mc.player == null) return SearchInvResult.notFound();
        
        if (useHotbar.getValue()) {
            // Search in entire inventory including hotbar (slots 0-35)
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != null && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return new SearchInvResult(i, true, stack);
                }
            }
        } else {
            // Search only in main inventory (slots 9-35, excluding hotbar slots 0-8)
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != null && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return new SearchInvResult(i, true, stack);
                }
            }
        }
        
        return SearchInvResult.notFound();
    }

    private void swapToOffhand(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        
        // Only open inventory automatically if auto open is enabled
        if (autoOpen.getValue() && !inventoryOpen) {
            mc.setScreen(new InventoryScreen(mc.player));
            inventoryOpen = true;
        }

        // If auto open is disabled, only work if inventory is already open
        if (!autoOpen.getValue() && !(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }

        Managers.ASYNC.run(() -> {
            if (mc.player == null || mc.interactionManager == null) return;

            // Make sure we're still in inventory screen
            if (!(mc.currentScreen instanceof InventoryScreen)) return;

            // Convert slot to proper inventory slot index
            int inventorySlot = slot < 9 ? slot + 36 : slot;

            // click totem slot
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    inventorySlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

            // click offhand slot (usually index 45)
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    45,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

            // place back cursor (if holding something else)
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    inventorySlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

        }, 100); // wait for screen to load
    }

    private void swapToOffhandDirect(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        
        // Convert slot to proper inventory slot index for direct interaction
        int inventorySlot = slot < 9 ? slot + 36 : slot;
        
        // Direct swap to offhand without opening inventory
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                inventorySlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                45, // offhand slot
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                inventorySlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
    }

    // Inventory mode methods
    public boolean isTotemEquipped() {
        // Only check offhand for closing inventory - hotbar is optional
        return this.mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
    }

    public boolean shouldOpenInventory() {
        if (this.hotbar.getValue()) {
            return (this.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || this.mc.player.getInventory().getStack(this.totemSlot.getValue() - 1).getItem() != Items.TOTEM_OF_UNDYING) && !(this.mc.currentScreen instanceof FakeInvScreen) && this.countTotems(item -> item == Items.TOTEM_OF_UNDYING) != 0;
        }
        return this.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && !(this.mc.currentScreen instanceof FakeInvScreen) && this.countTotems(item2 -> item2 == Items.TOTEM_OF_UNDYING) != 0;
    }

    private int findTotemSlot() {
        if (mc.player == null) return -1;
        
        final PlayerInventory inventory = this.mc.player.getInventory();
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (stack != null && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }

    private int countTotems(final Predicate<Item> predicate) {
        if (mc.player == null) return 0;
        
        int count = 0;
        final PlayerInventory inventory = this.mc.player.getInventory();
        for (int i = 0; i < inventory.main.size(); i++) {
            final ItemStack stack = inventory.main.get(i);
            if (stack != null && predicate.test(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }
}