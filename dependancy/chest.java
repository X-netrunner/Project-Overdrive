package dependancy;

import dependancy.map;
import dependancy.mobs;
import dependancy.play;

public class chest {

    public static int chestsSpawned;
    public static char chestRarity[];
    private final int rarity[] = config.chestRarity.clone();
    private final int maxChest[] = config.maxChest.clone();
    private int maxChestIndex = 1;

    public chest() {}

    public void open() {
        java.util.Random rand = new java.util.Random();
        int luck = 0;
        try { luck = Integer.parseInt(editor.stats[3]); } catch (Exception e) {}
        
        // Luck reduces the roll value, making low-threshold rarities (Legendary/Epic) easier to hit
        int roll = rand.nextInt(100) - (luck / 2); 
        int rarityLevel = 0;
        if (roll < rarity[3]) rarityLevel = 3;
        else if (roll < rarity[3] + rarity[2]) rarityLevel = 2;
        else if (roll < rarity[3] + rarity[2] + rarity[1]) rarityLevel = 1;

        item i = new item(rarityLevel);
        Logger.log(
            "Opening CONSTRUCT_CRATE... " + i.rarity + " signal detected."
        );
        i.processDiscovery();
    }
}
