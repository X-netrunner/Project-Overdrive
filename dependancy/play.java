package dependancy;

import dependancy.map;
import java.util.*;
import java.io.*;

public class play {

    public static double playerVector[] = { 1, 1, 0 }; 
    private int currentHealth;
    private int maxHealth;

    private void check() {
        int floorIdx = map.currentFloor - 1;

        // Check Mobs
        for (int j = 0; j < map.rooms[floorIdx]; j++) {
            if (
                (int) playerVector[0] == map.roomLocation[0][floorIdx][j][0] &&
                (int) playerVector[1] == map.roomLocation[0][floorIdx][j][1]
            ) {
                Logger.combat("Signal conflict at [" + (int) playerVector[0] + "," + (int) playerVector[1] + "]!");
                if (j == mobs.keyMobs[floorIdx] - 1) {
                    Logger.important("Key signal detected in construct unit.");
                }
                stats.addExp(25);
                map.roomLocation[0][floorIdx][j][0] = -1;
            }
        }

        // Check Chests
        for (int j = 0; j < map.chestCount[floorIdx]; j++) {
            if (
                (int) playerVector[0] == map.roomLocation[1][floorIdx][j][0] &&
                (int) playerVector[1] == map.roomLocation[1][floorIdx][j][1]
            ) {
                Logger.log("Crate found at [" + (int) playerVector[0] + "," + (int) playerVector[1] + "].");
                new chest().open();
                map.roomLocation[1][floorIdx][j][0] = -1;
            }
        }

        // Check Traps
        for (int j = 0; j < map.trapCount[floorIdx]; j++) {
            if (
                (int) playerVector[0] == map.roomLocation[2][floorIdx][j][0] &&
                (int) playerVector[1] == map.roomLocation[2][floorIdx][j][1]
            ) {
                map.roomLocation[2][floorIdx][j][0] = -1;
                triggerCLITrap();
            }
        }
        
        saveSession(false);
    }

    private void triggerCLITrap() {
        int type = (int)(Math.random() * 2);
        if (type == 0) {
            int dmg = Math.max(5, maxHealth / 5);
            currentHealth = Math.max(0, currentHealth - dmg);
            Logger.combat("DANGER: MINE DETONATED! Structural integrity compromised (-" + dmg + " HP).");
            System.out.println("CURRENT INTEGRITY: " + currentHealth + "/" + maxHealth);
            if (currentHealth <= 0) {
                Logger.combat("CRITICAL FAILURE: OVERDRIVE UNIT DESTROYED.");
                System.exit(0);
            }
        } else {
            int exp = Integer.parseInt(editor.stats[5]);
            int loss = Math.min(exp, 15);
            editor.stats[5] = String.valueOf(exp - loss);
            Logger.combat("WARNING: DATA CORRUPTION! Memory leak detected (-" + loss + " EXP).");
        }
    }

    public static void saveSession(boolean verbose) {
        if (editor.stats[0] == null) return;
        
        editor.stats[7] = String.valueOf(map.seed);
        editor.stats[8] = String.valueOf(map.currentFloor);
        editor.stats[9] = String.valueOf((int)playerVector[0]);
        editor.stats[10] = String.valueOf((int)playerVector[1]);
        editor.stats[28] = map.keyFound ? "1" : "0";
        new editor(null, true, false);
        saveDeadMobs();
        if (verbose) Logger.log("Core state successfully backed up to encrypted storage.");
    }

