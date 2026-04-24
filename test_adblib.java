import com.tananaev.adblib.*;
import java.lang.reflect.Method;

public class test_adblib {
    public static void main(String[] args) {
        System.out.println("AdbBase64 methods:");
        for(Method m : AdbBase64.class.getDeclaredMethods()) {
            System.out.println(m);
        }
        System.out.println("AdbCrypto methods:");
        for(Method m : AdbCrypto.class.getDeclaredMethods()) {
            System.out.println(m);
        }
    }
}
