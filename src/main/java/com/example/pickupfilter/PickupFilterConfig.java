package com.example.pickupfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PickupFilterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("pickupfilter.json");

    public static volatile boolean enabled = false;
    /** Item ids like "minecraft:gold_nugget" */
    public static final Set<String> allowed = ConcurrentHashMap.newKeySet();

    private static class Data {
        boolean enabled = false;
        List<String> items = new ArrayList<>();
    }

    /** Called by the mixin: should the player be able to pick this item up? */
    public static boolean canPickUp(Item item) {
        if (!enabled) return true;
        return allowed.contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                Data d = GSON.fromJson(Files.readString(PATH), Data.class);
                if (d != null) {
                    enabled = d.enabled;
                    allowed.clear();
                    if (d.items != null) allowed.addAll(d.items);
                }
            }
        } catch (Exception e) {
            System.err.println("[PickupFilter] Could not load config: " + e);
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.enabled = enabled;
            d.items.addAll(allowed);
            Files.writeString(PATH, GSON.toJson(d));
        } catch (IOException e) {
            System.err.println("[PickupFilter] Could not save config: " + e);
        }
    }
}
