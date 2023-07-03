package eu.jonahbauer.qed.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(Enclosed.class)
public class Preferences$ConfigTest {


    @RunWith(Parameterized.class)
    public static class ListSerializerTest {

        private final Preferences$Config.LinkedSetSerializer serializer = new Preferences$Config.LinkedSetSerializer();

        private final Set<String> input;

        public ListSerializerTest(List<String> input) {
            this.input = new LinkedHashSet<>(input);
        }

        @Parameterized.Parameters
        public static List<List<String>> parameters() {
            return List.of(
                    List.of(),
                    List.of("foo", "bar", "baz"),
                    List.of("\"", "'", ",", ""),
                    List.of("\"\"\"\",\"\"\",,\"", "\"foo\",\"bar\"", "baz,,\"quac\"")
            );
        }

        @Test
        public void test() {
            var serialized = serializer.serialize(input);
            var deserialized = serializer.deserialize(serialized);
            Assert.assertEquals(input, deserialized);
        }
    }
}