    private static void saveDeadMobs() {
        String file = "dependancy/mobs_state.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (int i = 0; i < map.floors; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(i).append(":");
                boolean first = true;
                for (int j = 0; j < map.rooms[i]; j++) {
                    if (map.roomLocation[0][i][j][0] == -1) {
                        if (!first) sb.append(",");
                        sb.append(j);
                        first = false;
                    }
                }
                if (!first) pw.println(sb.toString());
            }
        } catch (IOException e) {}
    }

    private static void loadDeadMobs() {
        String file = "dependancy/mobs_state.csv";
        java.io.File f = new java.io.File(file);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 2) continue;
                int floor = Integer.parseInt(parts[0]);
                String[] deadIndices = parts[1].split(",");
                for (String idx : deadIndices) {
                    int j = Integer.parseInt(idx);
                    if (floor < map.floors && j < map.rooms[floor]) {
                        map.roomLocation[0][floor][j][0] = -1;
                    }
                }
            }
        } catch (Exception e) {}
    }

    public static void clearDeadMobs() {
        String file = "dependancy/mobs_state.csv";
        new java.io.File(file).delete();
    }

    private void loadSession() {
        if (editor.stats[0] == null) {
            new editor(null, false, false);
        }
        
        try {
            maxHealth = Integer.parseInt(editor.stats[4]);
            currentHealth = maxHealth;
        } catch (Exception e) {
            maxHealth = 100;
            currentHealth = 100;
        }

        long seed = 0;
        try {
            if (editor.stats[7] != null && !editor.stats[7].trim().equals("0")) {
                seed = Long.parseLong(editor.stats[7].trim());
            }
        } catch (Exception e) {}

        if (seed != 0) {
            Logger.log("Restoring sector grid from backup...");
            new map(seed);
            loadDeadMobs();
            try {
                int savedFloor = Integer.parseInt(editor.stats[8].trim());
                int savedX = Integer.parseInt(editor.stats[9].trim());
                int savedY = Integer.parseInt(editor.stats[10].trim());
                map.keyFound = editor.stats[28].trim().equals("1");
                
                if (savedFloor > 0 && savedFloor <= map.floors) {
                    map.currentFloor = savedFloor;
                    playerVector[0] = savedX;
                    playerVector[1] = savedY;
                    Logger.important("RESUMING MISSION: FLOOR " + map.currentFloor + " AT POS [" + savedX + "," + savedY + "]");
                } else {
                    resetSession();
                }
            } catch (Exception e) {
                resetSession();
            }
        } else {
            resetSession();
        }
    }

    private void resetSession() {
        Logger.log("Initializing new sector exploration...");
        new map();
        playerVector[0] = 1;
        playerVector[1] = 1;
        map.currentFloor = 1;
        saveSession(false);
    }

    public play(boolean isGui) {
        loadSession();
    }

    public play() {
        loadSession();

        if (login.isAdmin) {
            map.exportLocations();
        }

        int dim = (int) Math.sqrt(map.currentArea);
        Scanner sc = new Scanner(System.in);
        
        outer: while (true) {
            System.out.println("\n--- SECTOR_0" + map.currentFloor + " ---");
            System.out.println("Position: [" + (int)playerVector[0] + "," + (int)playerVector[1] + "]");
            System.out.println("Head: " + stats.getHeadName());
            System.out.println("Weapon: " + editor.stats[13] + " (" + editor.stats[11] + ")");
            System.out.println("Armor: " + editor.stats[14] + " (" + editor.stats[12] + ")");
            System.out.println("1.Up 2.Down 3.Left 4.Right 5.Inventory 6.Stats 7.Ability 8.Back to Hub");
            System.out.print("COMMAND: ");
            
            int choice = 0;
            try {
                choice = sc.nextInt();
            } catch (Exception e) {
                sc.next();
                continue;
            }

            switch (choice) {
                case 1: if (playerVector[0] < dim - 1) { playerVector[0]++; check(); } break;
                case 2: if (playerVector[0] > 0) { playerVector[0]--; check(); } break;
                case 3: if (playerVector[1] > 0) { playerVector[1]--; check(); } break;
                case 4: if (playerVector[1] < dim - 1) { playerVector[1]++; check(); } break;
                case 5: openInventory(sc); break;
                case 6: stats.show(); break;
                case 7: System.out.println("\n" + useAbility()); break;
                case 8: saveSession(true); break outer;
                default: break;
            }
            
            // Exit logic
            int cf = map.currentFloor;
            if (map.keyFound && (int)playerVector[0] == map.exitPos[cf-1][0] && (int)playerVector[1] == map.exitPos[cf-1][1]) {
                 Logger.important("Elevator reached. Engaging vertical displacement...");
                 map.currentFloor++;
                 map.keyFound = false;
                 playerVector[0] = 1;
                 playerVector[1] = 1;
                 saveSession(false);
            }
        }
    }

    private void openInventory(Scanner sc) {
        System.out.println("\n--- BACKUP BUFFER (INVENTORY) ---");
        System.out.println("1. Swap Weapon: [" + editor.stats[17] + " (" + editor.stats[15] + ")]");
        System.out.println("2. Swap Armor: [" + editor.stats[18] + " (" + editor.stats[16] + ")]");
        System.out.println("3. Back");
        System.out.print("INPUT: ");
        int choice = 0;
        try { choice = sc.nextInt(); } catch (Exception e) { sc.next(); }
        if (choice == 1) item.swapInventory("Weapon");
        else if (choice == 2) item.swapInventory("Armor");
    }

    public static String useAbility() {
        String head = editor.stats[25];
        if (head == null || head.equals("0")) {
            return "[ERROR] No active head module installed.";
        }

        if (head.equals("1")) { // Radar
            int floorIdx = map.currentFloor - 1;
            int px = (int)playerVector[0];
            int py = (int)playerVector[1];
            double minDist = Double.MAX_VALUE;
            boolean found = false;
            
            for (int j = 0; j < map.rooms[floorIdx]; j++) {
                int x = map.roomLocation[0][floorIdx][j][0];
                int y = map.roomLocation[0][floorIdx][j][1];
                if (x != -1) {
                    double dist = Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2));
                    if (dist < minDist) minDist = dist;
                    found = true;
                }
            }
            if (!found) return "RADAR: No hostile signals detected.";
            return String.format("RADAR_SWEEP: Nearest hostile at %.1f units.", minDist);
        } else if (head.equals("2")) { // Pathfinder
            int floorIdx = map.currentFloor - 1;
            int ex = map.exitPos[floorIdx][0];
            int ey = map.exitPos[floorIdx][1];
            String status = map.keyFound ? "OPEN" : "LOCKED";
            return "PATHFINDER: Exit at [" + ex + "," + ey + "] | Status: " + status;
        } else if (head.equals("3")) { // Map Decompressor
            return "MAP_DECOMPRESSOR: Sector layout synchronized to local buffer.";
        }
        
        String hand = editor.stats[26];
        if (hand != null && hand.equals("3")) { // EMP Pulse
            return "EMP_PULSE: High-frequency burst emitted. Hostiles within 4 units disrupted.";
        }

        String leg = editor.stats[27];
        if (leg != null && leg.equals("3")) { // Phase Shift
            return "PHASE_SHIFT: Dimensional anchor released. Ready for blink.";
        }

        return "SYSTEM: Module idle.";
    }
}
