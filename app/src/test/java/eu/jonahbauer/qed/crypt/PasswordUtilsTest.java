package eu.jonahbauer.qed.crypt;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class PasswordUtilsTest {

    @Test
    public void testEncode() {
        Assert.assertArrayEquals(
                "baz=quack&foo=bar".getBytes(StandardCharsets.UTF_8),
                PasswordUtils.encode(new TreeMap<>(Map.of(
                        "foo", "bar",
                        "baz", "quack"
                )))
        );

        Assert.assertArrayEquals(
                "baz=quack&foo=bar".getBytes(StandardCharsets.UTF_8),
                PasswordUtils.encode(new TreeMap<>(Map.of(
                        "foo", "bar".toCharArray(),
                        "baz", "quack".toCharArray()
                )))
        );

        Assert.assertArrayEquals(
                "baz%25=qu%21ack&f%26oo=b%3Dar".getBytes(StandardCharsets.UTF_8),
                PasswordUtils.encode(new TreeMap<>(Map.of(
                        "f&oo", "b=ar".toCharArray(),
                        "baz%", "qu!ack".toCharArray()
                )))
        );
    }
}