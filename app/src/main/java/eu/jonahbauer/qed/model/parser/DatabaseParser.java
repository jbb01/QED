package eu.jonahbauer.qed.model.parser;

import android.net.Uri;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.networking.parser.HtmlParser;
import org.jsoup.nodes.Element;

public abstract class DatabaseParser<T> extends HtmlParser<T> {

    protected static long parseIdFromHref(@Nullable Element element, long defaultValue) {
        var id = parseIdFromHref(element);
        return id != null ? id : defaultValue;
    }

    @Nullable
    protected static Long parseIdFromHref(@Nullable Element element) {
        if (element == null) return null;
        try {
            var href = Uri.parse(element.attr("href"));
            return Long.parseLong(href.getLastPathSegment());
        } catch (Exception e) {
            return null;
        }
    }
}
