package dependancy;

public class Logger {
    public static void log(String msg) {
        System.out.println("\033[0;36m[SYSTEM_LOG]\033[0m " + msg);
    }
    
    public static void important(String msg) {
        System.out.println("\033[1;33m[IMPORTANT]\033[0m " + msg);
    }

    public static void combat(String msg) {
        System.out.println("\033[0;31m[COMBAT_DATA]\033[0m " + msg);
    }
}
