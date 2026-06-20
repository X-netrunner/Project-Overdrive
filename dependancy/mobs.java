package dependancy;

import dependancy.map;

public class mobs {

    //   public static String mobVector[][]; // Optimized to a 2D array: [FloorIndex][MobIndex] = "X.Y"
    public static int trapMobs;
    public static int mob[];
    public static int totalMobs;
    public static int keyMobs[];
    public static int minorBoss = 0;
    public static int MajorBoss = 0;
    double equiSpawn = config.equiSpawn;

    private void keyMobs() {
        for (int i = 0; i < mob.length; i++) {
            if (mob[i] > 0) {
                keyMobs[i] = (int) (map.rand.nextDouble() * mob[i]) + 1;
            } else {
                keyMobs[i] = 0;
            }
        }
    }

    public mobs() {
        int temp1 = map.floors / 10;
        int temp2 = map.floors % 10;
        if (temp2 >= 5) {
            minorBoss = 1;
        }
        if (temp1 >= 1) {
            minorBoss += temp1 * 1;
            MajorBoss += temp1;
        }
        mob = new int[map.floors];
        keyMobs = new int[map.floors];
        
        for (int j = 0; j < mob.length; j++) {
            int floorNum = j + 1;
            if (floorNum % 5 == 0) {
                mob[j] = 1; // Boss unit
            } else {
                // Progressive scaling: Base from config, increases every 2 floors
                int baseScaling = config.baseMobCount + (floorNum / 2);
                // Cap at area size / 8 to ensure the map isn't completely filled
                int cap = Math.max(config.baseMobCount + 1, map.currentArea / 8);
                mob[j] = Math.min(cap, baseScaling + (int)(map.rand.nextDouble() * 2));
            }
        }
        keyMobs();

        System.out.println("--- Key Mobs per Floor ---");
        for (int a : keyMobs) {
            System.out.println(a);
        }
        System.out.println("--- Total Mobs per Floor ---");
        for (int a : mob) {
            System.out.println(a);
        }
        /*
        int mapSize = map.currentArea;
        int dim = (int) Math.sqrt(mapSize);

        // Fixed allocation: [Floor][Total Max Mobs Possible]
        mobVector = new String[map.floors][totalMobs];

        for (int i = 0; i < map.floors; i++) {
            String[] hotFix = new String[mob[i]];
            int filledCount = 0;

            for (int j = 0; j < mob[i][0]; j++) {
                while (true) {
                    String temp =
                        (int) (map.rand.nextDouble() * dim) +
                        1 +
                        "." +
                        (int) ((map.rand.nextDouble() * dim) + 1);

                    boolean fixFlag = false;
                    for (int k = 0; k < filledCount; k++) {
                        if (hotFix[k] != null && hotFix[k].equals(temp)) {
                            fixFlag = true;
                            break;
                        }
                    }

                    if (fixFlag) continue;

                    mobVector[i][j] = temp;
                    hotFix[filledCount] = temp;
                    filledCount++;
                    break;
                }
            }
        }

        System.out.println("--- Mob Vector Coordinates ---");
        for (String[] floor : mobVector) {
            System.out.println("new floor");
            for (String mobPos : floor) {
                if (mobPos != null) {
                    System.out.println(mobPos);
                }
            }
            }*/
    }
}
