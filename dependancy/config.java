package dependancy;

import java.io.*;
import java.util.*;

public class config {
    public static int[] chestRarity = {50, 30, 15, 5};
    public static int[] maxChest = {5, 15, 25};
    public static int[] areaSizes = {625, 1600, 3600}; // 25x25, 40x40, 60x60
    public static double equiSpawn = 0.2;
    public static int baseFloorMin = 20;
    public static int baseFloorMax = 40;
    public static int baseMobCount = 3;
    public static boolean lowGraphics = false;
    
    // Keybinds (using KeyEvent codes)
    public static int keyHead = 82; // R
    public static int keyHand = 70; // F
    public static int keyLeg = 71;  // G

    private static final String FILE_PATH = "dependancy/configData.csv";
    private static encrypter en = new encrypter(true);

    public static void init() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            load();
        } else {
            save();
        }
    }

    public static void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {
            pw.println("chestRarity\t" + encryptArray(chestRarity));
            pw.println("maxChest\t" + encryptArray(maxChest));
            pw.println("areaSizes\t" + encryptArray(areaSizes));
            pw.println("equiSpawn\t" + en.retn(String.valueOf(equiSpawn), true)[0]);
            pw.println("baseFloorMin\t" + en.retn(String.valueOf(baseFloorMin), true)[0]);
            pw.println("baseFloorMax\t" + en.retn(String.valueOf(baseFloorMax), true)[0]);
            pw.println("baseMobCount\t" + en.retn(String.valueOf(baseMobCount), true)[0]);
            pw.println("lowGraphics\t" + en.retn(String.valueOf(lowGraphics), true)[0]);
            pw.println("keyHead\t" + en.retn(String.valueOf(keyHead), true)[0]);
            pw.println("keyHand\t" + en.retn(String.valueOf(keyHand), true)[0]);
            pw.println("keyLeg\t" + en.retn(String.valueOf(keyLeg), true)[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            editor ed = new editor();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 2) continue;
                String key = parts[0];
                String encryptedValue = parts[1];

                try {
                    if (key.equals("chestRarity")) chestRarity = decryptIntArray(encryptedValue, ed);
                    else if (key.equals("maxChest")) maxChest = decryptIntArray(encryptedValue, ed);
                    else if (key.equals("areaSizes")) areaSizes = decryptIntArray(encryptedValue, ed);
                    else if (key.equals("equiSpawn")) equiSpawn = Double.parseDouble(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("baseFloorMin")) baseFloorMin = Integer.parseInt(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("baseFloorMax")) baseFloorMax = Integer.parseInt(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("baseMobCount")) baseMobCount = Integer.parseInt(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("lowGraphics")) lowGraphics = Boolean.parseBoolean(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("keyHead")) keyHead = Integer.parseInt(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("keyHand")) keyHand = Integer.parseInt(ed.decrypter(new String[]{encryptedValue})[0]);
                    else if (key.equals("keyLeg")) keyLeg = Integer.parseInt(ed.decrypter(new String[]{encryptedValue})[0]);
                } catch (Exception e) {
                    System.out.println("Error loading config key: " + key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String encryptArray(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(en.retn(String.valueOf(arr[i]), true)[0]);
            if (i < arr.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    private static int[] decryptIntArray(String encrypted, editor ed) {
        String[] parts = encrypted.split(",");
        int[] result = new int[parts.length];
        String[] decrypted = ed.decrypter(parts);
        for (int i = 0; i < decrypted.length; i++) {
            result[i] = Integer.parseInt(decrypted[i]);
        }
        return result;
    }
}
