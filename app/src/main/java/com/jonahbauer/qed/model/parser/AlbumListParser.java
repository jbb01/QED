package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AlbumListParser extends HtmlParser<List<Album>> {
    private static final Pattern ALBUM_ID = Pattern.compile("id=(\\d+)");

    public static final AlbumListParser INSTANCE = new AlbumListParser();

    private AlbumListParser() {}

    @NonNull
    @Override
    protected List<Album> parse(@NonNull List<Album> list, Document document) {
        list.clear();

        document.select("main .menu li a")
                .stream()
                .map(a -> {
                    try {
                        Album album;

                        String href = a.attr("href");
                        Matcher matcher = ALBUM_ID.matcher(href);
                        if (matcher.find()) {
                            album = new Album(Long.parseLong(matcher.group(1)));
                        } else {
                            album = new Album(-1);
                        }

                        album.setName(a.text());
                        return album;
                    } catch (Exception e) {
                        Log.e(Application.LOG_TAG_ERROR, "Error parsing album list.", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(list::add);

        return list;
    }
}
