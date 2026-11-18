package dev.endlessexpress.player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Dining Room gift loot table (one roll per departure).
 * Gifts are encoded as "Display name|give-spec;give-spec" so they persist in JSON.
 */
public final class GiftTable {
    private GiftTable() {}

    private static String g(String name, String... specs) { return name + "|" + String.join(";", specs); }

    private static final String UNBREAKING = "minecraft:enchanted_book[minecraft:stored_enchantments={\"minecraft:unbreaking\":2}] 1";
    private static final String PROTECTION = "minecraft:enchanted_book[minecraft:stored_enchantments={\"minecraft:protection\":2}] 1";

    private static final String[][] TIERS = {
        // Common 45%
        { g("4x Cooked Beef", "minecraft:cooked_beef 4"), g("8x Coal", "minecraft:coal 8"), g("6x Iron Ingots", "minecraft:iron_ingot 6") },
        // Uncommon 30%
        { g("Golden Apple", "minecraft:golden_apple 1"),
          g("4x Redstone + 2x Gold Ingots", "minecraft:redstone 4", "minecraft:gold_ingot 2"),
          g("Iron Helmet", "minecraft:iron_helmet 1"), g("Iron Chestplate", "minecraft:iron_chestplate 1"),
          g("Iron Leggings", "minecraft:iron_leggings 1"), g("Iron Boots", "minecraft:iron_boots 1") },
        // Rare 18%
        { g("2x Diamonds", "minecraft:diamond 2"), g("Unbreaking II Book", UNBREAKING),
          g("Protection II Book", PROTECTION), g("2x Blaze Rods", "minecraft:blaze_rod 2") },
        // Festive Jackpot 7%
        { g("Totem of Undying", "minecraft:totem_of_undying 1"),
          g("Enchanted Golden Apple", "minecraft:enchanted_golden_apple 1"),
          g("Diamond Pickaxe", "minecraft:diamond_pickaxe 1") }
    };

    private static String pick(int tier) {
        String[] t = TIERS[tier];
        return t[ThreadLocalRandom.current().nextInt(t.length)];
    }

    public static String roll() {
        int r = ThreadLocalRandom.current().nextInt(100);
        return pick(r < 45 ? 0 : r < 75 ? 1 : r < 93 ? 2 : 3);
    }

    /** Extra roll for Nice List players: Uncommon (60%) or Rare (40%). */
    public static String rollBonus() {
        return pick(ThreadLocalRandom.current().nextInt(100) < 60 ? 1 : 2);
    }

    public static String name(String encoded) { return encoded.substring(0, encoded.indexOf('|')); }
    public static String[] specs(String encoded) { return encoded.substring(encoded.indexOf('|') + 1).split(";"); }
}
