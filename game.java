import dependancy.*;
import java.util.*;
import java.io.*;

public class game {

    public static void showStory() {
        System.out.println("\n--- [SYSTEM_LOG: PROJECT_OVERDRIVE] ---");
        System.out.println("YEAR 2142. EARTH IS SILENT.");
        System.out.println("THE ROBOTIC PLAGUE HAS ERADICATED ORGANIC LIFE.");
        System.out.println("DR. NEX LEFT ONE FINAL CONSTRUCT: OVERDRIVE.");
        System.out.println("MISSION: TOTAL DECOMMISSIONING OF ALL HOSTILE UNITS.");
        System.out.println("FINAL PROTOCOL: REACH THE CORE. CHOOSE THE FUTURE.");
        System.out.println("--- [END_LOG] ---\n");
        try { Thread.sleep(2000); } catch (Exception e) {}
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d", "true"); // For Windows compatibility
        System.setProperty("sun.java2d.pmoffscreen", "true");
        
        config.init();
        encrypter en = new encrypter();
        Scanner sc = new Scanner(System.in);
        String[] arr = en.retn("", false);

        new editor(arr, false, true); 
        if (!login.passCheck) {
            new editor(arr, true, true); 
            System.out.println("CORE INITIALIZED...");
            showStory();
            new editor(null, false, false);
        } else {
            System.out.println("AUTHENTICATION SUCCESSFUL.");
            new editor(null, false, false);
        }

        outer: while (true) {
            System.out.println("\n--- OVERDRIVE SYSTEM CONTROL ---");
            System.out.println("1. Diagnostic (Stats)");
            System.out.println("2. Upgrade (Spend Points)");
            System.out.println("3. Initiate Mission (GUI)");
            System.out.println("4. Initiate Mission (CLI)");
            System.out.println("5. Technical Manual (Help)");
            System.out.println("6. Architect Insights (Dev Docs)");
            System.out.println("7. System Settings (Keybinds/Graphics)");
            if (login.isAdmin) System.out.println("8. Admin Override Terminal");
            System.out.println("9. Terminate Session");
            System.out.print("COMMAND: ");
            
            int c = 0; try { c = sc.nextInt(); } catch (Exception e) { sc.next(); continue; }
            switch (c) {
                case 1: stats.show(); break;
                case 2: stats.spendPoints(); break;
                case 3: new play(true); new gui(); return;
                case 4: new play(); break;
                case 5: help.showHelp(); break;
                case 6: 
                    if (login.name.equals("nex") && login.pass.equals("nex")) help.showDevDocs();
                    else System.out.println("ACCESS DENIED: INCORRECT CREDENTIALS.");
                    break;
                case 7: userSettings(sc); break;
                case 8: if (login.isAdmin) adminMenu(sc); break;
                case 9: break outer;
            }
        }
    }

    private static void userSettings(Scanner sc) {
        while (true) {
            System.out.println("\n--- SYSTEM SETTINGS ---");
            System.out.println("1. Change Keybinds");
            System.out.println("2. Toggle Performance Mode (Graphics: " + (config.lowGraphics ? "LOW" : "ULTRA") + ")");
            System.out.println("3. Back");
            System.out.print("SELECT: ");
            int choice = 0; try { choice = sc.nextInt(); } catch (Exception e) { sc.next(); continue; }
            if (choice == 3) break;
            if (choice == 1) keybindMenu(sc);
            else if (choice == 2) {
                config.lowGraphics = !config.lowGraphics;
                config.save();
                System.out.println("Graphics setting updated. (Restart may be required for some effects)");
            }
        }
    }

    private static void adminMenu(Scanner sc) {
        while (true) {
            System.out.println("\n--- ADMIN OVERRIDE TERMINAL ---");
            System.out.println("1. Edit Global Config");
            System.out.println("2. User Management");
            System.out.println("3. Keybind Configuration");
            System.out.println("4. Exit Terminal");
            System.out.print("SELECT: ");
            int c = 0; try { c = sc.nextInt(); } catch (Exception e) { sc.next(); continue; }
            if (c == 4) break;
            if (c == 1) configMenu(sc);
            else if (c == 2) userManagement(sc);
            else if (c == 3) keybindMenu(sc);
        }
    }

