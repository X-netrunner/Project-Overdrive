package dependancy;

import java.util.Scanner;

public class help {
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String WHITE = "\u001B[37m";

    private static void printCentered(String text, String color) {
        int width = 80;
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) sb.append(" ");
        System.out.println(color + sb.toString() + text + RESET);
    }

    public static void showHelp() {
        System.out.println("\n" + CYAN + "╔══════════════════════════════════════════════════════════════════════════════╗" + RESET);
        printCentered("SYSTEM_MANUAL v2.60: PROJECT_OVERDRIVE OPERATIONAL GUIDE", WHITE);
        System.out.println(CYAN + "╠══════════════════════════════════════════════════════════════════════════════╣" + RESET);
        
        System.out.println(WHITE + " [ MISSION_PRIORITY ]" + RESET);
        System.out.println("  1. INFILTRATE: Move through sectors using " + YELLOW + "WASD" + RESET + ".");
        System.out.println("  2. ACQUIRE: Defeat the " + BOSS_PURPLE + "BOSS CONSTRUCT" + RESET + " to obtain the Sector Key.");
        System.out.println("  3. EXTRACT: Reach the " + GREEN + "ELEVATOR" + RESET + " to descend deeper into the Core.");
        
        System.out.println("\n" + CYAN + " [ TACTICAL_PROTOCOLS ]" + RESET);
        System.out.println(WHITE + "  DASH" + RESET + "       [SHIFT]: Rapid relocation. Grants brief " + CYAN + "INVULNERABILITY" + RESET + ".");
        System.out.println(WHITE + "  PARRY" + RESET + "      [V]    : Timing is critical. Deflects projectiles back at hostiles.");
        System.out.println(YELLOW + "               (!!!) REQUIRES: 'Reflector Arm' Hand Hardware (Buy in Upgrades)" + RESET);
        System.out.println(WHITE + "  BLINK" + RESET + "      [" + (char)config.keyLeg + "]    : Quantum leap through matter. Cost: " + YELLOW + "30 STAMINA" + RESET + ".");
        System.out.println(YELLOW + "               (!!!) REQUIRES: 'Phase Shift' Leg Hardware (Buy in Upgrades)" + RESET);
        System.out.println(WHITE + "  STRIKE" + RESET + "     [SPACE]: Kinetic discharge. Successive hits build " + YELLOW + "COMBO" + RESET + ".");
        
        System.out.println("\n" + RED + " [ HARDWARE_MODULES (Dynamic Binds) ]" + RESET);
        System.out.println(WHITE + "  HEAD [" + (char)config.keyHead + "]" + RESET + "    : Utility functions (Radar, Pathfinder, Map Analysis).");
        System.out.println(WHITE + "  HAND [" + (char)config.keyHand + "]" + RESET + "    : Combat enhancements (EMP Pulse, Power Claws).");
        System.out.println(WHITE + "  LEGS [" + (char)config.keyLeg + "]" + RESET + "    : Mobility upgrades (Phase Shift, Hydraulic Dampers).");
        
        System.out.println("\n" + YELLOW + " [ STRATEGY_TIPS ]" + RESET);
        System.out.println("  - " + CYAN + "INTEGRITY" + RESET + " is your life. Avoid mines and contact damage at all costs.");
        System.out.println("  - " + CYAN + "COMBO" + RESET + " increases damage output but resets if you take structural damage.");
        System.out.println("  - Use " + CYAN + "RADAR" + RESET + " frequently to avoid being flanked by cloaked snipers.");
        System.out.println("  - Keep an eye on " + CYAN + "STAMINA" + RESET + ". Exhaustion in a swarm is a death sentence.");
        
        System.out.println(CYAN + "╚══════════════════════════════════════════════════════════════════════════════╝" + RESET + "\n");
    }

    private static final String BOSS_PURPLE = "\u001B[35m";

    public static void showDevDocs() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println(CYAN + "\n================================================================================" + RESET);
            printCentered("TECHNICAL DOCUMENTATION: OVERDRIVE CORE ENGINE", WHITE);
            System.out.println(CYAN + "================================================================================" + RESET);
            System.out.println("1. Core & Auth Systems (game.java, login.java, editor.java)");
            System.out.println("2. Generation & World (map.java, mobs.java, chest.java)");
            System.out.println("3. Engine & Logic (play.java, stats.java, item.java)");
            System.out.println("4. Graphical Engine (gui.java, Particles, Projectiles)");
            System.out.println("5. Exit Documentation");
            System.out.print("SELECT CATEGORY: ");
            int c = 0; try { c = sc.nextInt(); } catch (Exception e) { sc.next(); continue; }
            if (c == 5) break;

            switch(c) {
                case 1: showCoreDocs(); break;
                case 2: showWorldDocs(); break;
                case 3: showEngineDocs(); break;
                case 4: showVisualDocs(); break;
            }
        }
    }

    private static void showCoreDocs() {
        System.out.println(YELLOW + "\n--- [ CATEGORY 1: CORE & AUTH ] ---" + RESET);
        System.out.println(WHITE + "[ game.java ]" + RESET);
        System.out.println("  - L22-24: Sets hardware acceleration properties for the graphics engine.");
        System.out.println("  - L29-33: Initial Auth Check. Calls editor(signup=true, write=false).");
        System.out.println("  - L34-37: Success path. If login.passCheck is true, skips intro story.");
        System.out.println("  - L41-65: Main Control Loop. Handles menu switching and admin override.");
        System.out.println(WHITE + "[ login.java ]" + RESET);
        System.out.println("  - L14-17: Captures username/password into static variables.");
        System.out.println("  - L34-37: Null Check. Ensures editor.userN is initialized before checking.");
        System.out.println("  - L39-50: Username Comparison. Checks typed name against user database.");
        System.out.println(WHITE + "[ editor.java ]" + RESET);
        System.out.println("  - L13-47: The Stat Order Array. Defines the 33-slot encrypted save format.");
        System.out.println("  - L61-94: Default Stats. Sets the starting state for new OD units (Lv1, 100HP).");
        System.out.println("  - L101: The 'Player : ' tag. Crucial for identifying credential lines in CSV.");
        System.out.println("  - L201: unencryptData. The brain of the save system. Splits stats from login data.");
        System.out.println("  - L223-231: Key Extraction. Pulls the random double used for that line's math.");
        System.out.println("  - L242: Character Restoration. Multiplies/Divides by key to reveal actual data.");
    }

    private static void showWorldDocs() {
        System.out.println(YELLOW + "\n--- [ CATEGORY 2: GENERATION & WORLD ] ---" + RESET);
        System.out.println(WHITE + "[ map.java ]" + RESET);
        System.out.println("  - L44: Seed initialization. Forces consistent generation for a given seed.");
        System.out.println("  - L45: Depth Calculation. Combines base floor config with random variance.");
        System.out.println("  - L65: Dim optimization. Forces odd-numbered dimensions for perfect maze center.");
        System.out.println("  - L81: 3D Array Initialization. Stores [Floor][X][Y] data for walls/fog.");
        System.out.println("  - L110: Floor Shifter. 30% chance for shifting layouts on non-boss floors.");
        System.out.println("  - L130-145: Elite Scaling. Bosses have 50 + (Floor * 25) HP (Floor 5 = 175 HP).");
        System.out.println("  - L158: The 'Carve' Method. Recursive backtracker that generates the maze.");
        System.out.println(WHITE + "[ mobs.java ]" + RESET);
        System.out.println("  - L30: Boss Logic. Ensures a Boss Construct on every 5th floor (e.g., 5, 10, 15).");
        System.out.println("  - L45: EquiSpawn. Config-driven chance for floor-wide mob population.");
        System.out.println("  - L14: Key Mobs. Randomly assigns the sector key to ONE unit per floor.");
        System.out.println(WHITE + "[ chest.java ]" + RESET);
        System.out.println("  - L15: Luck Integration. (Luck / 2) is subtracted from rarity rolls.");
        System.out.println("  - L19-23: Loot Tables. 5% Epic, 15% Rare, 80% Common weightings.");
    }

    private static void showEngineDocs() {
        System.out.println(YELLOW + "\n--- [ CATEGORY 3: ENGINE & LOGIC ] ---" + RESET);
        System.out.println(WHITE + "[ play.java ]" + RESET);
        System.out.println("  - L12: Collision Detector. Checks if PlayerX/Y matches MobX/Y coordinates.");
        System.out.println("  - L46: Mine Trigger. Logic for calculating damage based on % of max health.");
        System.out.println("  - L63: The Save Buffer. Syncs Seed, Floor, X, Y, and Key status to stats.");
        System.out.println("  - L117: CLI Game Loop. Handles WASD-equivalent numeric movement (1-4).");
        System.out.println("  - L143: Elevator logic. Increments floor depth if keyFound == true.");
        System.out.println("  - L164: Module Manager. Maps stat IDs to active abilities (Radar vs Pathfinder).");
        System.out.println(WHITE + "[ stats.java ]" + RESET);
        System.out.println("  - L16: XP Processor. Handles the Level Up loop and stat point generation.");
        System.out.println("  - L118-130: Bitmask Unlocking. Uses bitwise AND to check if a module was bought.");
        System.out.println(WHITE + "[ item.java ]" + RESET);
        System.out.println("  - L50: Swap Logic. Manages the 2-slot 'Equipped' vs 'Backup' buffer system.");
    }

    private static void showVisualDocs() {
        System.out.println(YELLOW + "\n--- [ CATEGORY 4: GRAPHICAL ENGINE ] ---" + RESET);
        System.out.println(WHITE + "[ gui.java ]" + RESET);
        System.out.println("  - GamePanel (Inner): The main rendering loop using double buffering.");
        System.out.println("  - KeyListener: Handles Dash (Shift), Parry (V), and Blink (G).");
        System.out.println("  - GameState: Enum tracking MENU, PLAYING, DIALOGUE, and DEAD states.");
        System.out.println("  - Camera Logic: Interpolates X/Y to keep player centered in the maze.");
        System.out.println(WHITE + "[ ParticleSystem.java ]" + RESET);
        System.out.println("  - L13: Particle Life. Uses random variance to fade out sparks over time.");
        System.out.println("  - L29: Spawn Loop. Spawns N particles at once for combat feedback.");
        System.out.println(WHITE + "[ ProjectileSystem.java ]" + RESET);
        System.out.println("  - L18: Vector Update. X/Y is incremented by VX/VY per frame tick.");
        System.out.println("  - L26: List Management. Automatically removes projectiles when life <= 0.");
    }
}
