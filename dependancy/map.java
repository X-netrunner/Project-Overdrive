package dependancy;

import java.util.Random;
import dependancy.mobs;
import java.io.*;

public class map {
    public static long seed;
    public static Random rand;
    public static int floors;

    public static int rooms[];
    public static int chestCount[];
    public static int trapCount[];
    public static int canisterCount[];
    private double small = 0.1;
    private double large = 0.4 - small;
    public static String size;
    public static int freeRooms;
    private final int area[] = config.areaSizes.clone();
    public static boolean keyFound = false;
    public static int currentFloor = 1;
    public static int currentArea;
    public enum FloorType { NORMAL, FOG, SHIFTER }
    public static FloorType[] floorTypes;
    public static boolean isWall[][][]; 
    public static boolean discovered[][][];
    public static int roomLocation[][][][]; 
    public static int mobHP[][];
    public static int exitPos[][]; 

    public map() {
        this(new Random().nextLong());
    }

    public map(long seed) {
        map.seed = seed;
        map.rand = new Random(seed);
        map.floors = (int) (rand.nextDouble() * config.baseFloorMax + rand.nextDouble() * config.baseFloorMin);
        if (map.floors <= 0) map.floors = 10;
        
        Logger.log("Generating sector grid with seed: " + seed);

        size = "";
        double mapSize = rand.nextDouble();
        if (mapSize < large + small) {
            if (mapSize < small) size = "small";
            else size = "large";
        } else {
            size = "medium";
        }
        
        switch (size.charAt(0)) {
            case 's': currentArea = area[0]; floors += (int) (rand.nextDouble() * 5) + 2; break;
            case 'm': currentArea = area[1]; floors += (int) (rand.nextDouble() * 3) + 1; break;
            case 'l': currentArea = area[2]; floors += (int) (rand.nextDouble() * 1) + 0; break;
        }

        int dim = (int) Math.sqrt(currentArea);
        if (dim % 2 == 0) dim--; 
        currentArea = dim * dim;

        Logger.log("Sector size optimized: " + dim + "x" + dim + " | Total depth: " + floors + " floors.");

        mobs mob = new mobs();
        rooms = new int[floors];
        chestCount = new int[floors];
        trapCount = new int[floors];
        canisterCount = new int[floors];
        roomLocation = new int[4][floors][currentArea][2];
        mobHP = new int[floors][currentArea];
        exitPos = new int[floors][2];
        isWall = new boolean[floors][dim][dim];
        discovered = new boolean[floors][dim][dim];
        floorTypes = new FloorType[floors];

        for (int i = 0; i < floors; i++) {
            if ((i + 1) % 5 == 0) {
                floorTypes[i] = FloorType.FOG;
            } else {
                double typeProb = rand.nextDouble();
                if (typeProb < 0.3) floorTypes[i] = FloorType.FOG;
                else if (typeProb < 0.6) floorTypes[i] = FloorType.SHIFTER;
                else floorTypes[i] = FloorType.NORMAL;
            }

            generateMaze(i, dim);
            
            boolean exitPlaced = false;
            for (int x = dim - 2; x > 0; x--) {
                for (int y = dim - 2; y > 0; y--) {
                    if (!isWall[i][x][y]) {
                        exitPos[i][0] = x;
                        exitPos[i][1] = y;
                        exitPlaced = true;
                        break;
                    }
                }
                if (exitPlaced) break;
            }

            rooms[i] = mobs.mob[i];
            for (int j = 0; j < rooms[i]; j++) {
                spawnInMaze(0, i, j, dim);
                // Initialize Mob HP - Progressive Difficulty
                if ((i + 1) % 5 == 0) { // Boss Floor
                    // Progressive Boss HP: Floor 5: 175, Floor 10: 300, etc. (Buffed from 20+10*f)
                    mobHP[i][j] = 50 + ((i + 1) * 25);
                } else {
                    // Progressive Mob HP: Floor 1: 15, Floor 2: 25, etc. (Previously 35+8*i)
                    mobHP[i][j] = 10 + (i * 10);
                }
            }
            
            chestCount[i] = (int) (rand.nextDouble() * 3) + 1;
            for (int j = 0; j < chestCount[i]; j++) spawnInMaze(1, i, j, dim);
            
            trapCount[i] = (int) (rand.nextDouble() * 2);
            for (int j = 0; j < trapCount[i]; j++) spawnInMaze(2, i, j, dim);

            canisterCount[i] = (int) (rand.nextDouble() * 4) + 2;
            for (int j = 0; j < canisterCount[i]; j++) spawnInMaze(3, i, j, dim);
        }
        freeRooms = currentArea - rooms.length;
    }

    private void generateMaze(int floor, int dim) {
        for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
                isWall[floor][x][y] = true;
            }
        }
        carve(floor, 1, 1, dim);
    }

    private void carve(int floor, int x, int y, int dim) {
        isWall[floor][x][y] = false;
        Integer[] dirs = {0, 1, 2, 3};
        java.util.Collections.shuffle(java.util.Arrays.asList(dirs), rand);
        
        for (int dir : dirs) {
            int nx = x, ny = y, fx = x, fy = y;
            if (dir == 0) { nx = x; ny = y - 2; fx = x; fy = y - 1; }
            else if (dir == 1) { nx = x; ny = y + 2; fx = x; fy = y + 1; }
            else if (dir == 2) { nx = x - 2; ny = y; fx = x - 1; fy = y; }
            else if (dir == 3) { nx = x + 2; ny = y; fx = x + 1; fy = y; }
            
            if (nx > 0 && nx < dim - 1 && ny > 0 && ny < dim - 1 && isWall[floor][nx][ny]) {
                isWall[floor][fx][fy] = false;
                carve(floor, nx, ny, dim);
            }
        }
    }

    private void spawnInMaze(int type, int floor, int idx, int dim) {
        int attempts = 0;
        while (attempts < 1000) {
            int rx = (int)(rand.nextDouble() * (dim - 2)) + 1;
            int ry = (int)(rand.nextDouble() * (dim - 2)) + 1;
            if (!isWall[floor][rx][ry] && (rx != 1 || ry != 1)) {
                roomLocation[type][floor][idx][0] = rx;
                roomLocation[type][floor][idx][1] = ry;
                return;
            }
            attempts++;
        }
    }

    public static void exportLocations() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("map_locations.txt"))) {
            pw.println("--- SECTOR MAPPING DATA ---");
            pw.println("Seed: " + seed);
            pw.println("Floors: " + floors);
            for (int i = 0; i < floors; i++) {
                pw.println("FLOOR_" + (i + 1));
                pw.print("  CONSTRUCTS: ");
                for (int j = 0; j < rooms[i]; j++) pw.print("[" + roomLocation[0][i][j][0] + "," + roomLocation[0][i][j][1] + "] ");
                pw.println("\n  ELEVATOR: [" + exitPos[i][0] + "," + exitPos[i][1] + "]");
                pw.println("----------------------------------");
            }
            Logger.log("Sector mapping exported to local buffer.");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