    private static void keybindMenu(Scanner sc) {
        System.out.println("\n--- KEYBIND CONFIGURATION ---");
        System.out.println("Current Binds (KeyCode):");
        System.out.println("1. Head Ability: " + config.keyHead + " (Default: 82/R)");
        System.out.println("2. Hand Ability: " + config.keyHand + " (Default: 70/F)");
        System.out.println("3. Leg Ability: " + config.keyLeg + " (Default: 71/G)");
        System.out.print("SELECT TO CHANGE (0 to back): ");
        int choice = sc.nextInt();
        if (choice == 0) return;
        System.out.print("ENTER NEW KEYCODE: ");
        int newKey = sc.nextInt();
        if (choice == 1) config.keyHead = newKey;
        else if (choice == 2) config.keyHand = newKey;
        else if (choice == 3) config.keyLeg = newKey;
        config.save();
        System.out.println("Keybinds updated.");
    }

    private static void configMenu(Scanner sc) {
        System.out.println("\n--- GLOBAL CONFIGURATION ---");
        System.out.println("1. Rarities: " + Arrays.toString(config.chestRarity));
        System.out.println("2. Max Chests: " + Arrays.toString(config.maxChest));
        System.out.println("3. Area Sizes: " + Arrays.toString(config.areaSizes));
        System.out.println("4. Mob Rate: " + config.equiSpawn);
        System.out.println("5. Floor Range: [" + config.baseFloorMin + "-" + config.baseFloorMax + "]");
        System.out.println("6. Base Mob Count: " + config.baseMobCount);
        System.out.print("EDIT INDEX (0 to back): ");
        int i = sc.nextInt();
        if (i == 1) for (int j=0; j<4; j++) config.chestRarity[j] = sc.nextInt();
        else if (i == 2) for (int j=0; j<3; j++) config.maxChest[j] = sc.nextInt();
        else if (i == 3) for (int j=0; j<3; j++) config.areaSizes[j] = sc.nextInt();
        else if (i == 4) config.equiSpawn = sc.nextDouble();
        else if (i == 5) { config.baseFloorMin = sc.nextInt(); config.baseFloorMax = sc.nextInt(); }
        else if (i == 6) config.baseMobCount = sc.nextInt();
        if (i > 0) { config.save(); System.out.println("Config persisted."); }
    }

    private static void userManagement(Scanner sc) {
        String f = "dependancy/playerData.csv";
        List<String> players = new ArrayList<>();
        editor ed = new editor();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            while ((l = br.readLine()) != null) {
                String n = ed.decrypter(l.split(",")[0].split("\t"))[0].replace("Player : ", "");
                players.add(n);
            }
        } catch (Exception e) {}
        
        System.out.println("\n--- USER DATABASE ---");
        for (int i=0; i<players.size(); i++) System.out.println((i+1) + ". " + players.get(i));
        System.out.print("SELECT INDEX (0 to back): ");
        int u = sc.nextInt();
        if (u > 0 && u <= players.size()) {
            String target = (String)players.get(u-1);
            String old = login.name;
            login.name = target;
            new editor(null, false, false);
            System.out.println("--- MODIFYING: " + target + " ---");
            for (int i=0; i<editor.order.length; i++) System.out.println(i + ". " + editor.order[i] + " [" + editor.stats[i] + "]");
            System.out.print("STAT INDEX: ");
            int s = sc.nextInt();
            if (s >= 0 && s < editor.order.length) {
                System.out.print("NEW VALUE: ");
                editor.stats[s] = sc.next();
                new editor(null, true, false);
                System.out.println("Core updated.");
            }
            login.name = old;
            new editor(null, false, false);
        }
    }
}
