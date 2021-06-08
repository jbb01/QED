package com.jonahbauer.qed.model.parser;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.parser.HtmlParseException;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AlbumParser extends HtmlParser<Album> {
    private static final Pattern IMAGE_ID = Pattern.compile("id=(\\d+)");
    private static final Pattern BY_OWNER = Pattern.compile("byowner=(\\d+)");
    private static final Pattern BY_DAY = Pattern.compile("byday=(\\d+-\\d+-\\d+)");
    private static final Pattern BY_CATEGORY = Pattern.compile("bycategory=([^&]*)");

    private static final String TITLE_SUFFIX = " - Alle Bilder";

    private static final String FILTER_KEY_OWNER = "Nach Besitzer:";
    private static final String FILTER_KEY_DATE = "Nach Datum:";
    private static final String FILTER_KEY_CATEGORY = "Nach Kategorie:";

    private static final String FILTER_OWNER_PREFIX = "Bilder von ";

    private static final String INFO_KEY_OWNER = "Albumersteller:";
    private static final String INFO_KEY_CREATION_DATE = "Erstellt am:";

    public static final AlbumParser INSTANCE = new AlbumParser();

    private AlbumParser() {}

    @NonNull
    @Override
    protected Album parse(@NonNull Album album, Document document) {
        checkError(document);

        // Album name
        String albumName = document.selectFirst("main h2").text();
        if (albumName.endsWith(TITLE_SUFFIX)) {
            albumName = albumName.substring(0, albumName.length() - TITLE_SUFFIX.length());
        }
        album.setName(albumName);

        // Filter
        parseFilters(album, document.select(".image_filter_nav section b"));

        // Infotable
        parseInfoTable(album, document.select(".infotable th"));

        // Imagetable
        parseImageTable(album, document.select(".imagetable img"));

        return album;
    }

    private void checkError(Document document) {
        Element error = document.selectFirst(".error");
        if (error != null) {
            if (error.text().contains("AlbumNotFoundException")) {
                throw new HtmlParseException(Reason.NOT_FOUND);
            } else {
                throw new HtmlParseException();
            }
        }
    }

    private void parseImageTable(Album album, Elements elements) {
        album.getImages().clear();
        elements.stream().map(img -> {
            Image image;

            String src = img.attr("src");
            Matcher matcher = IMAGE_ID.matcher(src);
            if (matcher.find()) {
                image = new Image(Long.parseLong(matcher.group(1)));
            } else {
                image = new Image(-1);
            }

            image.setName(img.attr("alt"));
            image.setAlbum(album);
            image.setAlbumName(album.getName());

            return image;
        }).forEach(album.getImages()::add);
    }

    private void parseFilters(Album album, Elements elements) {
        album.getPersons().clear();
        album.getDates().clear();
        album.getCategories().clear();

        elements.forEach(b -> {
            String key = b.text();
            Element value = b.nextElementSibling();
            switch (key) {
                case FILTER_KEY_OWNER: {
                    value.select("a").forEach(a -> {
                        Person person;

                        // extract id
                        String href = a.attr("href");
                        Matcher matcher = BY_OWNER.matcher(href);
                        if (matcher.find()) {
                            person = new Person(Long.parseLong(matcher.group(1)));
                        } else {
                            person = new Person(-1);
                        }

                        // extract name
                        String name = a.text();
                        if (name.startsWith(FILTER_OWNER_PREFIX)) {
                            name = name.substring(FILTER_OWNER_PREFIX.length());
                        }
                        person.setFirstName(name);

                        album.getPersons().add(person);
                    });
                    break;
                }
                case FILTER_KEY_DATE: {
                    value.select("a").forEach(a -> {
                        // extract id
                        String href = a.attr("href");
                        Matcher matcher = BY_DAY.matcher(href);
                        if (matcher.find()) {
                            Date date = parseDate(matcher.group(1));
                            if (date != null) {
                                album.getDates().add(date);
                            }
                        }
                    });
                    break;
                }
                case FILTER_KEY_CATEGORY: {
                    value.select("a").forEach(a -> {
                        String href = a.attr("href");
                        Matcher matcher = BY_CATEGORY.matcher(href);
                        if (matcher.find()) {
                            String category = matcher.group(1);
                            if (category.isEmpty()) {
                                album.getCategories().add("Sonstige");
                            } else {
                                album.getCategories().add(category);
                            }
                        }
                    });
                    break;
                }
            }
        });
    }

    private void parseInfoTable(Album album, Elements elements) {
        elements.forEach(th -> {
            String key = th.text();
            switch (key) {
                case INFO_KEY_OWNER: {
                    album.setOwner(th.nextElementSibling().text());
                    break;
                }
                case INFO_KEY_CREATION_DATE: {
                    album.setCreationDate(th.nextElementSibling().text());
                    break;
                }
            }
        });
    }
}
