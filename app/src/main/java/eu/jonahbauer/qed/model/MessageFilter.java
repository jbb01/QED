package eu.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.List;

import eu.jonahbauer.qed.model.parcel.ParcelExtensions;
import eu.jonahbauer.qed.model.room.MessageDao;
import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import io.reactivex.rxjava3.core.Single;
import lombok.Data;
import lombok.experimental.ExtensionMethod;

@Data
@ExtensionMethod(ParcelExtensions.class)
public class MessageFilter implements Parcelable {
    public static final MessageFilter EMPTY = new MessageFilter(null, null, null, null, null, null, null);

    private final @Nullable String channel;
    private final @Nullable String message;
    private final @Nullable String name;
    private final @Nullable Instant fromDate;
    private final @Nullable Instant toDate;
    private final @Nullable Long fromId;
    private final @Nullable Long toId;

    public Single<List<Message>> search(MessageDao dao, long limit) {
        return dao.findAll(channel, message, name, fromDate, toDate, fromId, toId, limit);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(channel);
        dest.writeString(message);
        dest.writeString(name);
        dest.writeInstant(fromDate);
        dest.writeInstant(toDate);
        dest.writeValue(fromId);
        dest.writeValue(toId);
    }

    @SuppressLint("ParcelClassLoader")
    public static final Creator<MessageFilter> CREATOR = new LambdaCreator<>(MessageFilter[]::new, source -> {
        return new MessageFilter(
                source.readString(),
                source.readString(),
                source.readString(),
                source.readInstant(),
                source.readInstant(),
                (Long) source.readValue(null),
                (Long) source.readValue(null)
        );
    });
}
