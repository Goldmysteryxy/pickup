package com.example.pickupfilter.mixin;

import com.example.pickupfilter.PickupFilterConfig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    // Runs whenever a player touches a dropped item. Cancelling = item stays on the ground.
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void pickupfilter$filter(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!PickupFilterConfig.canPickUp(self.getItem().getItem())) {
            ci.cancel();
        }
    }
}
