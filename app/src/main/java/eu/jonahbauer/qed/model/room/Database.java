package eu.jonahbauer.qed.model.room;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import eu.jonahbauer.qed.model.Album;
import eu.jonahbauer.qed.model.Image;
import eu.jonahbauer.qed.model.Message;

@androidx.room.Database(entities = {Album.class, Message.class, Image.class}, version = 10, exportSchema = false)
public abstract class Database extends RoomDatabase {
    private static final String DB_NAME = "qed_db";
    private static Database INSTANCE;

    public static synchronized Database getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Database.class, DB_NAME)
                           .fallbackToDestructiveMigration()
                           .build();
        }

        return INSTANCE;
    }

    public abstract AlbumDao albumDao();
    public abstract MessageDao messageDao();
}
