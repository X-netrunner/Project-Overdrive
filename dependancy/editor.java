package dependancy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class editor {

    public static String userN[];
    public static String stats[] = new String[33];
    public static String order[] = {
        "l", //lvl
        "a", //atk
        "d", //def
        "u", //luck
        "h", //hp
        "e", //exp
        "p", //points
        "s", //seed
        "f", //floor
        "x", //x coord
        "y", // y coord
        "w",
        "r",
        "wn",
        "an",
        "iw",
        "ia",
        "iwn",
        "ian",
        "m1c",
        "m1m",
        "m2c",
        "m2m",
        "m3c",
        "m3m",
        "h_up", // head upg
        "ha_up", // hand upg
        "l_up", //leg upg
        "kf",
        "mh",
        "uh",
        "uha",
        "ul",
    };
    encrypter en = new encrypter(true);

    public editor() {}

    public editor(String[] data, boolean write, boolean signup) {
        String file = "dependancy/playerData.csv";
        if (signup == true && write == true) {
            try (
                FileWriter writer = new FileWriter(file, true);
                PrintWriter pw = new PrintWriter(writer)
            ) {
                StringBuilder sb = new StringBuilder();
                // Default values for 33 slots
                String[] defs = {
                    "1",
                    "10",
                    "5",
                    "5",
                    "100",
                    "0",
                    "0",
                    "0",
                    "1",
                    "1",
                    "1",
                    "0",
                    "0",
                    "Empty",
                    "Empty",
                    "0",
                    "0",
                    "Empty",
                    "Empty",
                    "20",
                    "20",
                    "5",
                    "5",
                    "10",
                    "10",
                    "0",
                    "0",
                    "0",
                    "0",
                    "100",
                    "0",
                    "0",
                    "0",
                };
                for (int i = 0; i < order.length; i++) {
                    String val = (i < defs.length) ? defs[i] : "0";
                    String temp[] = en.retn(val + "_" + order[i], true);
                    sb.append(temp[0]).append("\t");
                }
                pw.println(
                    "Player : " + data[0] + "\t" + data[1] + "," + sb.toString()
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (signup == true && write == false) {
            if (login.name.equals("admin") && login.pass.equals("admin")) {
                login.passCheck = true;
                login.isAdmin = true;
                return;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                login.passCheck = false;
                while ((line = br.readLine()) != null) {
                    String[] d = unencryptData(line, signup);
                    if (login.name.equals(d[0])) {
                        if (login.pass.equals(d[1])) {
                            login.passCheck = true;
                        } else {
                            System.out.println("Error: Incorrect password.");
                            System.exit(1);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (signup == false && write == false) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] loginData = unencryptData(line, true);
                    if (login.name.equals(loginData[0])) {
                        String[] parts = line.split(",");
                        if (parts.length > 1) {
                            stats = unencryptData(parts[1], false);
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (signup == false && write == true) {
            String tempFile = "dependancy/playerData.tmp";
            try (
                BufferedReader br = new BufferedReader(new FileReader(file));
                PrintWriter pw = new PrintWriter(new FileWriter(tempFile))
            ) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] loginData = unencryptData(line, true);
                    if (login.name.equals(loginData[0])) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < order.length; i++) {
                            String val = (stats[i] == null ||
                                stats[i].isEmpty())
                                ? "0"
                                : stats[i];
                            String encrypted[] = en.retn(
                                val + "_" + order[i],
                                true
                            );
                            sb.append(encrypted[0]).append("\t");
                        }
                        String[] credentials = line.split(",")[0].split("\t");
                        pw.println(
                            credentials[0] +
                                "\t" +
                                credentials[1] +
                                "," +
                                sb.toString()
                        );
                    } else {
                        pw.println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            new java.io.File(tempFile).renameTo(new java.io.File(file));
        }
    }

    private String[] unencryptData(String data, boolean signup) {
        if (signup == true) {
            String preFilter[] = data.split(",");
            preFilter = preFilter[0].split("\t");
            preFilter[0] = preFilter[0].replace("Player : ", "");
            return decrypter(preFilter);
        } else {
            String finalData[] = new String[order.length];
            for (int i = 0; i < order.length; i++) finalData[i] = "0";

            String encryptedStats[] = data.split("\t");
            String decryptedFilter[] = decrypter(encryptedStats);

            for (int i = 0; i < decryptedFilter.length; i++) {
                String temp = decryptedFilter[i];
                boolean found = false;

                if (i < order.length) {
                    String suffix = "_" + order[i];
                    if (temp.endsWith(suffix)) {
                        finalData[i] = temp.substring(
                            0,
                            temp.length() - suffix.length()
                        );
                        found = true;
                    }
                }

                if (!found) {
                    for (int j = 0; j < order.length; j++) {
                        String suffix = "_" + order[j];
                        if (temp.endsWith(suffix)) {
                            finalData[j] = temp.substring(
                                0,
                                temp.length() - suffix.length()
                            );
                            found = true;
                            break;
                        }
                    }
                }
            }
            return finalData;
        }
    }

    public String[] decrypter(String[] preFilter) {
        String finalData[] = new String[preFilter.length];
        if (preFilter.length == 0) return finalData;

        String key[] = new String[preFilter.length];
        String[] workingCopy = preFilter.clone();
        for (int i = 0; i < workingCopy.length; i++) {
            int lastIdx = workingCopy[i].lastIndexOf("_");
            if (lastIdx != -1) {
                key[i] = workingCopy[i].substring(lastIdx + 1);
                workingCopy[i] = workingCopy[i].substring(0, lastIdx);
            } else {
                key[i] = "1";
            }
        }

        double keyk = 1.0;
        try {
            keyk = Double.parseDouble(key[0]);
        } catch (Exception e) {}

        for (int i = 0; i < workingCopy.length; i++) {
            String temp[] = workingCopy[i].split("_");
            StringBuilder word = new StringBuilder();
            for (String a : temp) {
                if (a.isEmpty()) continue;
                try {
                    double b = Double.parseDouble(a);
                    word.append((char) Math.round(b / keyk));
                } catch (Exception e) {}
            }
            finalData[i] = word.toString();
        }
        return finalData;
    }
}
