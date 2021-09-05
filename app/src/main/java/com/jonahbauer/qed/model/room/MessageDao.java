package com.jonahbauer.qed.model.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.Message;

import java.util.Collection;
import java.util.Date;
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
                                  @Nullable Date fromDate,
                                  @Nullable Date toDate,
                                  @Nullable Long fromId,
                                  @Nullable Long toId,
                                  long limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(Message... messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(Collection<Message> messages);

    @Query("DELETE FROM message")
    Completable clear();
}
