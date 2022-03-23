package com.jonahbauer.qed.model;

import android.os.Parcel;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MessageTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var message = dummyMessage();

        var parcel = Parcel.obtain();
        message.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        var out = Message.CREATOR.createFromParcel(parcel);
        assertEquals(message, out);
    }

    private static Message dummyMessage() {
        return new Message(
                0L,
                "   Max Mustermann   ",
                "Hello World!",
                Instant.now(),
                123,
                "MaxMustermann",
                "123456",
                "",
                0
        );
    }
}
