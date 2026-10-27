package com.example.pickupfilter;

import net.fabricmc.api.ModInitializer;

public class PickupFilterMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PickupFilterConfig.load();
    }
}
