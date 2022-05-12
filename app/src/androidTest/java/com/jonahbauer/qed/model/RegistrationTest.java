package com.jonahbauer.qed.model;

import android.os.Parcel;
import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;

public class RegistrationTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var registration = dummyRegistration();
        var copy = cloneWithParcelable(registration);
        assertEquals(registration, copy);
    }

    @Test
    public void testParcelableContainsAllNonTransientFields() throws IllegalAccessException {
        var registration = dummyRegistration();
        var copy = cloneWithParcelable(registration);

        var fields = Registration.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;
            assertEquals(field.getName(), field.get(registration), field.get(copy));
        }
    }

    private Registration cloneWithParcelable(Registration registration) {
        var parcel = Parcel.obtain();
        try {
            registration.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Registration.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    private static Registration dummyRegistration() {
        var registration = new Registration(1);
        registration.setOrganizer(true);
        registration.setStatus(Registration.Status.OPEN);
        return registration;
    }
}
