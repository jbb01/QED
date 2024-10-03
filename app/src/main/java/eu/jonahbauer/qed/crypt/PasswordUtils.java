package eu.jonahbauer.qed.crypt;

import androidx.annotation.NonNull;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@UtilityClass
public class PasswordUtils {
    private static final byte[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static byte[] encode(@NonNull Map<String, ?> entries) {
        byte[][] arrays = new byte[4 * entries.size() - 1][];

        var i = 0;
        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            arrays[i++] = encode(entry.getKey().toCharArray());
            arrays[i++] = new byte[] { '=' };
            if (entry.getValue() instanceof char[]) {
                arrays[i++] = encode((char[]) entry.getValue());
            } else if (entry.getValue() instanceof CharSequence) {
                arrays[i++] = encode(entry.getValue().toString().toCharArray());
            } else {
                throw new IllegalArgumentException("invalid value type: " + (entry.getValue() == null ? "null" : entry.getValue().getClass().getName()));
            }
            if (it.hasNext()) {
                arrays[i++] = new byte[] { '&' };
            }
        }

        return concat(arrays);
    }

    private static byte[] encode(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);

        byte[] out = new byte[getUrlEncodedLength(byteBuffer)];
        int i = 0;
        while (byteBuffer.hasRemaining()) {
            byte b = byteBuffer.get();
            if (isUnreserved(b)) {
                out[i++] = b;
            } else {
                out[i++] = '%';
                out[i++] = HEX[(b >> 4) & 0xF];
                out[i++] = HEX[b & 0xF];
            }
        }

        Arrays.fill(chars, (char) 0);
        wipe(byteBuffer.array());
        return out;
    }

    private static byte[] concat(byte[]... arrays) {
        int length = Arrays.stream(arrays).mapToInt(array -> array.length).sum();
        byte[] out = new byte[length];

        int pos = 0;
        for (var array : arrays) {
            System.arraycopy(array, 0, out, pos, array.length);
            pos += array.length;
            Arrays.fill(array, (byte) 0);
        }

        return out;
    }

    private static int getUrlEncodedLength(ByteBuffer buffer) {
        buffer.mark();
        int length = 0;
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (isUnreserved(b)) {
                length++;
            } else {
                length += 3;
            }
        }
        buffer.reset();
        return length;
    }

    private static boolean isUnreserved(byte b) {
        return '0' <= b && b <= '9' || 'a' <= b && b <= 'z' || 'A' <= b && b <= 'Z'
                || '-' == b || '.' == b || '_' == b || '~' == b;
    }

    public static void wipe(byte[] array) {
        Arrays.fill(array, (byte)0);
    }

    public static void wipe(char[] array) {
        Arrays.fill(array, '\u0000');
    }
}
