package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageParser extends HtmlParser<Image> {
    private static final String LOG_TAG = ImageParser.class.getName();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm:ss")
            .withLocale(Locale.GERMANY)
            .withZone(NetworkConstants.SERVER_TIME_ZONE);
    private static final Pattern ALBUM_ID = Pattern.compile("albumid=(\\d+)");

    private static final String INFO_KEY_ALBUM = "Album:";
    private static final String INFO_KEY_OWNER = "Besitzer:";
    private static final String INFO_KEY_FORMAT = "Dateiformat:";
    private static final String INFO_KEY_UPLOAD_DATE = "Hochgeladen am:";
    private static final String INFO_KEY_CREATION_DATE = "Aufgenommen am:";
    private static final String INFO_KEY_ORIENTATION = "Orientierung:";
    private static final String INFO_KEY_MANUFACTURER = "Kamerahersteller:";
    private static final String INFO_KEY_MODEL = "Kameramodel:";
    private static final String INFO_KEY_FOCAL_LENGTH = "Brennweite:";
    private static final String INFO_KEY_FOCAL_RATIO = "Blendenzahl:";
    private static final String INFO_KEY_EXPOSURE_TIME = "Belichtungszeit:";
    private static final String INFO_KEY_ISO = "ISO-Wert:";
    private static final String INFO_KEY_POSITION = "Koordiante:"; // TODO typo in gallery
    private static final String INFO_KEY_FLASH = "Blitz benutzt:";
    private static final String INFO_KEY_VISITS = "Anzahl der Anrufe:";

    public static final ImageParser INSTANCE = new ImageParser();

    private ImageParser() {}

    @NonNull
    @Override
    protected Image parse(@NonNull Image image, Document document) {
        image.setName(document.select("main div div b").text());

        String href = document.select("main > div:nth-child(3) > a")
                              .attr("href");
        Matcher matcher = ALBUM_ID.matcher(href);
        if (matcher.find()) {
            image.setAlbumId(Long.parseLong(matcher.group(1)));
        }

        document.select("main .infotable th").forEach(th -> {
            try {
                String key = th.text();
                //noinspection ConstantConditions
                String value = th.nextElementSibling().text();
                switch (key) {
                    case INFO_KEY_ALBUM:
                        image.setAlbumName(value);
                        image.getData().put(Image.DATA_KEY_ALBUM, value);
                        break;
                    case INFO_KEY_OWNER:
                        image.setOwner(value);
                        image.getData().put(Image.DATA_KEY_OWNER, value);
                        break;
                    case INFO_KEY_FORMAT:
                        image.setFormat(value);
                        image.getData().put(Image.DATA_KEY_FORMAT, value);
                        break;
                    case INFO_KEY_UPLOAD_DATE:
                        image.setUploadTime(parseInstant(value));
                        image.getData().put(Image.DATA_KEY_UPLOAD_DATE, value);
                        break;
                    case INFO_KEY_CREATION_DATE:
                        image.setCreationTime(parseInstant(value));
                        image.getData().put(Image.DATA_KEY_CREATION_DATE, value);
                        break;
                    case INFO_KEY_ORIENTATION:
                        image.getData().put(Image.DATA_KEY_ORIENTATION, value);
                        break;
                    case INFO_KEY_MANUFACTURER:
                        image.getData().put(Image.DATA_KEY_MANUFACTURER, value);
                        break;
                    case INFO_KEY_MODEL:
                        image.getData().put(Image.DATA_KEY_MODEL, value);
                        break;
                    case INFO_KEY_FOCAL_LENGTH:
                        image.getData().put(Image.DATA_KEY_FOCAL_LENGTH, value);
                        break;
                    case INFO_KEY_FOCAL_RATIO:
                        image.getData().put(Image.DATA_KEY_FOCAL_RATIO, value);
                        break;
                    case INFO_KEY_EXPOSURE_TIME:
                        image.getData().put(Image.DATA_KEY_EXPOSURE_TIME, value);
                        break;
                    case INFO_KEY_ISO:
                        image.getData().put(Image.DATA_KEY_ISO, value);
                        break;
                    case INFO_KEY_POSITION:
                        image.getData().put(Image.DATA_KEY_POSITION, value);
                        break;
                    case INFO_KEY_FLASH:
                        image.getData().put(Image.DATA_KEY_FLASH, value);
                        break;
                    case INFO_KEY_VISITS:
                        image.getData().put(Image.DATA_KEY_VISITS, value);
                        break;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing image info " + image.getId() + ".", e);
            }
        });

        return image;
    }

    protected static Instant parseInstant(@NonNull String date) {
        try {
            return Instant.from(DATE_TIME_FORMATTER.parse(date));
        } catch (DateTimeParseException e) {
            Log.w(LOG_TAG, "Could not parse instant \"" + date + "\".", e);
            return null;
        }
    }
}
