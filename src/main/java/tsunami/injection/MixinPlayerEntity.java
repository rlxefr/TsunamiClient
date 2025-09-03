package tsunami.injection;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tsunami.TsunamiClient;
import tsunami.core.manager.client.ModuleManager;
import tsunami.events.impl.EventAttack;
import tsunami.events.impl.EventEatFood;
import tsunami.events.impl.EventPlayerJump;
import tsunami.events.impl.EventPlayerTravel;
import tsunami.features.modules.client.Media;
import tsunami.features.modules.combat.Aura;
import tsunami.features.modules.movement.AutoSprint;
import tsunami.features.modules.movement.Speed;

import static tsunami.features.modules.Module.mc;

@Mixin(value = PlayerEntity.class, priority = 800)
public class MixinPlayerEntity {
    @Inject(method = "getAttackCooldownProgressPerTick", at = @At("HEAD"), cancellable = true)
    public void getAttackCooldownProgressPerTickHook(CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.aura.isEnabled() && ModuleManager.aura.switchMode.getValue() == Aura.Switch.Silent) {
            cir.setReturnValue(12.5f);
        }
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    public void getDisplayNameHook(CallbackInfoReturnable<Text> cir) {
        if (ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) {
            cir.setReturnValue(Text.of("Protected"));
        }
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = At.Shift.AFTER))
    public void attackAHook(CallbackInfo callbackInfo) {
        if (ModuleManager.autoSprint.isEnabled() && AutoSprint.sprint.getValue()) {
            final float multiplier = 0.6f + 0.4f * AutoSprint.motion.getValue();
            mc.player.setVelocity(mc.player.getVelocity().x / 0.6 * multiplier, mc.player.getVelocity().y, mc.player.getVelocity().z / 0.6 * multiplier);
            mc.player.setSprinting(true);
        }
    }

    @Inject(method = "getMovementSpeed", at = @At("HEAD"), cancellable = true)
    public void getMovementSpeedHook(CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.speed.isEnabled() && ModuleManager.speed.mode.is(Speed.Mode.Vanilla)) {
            cir.setReturnValue(ModuleManager.speed.boostFactor.getValue());
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attackAHook2(Entity target, CallbackInfo ci) {
        final EventAttack event = new EventAttack(target, false);
        TsunamiClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelhookPre(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null)
            return;

        final EventPlayerTravel event = new EventPlayerTravel(movementInput, true);
        TsunamiClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            mc.player.move(MovementType.SELF, mc.player.getVelocity());
            ci.cancel();
        }
    }


    @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
    private void onTravelhookPost(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null)
            return;
        final EventPlayerTravel event = new EventPlayerTravel(movementInput, false);
        TsunamiClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            mc.player.move(MovementType.SELF, mc.player.getVelocity());
            ci.cancel();
        }
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void onJumpPre(CallbackInfo ci) {
        TsunamiClient.EVENT_BUS.post(new EventPlayerJump(true));
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void onJumpPost(CallbackInfo ci) {
        TsunamiClient.EVENT_BUS.post(new EventPlayerJump(false));
    }

    @Inject(method = "eatFood", at = @At("RETURN"))
    public void eatFoodHook(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        TsunamiClient.EVENT_BUS.post(new EventEatFood(cir.getReturnValue()));
    }

    @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
    protected void shouldDismountHook(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.boatFly.isEnabled() && ModuleManager.boatFly.allowShift.getValue())
            cir.setReturnValue(false);
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
        if (ModuleManager.reach.isEnabled()) {
            if (ModuleManager.reach.Creative.getValue() && mc.player.isCreative()) {
                cir.setReturnValue((double) ModuleManager.reach.creativeBlocksRange.getValue());
            }
            else {
                cir.setReturnValue((double) ModuleManager.reach.blocksRange.getValue());
            }
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
        if (ModuleManager.reach.isEnabled()) {
            if (ModuleManager.reach.Creative.getValue() && mc.player.isCreative()) {
                cir.setReturnValue((double) ModuleManager.reach.creativeEntityRange.getValue());
            }
            else {
                cir.setReturnValue((double) ModuleManager.reach.entityRange.getValue());
            }
        }
    }
}
