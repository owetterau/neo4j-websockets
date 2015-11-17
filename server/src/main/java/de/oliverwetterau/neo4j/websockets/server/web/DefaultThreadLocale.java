package de.oliverwetterau.neo4j.websockets.server.web;

import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;

import java.util.Locale;

/**
 * Created by oliver on 10.11.15.
 */
public class DefaultThreadLocale implements ThreadLocale {
    private static ThreadLocal<Locale> localeThreadLocal = new ThreadLocal<>();

    @Override
    public void setLocale(Locale locale) {
        localeThreadLocal.set(locale);
    }

    @Override
    public Locale getLocale() {
        Locale locale = localeThreadLocal.get();

        return (locale == null) ? Locale.US : locale;
    }
}
