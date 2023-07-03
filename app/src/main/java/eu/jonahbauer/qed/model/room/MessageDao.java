package eu.jonahbauer.qed.model.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.util.MessageUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
@TypeConverters({Converters.class})
public interface MessageDao {
    @Query("SELECT * FROM message")
    Single<List<Message>> getAll();

    @Query("SELECT * FROM message " +
            "WHERE (:channel  IS NULL OR channel LIKE :channel)" +
            "  AND (:message  IS NULL OR message LIKE :message)" +
            "  AND (:name     IS NULL OR name    LIKE :name)" +
            "  AND (:fromDate IS NULL OR date     >=  :fromDate)" +
            "  AND (:toDate   IS NULL OR date     <=  :toDate)" +
            "  AND (:fromId   IS NULL OR id       >=  :fromId)" +
            "  AND (:toId     IS NULL OR id       <=  :toId)" +
            "LIMIT :limit")
    Single<List<Message>> findAll(@Nullable String channel,
                                  @Nullable String message,
                                  @Nullable String name,
                                  @Nullable Instant fromDate,
                                  @Nullable Instant toDate,
                                  @Nullable Long fromId,
                                  @Nullable Long toId,
                                  long limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(Message... messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(Collection<Message> messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(Collection<Message> messages);

    @Query("DELETE FROM message")
    Completable clear();

    /**
     * Returns all messages that are "near" a local time overlap due to daylight savings time.
     * @see MessageUtils#dateFixer()
     */
    @Query("SELECT * FROM message WHERE (date / 86400) % 7 == 3 AND strftime('%m-%d', date, 'unixepoch') BETWEEN '10-25' AND '10-31' ORDER BY id")
    Single<List<Message>> possibleDateErrors();
}
