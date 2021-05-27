package com.jonahbauer.qed.model.parser;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ImageParser extends HtmlParser<Image> {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);

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

        document.select("main .infotable th").forEach(th -> {
            String key = th.text();
            String value = th.nextElementSibling().text();
            switch (key) {
                case INFO_KEY_ALBUM:
                    image.setAlbumName(value);
                    image.getData().put(String.valueOf(R.string.image_info_album), value);
                    break;
                case INFO_KEY_OWNER:
                    image.setOwner(value);
                    image.getData().put(String.valueOf(R.string.image_info_owner), value);
                    break;
                case INFO_KEY_FORMAT:
                    image.setFormat(value);
                    image.getData().put(String.valueOf(R.string.image_info_format), value);
                    break;
                case INFO_KEY_UPLOAD_DATE:
                    image.setUploadDate(parseDate(value));
                    image.getData().put(String.valueOf(R.string.image_info_upload_date), value);
                    break;
                case INFO_KEY_CREATION_DATE:
                    image.setCreationDate(parseDate(value));
                    image.getData().put(String.valueOf(R.string.image_info_creation_date), value);
                    break;
                case INFO_KEY_ORIENTATION:
                    image.getData().put(String.valueOf(R.string.image_info_orientation), value);
                    break;
                case INFO_KEY_MANUFACTURER:
                    image.getData().put(String.valueOf(R.string.image_info_camera_manufacturer), value);
                    break;
                case INFO_KEY_MODEL:
                    image.getData().put(String.valueOf(R.string.image_info_camera_model), value);
                    break;
                case INFO_KEY_FOCAL_LENGTH:
                    image.getData().put(String.valueOf(R.string.image_info_focal_length), value);
                    break;
                case INFO_KEY_FOCAL_RATIO:
                    image.getData().put(String.valueOf(R.string.image_info_focal_ratio), value);
                    break;
                case INFO_KEY_EXPOSURE_TIME:
                    image.getData().put(String.valueOf(R.string.image_info_exposure_time), value);
                    break;
                case INFO_KEY_ISO:
                    image.getData().put(String.valueOf(R.string.image_info_iso), value);
                    break;
                case INFO_KEY_POSITION:
                    image.getData().put(String.valueOf(R.string.image_info_position), value);
                    break;
                case INFO_KEY_FLASH:
                    image.getData().put(String.valueOf(R.string.image_info_flash), value);
                    break;
                case INFO_KEY_VISITS:
                    image.getData().put(String.valueOf(R.string.image_info_number_of_calls), value);
                    break;
            }
        });

        return image;
    }

    @Override
    protected Date parseDate(String date) {
        try {
            return SIMPLE_DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
