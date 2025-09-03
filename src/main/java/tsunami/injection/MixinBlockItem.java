package tsunami.injection;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tsunami.TsunamiClient;
import tsunami.events.impl.EventPlaceBlock;
import tsunami.features.modules.Module;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"))
    private void onPlace(@NotNull ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> info) {
        if(Module.fullNullCheck()) return;
        if (context.getWorld().isClient)
            TsunamiClient.EVENT_BUS.post(new EventPlaceBlock(context.getBlockPos(), state.getBlock()));
    }
}
