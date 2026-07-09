package com.fretboard.fretboard.global.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class HtmlSanitizer {

    private static final Safelist CONTENT_SAFELIST = Safelist.relaxed();

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        return Jsoup.clean(html, CONTENT_SAFELIST);
    }
}
