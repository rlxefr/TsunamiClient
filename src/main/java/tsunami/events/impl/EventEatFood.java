package tsunami.events.impl;

import net.minecraft.item.ItemStack;
import tsunami.events.Event;

public class EventEatFood extends Event {
    private final ItemStack stack;

    public EventEatFood(ItemStack stack){
        this.stack = stack;
    }

    public ItemStack getFood() {
        return stack;
    }
}
