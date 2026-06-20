package dependancy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class gui extends JFrame {
    private GamePanel gamePanel;
    
    public enum GameState { EXPLORING, COMBAT, ENDING }
    private GameState currentState = GameState.EXPLORING;
    
    private int currentHealth;
    private int maxHealth;
    private double stamina = 100;
    
    // Combat State
    private int activeMobIdx = -1;
    private int enemyHP = 0;
    private int enemyMaxHP = 0;
    private String enemyType = "CONSTRUCT";
    private boolean isBoss = false;
    private String systemMessage = "MISSION START: PURGE ALL CORRUPT CONSTRUCTS.";
    private int combatMenuIndex = 0; 
    private int moveMenuIndex = 0;   
    private boolean inMoveMenu = false;
    
    // Ending State
    private int endingChoice = 0; // 0: Self-Destruct, 1: Live

    // Ability Visuals
    private java.util.List<int[]> currentPath = null;
    private long pathExpiry = 0;
    private boolean radarActive = false;
    private long radarExpiry = 0;
    private boolean mapDecompressorActive = false;
    private long mapDecompressorExpiry = 0;
    private boolean stunActive = false;
    private long stunExpiry = 0;
    private long messageExpiry = 0;
    private int lastMoveDir = KeyEvent.VK_W;

    // Visual State
    private double drawPosRow, drawPosCol; 
    private final double LERP_FACTOR = 0.15;
    private long lastActionTime = 0;
    private long lastMoveTime = 0;
    private final int ACTION_COOLDOWN = 200;
    private final int MOVE_COOLDOWN = 180;

    // FPS Tracking
    private int fps = 0;
    private int frameCount = 0;
    private long lastFpsTime = 0;

    // Theme
    private final Color BG_DARK = new Color(10, 10, 12);
    private final Color ACCENT_NEON = new Color(0, 255, 200);
    private final Color UI_FRAME = new Color(30, 35, 45);
    private final Color DANGER_RED = new Color(255, 50, 50);
    private final Color BOSS_PURPLE = new Color(200, 50, 255);

    // VFX State
    private float screenShake = 0;
    private String deathPrompt = null;
    private long deathPromptExpiry = 0;
    private int parryWindow = 0;
    private long lastParryTime = 0;
    private int combo = 0;
    private long comboTimer = 0;
    private java.util.List<FloatingText> floatingTexts = new ArrayList<>();
    private java.util.List<AfterImage> afterImages = new ArrayList<>();

    private static class AfterImage {
        double x, y; int life;
        AfterImage(double x, double y) { this.x = x; this.y = y; this.life = 15; }
    }

    private static class FloatingText {
        String text; double x, y; int life; Color col;
        FloatingText(String t, double x, double y, Color c) { text=t; this.x=x; this.y=y; life=40; col=c; }
    }

    public gui() {
        setTitle("OVERDRIVE - PROJECT_NEXUS");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        if (editor.stats[0] == null) {
            new editor(null, false, false);
        }
        
        // Safety check for null or empty stats
        for (int i=0; i<editor.stats.length; i++) {
            if (editor.stats[i] == null || editor.stats[i].isEmpty()) editor.stats[i] = "0";
        }
        if (editor.stats[4].equals("0")) editor.stats[4] = "100";

        maxHealth = Integer.parseInt(editor.stats[4]);
        currentHealth = Math.max(0, Integer.parseInt(editor.stats[4]));
        
        drawPosRow = play.playerVector[0];
        drawPosCol = play.playerVector[1];
        messageExpiry = System.currentTimeMillis() + 5000;
        updateFogOfWar();

        gamePanel = new GamePanel();
        add(gamePanel);

        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleInput(e);
            }
        });

        new javax.swing.Timer(16, e -> {
            double oldX = drawPosRow;
            double oldY = drawPosCol;
            drawPosRow += (play.playerVector[0] - drawPosRow) * LERP_FACTOR;
            drawPosCol += (play.playerVector[1] - drawPosCol) * LERP_FACTOR;
            
            // Spawn Afterimages for trail
            if (Math.sqrt(Math.pow(drawPosRow - oldX, 2) + Math.pow(drawPosCol - oldY, 2)) > 0.05) {
                if (parryWindow > 0) { // Dash/Blink is active
                    afterImages.add(new AfterImage(drawPosRow, drawPosCol));
                }
            }

            // Update Afterimages
            for (int i = afterImages.size() - 1; i >= 0; i--) {
                afterImages.get(i).life--;
                if (afterImages.get(i).life <= 0) afterImages.remove(i);
            }
            
            // Real-time Updates
            if (currentState == GameState.EXPLORING) {
                updateRealTimeCombat();
            }
            ParticleSystem.update();
            ProjectileSystem.update();
            updateFloatingTexts();
            
            // Passive Stamina Regen
            if (stamina < 100) stamina = Math.min(100, stamina + 0.15);
            
            if (parryWindow > 0) parryWindow--;
            if (screenShake > 0) screenShake *= 0.9f;

            gamePanel.repaint();
        }).start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveAndExit();
            }
        });
        
        setVisible(true);
    }

    private void updateFogOfWar() {
        int r = (int)play.playerVector[0];
        int c = (int)play.playerVector[1];
        int floorIdx = map.currentFloor - 1;
        int dim = (int) Math.sqrt(map.currentArea);
        int radius = 2;
        for (int i = r - radius; i <= r + radius; i++) {
            for (int j = c - radius; j <= c + radius; j++) {
                if (i >= 0 && i < dim && j >= 0 && j < dim) {
                    map.discovered[floorIdx][i][j] = true;
                }
            }
        }
    }

    private void updateFloatingTexts() {
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.y += 0.05;
            ft.life--;
            if (ft.life <= 0) floatingTexts.remove(i);
        }
    }

    private void updateRealTimeCombat() {
        int floorIdx = map.currentFloor - 1;
        double pr = play.playerVector[0], pc = play.playerVector[1];
        
        // Handle Projectiles
        for (int i = ProjectileSystem.projectiles.size() - 1; i >= 0; i--) {
            ProjectileSystem.Projectile p = ProjectileSystem.projectiles.get(i);
            
            // Parry/Reflect Logic
            if (p.isEnemy && parryWindow > 0 && Math.sqrt(Math.pow(pr - p.x, 2) + Math.pow(pc - p.y, 2)) < 1.8) {
                p.isEnemy = false;
                p.vx = -p.vx * 1.5;
                p.vy = -p.vy * 1.5;
                p.life = 100; // Reset life for reflected bullet
                screenShake = 10.0f;
                floatingTexts.add(new FloatingText("REFLECTED", pr, pc, Color.CYAN));
                ParticleSystem.spawn(p.x, p.y, Color.CYAN, 10);
                continue;
            }

            // Check Player Collision
            if (p.isEnemy && parryWindow <= 0 && Math.sqrt(Math.pow(pr - p.x, 2) + Math.pow(pc - p.y, 2)) < 0.6) {
                int playerDef = Integer.parseInt(editor.stats[2]) + Integer.parseInt(editor.stats[12]);
                // Damage: Floor 1: 3, Floor 2: 4, Floor 5: 7 (was 5+)
                int dmg = Math.max(2, (2 + map.currentFloor) - playerDef / 3);
                currentHealth -= dmg;
                combo = 0; // Reset combo on hit
                screenShake = 15.0f;
                floatingTexts.add(new FloatingText("-" + dmg, pr, pc, Color.YELLOW));
                ParticleSystem.spawn(pr, pc, Color.YELLOW, 8);
                ProjectileSystem.projectiles.remove(i);
                if (currentHealth <= 0) handleDeath();
                continue;
            }
            
            // Check Enemy Collision
            if (!p.isEnemy) {
                for (int j = 0; j < map.rooms[floorIdx]; j++) {
                    int mr = map.roomLocation[0][floorIdx][j][0];
                    int mc = map.roomLocation[0][floorIdx][j][1];
                    if (mr == -1) continue;
                    if (Math.sqrt(Math.pow(p.x - mr, 2) + Math.pow(p.y - mc, 2)) < 0.8) {
                        int dmg = Integer.parseInt(editor.stats[1]) + Integer.parseInt(editor.stats[11]);
                        map.mobHP[floorIdx][j] -= dmg;
                        floatingTexts.add(new FloatingText("-" + dmg, mr, mc, Color.WHITE));
                        ParticleSystem.spawn(mr, mc, Color.WHITE, 12);
                        
                        if (map.mobHP[floorIdx][j] <= 0) {
                            floatingTexts.add(new FloatingText("DECOMMISSIONED", mr, mc, Color.MAGENTA));
                            map.roomLocation[0][floorIdx][j][0] = -1;
                            if (j == mobs.keyMobs[floorIdx] - 1) map.keyFound = true;
                            stats.addExp(30);
                        }
                        
                        ProjectileSystem.projectiles.remove(i);
                        break;
                    }
                }
            }
            
            // Canister Collision
            for (int j = 0; j < map.canisterCount[floorIdx]; j++) {
                int cr = map.roomLocation[3][floorIdx][j][0], cc = map.roomLocation[3][floorIdx][j][1];
                if (cr != -1 && Math.sqrt(Math.pow(p.x - cr, 2) + Math.pow(p.y - cc, 2)) < 0.6) {
                    map.roomLocation[3][floorIdx][j][0] = -1;
                    detonateCanister(cr, cc);
                    ProjectileSystem.projectiles.remove(i);
                    break;
                }
            }
        }

        // --- STUN CHECK MOVED INSIDE LOOP FOR RANGE ---
        // if (stunActive && System.currentTimeMillis() < stunExpiry) return;

        for (int j = 0; j < map.rooms[floorIdx]; j++) {
            int mr = map.roomLocation[0][floorIdx][j][0];
            int mc = map.roomLocation[0][floorIdx][j][1];
            if (mr == -1) continue;

            double dist = Math.sqrt(Math.pow(pr - mr, 2) + Math.pow(pc - mc, 2));
            
            // EMP Pulse Range Check (Range: 4.0 units)
            if (stunActive && System.currentTimeMillis() < stunExpiry && dist < 4.0) {
                if (System.currentTimeMillis() % 500 < 30) {
                    ParticleSystem.spawn(mr, mc, Color.CYAN, 2);
                }
                continue; 
            }

            boolean isSniper = (j % 3 == 0 && !isBoss); // Every 3rd mob is a sniper
            
            // Enemy AI
            if (dist < 8.0 && System.currentTimeMillis() % 1000 < 30) {
                int nextR = mr, nextC = mc;
                if (isSniper) {
                    // Snipers try to keep distance
                    if (dist < 4.0) {
                        if (mr < pr) nextR--; else if (mr > pr) nextR++;
                        if (mc < pc) nextC--; else if (mc > pc) nextC++;
                    } else if (dist > 6.0) {
                        if (mr < pr) nextR++; else if (mr > pr) nextR--;
                        if (mc < pc) nextC++; else if (mc > pc) nextC--;
                    }
                    // Fire Projectile
                    if (dist < 7.0 && Math.random() > 0.95) {
                        double vx = (pr - mr) / dist * 0.15;
                        double vy = (pc - mc) / dist * 0.15;
                        ProjectileSystem.fire(mr, mc, vx, vy, true);
                    }
                } else {
                    // Normal chasers
                    if (mr < pr) nextR++; else if (mr > pr) nextR--;
                    if (mc < pc) nextC++; else if (mc > pc) nextC--;
                }

                if (!map.isWall[floorIdx][nextR][nextC]) {
                    map.roomLocation[0][floorIdx][j][0] = nextR;
                    map.roomLocation[0][floorIdx][j][1] = nextC;
                }
            }

            // Contact Damage
            if (dist < 1.0 && parryWindow <= 0) {
                int playerDef = Integer.parseInt(editor.stats[2]) + Integer.parseInt(editor.stats[12]);
                // Damage: Floor 1: 2, Floor 5: 6 (was 5+)
                int dmg = Math.max(1, (1 + map.currentFloor) - playerDef / 4);
                currentHealth -= dmg;
                combo = 0; // Reset combo on hit
                screenShake = 10.0f;
                floatingTexts.add(new FloatingText("-" + dmg, pr, pc, DANGER_RED));
                ParticleSystem.spawn(pr, pc, DANGER_RED, 5);
                if (currentHealth <= 0) handleDeath();
            }
        }
        
        // Combo Decay
        if (combo > 0 && System.currentTimeMillis() > comboTimer) {
            combo = 0;
            systemMessage = "SYSTEM: COMBO DROPPED.";
        }
    }

    private void handleInput(KeyEvent e) {
        long now = System.currentTimeMillis();
        int keyCode = e.getKeyCode();

        if (currentState == GameState.EXPLORING) {
            boolean isMoveKey = (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_D);
            if (isMoveKey) {
                if (now - lastMoveTime < MOVE_COOLDOWN) return;
                lastMoveTime = now;
            } else {
                if (now - lastActionTime < ACTION_COOLDOWN) return;
                lastActionTime = now;
            }
            handleExplorationInput(e);
        } else if (currentState == GameState.COMBAT) {
            if (now - lastActionTime < ACTION_COOLDOWN) return;
            lastActionTime = now;
            handleCombatInput(e);
        } else if (currentState == GameState.ENDING) {
            handleEndingInput(e);
        }
    }

    private void handleExplorationInput(KeyEvent e) {
        int dim = (int) Math.sqrt(map.currentArea);
        int floorIdx = map.currentFloor - 1;
        int nextRow = (int)play.playerVector[0];
        int nextCol = (int)play.playerVector[1];
        int keyCode = e.getKeyCode();
        boolean moved = false;

        if (keyCode == KeyEvent.VK_W) { nextRow++; lastMoveDir = KeyEvent.VK_W; moved = true; }
        else if (keyCode == KeyEvent.VK_S) { nextRow--; lastMoveDir = KeyEvent.VK_S; moved = true; }
        else if (keyCode == KeyEvent.VK_A) { nextCol--; lastMoveDir = KeyEvent.VK_A; moved = true; }
        else if (keyCode == KeyEvent.VK_D) { nextCol++; lastMoveDir = KeyEvent.VK_D; moved = true; }
        else if (keyCode == config.keyHead) {
            String head = editor.stats[25];
            if ("1".equals(head)) { // Radar
                radarActive = true;
                radarExpiry = System.currentTimeMillis() + 6000;
                systemMessage = "RADAR: Active scanning...";
            } else if ("2".equals(head)) { // Pathfinder
                calculatePath();
                pathExpiry = System.currentTimeMillis() + 5000;
                if (!map.keyFound) {
                    systemMessage = "PATHFINDER: Targeting Key Signal.";
                } else {
                    systemMessage = "PATHFINDER: Exit route highlighted.";
                }
            } else if ("3".equals(head)) { // Map Decompressor
                mapDecompressorActive = true;
                mapDecompressorExpiry = System.currentTimeMillis() + 10000;
                systemMessage = "MAP_DECOMPRESSOR: Full sector data revealed.";
            } else {
                systemMessage = "SYSTEM: No Head module installed.";
            }
            messageExpiry = System.currentTimeMillis() + 4000;
        } else if (keyCode == config.keyHand) {
            String hand = editor.stats[26];
            if ("3".equals(hand)) { // EMP Pulse
                stunActive = true;
                stunExpiry = System.currentTimeMillis() + 5000;
                systemMessage = "EMP_PULSE: High-frequency burst emitted.";
            } else {
                systemMessage = "SYSTEM: Hand hardware not compatible for active trigger.";
            }
            messageExpiry = System.currentTimeMillis() + 4000;
        } else if (keyCode == config.keyLeg) {
            String leg = editor.stats[27];
            if ("3".equals(leg)) { // Phase Shift (BLINK)
                int br = (int)play.playerVector[0], bc = (int)play.playerVector[1];
                int dr = 0, dc = 0;
                if (lastMoveDir == KeyEvent.VK_W) dr = 1;
                else if (lastMoveDir == KeyEvent.VK_S) dr = -1;
                else if (lastMoveDir == KeyEvent.VK_A) dc = -1;
                else if (lastMoveDir == KeyEvent.VK_D) dc = 1;

                int targetR = br + dr * 2;
                int targetC = bc + dc * 2;
                
                // Clamp and check destination
                targetR = Math.max(1, Math.min(dim - 2, targetR));
                targetC = Math.max(1, Math.min(dim - 2, targetC));

                if (!map.isWall[floorIdx][targetR][targetC]) {
                    // Success: Blinks 2 units (can skip 1 wall)
                    play.playerVector[0] = targetR;
                    play.playerVector[1] = targetC;
                } else {
                    // Try 1 unit if 2 units is a wall
                    targetR = br + dr;
                    targetC = bc + dc;
                    targetR = Math.max(1, Math.min(dim - 2, targetR));
                    targetC = Math.max(1, Math.min(dim - 2, targetC));
                    if (!map.isWall[floorIdx][targetR][targetC]) {
                        play.playerVector[0] = targetR;
                        play.playerVector[1] = targetC;
                    } else {
                        systemMessage = "PHASE_SHIFT: Error - Obstruction too thick.";
                        messageExpiry = System.currentTimeMillis() + 2000;
                        return;
                    }
                }
                
                parryWindow = 20; // Long I-frames for blink
                screenShake = 15.0f;
                updateFogOfWar();
                checkCollisions((int)play.playerVector[0], (int)play.playerVector[1]);
                systemMessage = "PHASE_SHIFT: Dimensional blink successful.";
                ParticleSystem.spawn(play.playerVector[0], play.playerVector[1], Color.MAGENTA, 25);
                
                // Spawn arrival afterimages for trail effect
                for(int i=1; i<=3; i++) {
                    afterImages.add(new AfterImage(play.playerVector[0], play.playerVector[1]));
                }
            } else {
                systemMessage = "SYSTEM: Leg hardware not compatible for active trigger.";
            }
            messageExpiry = System.currentTimeMillis() + 4000;
        } else if (keyCode == KeyEvent.VK_SHIFT) {
            // Dash Mechanic
            if (stamina >= 20) {
                int dr = 0, dc = 0;
                if (lastMoveDir == KeyEvent.VK_W) dr = 1;
                else if (lastMoveDir == KeyEvent.VK_S) dr = -1;
                else if (lastMoveDir == KeyEvent.VK_A) dc = -1;
                else if (lastMoveDir == KeyEvent.VK_D) dc = 1;
                
                int r = (int)play.playerVector[0];
                int c = (int)play.playerVector[1];
                int nr = r, nc = c;

                // Check first step
                int step1R = Math.max(1, Math.min(dim - 2, r + dr));
                int step1C = Math.max(1, Math.min(dim - 2, c + dc));
                
                if (!map.isWall[floorIdx][step1R][step1C]) {
                    nr = step1R; nc = step1C;
                    // Check second step
                    int step2R = Math.max(1, Math.min(dim - 2, r + dr * 2));
                    int step2C = Math.max(1, Math.min(dim - 2, c + dc * 2));
                    if (!map.isWall[floorIdx][step2R][step2C]) {
                        nr = step2R; nc = step2C;
                    }
                }

                if (nr != r || nc != c) {
                    stamina -= 20;
                    parryWindow = 10;
                    afterImages.add(new AfterImage(drawPosRow, drawPosCol));
                    play.playerVector[0] = nr;
                    play.playerVector[1] = nc;
                    screenShake = 5.0f;
                    ParticleSystem.spawn(play.playerVector[0], play.playerVector[1], ACCENT_NEON, 10);
                    systemMessage = "DASH: Kinetic boost engaged.";
                    updateFogOfWar();
                    checkCollisions(nr, nc);
                } else {
                    systemMessage = "DASH: Path obstructed.";
                }
            } else {
                systemMessage = "ERROR: LOW STAMINA.";
            }
            messageExpiry = System.currentTimeMillis() + 2000;
        } else if (keyCode == KeyEvent.VK_SPACE) {
            // Real-time Strike (Attack from exploration)
            for (int j = 0; j < map.rooms[floorIdx]; j++) {
                int mr = map.roomLocation[0][floorIdx][j][0];
                int mc = map.roomLocation[0][floorIdx][j][1];
                if (mr == -1) continue;
                double dist = Math.sqrt(Math.pow(play.playerVector[0] - mr, 2) + Math.pow(play.playerVector[1] - mc, 2));
                if (dist < 1.8) { 
                    int dmg = Integer.parseInt(editor.stats[1]) + Integer.parseInt(editor.stats[11]);
                    // Combo Bonus
                    dmg += (combo * 2);
                    combo++;
                    comboTimer = System.currentTimeMillis() + 3000;
                    
                    map.mobHP[floorIdx][j] -= dmg;
                    floatingTexts.add(new FloatingText("COMBO x" + combo + "! " + dmg, mr, mc, ACCENT_NEON));
                    ParticleSystem.spawn(mr, mc, Color.WHITE, 15);
                    
                    if (map.mobHP[floorIdx][j] <= 0) {
                        floatingTexts.add(new FloatingText("DECOMMISSIONED", mr, mc, Color.MAGENTA));
                        map.roomLocation[0][floorIdx][j][0] = -1;
                        if (j == mobs.keyMobs[floorIdx] - 1) map.keyFound = true;
                        stats.addExp(25 + combo);
                    }
                    screenShake = 5.0f;
                }
            }
        } else if (keyCode == KeyEvent.VK_I) {
            item.swapInventory("Weapon");
            systemMessage = "SYSTEM: Weapon systems swapped.";
            messageExpiry = System.currentTimeMillis() + 2000;
        } else if (keyCode == KeyEvent.VK_O) {
            item.swapInventory("Armor");
            systemMessage = "SYSTEM: Armor plating swapped.";
            messageExpiry = System.currentTimeMillis() + 2000;
        } else if (keyCode == KeyEvent.VK_Q) {
            // Overdrive Action (Q)
            int usages = Integer.parseInt(editor.stats[21]); // m2c
            if (usages > 0 && stamina >= 40) {
                stamina -= 40;
                editor.stats[21] = String.valueOf(usages - 1);
                int playerAtk = Integer.parseInt(editor.stats[1]) + Integer.parseInt(editor.stats[11]);
                int dmg = playerAtk * 2 + (int)(Math.random() * 20);
                
                // Strike everything in range
                for (int j = 0; j < map.rooms[floorIdx]; j++) {
                    int mr = map.roomLocation[0][floorIdx][j][0];
                    int mc = map.roomLocation[0][floorIdx][j][1];
                    if (mr == -1) continue;
                    double dist = Math.sqrt(Math.pow(play.playerVector[0] - mr, 2) + Math.pow(play.playerVector[1] - mc, 2));
                    if (dist < 2.5) { // Larger range for overdrive
                        map.mobHP[floorIdx][j] -= dmg;
                        floatingTexts.add(new FloatingText("OVERDRIVE: " + dmg, mr, mc, Color.ORANGE));
                        ParticleSystem.spawn(mr, mc, Color.ORANGE, 20);
                        if (map.mobHP[floorIdx][j] <= 0) {
                            map.roomLocation[0][floorIdx][j][0] = -1;
                            if (j == mobs.keyMobs[floorIdx] - 1) map.keyFound = true;
                        }
                    }
                }
                systemMessage = "ACTION: OVERDRIVE ENGAGED.";
                messageExpiry = System.currentTimeMillis() + 2000;
                screenShake = 15.0f;
                stats.addExp(50);
            } else {
                systemMessage = "ERROR: INSUFFICIENT ENERGY OR USAGES.";
                messageExpiry = System.currentTimeMillis() + 2000;
            }
        } else if (keyCode == KeyEvent.VK_E) {
            // Reboot Action (E)
            int usages = Integer.parseInt(editor.stats[23]); // m3c
            if (usages > 0) {
                editor.stats[23] = String.valueOf(usages - 1);
                int heal = 20 + (int)(Math.random() * 15);
                currentHealth = Math.min(maxHealth, currentHealth + heal);
                
                // Restore power to moves
                for (int i=0; i<3; i++) {
                    int cur = Integer.parseInt(editor.stats[19+(i*2)]);
                    int max = Integer.parseInt(editor.stats[20+(i*2)]);
                    editor.stats[19+(i*2)] = String.valueOf(Math.min(max, cur + 5));
                }
                
                floatingTexts.add(new FloatingText("REBOOT: +" + heal + " HP", play.playerVector[0], play.playerVector[1], Color.GREEN));
                ParticleSystem.spawn(play.playerVector[0], play.playerVector[1], Color.GREEN, 15);
                systemMessage = "SYSTEM: CORE REBOOT SUCCESSFUL.";
                messageExpiry = System.currentTimeMillis() + 2000;
            } else {
                systemMessage = "ERROR: REBOOT MODULE DEPLETED.";
                messageExpiry = System.currentTimeMillis() + 2000;
            }
        } else if (keyCode == KeyEvent.VK_V) {
            // Parry Action (V)
            if (editor.stats[26].equals("4")) { // Reflector Arm
                long now = System.currentTimeMillis();
                if (now - lastParryTime > 1000 && stamina >= 15) {
                    stamina -= 15;
                    parryWindow = 12; // ~200ms
                    lastParryTime = now;
                    floatingTexts.add(new FloatingText("PARRY_WINDOW", play.playerVector[0], play.playerVector[1], Color.WHITE));
                    ParticleSystem.spawn(play.playerVector[0], play.playerVector[1], Color.WHITE, 5);
                }
            } else {
                systemMessage = "ERROR: REFLECTOR ARM NOT INSTALLED.";
                messageExpiry = System.currentTimeMillis() + 2000;
            }
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            saveAndExit();
        }

        // Standard Movement Update
        if (moved && nextRow >= 0 && nextRow < dim && nextCol >= 0 && nextCol < dim && !map.isWall[floorIdx][nextRow][nextCol]) {
            play.playerVector[0] = nextRow;
            play.playerVector[1] = nextCol;
            updateFogOfWar();

            // Dynamic Pathfinder Update
            if (currentPath != null && System.currentTimeMillis() < pathExpiry) {
                calculatePath();
            }

            checkCollisions(nextRow, nextCol);

        }
    }

    private void calculatePath() {
        int dim = (int) Math.sqrt(map.currentArea);
        int floorIdx = map.currentFloor - 1;
        int startR = (int)play.playerVector[0];
        int startC = (int)play.playerVector[1];
        
        int endR = map.exitPos[floorIdx][0];
        int endC = map.exitPos[floorIdx][1];

        Queue<int[]> queue = new LinkedList<>();
        int[][][] parentMap = new int[dim][dim][2];
        boolean[][] visited = new boolean[dim][dim];
        for(int i=0; i<dim; i++) for(int j=0; j<dim; j++) parentMap[i][j][0] = -1;

        queue.add(new int[]{startR, startC});
        visited[startR][startC] = true;
        
        int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        boolean found = false;
        
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            if (curr[0] == endR && curr[1] == endC) {
                found = true;
                break;
            }
            
            for (int[] d : dirs) {
                int nr = curr[0] + d[0];
                int nc = curr[1] + d[1];
                if (nr >= 0 && nr < dim && nc >= 0 && nc < dim && !map.isWall[floorIdx][nr][nc] && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentMap[nr][nc][0] = curr[0];
                    parentMap[nr][nc][1] = curr[1];
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        
        if (found) {
            currentPath = new ArrayList<>();
            int cr = endR, cc = endC;
            while (cr != -1) {
                currentPath.add(new int[]{cr, cc});
                int pr = parentMap[cr][cc][0];
                int pc = parentMap[cr][cc][1];
                cr = pr; cc = pc;
            }
        }
    }

    private void checkCollisions(int r, int c) {
        int floorIdx = map.currentFloor - 1;
        
        // Floor Exit Check
        if (map.keyFound && r == map.exitPos[floorIdx][0] && c == map.exitPos[floorIdx][1]) {
            advanceFloor();
            return;
        }

        for (int j = 0; j < map.rooms[floorIdx]; j++) {
            if (map.roomLocation[0][floorIdx][j][0] == r && map.roomLocation[0][floorIdx][j][1] == c) {
                // initCombat(j); // Removed for real-time combat
                return;
            }
        }

        for (int j = 0; j < map.chestCount[floorIdx]; j++) {
            if (map.roomLocation[1][floorIdx][j][0] == r && map.roomLocation[1][floorIdx][j][1] == c) {
                map.roomLocation[1][floorIdx][j][0] = -1;
                new chest().open();
                break;
            }
        }

        // Canister Check
        for (int j = 0; j < map.canisterCount[floorIdx]; j++) {
            if (map.roomLocation[3][floorIdx][j][0] == r && map.roomLocation[3][floorIdx][j][1] == c) {
                map.roomLocation[3][floorIdx][j][0] = -1;
                detonateCanister(r, c);
                break;
            }
        }

        // Trap Check
        for (int j = 0; j < map.trapCount[floorIdx]; j++) {
            if (map.roomLocation[2][floorIdx][j][0] == r && map.roomLocation[2][floorIdx][j][1] == c) {
                map.roomLocation[2][floorIdx][j][0] = -1; // Remove trap
                triggerTrap();
                break;
            }
        }
        play.saveSession(false);
    }

    private void detonateCanister(int r, int c) {
        int floorIdx = map.currentFloor - 1;
        screenShake = 20.0f;
        systemMessage = "DANGER: PLASMA CANISTER DETONATED.";
        messageExpiry = System.currentTimeMillis() + 3000;
        ParticleSystem.spawn(r, c, Color.ORANGE, 25);
        
        // Damage nearby enemies
        for (int j = 0; j < map.rooms[floorIdx]; j++) {
            int mr = map.roomLocation[0][floorIdx][j][0];
            int mc = map.roomLocation[0][floorIdx][j][1];
            if (mr == -1) continue;
            if (Math.abs(mr - r) <= 1 && Math.abs(mc - c) <= 1) {
                int dmg = 100;
                map.mobHP[floorIdx][j] -= dmg;
                floatingTexts.add(new FloatingText("BLAST: " + dmg, mr, mc, Color.ORANGE));
                if (map.mobHP[floorIdx][j] <= 0) {
                    map.roomLocation[0][floorIdx][j][0] = -1;
                    if (j == mobs.keyMobs[floorIdx] - 1) map.keyFound = true;
                    stats.addExp(50);
                }
            }
        }
        
        // Damage player if too close (Check for Thermal Plating)
        if (Math.abs(play.playerVector[0] - r) <= 1 && Math.abs(play.playerVector[1] - c) <= 1) {
            boolean hasInsulation = "3".equals(editor.stats[22]); 
            if (!hasInsulation) {
                int dmg = 20;
                currentHealth = Math.max(1, currentHealth - dmg);
                floatingTexts.add(new FloatingText("-" + dmg, play.playerVector[0], play.playerVector[1], Color.CYAN));
            } else {
                floatingTexts.add(new FloatingText("REFLECTED", play.playerVector[0], play.playerVector[1], Color.CYAN));
            }
        }
    }

    private void triggerTrap() {
        int type = (int)(Math.random() * 3);
        if (type == 0) { // MINE
            int dmg = maxHealth / 4;
            currentHealth = Math.max(1, currentHealth - dmg);
            systemMessage = "DANGER: PRESSURE PLATE TRIGGERED. MINE DETONATED (-" + dmg + " HP)";
        } else if (type == 1) { // STASIS
            stamina = 0;
            systemMessage = "WARNING: STASIS FIELD ACTIVE. STAMINA DRAINED.";
        } else { // MALWARE
            mapDecompressorExpiry = 0;
            radarExpiry = 0;
            pathExpiry = 0;
            systemMessage = "CRITICAL: MALWARE DETECTED. ALL ACTIVE MODULES OFFLINE.";
        }
        messageExpiry = System.currentTimeMillis() + 4000;
        Logger.combat(systemMessage);
    }

    private void advanceFloor() {
        if (map.currentFloor < map.floors) {
            map.currentFloor++;
            map.keyFound = false;
            play.playerVector[0] = 1;
            play.playerVector[1] = 1;
            drawPosRow = 1; drawPosCol = 1;
            systemMessage = "SYSTEM: SECTOR CLEARED. DESCENDING TO FLOOR " + map.currentFloor;
            Logger.important("Descending to floor " + map.currentFloor);
            play.saveSession(false);
        } else {
            currentState = GameState.ENDING;
        }
    }

    private void initCombat(int mobIdx) {
        currentState = GameState.COMBAT;
        activeMobIdx = mobIdx;
        
        if (mobIdx == -99) { // BOSS
            isBoss = true;
            int bossType = (map.currentFloor / 5) % 3;
            if (bossType == 1) {
                enemyType = "TITAN_CONSTRUCT";
                enemyMaxHP = 200 + (map.currentFloor * 25);
                systemMessage = "WARNING: MASSIVE HEAT SIGNATURE. TITAN_CONSTRUCT DETECTED.";
            } else if (bossType == 2) {
                enemyType = "WASP_SWARM_CORE";
                enemyMaxHP = 150 + (map.currentFloor * 20);
                systemMessage = "SCAN: MULTIPLE SIGNALS CONVERGING. WASP_SWARM_CORE ONLINE.";
            } else {
                enemyType = "SENTINEL_PRIME";
                enemyMaxHP = 180 + (map.currentFloor * 22);
                systemMessage = "ALERT: HIGH-LEVEL ENCRYPTION DETECTED. SENTINEL_PRIME ENGAGED.";
            }
        } else {
            isBoss = false;
            // Progressive normal mobs
            int variant = Math.min(5, 1 + (map.currentFloor / 10));
            enemyType = "CONSTRUCT_V" + (variant + (int)(Math.random()*2));
            enemyMaxHP = 40 + (map.currentFloor * 10);
            systemMessage = "SCAN: " + enemyType + " DETECTED. INITIATING ENGAGEMENT.";
        }
        
        enemyHP = enemyMaxHP;
        combatMenuIndex = 0;
        inMoveMenu = false;
    }

    private void handleCombatInput(KeyEvent e) {
        if (!inMoveMenu) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_A: if (combatMenuIndex > 0) combatMenuIndex--; break;
                case KeyEvent.VK_D: if (combatMenuIndex < 2) combatMenuIndex++; break;
                case KeyEvent.VK_SPACE: case KeyEvent.VK_ENTER: executeMainMenu(); break;
            }
        } else {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W: if (moveMenuIndex >= 2) moveMenuIndex -= 2; break;
                case KeyEvent.VK_S: if (moveMenuIndex <= 1) moveMenuIndex += 2; break;
                case KeyEvent.VK_A: if (moveMenuIndex % 2 == 1) moveMenuIndex--; break;
                case KeyEvent.VK_D: if (moveMenuIndex % 2 == 0) moveMenuIndex++; break;
                case KeyEvent.VK_BACK_SPACE: case KeyEvent.VK_X: inMoveMenu = false; break;
                case KeyEvent.VK_SPACE: case KeyEvent.VK_ENTER: executeMove(); break;
            }
        }
    }

    private void executeMainMenu() {
        if (combatMenuIndex == 0) inMoveMenu = true;
        else if (combatMenuIndex == 2) {
            if (isBoss) systemMessage = "ERROR: BOSS JAMMING SIGNAL. ESCAPE IMPOSSIBLE.";
            else {
                systemMessage = "RETREAT: DISENGAGING TARGET...";
                new javax.swing.Timer(800, e -> currentState = GameState.EXPLORING).start();
            }
        }
    }

    private void executeMove() {
        int playerAtk = Integer.parseInt(editor.stats[1]) + Integer.parseInt(editor.stats[11]);
        if (moveMenuIndex == 3) { inMoveMenu = false; return; }

        int staminaCost = 0;
        if (moveMenuIndex == 0) staminaCost = 20;
        else if (moveMenuIndex == 1) staminaCost = 50;
        else if (moveMenuIndex == 2) staminaCost = 40;

        if (stamina < staminaCost) {
            systemMessage = "ERROR: INSUFFICIENT STAMINA (" + (int)stamina + "/" + staminaCost + ")";
            return;
        }

        stamina -= staminaCost;

        // Action logic
        if (moveMenuIndex == 0) { // STRIKE
            int dmg = playerAtk + (int)(Math.random() * 10);
            enemyHP -= dmg;
            systemMessage = "ACTION: STRIKE SUCCESSFUL (-" + dmg + " HP)";
            ParticleSystem.spawn(drawPosRow, drawPosCol, Color.WHITE, 5);
        } else if (moveMenuIndex == 1) { // OVERDRIVE
            int dmg = playerAtk * 2 + (int)(Math.random() * 25);
            enemyHP -= dmg;
            systemMessage = "ACTION: OVERDRIVE ENGAGED (-" + dmg + " HP)";
            screenShake = 10.0f;
            ParticleSystem.spawn(drawPosRow, drawPosCol, ACCENT_NEON, 15);
        } else if (moveMenuIndex == 2) { // REBOOT
            int heal = 40 + (int)(Math.random() * 20);
            currentHealth = Math.min(maxHealth, currentHealth + heal);
            systemMessage = "SYSTEM: REBOOT SUCCESSFUL (+" + heal + " HP)";
            ParticleSystem.spawn(drawPosRow, drawPosCol, Color.GREEN, 10);
        }

        if (enemyHP <= 0) {
            systemMessage = "STATUS: TARGET DECOMMISSIONED.";
            new javax.swing.Timer(1000, e -> {
                if (isBoss) {
                    advanceFloor();
                }
                else {
                    int floorIdx = map.currentFloor - 1;
                    map.roomLocation[0][floorIdx][activeMobIdx][0] = -1;
                    if (activeMobIdx == mobs.keyMobs[floorIdx] - 1) map.keyFound = true;
                }
                play.saveSession(false);
                currentState = GameState.EXPLORING;
            }).start();
        } else {
            enemyTurn();
        }
    }

    private void enemyTurn() {
        int playerDef = Integer.parseInt(editor.stats[2]) + Integer.parseInt(editor.stats[12]);
        int baseDamage = (8 + map.currentFloor * 2);
        int eDmg = Math.max(2, baseDamage - playerDef/2);

        if (isBoss) {
            if (enemyType.equals("TITAN_CONSTRUCT")) {
                eDmg = (int)(eDmg * 2.2); // Titan hits very hard
                systemMessage = "BOSS_ACTION: TITAN_SMASH (-" + eDmg + " HP)";
            } else if (enemyType.equals("WASP_SWARM_CORE")) {
                int hits = 3 + (int)(Math.random() * 3);
                eDmg = (eDmg / 2) * hits; // Swarm hits multiple times
                systemMessage = "BOSS_ACTION: SWARM_BARRAGE x" + hits + " (-" + eDmg + " HP)";
            } else if (enemyType.equals("SENTINEL_PRIME")) {
                eDmg = (int)(eDmg * 1.5);
                if (enemyHP < enemyMaxHP * 0.4 && Math.random() > 0.5) {
                    int heal = 30 + (map.currentFloor * 5);
                    enemyHP = Math.min(enemyMaxHP, enemyHP + heal);
                    systemMessage = "BOSS_ACTION: REPAIR_PROTOCOL (+" + heal + " HP)";
                } else {
                    systemMessage = "BOSS_ACTION: PRECISION_BEAM (-" + eDmg + " HP)";
                }
            }
        } else {
            systemMessage = "ENEMY_ACTION: TARGET_ENGAGED (-" + eDmg + " HP)";
        }
        
        currentHealth -= eDmg;
        stamina = Math.min(100, stamina + 20);
        if (currentHealth <= 0) {
            handleDeath();
        }
    }

    private void handleEndingInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: endingChoice = 0; break;
            case KeyEvent.VK_D: endingChoice = 1; break;
            case KeyEvent.VK_SPACE: case KeyEvent.VK_ENTER:
                if (endingChoice == 0) {
                    System.out.println("FINAL LOG: SELF-DESTRUCT INITIATED. THE STEEL PLAGUE ENDS HERE.");
                } else {
                    System.out.println("FINAL LOG: LIVING IN THE SILENCE. THE LAST ROBOT STANDS.");
                }
                System.exit(0);
                break;
        }
    }

    private void saveAndExit() {
        // Stop overwriting Max Health with current health
        // editor.stats[4] = String.valueOf(currentHealth); 
        play.saveSession(false);
        System.exit(0);
    }

    private void handleDeath() {
        int checkpoint = (map.currentFloor < 5) ? 1 : (map.currentFloor / 5) * 5;
        Logger.combat("CRITICAL FAILURE. OVERDRIVE UNIT DESTROYED.");
        
        deathPrompt = "REBOOTING AT CHECKPOINT: FLOOR " + checkpoint;
        deathPromptExpiry = System.currentTimeMillis() + 3000;
        
        // Reset state
        currentHealth = maxHealth;
        map.currentFloor = checkpoint;
        play.playerVector[0] = 1;
        play.playerVector[1] = 1;
        map.keyFound = false;
        
        play.clearDeadMobs();
        
        // Visual feedback
        screenShake = 30.0f;
        systemMessage = "SYSTEM_REBOOT: DATA RESTORED.";
        messageExpiry = System.currentTimeMillis() + 5000;
        
        updateFogOfWar();
        play.saveSession(false);
    }

    private boolean hasLineOfSight(int r1, int c1, int r2, int c2) {
        int dr = Math.abs(r2 - r1);
        int dc = Math.abs(c2 - c1);
        int sr = r1 < r2 ? 1 : -1;
        int sc = c1 < c2 ? 1 : -1;
        int err = dr - dc;

        int currR = r1;
        int currC = c1;
        int floorIdx = map.currentFloor - 1;

        while (true) {
            if (currR == r2 && currC == c2) return true;
            // Check wall at current step (don't block on the starting player tile)
            if (map.isWall[floorIdx][currR][currC] && (currR != r1 || currC != c1)) return false;

            int e2 = 2 * err;
            if (e2 > -dc) {
                err -= dc;
                currR += sr;
            }
            if (e2 < dr) {
                err += dr;
                currC += sc;
            }
            
            // Safety bound check
            if (currR < 0 || currR >= map.isWall[floorIdx].length || currC < 0 || currC >= map.isWall[floorIdx][0].length) return false;
        }
    }

    private Color lerpColor(Color c1, Color c2, float t) {
        t = Math.min(1.0f, Math.max(0.0f, t));
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
        return new Color(r, g, b);
    }

    private Color getRarityColor(String name) {
        if (name == null) return Color.GRAY;
        if (name.startsWith("Common")) return new Color(180, 180, 185);
        if (name.startsWith("Rare")) return new Color(255, 150, 0);
        if (name.startsWith("Epic")) return new Color(160, 50, 255);
        if (name.startsWith("Legendary")) return new Color(255, 50, 50);
        return Color.GRAY;
    }

    private void drawModernGear(Graphics2D g2, int x, int y, String label, String itemName, String itemStat, boolean isSmall) {
        g2.setFont(new Font("Monospaced", isSmall ? Font.PLAIN : Font.BOLD, isSmall ? 10 : 12));
        g2.setColor(isSmall ? Color.GRAY : Color.WHITE);
        g2.drawString(label, x, y);
        
        Color rarityCol = getRarityColor(itemName);
        g2.setColor(rarityCol);
        g2.fillOval(x + (isSmall ? 35 : 50), y - (isSmall ? 7 : 9), isSmall ? 6 : 8, isSmall ? 6 : 8);
        
        g2.setColor(isSmall ? Color.GRAY : Color.WHITE);
        g2.drawString("[" + itemStat + "]", x + (isSmall ? 45 : 65), y);
    }

    private RadialGradientPaint cachedVignette = null;
    private int lastVignetteW = -1, lastVignetteH = -1;

    private void drawVignette(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        if (cachedVignette == null || w != lastVignetteW || h != lastVignetteH) {
            float[] dists = {0.0f, 1.0f};
            Color[] colors = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)};
            cachedVignette = new RadialGradientPaint(w/2, h/2, (float)Math.sqrt(w*w+h*h)/2, dists, colors);
            lastVignetteW = w;
            lastVignetteH = h;
        }
        g2.setPaint(cachedVignette);
        g2.fillRect(0, 0, w, h);
    }

    private void drawScanlines(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(255, 255, 255, 10));
        g2.setStroke(new BasicStroke(1));
        for (int i = 0; i < h; i += 4) {
            g2.drawLine(0, i, w, i);
        }
        
        // Horizontal Glitch/Scan Bar
        int scanY = (int)((System.currentTimeMillis() / 10) % h);
        g2.setColor(new Color(255, 255, 255, 15));
        g2.fillRect(0, scanY, w, 2);
    }

    private void drawGrid(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(ACCENT_NEON.getRed(), ACCENT_NEON.getGreen(), ACCENT_NEON.getBlue(), 15));
        for (int i = 0; i < w; i += 50) g2.drawLine(i, 0, i, h);
        for (int i = 0; i < h; i += 50) g2.drawLine(0, i, w, i);
    }

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            // FPS Logic
            frameCount++;
            long now = System.currentTimeMillis();
            if (now - lastFpsTime >= 1000) {
                fps = frameCount;
                frameCount = 0;
                lastFpsTime = now;
            }
            
            // Optimization: Prioritize speed
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            
            int w = getWidth(), h = getHeight();

            // Background Grid & Base
            g2.setColor(BG_DARK);
            g2.fillRect(0, 0, w, h);
            drawGrid(g2);
            // Apply Screen Shake & Glitch Effect
            if (screenShake > 0) {
                g2.translate((Math.random() - 0.5) * screenShake, (Math.random() - 0.5) * screenShake);
                if (Math.random() > 0.8) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRect(0, (int)(Math.random() * h), w, (int)(Math.random() * 20));
                }
            }

            if (currentState == GameState.EXPLORING) drawExploration(g2);
            else if (currentState == GameState.COMBAT) drawBattle(g2);
            else if (currentState == GameState.ENDING) drawEnding(g2);

            if (!config.lowGraphics) {
                drawVignette(g2);
                drawScanlines(g2);
            }
            
            // Global Glitch Effect when low health
            if (currentHealth < maxHealth * 0.2 && System.currentTimeMillis() % 100 < 30) {
                g2.setColor(new Color(255, 0, 0, 40));
                g2.fillRect(0, 0, w, h);
            }

            // Death Prompt Overlay
            if (deathPrompt != null && System.currentTimeMillis() < deathPromptExpiry) {
                long remaining = deathPromptExpiry - System.currentTimeMillis();
                int alpha = (int)Math.min(255, (remaining / 3000.0) * 500); // Faster fade at the end
                if (alpha > 0) {
                    g2.setColor(new Color(0, 0, 0, (int)(alpha * 0.8)));
                    g2.fillRect(0, h/2 - 50, w, 100);
                    
                    g2.setColor(new Color(255, 50, 50, alpha));
                    g2.setFont(new Font("Monospaced", Font.BOLD, 24));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(deathPrompt, w/2 - fm.stringWidth(deathPrompt)/2, h/2 + 8);
                    
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(0, h/2 - 50, w, 100);
                }
            } else {
                deathPrompt = null;
            }
        }

        private void drawProceduralRobot(Graphics2D g2, int x, int y, int size, Color coreColor, boolean isPlayer) {
        int cx = x + size/2;
        int cy = y + size/2;
        
        // Outer Shadow/Glow
        g2.setColor(new Color(coreColor.getRed(), coreColor.getGreen(), coreColor.getBlue(), 40));
        g2.fillOval(x - 4, y - 4, size + 8, size + 8);

        // Armor Plating (Octagon-ish)
        g2.setColor(new Color(30, 35, 45));
        int[] px = {x+size/4, x+3*size/4, x+size, x+size, x+3*size/4, x+size/4, x, x};
        int[] py = {y, y, y+size/4, y+3*size/4, y+size, y+size, y+3*size/4, y+size/4};
        g2.fillPolygon(px, py, 8);
        g2.setColor(coreColor.darker());
        g2.setStroke(new BasicStroke(1));
        g2.drawPolygon(px, py, 8);

        // Tech Lines
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawLine(x+size/2, y, x+size/2, y+size);
        g2.drawLine(x, y+size/2, x+size, y+size/2);

        // Core / Eye
        long pulse = System.currentTimeMillis() % 1000;
        int pulseSize = (int)(size/3 + (Math.sin(pulse * Math.PI / 500) * 2));
        g2.setColor(coreColor);
        g2.fillOval(cx - pulseSize/2, cy - pulseSize/2, pulseSize, pulseSize);
        
        // Inner Lens
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - size/10, cy - size/10, size/5, size/5);
        
        if (isPlayer) {
            // Player specific orbital ring
            double angle = (System.currentTimeMillis() / 5.0) % 360;
            int ox = (int)(cx + Math.cos(Math.toRadians(angle)) * (size/2 + 5));
            int oy = (int)(cy + Math.sin(Math.toRadians(angle)) * (size/2 + 5));
            g2.setColor(ACCENT_NEON);
            g2.fillOval(ox - 3, oy - 3, 6, 6);
        }
    }

    private void drawExploration(Graphics2D g2) {
            setBackground(BG_DARK);
            int w = getWidth(), h = getHeight();

            int dim = (int) Math.sqrt(map.currentArea);
            int floorIdx = map.currentFloor - 1;
            double tile = 64.0;
            double camX = w/2.0 - (drawPosCol + 0.5) * tile;
            double camY = h/2.0 + (drawPosRow - 0.5) * tile;

            for (int r = 0; r < dim; r++) {
                for (int c = 0; c < dim; c++) {
                    int dx = (int)(camX + c * tile);
                    int dy = (int)(camY - r * tile);
                    
                    boolean discovered = false;
                    map.FloorType type = map.floorTypes[floorIdx];

                    // Radar Passive: Clear vision of nearby tiles regardless of fog/shifter
                    if ("1".equals(editor.stats[25])) {
                        double distToPlayer = Math.sqrt(Math.pow(r - drawPosRow, 2) + Math.pow(c - drawPosCol, 2));
                        if (distToPlayer < 5.0) discovered = true;
                    }
                    
                    if (!discovered) {
                        if (type == map.FloorType.NORMAL) {
                            discovered = map.discovered[floorIdx][r][c] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry);
                        } else if (type == map.FloorType.FOG) {
                            discovered = map.discovered[floorIdx][r][c] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry);
                        } else if (type == map.FloorType.SHIFTER) {
                            // Maze Shifter: Fog that flickers open every few seconds
                            boolean flicker = (System.currentTimeMillis() / 2000) % 4 == 0; // Show for 500ms every 2s
                            discovered = flicker || map.discovered[floorIdx][r][c] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry);
                        }
                    }
                    
                    if (discovered) {
                        if (config.lowGraphics) {
                            g2.setColor(map.isWall[floorIdx][r][c] ? new Color(25, 25, 30) : new Color(15, 15, 18));
                            g2.fillRect(dx, dy, (int)tile, (int)tile);
                        } else {
                            // Dynamic Lighting Calculation
                            double dist = Math.sqrt(Math.pow(r - drawPosRow, 2) + Math.pow(c - drawPosCol, 2));
                            float light = (float)Math.max(0.1, 1.0 - (dist / 8.0));
                            
                            if (map.isWall[floorIdx][r][c]) {
                                g2.setColor(lerpColor(new Color(5, 5, 7), new Color(25, 25, 30), light));
                                g2.fillRect(dx, dy, (int)tile, (int)tile);
                            } else {
                                g2.setColor(lerpColor(new Color(2, 2, 4), new Color(15, 15, 18), light));
                                g2.fillRect(dx, dy, (int)tile, (int)tile);
                            }
                        }
                    } else {
                        g2.setColor(new Color(5, 5, 7));
                        g2.fillRect(dx, dy, (int)tile, (int)tile);
                    }
                }
            }

            // Draw Afterimages (Dash/Blink Trails)
            for (AfterImage ai : afterImages) {
                int ax = (int)(camX + ai.y * tile + 16);
                int ay = (int)(camY - ai.x * tile + 16);
                g2.setColor(new Color(ACCENT_NEON.getRed(), ACCENT_NEON.getGreen(), ACCENT_NEON.getBlue(), (int)(150 * (ai.life / 15.0))));
                g2.drawRect(ax, ay, 32, 32);
                g2.fillRect(ax + 8, ay + 8, 16, 16);
            }

            // Draw Projectiles
            g2.setStroke(new BasicStroke(3));
            for (ProjectileSystem.Projectile p : ProjectileSystem.projectiles) {
                int px = (int)(camX + p.y * tile + 32);
                int py = (int)(camY - p.x * tile + 32);
                g2.setColor(p.isEnemy ? Color.YELLOW : ACCENT_NEON);
                g2.drawLine(px, py, (int)(px - p.vy * 10), (int)(py + p.vx * 10));
            }

            // Draw Particles
            for (ParticleSystem.Particle p : ParticleSystem.particles) {
                int px = (int)(camX + p.y * tile + 32);
                int py = (int)(camY - p.x * tile + 32);
                g2.setColor(p.col);
                g2.fillRect(px, py, (int)p.size, (int)p.size);
            }

            // Draw Floating Text
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            for (FloatingText ft : floatingTexts) {
                int fx = (int)(camX + ft.y * tile + 32);
                int fy = (int)(camY - ft.x * tile);
                g2.setColor(new Color(ft.col.getRed(), ft.col.getGreen(), ft.col.getBlue(), (int)(255 * (ft.life / 40.0))));
                g2.drawString(ft.text, fx, fy);
            }

            // EMP Pulse Visual Overlay (Localized)
            if (stunActive && System.currentTimeMillis() < stunExpiry) {
                int px = w/2, py = h/2;
                int rad = (int)(4.0 * tile);
                g2.setColor(new Color(0, 200, 255, 30));
                g2.fillOval(px - rad, py - rad, rad * 2, rad * 2);
                g2.setColor(new Color(0, 200, 255, 100));
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(px - rad, py - rad, rad * 2, rad * 2);

                // Static pulses
                g2.setColor(new Color(255, 255, 255, 20));
                for(int i=0; i<5; i++) {
                    int rrad = (int)(Math.random() * rad);
                    g2.drawOval(px - rrad, py - rrad, rrad * 2, rrad * 2);
                }
            }

            // Draw Path (Pathfinder)
            if (currentPath != null && System.currentTimeMillis() < pathExpiry) {
                g2.setColor(map.keyFound ? new Color(0, 255, 200, 180) : new Color(255, 255, 0, 180));
                for (int[] p : currentPath) {
                    if (map.discovered[floorIdx][p[0]][p[1]] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry)) {
                        int px = (int)(camX + p[1] * tile + 24);
                        int py = (int)(camY - p[0] * tile + 24);
                        g2.fillOval(px, py, 16, 16);
                    }
                }
            }

            // Draw Enemies (With High Quality Procedural Rendering & Radar Logic)
            boolean hasRadarModule = "1".equals(editor.stats[25]);
            boolean isRadarActive = radarActive && System.currentTimeMillis() < radarExpiry;
            
            for (int j = 0; j < map.rooms[floorIdx]; j++) {
                int r = map.roomLocation[0][floorIdx][j][0], c = map.roomLocation[0][floorIdx][j][1];
                if (r != -1) {
                    int pr = (int)play.playerVector[0], pc = (int)play.playerVector[1];
                    boolean inLoS = hasLineOfSight(pr, pc, r, c);
                    boolean discovered = map.discovered[floorIdx][r][c] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry);
                    
                    int rx = (int)(camX + c * tile);
                    int ry = (int)(camY - r * tile);
                    
                    // Visibility condition: (Radar Ability Active) OR (Tile Discovered AND Line of Sight Clear)
                    if (isRadarActive || (discovered && inLoS)) {
                        Color enemyCol;
                        if (hasRadarModule) {
                            boolean isSniper = (j % 3 == 0 && !isBoss);
                            enemyCol = j == mobs.keyMobs[floorIdx]-1 ? Color.MAGENTA : (isSniper ? Color.YELLOW : DANGER_RED);
                        } else {
                            enemyCol = DANGER_RED; // Generic ID for non-radar units
                        }
                        drawProceduralRobot(g2, rx + 16, ry + 16, 32, enemyCol, false);
                    }
                    
                    // Radar Glow (Reveals enemies even in smoke/walls)
                    if (isRadarActive) {
                        g2.setColor(ACCENT_NEON);
                        g2.setStroke(new BasicStroke(2));
                        g2.drawOval(rx + 4, ry + 4, 56, 56);
                        
                        // Targeted tracking line from player to enemy
                        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
                        g2.drawLine(w/2, h/2, rx + 32, ry + 32);
                    }
                }
            }

            // Draw Canisters
            for (int j = 0; j < map.canisterCount[floorIdx]; j++) {
                int r = map.roomLocation[3][floorIdx][j][0], c = map.roomLocation[3][floorIdx][j][1];
                if (r != -1 && (map.discovered[floorIdx][r][c] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry))) {
                    int rx = (int)(camX + c * tile);
                    int ry = (int)(camY - r * tile);
                    g2.setColor(new Color(0, 150, 255)); // Plasma Blue
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(rx + 20, ry + 20, 24, 24);
                    g2.fillRect(rx + 26, ry + 26, 12, 12);
                }
            }

            for (int j = 0; j < map.chestCount[floorIdx]; j++) {
                int r = map.roomLocation[1][floorIdx][j][0], c = map.roomLocation[1][floorIdx][j][1];
                if (r != -1 && (map.discovered[floorIdx][r][c] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry))) {
                    g2.setColor(Color.ORANGE);
                    g2.drawRect((int)(camX+c*tile+20), (int)(camY-r*tile+20), 24, 24);
                }
            }
            int er = map.exitPos[floorIdx][0], ec = map.exitPos[floorIdx][1];
            if (map.discovered[floorIdx][er][ec] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry)) {
                g2.setColor(map.keyFound ? ACCENT_NEON : Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(3));
                g2.drawRect((int)(camX+ec*tile+10), (int)(camY-er*tile+10), (int)tile-20, (int)tile-20);
            }

            drawProceduralRobot(g2, w/2-16, h/2-16, 32, ACCENT_NEON, true);
            
            drawHUD(g2);
        }

        private void drawBattle(Graphics2D g2) {
            setBackground(BG_DARK);
            int w = getWidth(), h = getHeight();

            // Background HUD elements for Combat
            g2.setColor(new Color(255, 0, 0, 10));
            g2.fillRect(0, 0, w, h);

            // Enemy Visual Frame
            g2.setColor(isBoss ? BOSS_PURPLE : DANGER_RED);
            g2.setStroke(new BasicStroke(isBoss ? 6 : 4));
            int ex = w - 280, ey = 80, ew = 180, eh = 180;
            g2.drawRect(ex, ey, ew, eh);
            
            // Decorative lines for enemy frame
            g2.setStroke(new BasicStroke(1));
            for(int i=0; i<ew; i+=20) g2.drawLine(ex+i, ey, ex+i, ey+eh);
            
            if (isBoss) {
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(ex, ey, ex+ew, ey+eh);
                g2.drawLine(ex, ey+eh, ex+ew, ey);
            }
            
            // High Quality Enemy in Combat
            drawProceduralRobot(g2, ex + ew/2 - 40, ey + eh/2 - 40, 80, isBoss ? BOSS_PURPLE : DANGER_RED, false);

            // Player Visual Frame (Bottom Left)
            g2.setColor(ACCENT_NEON);
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(80, h-350, 160, 160);
            
            // Radar-like circles for player
            g2.setStroke(new BasicStroke(1));
            g2.drawOval(70, h-360, 180, 180);
            
            // High Quality Player in Combat
            drawProceduralRobot(g2, 80 + 80 - 40, h - 350 + 80 - 40, 80, ACCENT_NEON, true);

            drawDigitalHP(g2, 50, 50, "ID: " + enemyType, enemyHP, enemyMaxHP, isBoss ? BOSS_PURPLE : DANGER_RED);
            drawDigitalHP(g2, w-350, h-250, "ID: OVERDRIVE_CORE", currentHealth, maxHealth, ACCENT_NEON);

            // Command Console Frame
            g2.setColor(new Color(15, 20, 25, 240));
            g2.fillRect(20, h-120, w-40, 100);
            g2.setColor(ACCENT_NEON);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(20, h-120, w-40, 100);
            
            // Console Header
            g2.setColor(new Color(ACCENT_NEON.getRed(), ACCENT_NEON.getGreen(), ACCENT_NEON.getBlue(), 50));
            g2.fillRect(20, h-120, w-40, 25);
            g2.setColor(ACCENT_NEON);
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.drawString("COMBAT_INTERFACE_v4.2", 40, h-103);
            
            g2.setFont(new Font("Monospaced", Font.BOLD, 18));
            g2.drawString("> " + systemMessage, 40, h-65);

            if (!inMoveMenu) {
                String[] m = {"[ENGAGE]", "[SYSTEMS]", "[RETREAT]"};
                for (int i=0; i<3; i++) {
                    boolean active = combatMenuIndex == i;
                    g2.setColor(active ? ACCENT_NEON : Color.GRAY);
                    if (active) g2.fillRect(50 + (i*160), h-40, 120, 2);
                    g2.drawString(m[i], 50 + (i*160), h-30);
                }
            } else {
                g2.setColor(new Color(10, 15, 20, 250));
                g2.fillRect(w-320, h-110, 300, 95);
                g2.setColor(ACCENT_NEON);
                g2.drawRect(w-320, h-110, 300, 95);
                
                String[] ms = {"STRIKE", "OVERDRIVE", "REBOOT", "BACK"};
                for (int i=0; i<4; i++) {
                    boolean active = moveMenuIndex == i;
                    g2.setColor(active ? ACCENT_NEON : Color.GRAY);
                    int mx = w-280 + (i%2*140);
                    int my = h-70 + (i/2*35);
                    g2.drawString(ms[i], mx, my);
                    if (i < 3) {
                        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                        g2.drawString("COST: " + editor.stats[19+(i*2)] + " ST", mx, my+12);
                        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
                    }
                }
            }
        }

        private void drawDigitalHP(Graphics2D g2, int x, int y, String name, int cur, int max, Color col) {
            g2.setColor(new Color(10, 15, 20, 200));
            g2.fillRoundRect(x, y, 320, 80, 5, 5);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 100));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(x, y, 320, 80, 5, 5);
            
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString(name, x+15, y+25);
            
            // Bar segments
            for (int i=0; i<20; i++) {
                if ((double)cur/max >= (i+1)/20.0) {
                    g2.setColor(col);
                    g2.fillRect(x+15+(i*14), y+35, 10, 15);
                } else {
                    g2.setColor(new Color(40, 40, 45));
                    g2.fillRect(x+15+(i*14), y+35, 10, 15);
                }
            }
            
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(col);
            g2.drawString("INTEGRITY_INDEX: " + cur + "/" + max, x+15, y+68);
        }

        private void drawHUD(Graphics2D g2) {
            int w = getWidth(), h = getHeight();
            
            // --- TOP LEFT: NEURAL LINK STATUS ---
            // Main Frame
            g2.setColor(new Color(15, 20, 25, 220));
            g2.fillRoundRect(20, 20, 320, 110, 10, 10);
            g2.setColor(new Color(ACCENT_NEON.getRed(), ACCENT_NEON.getGreen(), ACCENT_NEON.getBlue(), 100));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(20, 20, 320, 110, 10, 10);
            
            // Corner Accents
            g2.setColor(ACCENT_NEON);
            // Top Left
            g2.fillRect(20, 20, 20, 4);
            g2.fillRect(20, 20, 4, 20);
            // Top Right
            g2.fillRect(320, 20, 20, 4);
            g2.fillRect(336, 20, 4, 20);
            // Bottom Left
            g2.fillRect(20, 126, 20, 4);
            g2.fillRect(20, 110, 4, 20);
            // Bottom Right
            g2.fillRect(320, 126, 20, 4);
            g2.fillRect(336, 110, 4, 20);

            // Animated Integrity (Health) Bar
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("INTEGRITY_CORE", 40, 45);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(ACCENT_NEON);
            g2.drawString(currentHealth + "/" + maxHealth, 250, 45);

            g2.setColor(new Color(40, 45, 50));
            g2.fillRect(40, 55, 280, 15);
            
            float hpRatio = (float)currentHealth / maxHealth;
            Color hpCol = hpRatio > 0.4 ? ACCENT_NEON : (hpRatio > 0.15 ? Color.ORANGE : DANGER_RED);
            GradientPaint hpGrad = new GradientPaint(40, 0, hpCol, 320, 0, hpCol.darker());
            g2.setPaint(hpGrad);
            g2.fillRect(40, 55, (int)(280 * hpRatio), 15);
            
            // Health Bar Grid Overlay
            g2.setColor(new Color(0, 0, 0, 100));
            for(int i=0; i<10; i++) g2.drawLine(40 + (i*28), 55, 40 + (i*28), 70);

            // Stamina Bar
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("STAMINA_LINK", 40, 90);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(new Color(0, 150, 255));
            g2.drawString((int)stamina + "%", 280, 90);

            g2.setColor(new Color(40, 45, 50));
            g2.fillRect(40, 100, 280, 8);
            GradientPaint stGrad = new GradientPaint(40, 0, new Color(0, 150, 255), 320, 0, new Color(0, 50, 150));
            g2.setPaint(stGrad);
            g2.fillRect(40, 100, (int)(280 * (stamina / 100.0)), 8);
            
            // --- COMBO METER ---
            if (combo > 0) {
                g2.setFont(new Font("Monospaced", Font.ITALIC | Font.BOLD, 28));
                g2.setColor(ACCENT_NEON);
                g2.drawString("X" + combo, 40, 160);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                g2.drawString("COMBO_MULT", 40, 175);
                
                g2.setColor(new Color(ACCENT_NEON.getRed(), ACCENT_NEON.getGreen(), ACCENT_NEON.getBlue(), 100));
                g2.fillRect(40, 180, (int)(100 * ((comboTimer - System.currentTimeMillis()) / 3000.0)), 4);
            }

            // --- BOTTOM LEFT: SYSTEM LOGS ---
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(new Color(ACCENT_NEON.getRed(), ACCENT_NEON.getGreen(), ACCENT_NEON.getBlue(), 180));
            
            // FPS Display (Repositioned)
            g2.drawString("> FPS: " + fps, 25, h - 45);

            if (System.currentTimeMillis() < messageExpiry) {
                g2.drawString("> " + systemMessage, 25, h - 30);
            } else {
                g2.drawString("> SYSTEM_IDLE", 25, h - 30);
            }

            // --- MODULE STATUS ICONS ---
            drawModuleIcon(g2, 350, 20, "HEAD", !editor.stats[25].equals("0"));
            drawModuleIcon(g2, 350, 55, "HAND", !editor.stats[26].equals("0"));
            drawModuleIcon(g2, 350, 90, "LEGS", !editor.stats[27].equals("0"));

            // MINI-MAP (Map Decompressor Upgrade)
            if (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry) {
                int mapX = w - 170, mapY = 100, mapSize = 150;
                int dim = (int) Math.sqrt(map.currentArea);
                int floorIdx = map.currentFloor - 1;
                double mTile = (double)mapSize / dim;

                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(mapX, mapY, mapSize, mapSize);
                g2.setColor(ACCENT_NEON);
                g2.drawRect(mapX, mapY, mapSize, mapSize);

                for (int r = 0; r < dim; r++) {
                    for (int c = 0; c < dim; c++) {
                        if (map.isWall[floorIdx][r][c]) {
                            g2.setColor(new Color(40, 45, 50));
                            g2.fillRect((int)(mapX + c * mTile), (int)(mapY + (dim-1-r) * mTile), (int)mTile + 1, (int)mTile + 1);
                        }
                    }
                }
                
                // Draw Exit
                int er = map.exitPos[floorIdx][0], ec = map.exitPos[floorIdx][1];
                if (map.discovered[floorIdx][er][ec] || (mapDecompressorActive && System.currentTimeMillis() < mapDecompressorExpiry)) {
                    g2.setColor(Color.MAGENTA);
                    g2.fillRect((int)(mapX + ec * mTile), (int)(mapY + (dim-1-er) * mTile), (int)mTile + 2, (int)mTile + 2);
                }

                // Draw Player on Mini-map
                g2.setColor(ACCENT_NEON);
                int pr = (int)play.playerVector[0], pc = (int)play.playerVector[1];
                g2.fillOval((int)(mapX + pc * mTile - 2), (int)(mapY + (dim-1-pr) * mTile - 2), 5, 5);
                
                g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                g2.drawString("MINI_MAP: ACTIVE", mapX, mapY - 5);
            }

            g2.setColor(ACCENT_NEON);
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2.drawString("SECTOR: " + map.currentFloor, w - 150, 40);
            
            // Modern Equipment HUD
            int eqX = w - 140;
            int eqY = h - 110;
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawString("EQUIPMENT", eqX, eqY - 20);
            
            drawModernGear(g2, eqX, eqY, "WPN", editor.stats[13], editor.stats[11], false);
            drawModernGear(g2, eqX, eqY + 15, "AMR", editor.stats[14], editor.stats[12], false);
            
            drawModernGear(g2, eqX, eqY + 35, "BKP_W", editor.stats[17], editor.stats[15], true);
            drawModernGear(g2, eqX, eqY + 45, "BKP_A", editor.stats[18], editor.stats[16], true);

            if (map.keyFound) {
                g2.setColor(new Color(255, 0, 255));
                g2.drawString("KEY_SIGNAL: LOCKED", w - 200, 70);
            }

            // System message box
            if (System.currentTimeMillis() < messageExpiry) {
                g2.setColor(new Color(0,0,0,150));
                g2.fillRect(w/2 - 200, h-100, 400, 35);
                g2.setColor(ACCENT_NEON);
                g2.drawRect(w/2 - 200, h-100, 400, 35);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(systemMessage);
                g2.drawString(systemMessage, w/2 - tw/2, h-77);
            }
        }

        private void drawModuleIcon(Graphics2D g2, int x, int y, String label, boolean active) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(x, y, 60, 30);
            g2.setColor(active ? ACCENT_NEON : Color.DARK_GRAY);
            g2.drawRect(x, y, 60, 30);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.drawString(label, x + 5, y + 18);
            if (active) {
                g2.fillRect(x + 50, y + 5, 5, 5); // Status light
            }
        }

        private void drawEnding(Graphics2D g2) {
            setBackground(Color.BLACK);
            int w = getWidth(), h = getHeight();
            g2.setColor(ACCENT_NEON);
            g2.setFont(new Font("Monospaced", Font.BOLD, 30));
            g2.drawString("CORE_ACCESS_GRANTED", w/2-150, 100);
            
            g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
            g2.drawString("CHOOSE THE FUTURE:", w/2-100, 250);
            
            g2.setColor(endingChoice == 0 ? ACCENT_NEON : Color.GRAY);
            g2.drawString("[A] SELF-DESTRUCT", w/2-250, 350);
            
            g2.setColor(endingChoice == 1 ? ACCENT_NEON : Color.GRAY);
            g2.drawString("[D] REMAIN_ACTIVE", w/2+50, 350);
        }
    }
}
