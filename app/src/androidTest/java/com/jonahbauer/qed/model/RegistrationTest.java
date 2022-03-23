package com.jonahbauer.qed.model;

import android.os.Parcel;
import androidx.core.util.Pair;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class RegistrationTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var registration = dummyRegistration();

        var parcel = Parcel.obtain();
        registration.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        var out = Registration.CREATOR.createFromParcel(parcel);
        assertEquals(registration, out);
    }

    private static Registration dummyRegistration() {
        var registration = new Registration(1);
        registration.setOrganizer(true);
        registration.setStatus(Registration.Status.OPEN);
        return registration;
    }
}
