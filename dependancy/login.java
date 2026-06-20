package dependancy;

import java.util.Scanner;

class caller {}

public class login {

    public static String name, pass;
    public static boolean passCheck, isAdmin;

    public login() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter Username : ");
        name = sc.nextLine();
        System.out.print("Enter Password : ");
        pass = sc.nextLine();
        boolean checking[] = check();
        if (checking[0] && checking[1]) {
        } else if (checking[0]) {
            if (checking[1]) {
                //should exit from creating an user and log them in
            } else {
                //should throw an error
                System.exit(1);
            }
        }
    }

    public login(boolean bin) {}

    public boolean[] check() {
        boolean checking[] = { false, false };
        if (editor.userN == null) {
            return checking;
        }

        for (int i = 0; i < editor.userN.length; i++) {
            if (name.equals(editor.userN[i])) {
                System.out.println(editor.userN[i]);
                checking[0] = true;
                if (passCheck) {
                    checking[1] = true;
                    return checking;
                }
                return checking;
            }
        }
        return checking;
    }
}
