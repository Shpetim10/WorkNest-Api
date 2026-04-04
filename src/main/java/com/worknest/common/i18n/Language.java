package com.worknest.common.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum Language {
    EN("en", Locale.ENGLISH),
    SQ("sq", new Locale("sq", "AL")),
    IT("it", Locale.ITALIAN),
    DE("de", Locale.GERMAN);

    private final String code;
    private final Locale locale;

    public static Language fromCode(String code) {
        if (code == null) return EN;
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return EN; // Default fallback
    }

    public static Locale getLocaleOrDefault(String code) {
        return fromCode(code).getLocale();
    }
}
