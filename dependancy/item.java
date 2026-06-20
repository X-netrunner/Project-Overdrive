package dependancy;

import java.util.Random;
import java.util.Scanner;

public class item {
    public String name;
    public String type; 
    public int stat;    
    public String rarity;

    public item(int rarityLevel) {
        Random rand = new Random();
        String rarities[] = {"Common", "Rare", "Epic", "Legendary"};
        this.rarity = rarities[rarityLevel];
        
        if (rand.nextBoolean()) {
            this.type = "Weapon";
            this.name = this.rarity + " Blade";
            this.stat = (rarityLevel + 1) * 10 + rand.nextInt(10 * (rarityLevel + 1));
        } else {
            this.type = "Armor";
            this.name = this.rarity + " Plating";
            this.stat = (rarityLevel + 1) * 5 + rand.nextInt(5 * (rarityLevel + 1));
        }
    }

    public void processDiscovery() {
        Logger.important("CONSTRUCT_UPGRADE detected: " + name + " (" + type + ") [" + stat + "]");
        System.out.println("\n--- NEW ITEM FOUND ---");
        System.out.println("Name: " + name);
        System.out.println("Type: " + type);
        System.out.println("Stat: " + stat);
        System.out.println("1. Equip Immediately (Replaces current)");
        System.out.println("2. Store in Backup (Replaces backup)");
        System.out.println("3. Swap and Equip (Current -> Backup, New -> Equipped)");
        System.out.println("4. Decommission (Scrap)");
        
        Scanner sc = new Scanner(System.in);
        int choice = 0;
        try { choice = sc.nextInt(); } catch (Exception e) { sc.next(); }

        if (choice == 1) equip();
        else if (choice == 2) store();
        else if (choice == 3) swapAndEquip();
        else Logger.log("Item decommissioned for raw materials.");
    }
    
    public void swapAndEquip() {
        Logger.log("Reconfiguring buffers: Current item moving to backup...");
        if (type.equals("Weapon")) {
            editor.stats[15] = editor.stats[11]; // Current to Backup Stat
            editor.stats[17] = editor.stats[13]; // Current to Backup Name
            editor.stats[11] = String.valueOf(stat); // New to Current Stat
            editor.stats[13] = name; // New to Current Name
        } else {
            editor.stats[16] = editor.stats[12];
            editor.stats[18] = editor.stats[14];
            editor.stats[12] = String.valueOf(stat);
            editor.stats[14] = name;
        }
        new editor(null, true, false);
        Logger.important("Synchronization complete. New hardware online.");
    }

    public void equip() {
        Logger.log("Synchronizing " + name + " to main systems...");
        if (type.equals("Weapon")) {
            editor.stats[11] = String.valueOf(stat); 
            editor.stats[13] = name;
        } else {
            editor.stats[12] = String.valueOf(stat); 
            editor.stats[14] = name;
        }
        new editor(null, true, false); 
    }

    public void store() {
        Logger.log("Storing " + name + " in backup buffer...");
        if (type.equals("Weapon")) {
            editor.stats[15] = String.valueOf(stat); 
            editor.stats[17] = name;
        } else {
            editor.stats[16] = String.valueOf(stat); 
            editor.stats[18] = name;
        }
        new editor(null, true, false);
    }

    public static void swapInventory(String type) {
        if (type.equals("Weapon")) {
            String curName = editor.stats[13];
            String curStat = editor.stats[11];
            editor.stats[13] = editor.stats[17];
            editor.stats[11] = editor.stats[15];
            editor.stats[17] = curName;
            editor.stats[15] = curStat;
            Logger.log("Weapon systems swapped with backup.");
        } else {
            String curName = editor.stats[14];
            String curStat = editor.stats[12];
            editor.stats[14] = editor.stats[18];
            editor.stats[12] = editor.stats[16];
            editor.stats[18] = curName;
            editor.stats[16] = curStat;
            Logger.log("Armor plating swapped with backup.");
        }
        new editor(null, true, false);
    }
}
