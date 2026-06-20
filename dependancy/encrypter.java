package dependancy;

public class encrypter extends login {

    public encrypter() { super(); }

    public encrypter(boolean bin) {
        super(bin);
    }

    final String data[] = { "0", "0" };
    boolean check;
    String temp = "";
    double ran = (int) (Math.random() * 1000);

    private void name() {
        char arr[] = super.name.toCharArray();
        for (char i : arr) {
            temp = temp + "_" + (int) i * ran;
        }
        data[0] = temp + "_" + ran;
        temp = "";
    }

    private void pass() {
        char arr[] = super.pass.toCharArray();
        for (char i : arr) {
            temp = temp + "_" + (int) i * ran;
        }
        data[1] = temp + "_" + ran;
        temp = "";
    }

    private void general(String value) {
        char arr[] = value.toCharArray();
        for (char i : arr) {
            temp = temp + "_" + (int) i * ran;
        }
        data[0] = temp + "_" + ran;
        temp = "";
    }

    public String[] retn(String value, Boolean Fvalue) {
        if (Fvalue) {
            general(value);
            return data;
        } else {
            name();
            pass();
            return data;
        }
    }
}
