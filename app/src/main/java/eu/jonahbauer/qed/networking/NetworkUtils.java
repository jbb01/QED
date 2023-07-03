package eu.jonahbauer.qed.networking;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;


@UtilityClass
public class NetworkUtils {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The same as {@code InputStream#readAllByte()} introduced in Java 9.
     */
    @NonNull
    public static byte[] readAllBytes(@NonNull InputStream in) throws IOException {
        return readNBytes(in, Integer.MAX_VALUE);
    }

    /**
     * The same as {@code InputStream#readNBytes(int len)} introduced in Java 9.
     */
    @NonNull
    public static byte[] readNBytes(@NonNull InputStream in, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = in.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                    result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

    /**
     * Reads the {@code InputStream} of the given connection to a string.
     */
    @NonNull
    public static String readPage(@NonNull HttpURLConnection connection) throws IOException {
        try (InputStream in = connection.getInputStream()) {
            return new String(readAllBytes(in), StandardCharsets.UTF_8);
        }
    }


    public static boolean isLoginError(Feature feature, HttpURLConnection connection) {
        String location = connection.getHeaderField("Location");

        switch (feature) {
            case CHAT:
                Map<String, List<String>> headers = connection.getHeaderFields();
                if (headers.containsKey("Set-Cookie")) {
                    List<String> setCookie = headers.get("Set-Cookie");
                    if (setCookie != null) for (String s : setCookie) {
                        if (s.contains("userid=;") || s.contains("pwhash=;")) {
                            return true;
                        }
                    }
                }
            case GALLERY:
                return location != null && location.startsWith("account");
            case DATABASE:
                return location != null && location.contains("login");
            default:
                throw new IllegalArgumentException("unknown feature " + feature);
        }
    }
}
