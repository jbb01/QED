package eu.jonahbauer.qed.crypt;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PasswordUtils {
    public static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }

    public static char[] toChars(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
        char[] chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        Arrays.fill(charBuffer.array(), '\u0000');
        return chars;
    }

    public static byte[] combine(byte[] bytes1, byte[] bytes2) {
        byte[] out = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, out, 0, bytes1.length);
        System.arraycopy(bytes2, 0, out, bytes1.length, bytes2.length);
        return out;
    }

    public static void wipe(byte[] array) {
        Arrays.fill(array, (byte)0);
    }

    public static void wipe(char[] array) {
        Arrays.fill(array, '\u0000');
    }
}
