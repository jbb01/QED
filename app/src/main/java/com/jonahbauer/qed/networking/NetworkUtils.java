package com.jonahbauer.qed.networking;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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

    /**
     * Advances the given reader until after the first occurrence of the given pattern.
     * @param reader a string reader
     * @param pattern a pattern to search for
     * @return true if the pattern was found, false if not. When false is returned the reader will
     *         be at its end.
     * @throws IOException If an I/O error occurs
     */
    public static boolean readUntilAfter(StringReader reader, String pattern) throws IOException {
        char[] patternArray = pattern.toCharArray();
        int length = pattern.length();
        int index = 0;

        int chr;
        while ((chr = reader.read()) != -1) {
            if (chr == patternArray[index]) {
                index++;
                if (index == length) return true;
            } else {
                index = 0;
            }
        }

        return false;
    }

    /**
     * Same as {@link #readUntilAfter(StringReader, String)} but appends everything until before
     * the search pattern to the {@code StringBuilder} provided.
     */
    public static boolean readUntilAfter(StringReader reader, StringBuilder builder, String pattern) throws IOException {
        char[] patternArray = pattern.toCharArray();
        int length = pattern.length();
        int index = 0;

        int chr;
        while ((chr = reader.read()) != -1) {
            builder.appendCodePoint(chr);

            if (chr == patternArray[index]) {
                index++;
                if (index == length) {
                    builder.setLength(builder.length() - length);
                    return true;
                }
            } else {
                index = 0;
            }
        }

        return false;
    }

    /**
     * Reads an integer from the given {@code StringReader}. After this method the reader will be
     * positioned on after the last digit. This means that even if there is no number the reader will
     * advance one step.
     *
     * @return the number read, 0 if none was found
     */
    public static int readInt(StringReader reader) throws IOException {
        int out = 0;

        int chr;
        while ((chr = reader.read()) != -1) {
            if ('0' <= chr && chr <= '9') {
                out = out * 10 + (chr - '0');
            } else {
                return out;
            }
        }

        return out;
    }
}
