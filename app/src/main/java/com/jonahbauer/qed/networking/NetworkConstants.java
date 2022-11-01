package com.jonahbauer.qed.networking;

import java.time.ZoneId;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NetworkConstants {
    public static final String CHAT_SERVER_LOGIN = "https://chat.qed-verein.de/rubychat/account";
    public static final String CHAT_SERVER_LOGOUT = "https://chat.qed-verein.de/rubychat/account";
    public static final String CHAT_SERVER_HISTORY = "https://chat.qed-verein.de/rubychat/history";
    public static final String CHAT_WEBSOCKET = "wss://chat.qed-verein.de/websocket";
    public static final String CHAT_VERSION = "20171030131648";

    public static final String DATABASE_SERVER_LOGIN = "https://qeddb.qed-verein.de/login";
    public static final String DATABASE_SERVER_LOGOUT = "https://qeddb.qed-verein.de/logout";
    public static final String DATABASE_SERVER_PERSONS = "https://qeddb.qed-verein.de/people_as_table";
    public static final String DATABASE_SERVER_PERSON = "https://qeddb.qed-verein.de/people/%d";
    public static final String DATABASE_SERVER_EVENTS = "https://qeddb.qed-verein.de/events_as_table";
    public static final String DATABASE_SERVER_EVENT = "https://qeddb.qed-verein.de/events/%d";
    public static final String DATABASE_SERVER_REGISTRATION = "https://qeddb.qed-verein.de/registrations/%d";

    public static final String GALLERY_SERVER_LOGIN = "https://qedgallery.qed-verein.de/account.php";
    public static final String GALLERY_SERVER_LOGOUT = "https://qedgallery.qed-verein.de/account.php?logout=logout";
    public static final String GALLERY_SERVER_LIST = "https://qedgallery.qed-verein.de/album_list.php";
    public static final String GALLERY_SERVER_ALBUM = "https://qedgallery.qed-verein.de/album_view.php?page=0&albumid=%d%s";
    public static final String GALLERY_SERVER_MAIN = "https://qedgallery.qed-verein.de/";
    public static final String GALLERY_SERVER_IMAGE_INFO = "https://qedgallery.qed-verein.de/image_view.php?imageid=%d";
    public static final String GALLERY_SERVER_IMAGE = "https://qedgallery.qed-verein.de/image.php?type=%s&imageid=%d";
    public static final String GALLERY_SERVER_IMAGE_VIEW = "https://qedgallery.qed-verein.de/image_view.php?imageid=%d";

    public static final String GIT_HUB = "https://github.com/jbb01/QED";
    public static final String GIT_HUB_ISSUE_TRACKER = "https://github.com/jbb01/QED/issues/new";
    public static final String GIT_HUB_API = "https://api.github.com/repos/jbb01/QED";

    public static final ZoneId SERVER_TIME_ZONE = ZoneId.of("Europe/Berlin");
}
