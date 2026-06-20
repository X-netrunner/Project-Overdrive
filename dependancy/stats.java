package dependancy;

import java.util.Scanner;

public class stats {

    String name = login.name;

    public stats() {
        // Load stats if not loaded
        if (editor.stats[0] == null) {
            String bin[] = { "0", "0" };
            new editor(bin, false, false);
        }
        show();
    }

    public static void addExp(int amount) {
        int exp = Integer.parseInt(editor.stats[5]);
        int level = Integer.parseInt(editor.stats[0]);
        int points = Integer.parseInt(editor.stats[6]);
        
        exp += amount;
        System.out.println("Gained " + amount + " EXP!");
        
        while (exp >= 100) {
            exp -= 100;
            level++;
            points += 5;
            System.out.println("LEVEL UP! You are now level " + level + ". Gained 5 stat points!");
        }
        
        editor.stats[5] = String.valueOf(exp);
        editor.stats[0] = String.valueOf(level);
        editor.stats[6] = String.valueOf(points);
        
        new editor(null, true, false); // Save
    }

    public static void show() {
        System.out.println("\n--- PLAYER STATS ---");
        System.out.println("Level: " + editor.stats[0]);
        System.out.println("EXP: " + editor.stats[5] + "/100");
        System.out.println("Stat Points Available: " + editor.stats[6]);
        System.out.println("Base Attack: " + editor.stats[1] + " (Weapon: +" + editor.stats[11] + ")");
        System.out.println("Base Defense: " + editor.stats[2] + " (Armor: +" + editor.stats[12] + ")");
        System.out.println("Luck: " + editor.stats[3]);
        System.out.println("Health: " + editor.stats[4]);
        System.out.println("\n--- ROBOT PARTS ---");
        System.out.println("Head: " + getHeadName());
        System.out.println("Hand: " + getHandName());
        System.out.println("Leg:  " + getLegName());
        System.out.println("--------------------\n");
    }

    public static void spendPoints() {
        Scanner sc = new Scanner(System.in);
        int points = Integer.parseInt(editor.stats[6]);
        
        System.out.println("\n--- UPGRADE INTERFACE ---");
        System.out.println("Available Points: " + points);
        System.out.println("1. Core Stats (Atk, Def, Luck, HP)");
        System.out.println("2. Robot Customization (Head, Hand, Leg)");
        System.out.println("3. Back");
        System.out.print("SELECT: ");
        int choice = 0; try { choice = sc.nextInt(); } catch (Exception e) { sc.next(); return; }

        if (choice == 1) upgradeCoreStats(sc);
        else if (choice == 2) upgradeRobotParts(sc);
    }

    private static void upgradeCoreStats(Scanner sc) {
        int points = Integer.parseInt(editor.stats[6]);
        if (points <= 0) {
            System.out.println("No points to spend.");
            return;
        }

        System.out.println("1. Attack (+1)\n2. Defense (+1)\n3. Luck (+1)\n4. Health (+10)\n5. Cancel");
        int choice = sc.nextInt();
        if (choice >= 1 && choice <= 4) {
            points--;
            editor.stats[6] = String.valueOf(points);
            if (choice == 1) editor.stats[1] = String.valueOf(Integer.parseInt(editor.stats[1]) + 1);
            if (choice == 2) editor.stats[2] = String.valueOf(Integer.parseInt(editor.stats[2]) + 1);
            if (choice == 3) editor.stats[3] = String.valueOf(Integer.parseInt(editor.stats[3]) + 1);
            if (choice == 4) editor.stats[4] = String.valueOf(Integer.parseInt(editor.stats[4]) + 10);
            
            new editor(null, true, false); // Save
            System.out.println("Core upgrade successful!");
        }
    }

