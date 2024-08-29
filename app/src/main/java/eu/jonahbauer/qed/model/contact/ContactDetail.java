package eu.jonahbauer.qed.model.contact;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ContactDetail implements Parcelable {

     @NonNull
     String label;

     @NonNull
     String value;

     public static final Creator<ContactDetail> CREATOR = new LambdaCreator<>(
             ContactDetail[]::new,
             source -> new ContactDetail(
                     Objects.requireNonNull(source.readString()),
                     Objects.requireNonNull(source.readString())
             )
     );

     @NonNull
     public ContactDetailType getType(){
          return ContactDetailType.byLabel(label);
     }

     @Override
     public int describeContents() {
          return 0;
     }

     @Override
     public void writeToParcel(@NonNull Parcel parcel, int i) {
          parcel.writeString(label);
          parcel.writeString(value);
     }
}