    private static void upgradeRobotParts(Scanner sc) {
        while (true) {
            int points = Integer.parseInt(editor.stats[6]);
            System.out.println("\n--- ROBOT CUSTOMIZATION (Points: " + points + ") ---");
            System.out.println("1. Head: " + getHeadName());
            System.out.println("2. Hand: " + getHandName());
            System.out.println("3. Leg:  " + getLegName());
            System.out.println("4. Back");
            System.out.print("SELECT PART TO UPGRADE: ");
            int part = 0; try { part = sc.nextInt(); } catch (Exception e) { sc.next(); continue; }
            if (part == 4) break;

            if (part == 1) {
                System.out.println("HEAD UPGRADES (Cost: 5 if new):");
                System.out.println("1. Radar (Reveal Enemy Signals)");
                System.out.println("2. Pathfinder (Highlight Core Exit)");
                System.out.println("3. Map Decompressor (Reveal Sector Data)");
                int sub = sc.nextInt();
                if (sub >= 1 && sub <= 3) {
                    if (editor.stats[25].equals(String.valueOf(sub))) {
                        System.out.println("This module is already synchronized.");
                        continue;
                    }
                    
                    int unlocked = Integer.parseInt(editor.stats[30]);
                    boolean isUnlocked = (unlocked & (1 << (sub - 1))) != 0;
                    
                    if (!isUnlocked && points < 5) {
                        System.out.println("Insufficient points to unlock this module.");
                        continue;
                    }
                    
                    if (!isUnlocked) {
                        deductPoints(5);
                        editor.stats[30] = String.valueOf(unlocked | (1 << (sub - 1)));
                    }
                    editor.stats[25] = String.valueOf(sub);
                    System.out.println("Head module synchronized.");
                }
            } else if (part == 2) {
                System.out.println("HAND UPGRADES (Cost: 5 if new, Reflector: 8):");
                System.out.println("1. Power Claws (+5 Attack)");
                System.out.println("2. Precision Grip (+5 Luck)");
                System.out.println("3. EMP Pulse (Combat Stun)");
                System.out.println("4. Reflector Arm (Parry/Reflect Projectiles)");
                int sub = sc.nextInt();
                if (sub >= 1 && sub <= 4) {
                    if (editor.stats[26].equals(String.valueOf(sub))) {
                        System.out.println("This hardware is already installed.");
                        continue;
                    }
                    
                    int unlocked = Integer.parseInt(editor.stats[31]);
                    boolean isUnlocked = (unlocked & (1 << (sub - 1))) != 0;
                    int cost = (sub == 4) ? 8 : 5;
                    
                    if (!isUnlocked && points < cost) {
                        System.out.println("Insufficient points to unlock this hardware.");
                        continue;
                    }
                    
                    if (!isUnlocked) {
                        deductPoints(cost);
                        editor.stats[31] = String.valueOf(unlocked | (1 << (sub - 1)));
                        if (sub == 1) editor.stats[1] = String.valueOf(Integer.parseInt(editor.stats[1]) + 5);
                        else if (sub == 2) editor.stats[3] = String.valueOf(Integer.parseInt(editor.stats[3]) + 5);
                    }
                    editor.stats[26] = String.valueOf(sub);
                    System.out.println("Hand hardware synchronized.");
                }
            } else if (part == 3) {
                System.out.println("LEG UPGRADES (Cost: 5 if new):");
                System.out.println("1. Titanium Plating (+5 Defense)");
                System.out.println("2. Hydraulic Dampers (+50 HP)");
                System.out.println("3. Phase Shift (Short-Range Blink)");
                int sub = sc.nextInt();
                if (sub >= 1 && sub <= 3) {
                    if (editor.stats[27].equals(String.valueOf(sub))) {
                        System.out.println("This hardware is already installed.");
                        continue;
                    }
                    
                    int unlocked = Integer.parseInt(editor.stats[32]);
                    boolean isUnlocked = (unlocked & (1 << (sub - 1))) != 0;
                    
                    if (!isUnlocked && points < 5) {
                        System.out.println("Insufficient points to unlock this hardware.");
                        continue;
                    }
                    
                    if (!isUnlocked) {
                        deductPoints(5);
                        editor.stats[32] = String.valueOf(unlocked | (1 << (sub - 1)));
                        if (sub == 1) editor.stats[2] = String.valueOf(Integer.parseInt(editor.stats[2]) + 5);
                        else if (sub == 2) editor.stats[4] = String.valueOf(Integer.parseInt(editor.stats[4]) + 50);
                    }
                    editor.stats[27] = String.valueOf(sub);
                    System.out.println("Leg hardware synchronized.");
                }
            }
            new editor(null, true, false);
        }
    }

    private static void deductPoints(int p) {
        int points = Integer.parseInt(editor.stats[6]);
        editor.stats[6] = String.valueOf(points - p);
    }

    public static String getHeadName() {
        String h = editor.stats[25];
        if (h.equals("1")) return "Radar Module";
        if (h.equals("2")) return "Pathfinder Module";
        if (h.equals("3")) return "Map Decompressor";
        return "Stock Sensors";
    }

    public static String getHandName() {
        String h = editor.stats[26];
        if (h.equals("1")) return "Power Claws";
        if (h.equals("2")) return "Precision Grip";
        if (h.equals("3")) return "EMP Pulse Arm";
        if (h.equals("4")) return "Reflector Arm";
        return "Standard Manipulators";
    }

    public static String getLegName() {
        String l = editor.stats[27];
        if (l.equals("1")) return "Titanium Plating";
        if (l.equals("2")) return "Hydraulic Dampers";
        if (l.equals("3")) return "Phase Shift Actuators";
        return "Basic Actuators";
    }
}
